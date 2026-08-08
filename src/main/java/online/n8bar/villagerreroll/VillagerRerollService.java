package online.n8bar.villagerreroll;

import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import online.n8bar.villagerreroll.api.VillagerPreRerollEvent;
import online.n8bar.villagerreroll.api.WanderingTraderPreRerollEvent;
import org.slf4j.Logger;

final class VillagerRerollService {
    private static final double MAX_DISTANCE_SQUARED=36.0;
    private static final Logger LOGGER=LogUtils.getLogger();
    private final TradePoolRegistry tradePools=new TradePoolRegistry();
    private final Set<UUID> reservations=new HashSet<>();
    private final CommitGuard commitGuard=new CommitGuard();
    private VillagerTrades.ItemListing[] wanderingOrdinary=new VillagerTrades.ItemListing[0];
    private VillagerTrades.ItemListing[] wanderingRare=new VillagerTrades.ItemListing[0];

    VillagerRerollService(){ MinecraftForge.EVENT_BUS.register(tradePools); }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void attack(AttackEntityEvent event){
        boolean genuineManual=RetrainingManual.isGenuine(event.getEntity().getMainHandItem());
        if (!shouldCancel(genuineManual)) return;
        // ForgeHooks.onPlayerAttackTarget posts this before Player#attack performs any attack effects.
        event.setCanceled(true);
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) return;
        Entity target=event.getTarget();
        boolean supported=target instanceof Villager||target instanceof WanderingTrader;
        if(!event.getEntity().isShiftKeyDown()&&supported){
            tell(player,"Sneak + left-click to retrain.");
            return;
        }
        if(!event.getEntity().isShiftKeyDown())return;
        if(target instanceof Villager villager)transact(player,villager);
        else if(target instanceof WanderingTrader trader)transact(player,trader);
        else if(target instanceof Sheep sheep)recolor(sheep);
    }

    static boolean shouldCancel(boolean genuineManual){
        return genuineManual;
    }

    static boolean shouldTransact(boolean sneaking, boolean genuineManual, boolean villagerTarget){
        return sneaking && genuineManual && villagerTarget;
    }

    // A 0..14 draw maps bijectively over every color except the current one; no retry bias.
    static DyeColor differentColor(DyeColor current,int draw){
        if(draw<0||draw>=15)throw new IllegalArgumentException("draw must be in [0,15)");
        return DyeColor.values()[(current.ordinal()+1+draw)%DyeColor.values().length];
    }

    private static void recolor(Sheep sheep){
        DyeColor next=differentColor(sheep.getColor(),sheep.getRandom().nextInt(15));
        sheep.setColor(next);
        ServerLevel world=(ServerLevel)sheep.level();
        world.sendParticles(ParticleTypes.POOF,sheep.getX(),sheep.getY()+.6,sheep.getZ(),3,.2,.2,.2,0);
        world.playSound(null,sheep,SoundEvents.WOOL_PLACE,SoundSource.NEUTRAL,.35f,1.15f);
    }

    private void transact(ServerPlayer player,Villager villager){
        UUID id=villager.getUUID(); long now=player.getServer().getTickCount();
        if (!commitGuard.mayCommit(id,now)||!reservations.add(id)){ tell(player,"That villager is already being retrained."); return; }
        try{
            String invalid=invalidReason(player,villager);
            if(invalid!=null){ tell(player,invalid); return; }
            RerollPlanner.Result result=RerollPlanner.prepare(villager,
                    tradePools.forProfession(villager.getVillagerData().getProfession()));
            String profession=ForgeRegistries.VILLAGER_PROFESSIONS.getKey(villager.getVillagerData().getProfession()).toString();
            if(!result.accepted()){
                LOGGER.info("Villager retraining refused: player={} villager={} profession={} level={} reason=ambiguous_provenance",
                        player.getUUID(),id,profession,villager.getVillagerData().getLevel());
                tell(player,result.refusal()); return;
            }
            if(result.rerolled().isEmpty()){
                LOGGER.info("Villager retraining no-op: player={} villager={} profession={} level={}",player.getUUID(),id,profession,villager.getVillagerData().getLevel());
                tell(player,"No trades can change safely; no Manual was spent."); return;
            }
            PreparedReroll plan=new PreparedReroll(id,villager.getVillagerData().getProfession(),
                    villager.getVillagerData().getLevel(),villager.getOffers().createTag().copy(),
                    result.offers(),result.counts().clone(),result.rerolled(),result.preserved());
            commit(player,villager,plan,now);
        }finally{ reservations.remove(id); }
    }

    private void commit(ServerPlayer player,Villager villager,PreparedReroll plan,long now){
        String invalid=validate(player,villager,plan);
        if(invalid!=null){ tell(player,invalid); return; }
        MerchantOffers eventProposal=OfferCopies.deepCopy(plan.proposal());
        net.minecraft.nbt.CompoundTag eventTag=eventProposal.createTag().copy();
        VillagerPreRerollEvent pre=new VillagerPreRerollEvent(player,villager,Items.WRITABLE_BOOK,1,
                List.copyOf(eventProposal),plan.rerolledTiers(),plan.preservedTiers());
        if(MinecraftForge.EVENT_BUS.post(pre)){ tell(player,"Retraining was blocked; no Manual was spent."); return; }
        invalid=validate(player,villager,plan);
        MerchantOffers returned=new MerchantOffers(); returned.addAll(pre.getProposedOffers());
        if(invalid!=null||!RerollValidation.hasExactOffers(pre.getProposedOffers(),plan.tierCounts())
                ||!RerollValidation.sameFullOffers(eventTag,returned.createTag())){
            tell(player,"Trades changed during retraining; no Manual was spent."); return;
        }
        MerchantOffers committed=OfferCopies.deepCopy(plan.proposal()), old=villager.getOffers();
        ItemStack hand=player.getMainHandItem(); hand.shrink(1); player.getInventory().setChanged();
        try{
            villager.setOffers(committed);
            ProvenanceLedger.write(villager,ForgeRegistries.VILLAGER_PROFESSIONS.getKey(plan.profession()).toString(),plan.level(),plan.tierCounts(),committed);
        }catch(RuntimeException failure){ hand.grow(1); villager.setOffers(old); player.getInventory().setChanged(); throw failure; }
        player.containerMenu.broadcastChanges(); commitGuard.recordSuccess(plan.villagerId(),now);
        LOGGER.info("Villager retraining committed: player={} ({}) villager={} profession={} level={} offers={} rerolledTiers={} preservedTiers={} payment=trade_retraining_manual:1",
                player.getGameProfile().getName(),player.getUUID(),plan.villagerId(),
                ForgeRegistries.VILLAGER_PROFESSIONS.getKey(plan.profession()),plan.level(),committed.size(),plan.rerolledTiers(),plan.preservedTiers());
        ServerLevel world=(ServerLevel)villager.level();
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER,villager.getX(),villager.getY()+1,villager.getZ(),6,.3,.4,.3,0);
        world.playSound(null,villager,SoundEvents.VILLAGER_YES,SoundSource.NEUTRAL,.55f,1.1f);
        tell(player,resultMessage(plan));
    }

    private void transact(ServerPlayer player,WanderingTrader trader){
        UUID id=trader.getUUID(); long now=player.getServer().getTickCount();
        if(!commitGuard.mayCommit(id,now)||!reservations.add(id)){
            tell(player,"That trader is already being retrained."); return;
        }
        try{
            String invalid=invalidReason(player,trader);
            if(invalid!=null){tell(player,invalid);return;}
            var proposal=WanderingTraderPlanner.prepare(trader,wanderingOrdinary,wanderingRare);
            if(proposal.isEmpty()){
                LOGGER.info("Wandering trader retraining no-op: player={} trader={} reason=incomplete_trade_pool",
                        player.getUUID(),id);
                tell(player,"Final wandering trade pools are unavailable; no Manual was spent."); return;
            }
            net.minecraft.nbt.CompoundTag oldTag=trader.getOffers().createTag().copy();
            if(!shouldConsumeWandering(oldTag,proposal.get())){
                tell(player,"Trades did not change; no Manual was spent."); return;
            }
            PreparedWanderingReroll plan=new PreparedWanderingReroll(id,oldTag,proposal.get());
            commit(player,trader,plan,now);
        }finally{reservations.remove(id);}
    }

    private void commit(ServerPlayer player,WanderingTrader trader,PreparedWanderingReroll plan,long now){
        String invalid=validate(player,trader,plan);
        if(invalid!=null){tell(player,invalid);return;}
        MerchantOffers eventProposal=OfferCopies.deepCopy(plan.proposal());
        net.minecraft.nbt.CompoundTag eventTag=eventProposal.createTag().copy();
        WanderingTraderPreRerollEvent pre=new WanderingTraderPreRerollEvent(player,trader,
                Items.WRITABLE_BOOK,1,List.copyOf(eventProposal));
        if(MinecraftForge.EVENT_BUS.post(pre)){
            tell(player,"Retraining was blocked; no Manual was spent."); return;
        }
        invalid=validate(player,trader,plan);
        MerchantOffers returned=new MerchantOffers(); returned.addAll(pre.getProposedOffers());
        if(invalid!=null||returned.size()!=plan.proposal().size()
                ||!RerollValidation.sameFullOffers(eventTag,returned.createTag())){
            tell(player,"Trades changed during retraining; no Manual was spent."); return;
        }
        MerchantOffers committed=OfferCopies.deepCopy(plan.proposal());
        MerchantOffers live=trader.getOffers(),old=new MerchantOffers(plan.oldOffersTag().copy());
        ItemStack hand=player.getMainHandItem(); hand.shrink(1); player.getInventory().setChanged();
        try{
            live.clear(); live.addAll(committed);
        }catch(RuntimeException failure){
            live.clear(); live.addAll(old); hand.grow(1); player.getInventory().setChanged(); throw failure;
        }
        player.containerMenu.broadcastChanges(); commitGuard.recordSuccess(plan.traderId(),now);
        LOGGER.info("Wandering trader retraining committed: player={} ({}) trader={} offers={} payment=trade_retraining_manual:1",
                player.getGameProfile().getName(),player.getUUID(),plan.traderId(),committed.size());
        ServerLevel world=(ServerLevel)trader.level();
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER,trader.getX(),trader.getY()+1,trader.getZ(),6,.3,.4,.3,0);
        world.playSound(null,trader,SoundEvents.WANDERING_TRADER_YES,SoundSource.NEUTRAL,.55f,1.1f);
        tell(player,"Retrained: "+committed.size()+" wandering trades changed.");
    }

    private static String validate(ServerPlayer player,WanderingTrader trader,PreparedWanderingReroll plan){
        String invalid=invalidReason(player,trader); if(invalid!=null)return invalid;
        if(!trader.getUUID().equals(plan.traderId()))return "That trader changed; try again.";
        if(!RerollValidation.sameFullOffers(plan.oldOffersTag(),trader.getOffers().createTag()))
            return "Those trades changed; try again.";
        if(!RetrainingManual.isGenuine(player.getMainHandItem()))
            return "Hold one genuine Trade Retraining Manual.";
        return null;
    }

    private static String invalidReason(ServerPlayer player,WanderingTrader trader){
        if(!trader.isAlive()||trader.isRemoved())return "That trader is no longer available.";
        if(trader.getTradingPlayer()!=null)return "That trader is already trading.";
        if(player.level()!=trader.level())return "That trader is in another dimension.";
        if(player.distanceToSqr(trader)>MAX_DISTANCE_SQUARED)return "Move closer to that trader.";
        if(!player.hasLineOfSight(trader))return "You need line of sight to that trader.";
        return null;
    }

    static boolean shouldConsumeWandering(net.minecraft.nbt.CompoundTag oldOffers,
            MerchantOffers proposal){
        return !RerollValidation.sameFullOffers(oldOffers,proposal.createTag());
    }

    static String resultMessage(PreparedReroll plan){
        String message="Retrained: "+plan.rerolledOffers()+" trades changed";
        if(plan.preservedOffers()>0) message+=", "+plan.preservedOffers()+" "+tierNames(plan.preservedTiers())
                +" trade"+(plan.preservedOffers()==1?"":"s")+" preserved";
        return message+".";
    }
    private static String tierNames(List<Integer> tiers){
        String[] names={"","Novice","Apprentice","Journeyman","Expert","Master"};
        return tiers.stream().map(t->names[t]).reduce((a,b)->a+"/"+b).orElse("");
    }
    private static String validate(ServerPlayer player,Villager villager,PreparedReroll plan){
        String invalid=invalidReason(player,villager); if(invalid!=null)return invalid;
        if(!villager.getUUID().equals(plan.villagerId())||villager.getVillagerData().getProfession()!=plan.profession()
                ||villager.getVillagerData().getLevel()!=plan.level())return "That villager changed; try again.";
        if(!RerollValidation.sameFullOffers(plan.oldOffersTag(),villager.getOffers().createTag()))return "Those trades changed; try again.";
        if(!RetrainingManual.isGenuine(player.getMainHandItem()))return "Hold one genuine Trade Retraining Manual.";
        return null;
    }
    private static String invalidReason(ServerPlayer player,Villager villager){
        if(!villager.isAlive()||villager.isRemoved())return "That villager is no longer available.";
        if(villager.isBaby())return "Only adult villagers can be retrained.";
        VillagerProfession p=villager.getVillagerData().getProfession();
        if(p==VillagerProfession.NONE||p==VillagerProfession.NITWIT)return "That villager needs an employed profession.";
        if(villager.getTradingPlayer()!=null)return "That villager is already trading.";
        if(player.level()!=villager.level())return "That villager is in another dimension.";
        if(player.distanceToSqr(villager)>MAX_DISTANCE_SQUARED)return "Move closer to that villager.";
        if(!player.hasLineOfSight(villager))return "You need line of sight to that villager.";
        return null;
    }
    private static void tell(ServerPlayer player,String text){player.displayClientMessage(Component.literal(text),true);}
    @SubscribeEvent public void starting(ServerStartingEvent e){
        var pools=VillagerTrades.WANDERING_TRADER_TRADES;
        wanderingOrdinary=pools.get(1)==null?new VillagerTrades.ItemListing[0]:pools.get(1).clone();
        wanderingRare=pools.get(2)==null?new VillagerTrades.ItemListing[0]:pools.get(2).clone();
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e){if(e.phase==TickEvent.Phase.END&&e.getServer().getTickCount()%20==0)commitGuard.expireBefore(e.getServer().getTickCount()-1);}
    @SubscribeEvent public void stopped(ServerStoppedEvent e){
        reservations.clear();commitGuard.clearAll();
        wanderingOrdinary=new VillagerTrades.ItemListing[0];
        wanderingRare=new VillagerTrades.ItemListing[0];
    }
}

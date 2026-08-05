package online.n8bar.villagerreroll;

import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import online.n8bar.villagerreroll.api.VillagerPreRerollEvent;
import org.slf4j.Logger;

final class VillagerRerollService {
    private static final double MAX_DISTANCE_SQUARED = 36.0;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ConfirmationTracker<ResourceKey<Level>, PreparedReroll> confirmations = new ConfirmationTracker<>();
    private final TradePoolRegistry tradePools = new TradePoolRegistry();
    private final Set<UUID> reservations = new HashSet<>();
    private final CommitGuard commitGuard = new CommitGuard();

    VillagerRerollService() { MinecraftForge.EVENT_BUS.register(tradePools); }

    @SubscribeEvent
    public void interact(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof Villager villager) || !player.isSecondaryUseActive()
                || !RetrainingManual.isGenuine(player.getMainHandItem())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        String invalid = invalidReason(player, villager);
        if (invalid != null) { confirmations.clear(player.getUUID()); tell(player, invalid); return; }

        long now = player.getServer().getTickCount();
        PreparedReroll prepared = confirmations.take(player.getUUID(), villager.getUUID(),
                player.level().dimension(), now).orElse(null);
        if (prepared == null) { prepare(player, villager, now); return; }
        commit(player, villager, prepared, now);
    }

    private void prepare(ServerPlayer player, Villager villager, long now) {
        RerollPlanner.Result result = RerollPlanner.prepare(villager,
                tradePools.forProfession(villager.getVillagerData().getProfession()));
        String profession = ForgeRegistries.VILLAGER_PROFESSIONS
                .getKey(villager.getVillagerData().getProfession()).toString();
        if (!result.accepted()) {
            LOGGER.info("Villager reroll refused during preparation: player={} villager={} profession={} level={} reason=ambiguous_provenance",
                    player.getUUID(), villager.getUUID(), profession, villager.getVillagerData().getLevel());
            tell(player, result.refusal());
            return;
        }
        if (result.rerolled().isEmpty()) {
            LOGGER.info("Villager reroll no-op during preparation: player={} villager={} profession={} level={}",
                    player.getUUID(), villager.getUUID(), profession, villager.getVillagerData().getLevel());
            tell(player, "None of this villager's tiers can be rerolled safely; no payment was taken.");
            return;
        }
        PreparedReroll plan = new PreparedReroll(villager.getUUID(), villager.level().dimension(),
                villager.getVillagerData().getProfession(), villager.getVillagerData().getLevel(),
                Items.WRITABLE_BOOK, 1, VillagerRerollConfig.confirmationTicks(),
                villager.getOffers().createTag().copy(), result.offers(),
                result.counts().clone(), result.rerolled(), result.preserved());
        confirmations.prepare(player.getUUID(), villager.getUUID(), player.level().dimension(), now,
                VillagerRerollConfig.confirmationTicks(), plan);
        int seconds = (VillagerRerollConfig.confirmationTicks() + 19) / 20;
        String preserved = result.preserved().isEmpty() ? ""
                : " Preserving " + plan.preservedOffers() + " old offer(s) from "
                + tierNames(result.preserved()) + ".";
        tell(player, "Prepared " + plan.rerolledOffers() + " fresh offers." + preserved
                + " Confirm within " + seconds + " seconds to spend 1 Trade Retraining Manual.");
        LOGGER.info("Villager reroll prepared: player={} villager={} profession={} level={} rerolledTiers={} preservedTiers={} offers={}",
                player.getUUID(), villager.getUUID(), profession, plan.level(), plan.rerolledTiers(),
                plan.preservedTiers(), plan.rerolledOffers());
    }

    private void commit(ServerPlayer player, Villager villager, PreparedReroll plan, long now) {
        UUID id = villager.getUUID();
        if (!commitGuard.mayCommit(id, now) || !reservations.add(id)) {
            tell(player, "That villager is already being rerolled."); return;
        }
        try {
            String invalid = validatePrepared(player, villager, plan);
            if (invalid != null) { tell(player, invalid); return; }
            MerchantOffers eventProposal = OfferCopies.deepCopy(plan.proposal());
            net.minecraft.nbt.CompoundTag eventProposalTag = eventProposal.createTag().copy();
            VillagerPreRerollEvent pre = new VillagerPreRerollEvent(player, villager, Items.WRITABLE_BOOK,
                    1, List.copyOf(eventProposal), plan.rerolledTiers(), plan.preservedTiers());
            if (MinecraftForge.EVENT_BUS.post(pre)) { tell(player, "The reroll was blocked; no manual was taken."); return; }
            invalid = validatePrepared(player, villager, plan);
            MerchantOffers returnedProposal = new MerchantOffers();
            returnedProposal.addAll(pre.getProposedOffers());
            if (invalid != null || !RerollValidation.hasExactOffers(pre.getProposedOffers(), plan.tierCounts())
                    || !RerollValidation.sameFullOffers(eventProposalTag, returnedProposal.createTag())) {
                tell(player, "The villager, manual, or prepared trades changed; no payment was taken."); return;
            }
            MerchantOffers committed = OfferCopies.deepCopy(plan.proposal());
            MerchantOffers old = villager.getOffers();
            ItemStack hand = player.getMainHandItem();
            hand.shrink(1);
            player.getInventory().setChanged();
            try {
                villager.setOffers(committed);
                ProvenanceLedger.write(villager, ForgeRegistries.VILLAGER_PROFESSIONS.getKey(plan.profession()).toString(),
                        plan.level(), plan.tierCounts(), committed);
            } catch (RuntimeException failure) {
                hand.grow(1); villager.setOffers(old); player.getInventory().setChanged(); throw failure;
            }
            player.containerMenu.broadcastChanges();
            commitGuard.recordSuccess(id, now);
            confirmations.clearVillager(id);
            LOGGER.info("Villager reroll committed: player={} ({}) villager={} profession={} level={} offers={} rerolledTiers={} preservedTiers={} payment=trade_retraining_manual:1",
                    player.getGameProfile().getName(), player.getUUID(), id,
                    ForgeRegistries.VILLAGER_PROFESSIONS.getKey(plan.profession()), plan.level(), committed.size(),
                    plan.rerolledTiers(), plan.preservedTiers());
            ServerLevel world = (ServerLevel) villager.level();
            world.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY()+1,
                    villager.getZ(), 6, .3, .4, .3, 0);
            world.playSound(null, villager, SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, .55f, 1.1f);
            tell(player, "Rerolled " + plan.rerolledOffers() + " offers; preserved "
                    + (committed.size() - plan.rerolledOffers()) + ".");
        } finally { reservations.remove(id); }
    }

    private static String validatePrepared(ServerPlayer player, Villager villager, PreparedReroll plan) {
        String invalid = invalidReason(player, villager);
        if (invalid != null) return invalid;
        if (!villager.getUUID().equals(plan.villagerId()) || !villager.level().dimension().equals(plan.dimension())
                || villager.getVillagerData().getProfession() != plan.profession()
                || villager.getVillagerData().getLevel() != plan.level()) return "That villager changed; prepare again.";
        if (!RerollValidation.sameFullOffers(plan.oldOffersTag(), villager.getOffers().createTag()))
            return "Those trades changed; prepare again.";
        if (!RetrainingManual.isGenuine(player.getMainHandItem()) || player.getMainHandItem().getCount() < 1)
            return "Hold one genuine Trade Retraining Manual in your main hand.";
        if (VillagerRerollConfig.confirmationTicks() != plan.confirmationTicks()) return "Reroll settings changed; prepare again.";
        return null;
    }

    private static String tierNames(List<Integer> tiers) {
        String[] names = {"", "Novice", "Apprentice", "Journeyman", "Expert", "Master"};
        return tiers.stream().map(tier -> names[tier]).reduce((a,b) -> a + ", " + b).orElse("no tiers");
    }
    private static String invalidReason(ServerPlayer player, Villager villager) {
        if (!villager.isAlive() || villager.isRemoved()) return "That villager is no longer available.";
        if (villager.isBaby()) return "Only adult villagers can be rerolled.";
        VillagerProfession profession = villager.getVillagerData().getProfession();
        if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) return "That villager needs an employed profession.";
        if (villager.getTradingPlayer() != null) return "That villager is already trading.";
        if (player.level() != villager.level()) return "That villager is in another dimension.";
        if (player.distanceToSqr(villager) > MAX_DISTANCE_SQUARED) return "Move closer to that villager.";
        if (!player.hasLineOfSight(villager)) return "You need line of sight to that villager.";
        return null;
    }
    private static void tell(ServerPlayer player, String text) { player.displayClientMessage(Component.literal(text), true); }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e) { confirmations.clear(e.getEntity().getUUID()); }
    @SubscribeEvent public void changedDimension(PlayerEvent.PlayerChangedDimensionEvent e) { confirmations.clear(e.getEntity().getUUID()); }
    @SubscribeEvent public void died(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) confirmations.clear(p.getUUID()); }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.getServer().getTickCount()%20 == 0) {
            long now=e.getServer().getTickCount(); confirmations.expire(now); commitGuard.expireBefore(now-1);
        }
    }
    @SubscribeEvent public void stopped(ServerStoppedEvent e) {
        confirmations.clearAll(); reservations.clear(); commitGuard.clearAll();
    }
}

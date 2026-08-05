package online.n8bar.villagerreroll;

import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    private final ConfirmationTracker<ResourceKey<Level>> confirmations = new ConfirmationTracker<>();
    private final TradePoolRegistry tradePools = new TradePoolRegistry();
    private final Set<UUID> reservations = new HashSet<>();
    private final CommitGuard commitGuard = new CommitGuard();
    private boolean loggedInvalidPayment;

    VillagerRerollService() {
        MinecraftForge.EVENT_BUS.register(tradePools);
    }

    @SubscribeEvent
    public void interact(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || event.getLevel().isClientSide
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof Villager villager)
                || !player.isSecondaryUseActive()) {
            return;
        }

        Item payment = VillagerRerollConfig.paymentItem();
        int count = VillagerRerollConfig.paymentCount();
        if (payment == null || count == 0) {
            if (!loggedInvalidPayment) {
                LOGGER.error("Villager reroll payment configuration is invalid or exceeds the item's maximum stack size; interactions are disabled");
                loggedInvalidPayment = true;
            }
            return;
        }
        if (!player.getMainHandItem().is(payment)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        String invalid = invalidReason(player, villager);
        if (invalid != null) {
            confirmations.clear(player.getUUID());
            tell(player, invalid);
            return;
        }
        if (!hasPayment(player, payment, count)) {
            confirmations.clear(player.getUUID());
            tell(player, "Hold " + count + " " + payment.getDescription().getString() + " in your main hand.");
            return;
        }

        long now = player.getServer().getTickCount();
        ConfirmationTracker.Result result = confirmations.armOrConfirm(
                player.getUUID(), villager.getUUID(), player.level().dimension(), now,
                VillagerRerollConfig.confirmationTicks());
        if (result == ConfirmationTracker.Result.ARMED) {
            int seconds = (VillagerRerollConfig.confirmationTicks() + 19) / 20;
            tell(player, "Sneak-right-click this same villager again within " + seconds + " seconds to spend "
                    + count + " " + payment.getDescription().getString() + ".");
            return;
        }

        reroll(player, villager, payment, count);
    }

    private void reroll(ServerPlayer player, Villager villager, Item payment, int count) {
        UUID villagerId = villager.getUUID();
        long now = player.getServer().getTickCount();
        if (!commitGuard.mayCommit(villagerId, now)) {
            tell(player, "That villager was already rerolled this tick.");
            return;
        }
        if (!reservations.add(villagerId)) {
            tell(player, "That villager is already being rerolled.");
            return;
        }
        try {
            String invalid = invalidReason(player, villager);
            if (invalid != null || !hasPayment(player, payment, count)) {
                tell(player, invalid != null ? invalid : "The payment is no longer in your main hand.");
                return;
            }

            int level = villager.getVillagerData().getLevel();
            VillagerProfession profession = villager.getVillagerData().getProfession();
            ResourceKey<Level> dimension = villager.level().dimension();
            Optional<MerchantOffers> built = OfferBuilder.build(villager, level,
                    tradePools.forProfession(profession),
                    villager.getRandom());
            if (built.isEmpty()) {
                tell(player, "That profession does not have two valid offers for every unlocked tier.");
                return;
            }

            MerchantOffers proposed = built.get();
            List<net.minecraft.world.item.trading.MerchantOffer> proposal = List.copyOf(proposed);
            VillagerPreRerollEvent pre = new VillagerPreRerollEvent(player, villager, payment, count, proposal);
            if (MinecraftForge.EVENT_BUS.post(pre)) {
                tell(player, "The reroll was blocked; no payment was taken.");
                return;
            }

            String postEventInvalid = invalidReason(player, villager);
            boolean identityChanged = !villager.getUUID().equals(villagerId)
                    || !villager.level().dimension().equals(dimension)
                    || villager.getVillagerData().getProfession() != profession
                    || villager.getVillagerData().getLevel() != level;
            if (postEventInvalid != null || identityChanged || !hasPayment(player, payment, count)
                    || !RerollValidation.hasExactOffers(pre.getProposedOffers(), level)) {
                tell(player, "The villager, payment, or proposed trades changed; no payment was taken.");
                return;
            }

            MerchantOffers committed = new MerchantOffers();
            committed.addAll(pre.getProposedOffers());
            ItemStack hand = player.getMainHandItem();
            MerchantOffers oldOffers = villager.getOffers();
            hand.shrink(count);
            if (hand.getCount() < 0) {
                hand.grow(count);
                tell(player, "The payment could not be taken safely.");
                return;
            }
            player.getInventory().setChanged();
            try {
                villager.setOffers(committed);
            } catch (RuntimeException failure) {
                hand.grow(count);
                player.getInventory().setChanged();
                villager.setOffers(oldOffers);
                throw failure;
            }
            player.containerMenu.broadcastChanges();
            commitGuard.recordSuccess(villagerId, now);
            confirmations.clearVillager(villagerId);
            LOGGER.info("Villager reroll committed: player={} ({}) villager={} profession={} level={} offers={}",
                    player.getGameProfile().getName(), player.getUUID(), villagerId,
                    ForgeRegistries.VILLAGER_PROFESSIONS.getKey(profession), level, committed.size());

            ServerLevel levelWorld = (ServerLevel) villager.level();
            levelWorld.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(),
                    villager.getY() + 1.0, villager.getZ(), 6, 0.3, 0.4, 0.3, 0.0);
            levelWorld.playSound(null, villager, SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.55f, 1.1f);
            tell(player, "Trades rerolled: " + committed.size() + " fresh offers.");
        } finally {
            reservations.remove(villagerId);
        }
    }

    private static String invalidReason(ServerPlayer player, Villager villager) {
        if (!villager.isAlive() || villager.isRemoved()) return "That villager is no longer available.";
        if (villager.isBaby()) return "Only adult villagers can be rerolled.";
        VillagerProfession profession = villager.getVillagerData().getProfession();
        if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
            return "That villager needs an employed profession.";
        }
        if (villager.getTradingPlayer() != null) return "That villager is already trading.";
        if (player.level() != villager.level()) return "That villager is in another dimension.";
        if (player.distanceToSqr(villager) > MAX_DISTANCE_SQUARED) return "Move closer to that villager.";
        if (!player.hasLineOfSight(villager)) return "You need line of sight to that villager.";
        return null;
    }

    private static boolean hasPayment(ServerPlayer player, Item payment, int count) {
        ItemStack hand = player.getMainHandItem();
        return hand.is(payment) && hand.getCount() >= count;
    }

    private static void tell(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        confirmations.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        confirmations.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            confirmations.clear(player.getUUID());
        }
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer().getTickCount() % 20 == 0) {
            confirmations.expire(event.getServer().getTickCount());
        }
    }

    @SubscribeEvent
    public void serverStopped(ServerStoppedEvent event) {
        confirmations.clearAll();
        reservations.clear();
        commitGuard.clearAll();
        loggedInvalidPayment = false;
    }
}

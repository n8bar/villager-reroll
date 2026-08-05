package online.n8bar.villagerreroll;

import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

record PreparedReroll(UUID villagerId, ResourceKey<Level> dimension, VillagerProfession profession,
        int level, Item payment, int paymentCount, int confirmationTicks, CompoundTag oldOffersTag, MerchantOffers proposal,
        int[] tierCounts, List<Integer> rerolledTiers, List<Integer> preservedTiers) {
    int rerolledOffers() { return rerolledTiers.size() * 2; }
    int preservedOffers() {
        return preservedTiers.stream().mapToInt(tier -> tierCounts[tier - 1]).sum();
    }
    boolean changesAnything() { return !rerolledTiers.isEmpty(); }
}

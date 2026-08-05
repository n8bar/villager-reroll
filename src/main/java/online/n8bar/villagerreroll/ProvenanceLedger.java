package online.n8bar.villagerreroll;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

final class ProvenanceLedger {
    private static final String KEY = VillagerReroll.MOD_ID + ":offer_ledger";
    private static final int VERSION = 1;
    private ProvenanceLedger() { }

    static Optional<int[]> read(Villager villager, String profession, int level, MerchantOffers offers) {
        CompoundTag root = villager.getPersistentData().getCompound(KEY);
        if (root.getInt("version") != VERSION || !root.getString("profession").equals(profession)
                || root.getInt("level") != level) return Optional.empty();
        int[] counts = root.getIntArray("tierCounts");
        ListTag signatures = root.getList("structuralOffers", 10);
        if (!TierLayout.valid(counts, level, offers.size()) || signatures.size() != offers.size()) return Optional.empty();
        for (int index = 0; index < offers.size(); index++) {
            if (!signatures.getCompound(index).equals(structuralTag(offers.get(index)))) return Optional.empty();
        }
        return Optional.of(counts);
    }

    static void write(Villager villager, String profession, int level, int[] counts, MerchantOffers offers) {
        CompoundTag root = new CompoundTag();
        root.putInt("version", VERSION); root.putString("profession", profession); root.putInt("level", level);
        root.putIntArray("tierCounts", counts);
        ListTag signatures = new ListTag();
        offers.forEach(offer -> signatures.add(structuralTag(offer)));
        root.put("structuralOffers", signatures);
        villager.getPersistentData().put(KEY, root);
    }

    static CompoundTag structuralTag(MerchantOffer offer) {
        CompoundTag tag = offer.createTag().copy();
        tag.remove("uses"); tag.remove("demand"); tag.remove("specialPrice");
        return tag;
    }
}

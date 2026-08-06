package online.n8bar.villagerreroll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

final class RerollPlanner {
    private RerollPlanner() { }

    static Result prepare(Villager villager, Map<Integer, List<VillagerTrades.ItemListing>> pools) {
        int level = villager.getVillagerData().getLevel();
        String profession = BuiltInRegistries.VILLAGER_PROFESSION
                .getKey(villager.getVillagerData().getProfession()).toString();
        MerchantOffers old = villager.getOffers();
        Map<Integer, Optional<List<MerchantOffer>>> generated = new HashMap<>();
        boolean needsPreservation = false;
        for (int tier = 1; tier <= level; tier++) {
            Optional<List<MerchantOffer>> built = OfferBuilder.buildTier(villager, tier, pools.get(tier), villager.getRandom());
            generated.put(tier, built);
            needsPreservation |= built.isEmpty();
        }

        int[] oldCounts = null;
        if (needsPreservation) {
            oldCounts = ProvenanceLedger.read(villager, profession, level, old).orElse(null);
            if (oldCounts == null) {
                Map<Integer, Integer> sizes = new HashMap<>();
                pools.forEach((tier, entries) -> sizes.put(tier, entries.size()));
                oldCounts = TierLayout.inferLegacy(profession, level, sizes, old.size()).orElse(null);
            }
            if (oldCounts == null) return Result.refused("Existing trade tiers cannot be proven safely; no payment was taken.");
        }

        List<Integer> rerolled = new ArrayList<>();
        List<Integer> preserved = new ArrayList<>();
        int[] newCounts = new int[level];
        for (int tier = 1; tier <= level; tier++) {
            Optional<List<MerchantOffer>> built = generated.get(tier);
            if (built.isPresent()) {
                newCounts[tier - 1] = 2;
                rerolled.add(tier);
            } else {
                int count = oldCounts[tier - 1];
                newCounts[tier - 1] = count;
                preserved.add(tier);
            }
        }
        MerchantOffers proposal = OfferCopies.deepCopy(splice(old, oldCounts, generated, level));
        return Result.prepared(proposal, newCounts, rerolled, preserved);
    }

    static <T> List<T> splice(List<T> old, int[] oldCounts,
            Map<Integer, Optional<List<T>>> generated, int level) {
        List<T> result = new ArrayList<>();
        int oldOffset = 0;
        for (int tier = 1; tier <= level; tier++) {
            Optional<List<T>> fresh = generated.get(tier);
            if (fresh.isPresent()) result.addAll(fresh.get());
            else {
                int count = oldCounts[tier - 1];
                result.addAll(old.subList(oldOffset, oldOffset + count));
            }
            if (oldCounts != null) oldOffset += oldCounts[tier - 1];
        }
        return result;
    }

    record Result(MerchantOffers offers, int[] counts, List<Integer> rerolled,
            List<Integer> preserved, String refusal) {
        static Result prepared(MerchantOffers offers, int[] counts, List<Integer> rerolled, List<Integer> preserved) {
            return new Result(offers, counts, List.copyOf(rerolled), List.copyOf(preserved), null);
        }
        static Result refused(String reason) { return new Result(null, null, List.of(), List.of(), reason); }
        boolean accepted() { return refusal == null; }
    }
}

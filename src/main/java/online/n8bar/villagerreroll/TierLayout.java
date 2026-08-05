package online.n8bar.villagerreroll;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

final class TierLayout {
    private TierLayout() { }

    static Optional<int[]> inferLegacy(String professionId, int level,
            Map<Integer, Integer> poolSizes, int offerCount) {
        if (!professionId.startsWith("minecraft:") || level < 1 || level > 5) return Optional.empty();
        int[] counts = new int[level];
        int total = 0;
        for (int tier = 1; tier <= level; tier++) {
            int count = Math.min(2, poolSizes.getOrDefault(tier, 0));
            if (count == 0) return Optional.empty();
            counts[tier - 1] = count;
            total += count;
        }
        return total == offerCount ? Optional.of(counts) : Optional.empty();
    }

    static boolean valid(int[] counts, int level, int offers) {
        return counts.length == level && Arrays.stream(counts).allMatch(value -> value > 0)
                && Arrays.stream(counts).sum() == offers;
    }
}

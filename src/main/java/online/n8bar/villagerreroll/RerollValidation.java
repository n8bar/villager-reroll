package online.n8bar.villagerreroll;

import java.util.Collection;
import net.minecraft.nbt.CompoundTag;

final class RerollValidation {
    private RerollValidation() { }

    static boolean hasExactOffers(Collection<?> offers, int villagerLevel) {
        return villagerLevel >= 1
                && offers.size() == villagerLevel * 2
                && offers.stream().noneMatch(java.util.Objects::isNull);
    }

    static boolean hasExactOffers(Collection<?> offers, int[] tierCounts) {
        return TierLayout.valid(tierCounts, tierCounts.length, offers.size())
                && offers.stream().noneMatch(java.util.Objects::isNull);
    }

    static boolean sameFullOffers(CompoundTag expected, CompoundTag current) {
        return expected.equals(current);
    }
}

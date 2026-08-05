package online.n8bar.villagerreroll;

import java.util.Collection;

final class RerollValidation {
    private RerollValidation() { }

    static boolean hasExactOffers(Collection<?> offers, int villagerLevel) {
        return villagerLevel >= 1
                && offers.size() == villagerLevel * 2
                && offers.stream().noneMatch(java.util.Objects::isNull);
    }
}

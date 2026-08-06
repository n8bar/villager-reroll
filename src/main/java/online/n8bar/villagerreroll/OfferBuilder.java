package online.n8bar.villagerreroll;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.trading.MerchantOffer;
import org.slf4j.Logger;

final class OfferBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();
    private OfferBuilder() { }

    static Optional<List<MerchantOffer>> buildTier(Villager villager, int tier,
            List<VillagerTrades.ItemListing> candidates, RandomSource random) {
        if (candidates == null || candidates.size() < 2) return Optional.empty();
        List<VillagerTrades.ItemListing> shuffled = new ArrayList<>(candidates);
        shuffle(shuffled, random);
        List<MerchantOffer> result = new ArrayList<>(2);
        for (VillagerTrades.ItemListing listing : shuffled) {
            try {
                MerchantOffer offer = listing.getOffer(villager, random);
                if (offer != null) result.add(offer);
            } catch (RuntimeException failure) {
                LOGGER.warn("Trade listing failed for villager {} tier {}; preserving that tier",
                        villager.getUUID(), tier, failure);
                return Optional.empty();
            }
            if (result.size() == 2) return Optional.of(List.copyOf(result));
        }
        return Optional.empty();
    }

    static <T> void shuffle(List<T> values, IntRandom random) {
        for (int index = values.size() - 1; index > 0; index--) {
            int swap = random.nextInt(index + 1);
            T value = values.get(index);
            values.set(index, values.get(swap));
            values.set(swap, value);
        }
    }
    private static <T> void shuffle(List<T> values, RandomSource random) { shuffle(values, random::nextInt); }
    @FunctionalInterface interface IntRandom { int nextInt(int bound); }
}

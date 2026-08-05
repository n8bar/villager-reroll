package online.n8bar.villagerreroll;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.mojang.logging.LogUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.slf4j.Logger;

final class OfferBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();
    private OfferBuilder() { }

    static Optional<MerchantOffers> build(
            Villager villager,
            int unlockedLevel,
            Map<Integer, List<VillagerTrades.ItemListing>> tiers,
            RandomSource random) {
        MerchantOffers result = new MerchantOffers();
        for (int tier = 1; tier <= unlockedLevel; tier++) {
            List<VillagerTrades.ItemListing> candidates = tiers.get(tier);
            if (candidates == null || candidates.size() < 2) {
                return Optional.empty();
            }

            List<VillagerTrades.ItemListing> shuffled = new ArrayList<>(candidates);
            shuffle(shuffled, random);
            int added = 0;
            for (VillagerTrades.ItemListing listing : shuffled) {
                MerchantOffer offer;
                try {
                    offer = listing.getOffer(villager, random);
                } catch (RuntimeException badListing) {
                    LOGGER.warn("Trade listing failed while building reroll offers for villager {} at tier {}; aborting reroll",
                            villager.getUUID(), tier, badListing);
                    return Optional.empty();
                }
                if (offer != null) {
                    result.add(offer);
                    if (++added == 2) {
                        break;
                    }
                }
            }
            if (added != 2) {
                return Optional.empty();
            }
        }
        return Optional.of(result);
    }

    static <T> void shuffle(List<T> values, IntRandom random) {
        for (int index = values.size() - 1; index > 0; index--) {
            int swap = random.nextInt(index + 1);
            T value = values.get(index);
            values.set(index, values.get(swap));
            values.set(swap, value);
        }
    }

    private static <T> void shuffle(List<T> values, RandomSource random) {
        shuffle(values, random::nextInt);
    }

    @FunctionalInterface
    interface IntRandom {
        int nextInt(int bound);
    }
}

package online.n8bar.villagerreroll;

import java.util.Collection;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

final class OfferCopies {
    private OfferCopies() { }

    static MerchantOffer deepCopy(MerchantOffer offer) {
        return new MerchantOffer(offer.createTag().copy());
    }

    static MerchantOffers deepCopy(Collection<MerchantOffer> offers) {
        MerchantOffers copy = new MerchantOffers();
        offers.forEach(offer -> copy.add(deepCopy(offer)));
        return copy;
    }
}

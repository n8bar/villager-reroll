package online.n8bar.villagerreroll;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

class OfferCopiesTest {
    @BeforeAll static void bootstrapMinecraftRegistries() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }
    @Test void deepCopyPreservesSerializedStateWithoutSharingObject() {
        MerchantOffer live=new MerchantOffer(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                3, 7, 12, 0.2f, 4);
        live.increaseUses(); live.setSpecialPriceDiff(-2); live.updateDemand();
        MerchantOffer copy=OfferCopies.deepCopy(live);
        assertNotSame(live,copy);
        assertEquals(live.createTag(),copy.createTag());
        copy.increaseUses();
        assertNotEquals(live.createTag(),copy.createTag());
    }

    @Test void eventCopyMutationCannotReachCachedPlan() {
        MerchantOffer cached=new MerchantOffer(ItemStack.EMPTY, ItemStack.EMPTY, 0, 4, 0.05f);
        MerchantOffer event=OfferCopies.deepCopy(cached);
        event.setSpecialPriceDiff(9);
        assertEquals(0,cached.getSpecialPriceDiff());
        assertNotEquals(cached.createTag(),event.createTag());
    }
}

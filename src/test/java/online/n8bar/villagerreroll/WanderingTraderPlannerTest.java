package online.n8bar.villagerreroll;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import online.n8bar.villagerreroll.api.WanderingTraderPreRerollEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WanderingTraderPlannerTest {
    @BeforeAll static void bootstrapMinecraft(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}

    private static MerchantOffer offer(int xp){
        return new MerchantOffer(ItemStack.EMPTY,ItemStack.EMPTY,4,xp,0.05f);
    }

    @Test void smallOrdinaryPoolUsesOriginalOrderAndNoSelectionRng(){
        VillagerTrades.ItemListing[] pool=new VillagerTrades.ItemListing[3];
        for(int i=0;i<pool.length;i++){int xp=i;pool[i]=(trader,random)->offer(xp);}
        int[] calls={0};
        var selected=WanderingTraderPlanner.selectOrdinary(pool,5,bound->{calls[0]++;return 0;});
        assertEquals(List.of(pool),selected);
        assertEquals(0,calls[0]);
    }

    @Test void largeOrdinaryPoolRepeatsDrawsUntilFiveDistinctIndices(){
        VillagerTrades.ItemListing[] pool=new VillagerTrades.ItemListing[7];
        for(int i=0;i<pool.length;i++){int xp=i;pool[i]=(trader,random)->offer(xp);}
        int[] draws={4,4,2,6,1,3}; int[] cursor={0};
        java.util.ArrayList<Integer> bounds=new java.util.ArrayList<>();
        var selected=WanderingTraderPlanner.selectOrdinary(pool,5,bound->{
            bounds.add(bound); return draws[cursor[0]++];
        });
        assertEquals(List.of(pool[1],pool[2],pool[3],pool[4],pool[6]),selected);
        assertEquals(List.of(7,7,7,7,7,7),bounds);
        assertTrue(WanderingTraderPlanner.selectOrdinary(null,5,bound->0).isEmpty());
        assertTrue(WanderingTraderPlanner.selectOrdinary(new VillagerTrades.ItemListing[0],5,bound->0).isEmpty());
    }

    @Test void rareSelectionUsesExactlyOneFullPoolDraw(){
        VillagerTrades.ItemListing[] pool=new VillagerTrades.ItemListing[4];
        for(int i=0;i<pool.length;i++)pool[i]=(trader,random)->null;
        java.util.ArrayList<Integer> bounds=new java.util.ArrayList<>();
        assertSame(pool[2],WanderingTraderPlanner.selectRare(pool,bound->{bounds.add(bound);return 2;}));
        assertEquals(List.of(4),bounds);
    }

    @Test void selectedListingsAreInvokedOnceWithoutNullOrFailureRetries(){
        int[] calls={0,0,0};
        MerchantOffer original=offer(1);
        VillagerTrades.ItemListing good=(trader,random)->{calls[0]++;return original;};
        VillagerTrades.ItemListing absent=(trader,random)->{calls[1]++;return null;};
        VillagerTrades.ItemListing broken=(trader,random)->{calls[2]++;throw new IllegalStateException();};
        MerchantOffers built=new MerchantOffers();
        assertFalse(WanderingTraderPlanner.appendSelected(built,null,RandomSource.create(1),
                List.of(good,absent,broken),"generic"));
        assertEquals(1,built.size());
        assertArrayEquals(new int[]{1,1,1},calls);
        assertNotSame(original,built.get(0));
    }

    @Test void healthyVanillaShapeBuildsFiveOrdinaryAndOneRare(){
        VillagerTrades.ItemListing listing=(trader,random)->offer(1);
        MerchantOffers built=new MerchantOffers(); RandomSource random=RandomSource.create(1);
        assertTrue(WanderingTraderPlanner.appendSelected(built,null,random,
                List.of(listing,listing,listing,listing,listing),"generic"));
        assertTrue(WanderingTraderPlanner.appendSelected(built,null,random,List.of(listing),"rare"));
        assertEquals(6,built.size());
    }

    @Test void nullListingAbortsSafely(){
        MerchantOffers built=new MerchantOffers();
        assertFalse(WanderingTraderPlanner.appendSelected(built,null,RandomSource.create(1),
                java.util.Collections.singletonList(null),"rare"));
        assertTrue(built.isEmpty());
    }

    @Test void eventProposalIsDeeplyIsolatedAndMutationDetectable(){
        MerchantOffers source=new MerchantOffers();source.add(offer(1));
        CompoundTag sourceTag=source.createTag().copy();
        var event=new WanderingTraderPreRerollEvent(null,null,null,1,List.copyOf(source));
        assertThrows(UnsupportedOperationException.class,()->event.getProposedOffers().add(offer(2)));
        event.getProposedOffers().get(0).increaseUses();
        MerchantOffers changed=new MerchantOffers();changed.addAll(event.getProposedOffers());
        assertEquals(sourceTag,source.createTag());
        assertFalse(RerollValidation.sameFullOffers(sourceTag,changed.createTag()));
    }

    @Test void paymentPredicateRejectsAnExactNoOp(){
        MerchantOffers proposal=new MerchantOffers();proposal.add(offer(1));
        CompoundTag old=proposal.createTag().copy();
        assertFalse(VillagerRerollService.shouldConsumeWandering(old,proposal));
        proposal.get(0).increaseUses();
        assertTrue(VillagerRerollService.shouldConsumeWandering(old,proposal));
    }
}

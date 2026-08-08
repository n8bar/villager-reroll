package online.n8bar.villagerreroll;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.slf4j.Logger;

final class WanderingTraderPlanner {
    private static final Logger LOGGER=LogUtils.getLogger();
    private WanderingTraderPlanner() { }

    static Optional<MerchantOffers> prepare(WanderingTrader trader,
            VillagerTrades.ItemListing[] ordinaryPool,VillagerTrades.ItemListing[] rarePool){
        if(ordinaryPool==null||ordinaryPool.length==0||rarePool==null||rarePool.length==0)
            return Optional.empty();
        RandomSource random=trader.getRandom();
        List<VillagerTrades.ItemListing> ordinary=selectOrdinary(
                ordinaryPool,5,random::nextInt);
        MerchantOffers offers=new MerchantOffers();
        if(!appendSelected(offers,trader,random,ordinary,"generic"))return Optional.empty();
        // Vanilla chooses the rare listing only after generic listings have consumed their RNG.
        VillagerTrades.ItemListing rare=selectRare(rarePool,random::nextInt);
        if(!appendSelected(offers,trader,random,java.util.Collections.singletonList(rare),"rare"))
            return Optional.empty();
        return Optional.of(offers);
    }

    static List<VillagerTrades.ItemListing> selectOrdinary(VillagerTrades.ItemListing[] pool,
            int count,IntUnaryOperator random){
        if(pool==null||pool.length==0||count<0)return List.of();
        Set<Integer> indices=new HashSet<>();
        if(pool.length<=count){
            for(int i=0;i<pool.length;i++)indices.add(i);
        }else{
            while(indices.size()<count){
                int draw=random.applyAsInt(pool.length);
                if(draw<0||draw>=pool.length)return List.of();
                indices.add(draw);
            }
        }
        ArrayList<VillagerTrades.ItemListing> selected=new ArrayList<>(indices.size());
        for(int index:indices)selected.add(pool[index]);
        return List.copyOf(selected);
    }

    static VillagerTrades.ItemListing selectRare(VillagerTrades.ItemListing[] pool,
            IntUnaryOperator random){
        if(pool==null||pool.length==0)return null;
        int draw=random.applyAsInt(pool.length);
        return draw<0||draw>=pool.length?null:pool[draw];
    }

    static boolean appendSelected(MerchantOffers offers,WanderingTrader trader,RandomSource random,
            List<VillagerTrades.ItemListing> selected,String poolName){
        for(VillagerTrades.ItemListing listing:selected){
            try{
                MerchantOffer offer=listing.getOffer(trader,random);
                if(offer!=null)offers.add(OfferCopies.deepCopy(offer));
            }catch(RuntimeException failure){
                LOGGER.warn("Wandering trade listing failed: trader={} pool={} listing={} exception={}",
                        trader==null?"unavailable":trader.getUUID(),poolName,
                        listing==null?"null":listing.getClass().getName(),failure.getClass().getName());
                return false;
            }
        }
        return true;
    }
}

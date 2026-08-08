package online.n8bar.villagerreroll.api;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/** Fired on the server thread after validation/build and before payment or mutation. */
@Cancelable
public final class WanderingTraderPreRerollEvent extends Event {
    private final ServerPlayer player;
    private final WanderingTrader trader;
    private final Item paymentItem;
    private final int paymentCount;
    private final List<MerchantOffer> proposedOffers;

    public WanderingTraderPreRerollEvent(ServerPlayer player,WanderingTrader trader,Item paymentItem,
            int paymentCount,List<MerchantOffer> proposedOffers){
        this.player=player; this.trader=trader; this.paymentItem=paymentItem;
        this.paymentCount=paymentCount;
        this.proposedOffers=proposedOffers.stream()
                .map(offer->new MerchantOffer(offer.createTag().copy())).toList();
    }
    public ServerPlayer getPlayer(){return player;}
    public WanderingTrader getTrader(){return trader;}
    public Item getPaymentItem(){return paymentItem;}
    public int getPaymentCount(){return paymentCount;}
    /** Structurally immutable, deeply isolated snapshot of the proposed complete offer list. */
    public List<MerchantOffer> getProposedOffers(){return proposedOffers;}
}

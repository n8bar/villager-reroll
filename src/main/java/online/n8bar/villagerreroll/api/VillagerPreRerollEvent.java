package online.n8bar.villagerreroll.api;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/** Fired on the server thread after validation/build and before payment or mutation. */
@Cancelable
public final class VillagerPreRerollEvent extends Event {
    private final ServerPlayer player;
    private final Villager villager;
    private final Item paymentItem;
    private final int paymentCount;
    private final List<MerchantOffer> proposedOffers;

    public VillagerPreRerollEvent(ServerPlayer player, Villager villager, Item paymentItem,
            int paymentCount, List<MerchantOffer> proposedOffers) {
        this.player = player;
        this.villager = villager;
        this.paymentItem = paymentItem;
        this.paymentCount = paymentCount;
        this.proposedOffers = List.copyOf(proposedOffers);
    }

    public ServerPlayer getPlayer() { return player; }
    public Villager getVillager() { return villager; }
    public Item getPaymentItem() { return paymentItem; }
    public int getPaymentCount() { return paymentCount; }
    /** Structurally immutable proposal; individual vanilla offers remain readable trade objects. */
    public List<MerchantOffer> getProposedOffers() { return proposedOffers; }
}

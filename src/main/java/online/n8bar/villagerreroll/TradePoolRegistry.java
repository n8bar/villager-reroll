package online.n8bar.villagerreroll;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Captures Forge's finalized trade pools, including contributions from other mods. */
final class TradePoolRegistry {
    private final Map<VillagerProfession, Map<Integer, List<VillagerTrades.ItemListing>>> pools =
            new HashMap<>();

    @SubscribeEvent
    public void capture(ServerStartingEvent event) {
        pools.clear();
        VillagerTrades.TRADES.forEach((profession, source) -> {
            Map<Integer, List<VillagerTrades.ItemListing>> tiers = new HashMap<>();
            for (Int2ObjectMap.Entry<VillagerTrades.ItemListing[]> entry
                    : source.int2ObjectEntrySet()) {
                tiers.put(entry.getIntKey(), List.copyOf(Arrays.asList(entry.getValue())));
            }
            pools.put(profession, Map.copyOf(tiers));
        });
    }

    @SubscribeEvent
    public void clear(ServerStoppedEvent event) {
        pools.clear();
    }

    Map<Integer, List<VillagerTrades.ItemListing>> forProfession(VillagerProfession profession) {
        return pools.getOrDefault(profession, Map.of());
    }
}

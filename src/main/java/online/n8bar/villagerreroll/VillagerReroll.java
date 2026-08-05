package online.n8bar.villagerreroll;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Forge entry point.
 *
 * <p>All gameplay runs on the logical server through Forge events; clients need no copy.</p>
 */
@Mod(VillagerReroll.MOD_ID)
public final class VillagerReroll {
    public static final String MOD_ID = "villager_reroll";
    private static final Logger LOGGER = LogUtils.getLogger();

    public VillagerReroll() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, VillagerRerollConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new VillagerRerollService());
        LOGGER.info("Villager Reroll loaded (server-authoritative; clients do not require the mod)");
    }
}

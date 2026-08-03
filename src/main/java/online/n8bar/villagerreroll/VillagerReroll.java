package online.n8bar.villagerreroll;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Forge entry point.
 *
 * <p>This initial project intentionally contains no reroll event handler. It proves the mod metadata
 * and build plumbing before gameplay behavior is added and tested.</p>
 */
@Mod(VillagerReroll.MOD_ID)
public final class VillagerReroll {
    public static final String MOD_ID = "villager_reroll";
    private static final Logger LOGGER = LogUtils.getLogger();

    public VillagerReroll() {
        LOGGER.info("Villager Reroll scaffold loaded; gameplay behavior is not implemented yet");
    }
}

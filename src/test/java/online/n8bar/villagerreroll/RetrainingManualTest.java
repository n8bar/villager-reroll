package online.n8bar.villagerreroll;

import com.google.gson.JsonParser;
import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStreamReader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RetrainingManualTest {
    @BeforeAll static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }
    @Test void authenticityRequiresPrivateMarker() {
        CompoundTag ordinary=new CompoundTag();
        ordinary.putString("displayName", "Trade Retraining Manual");
        assertFalse(RetrainingManual.hasMarker(ordinary));
        RetrainingManual.stampMarker(ordinary);
        assertTrue(RetrainingManual.hasMarker(ordinary));
    }
    @Test void canonicalManualCarriesInstructionsAndReadLore() {
        CompoundTag tag=RetrainingManual.canonicalTag();
        assertEquals(tag,RetrainingManual.create().getTag());
        assertTrue(RetrainingManual.hasMarker(tag));
        ListTag pages=tag.getList("pages",8);
        assertEquals(1,pages.size());
        assertEquals(RetrainingManual.INSTRUCTIONS,pages.getString(0));
        assertFalse(pages.getString(0).startsWith("{"));
        assertTrue(tag.getCompound("display").getList("Lore",8).getString(0).contains("Right-click air to read"));
    }
    @Test void recipeResultNbtExactlyMatchesCanonicalManual() throws Exception {
        try (var stream=getClass().getResourceAsStream(
                "/data/villager_reroll/recipes/trade_retraining_manual.json")) {
            assertNotNull(stream);
            var json=JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
            CompoundTag recipe=net.minecraftforge.common.crafting.CraftingHelper.getNBT(
                    json.getAsJsonObject("result").get("nbt"));
            assertEquals(RetrainingManual.canonicalTag(),recipe);
            assertEquals(RetrainingManual.create().getTag(),recipe);
            assertEquals(RetrainingManual.INSTRUCTIONS,recipe.getList("pages",8).getString(0));
            assertFalse(recipe.getList("pages",8).getString(0).startsWith("{"));
        }
    }
    @Test void masterLibrarianTradeCostsTwelveEmeraldsOnly() {
        var offer=RetrainingManual.createLibrarianOffer();
        assertTrue(offer.getBaseCostA().is(Items.EMERALD));
        assertEquals(12,offer.getBaseCostA().getCount());
        assertTrue(offer.getCostB().isEmpty());
        assertTrue(RetrainingManual.isGenuine(offer.getResult()));
        assertEquals(RetrainingManual.canonicalTag(),offer.getResult().getTag());
        assertEquals(6,offer.getMaxUses());
        assertEquals(30,offer.getXp());
        assertEquals(0.05f,offer.getPriceMultiplier());
    }
}

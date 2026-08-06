package online.n8bar.villagerreroll;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TierLayoutTest {
    @Test void manualTradeMakesCurrentMasterLibrarianCapTen() {
        Map<Integer,Integer> pools=Map.of(1,3,2,3,3,3,4,4,5,2);
        assertArrayEquals(new int[]{2,2,2,2,2}, TierLayout.inferLegacy("minecraft:librarian",5,pools,10).orElseThrow());
        assertTrue(TierLayout.inferLegacy("minecraft:librarian",5,pools,9).isEmpty());
    }
    @Test void refusesModdedOrMissingTierInference() {
        assertTrue(TierLayout.inferLegacy("other:librarian",2,Map.of(1,3,2,3),4).isEmpty());
        assertTrue(TierLayout.inferLegacy("minecraft:librarian",2,Map.of(1,3),2).isEmpty());
    }
    @Test void validatesCountsAndOfferTotal() {
        assertTrue(TierLayout.valid(new int[]{2,2,1},3,5));
        assertFalse(TierLayout.valid(new int[]{2,0,1},3,3));
    }
}

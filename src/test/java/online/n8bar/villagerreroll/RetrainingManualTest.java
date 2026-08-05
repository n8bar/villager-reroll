package online.n8bar.villagerreroll;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class RetrainingManualTest {
    @Test void authenticityRequiresPrivateMarker() {
        CompoundTag ordinary=new CompoundTag();
        ordinary.putString("displayName", "Trade Retraining Manual");
        assertFalse(RetrainingManual.hasMarker(ordinary));
        RetrainingManual.stampMarker(ordinary);
        assertTrue(RetrainingManual.hasMarker(ordinary));
    }
}

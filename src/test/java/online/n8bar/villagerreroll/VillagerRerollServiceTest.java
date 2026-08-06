package online.n8bar.villagerreroll;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.trading.MerchantOffers;
import org.junit.jupiter.api.Test;

class VillagerRerollServiceTest {
    @Test void genuineManualNeverAttacksVillager() {
        assertTrue(VillagerRerollService.shouldCancel(true,true));
        assertFalse(VillagerRerollService.shouldCancel(false,true));
        assertFalse(VillagerRerollService.shouldCancel(true,false));
    }
    @Test void transactionRequiresSneakingAfterCancellation() {
        assertTrue(VillagerRerollService.shouldTransact(true,true,true));
        assertFalse(VillagerRerollService.shouldTransact(false,true,true));
        assertFalse(VillagerRerollService.shouldTransact(true,false,true));
        assertFalse(VillagerRerollService.shouldTransact(true,true,false));
    }
    @Test void conciseResultNamesChangedAndPreservedTier() {
        PreparedReroll plan=new PreparedReroll(UUID.randomUUID(),null,5,new CompoundTag(),
                new MerchantOffers(),new int[]{2,2,2,2,1},List.of(1,2,3,4),List.of(5));
        assertEquals("Retrained: 8 trades changed, 1 Master trade preserved.",
                VillagerRerollService.resultMessage(plan));
    }
}

package online.n8bar.villagerreroll;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.trading.MerchantOffers;
import org.junit.jupiter.api.Test;

class VillagerRerollServiceTest {
    @Test void genuineManualCancelsAgainstEveryEntity() {
        assertTrue(VillagerRerollService.shouldCancel(true));
        assertFalse(VillagerRerollService.shouldCancel(false));
    }
    @Test void alternateColorMappingIsUniformAndNeverKeepsCurrent() {
        for(var current:net.minecraft.world.item.DyeColor.values()){
            java.util.Set<net.minecraft.world.item.DyeColor> results=new java.util.HashSet<>();
            for(int draw=0;draw<15;draw++)results.add(VillagerRerollService.differentColor(current,draw));
            assertEquals(15,results.size());
            assertFalse(results.contains(current));
        }
        assertThrows(IllegalArgumentException.class,()->VillagerRerollService.differentColor(
                net.minecraft.world.item.DyeColor.WHITE,-1));
        assertThrows(IllegalArgumentException.class,()->VillagerRerollService.differentColor(
                net.minecraft.world.item.DyeColor.WHITE,15));
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

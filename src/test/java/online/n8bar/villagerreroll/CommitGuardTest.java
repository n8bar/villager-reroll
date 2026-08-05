package online.n8bar.villagerreroll;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommitGuardTest {
    @Test
    void blocksSecondSuccessForVillagerInSameTick() {
        CommitGuard guard = new CommitGuard();
        UUID villager = UUID.randomUUID();
        assertTrue(guard.mayCommit(villager, 42));
        guard.recordSuccess(villager, 42);
        assertFalse(guard.mayCommit(villager, 42));
        assertTrue(guard.mayCommit(villager, 43));
    }

    @Test
    void clearAllResetsSuccessHistory() {
        CommitGuard guard = new CommitGuard();
        UUID villager = UUID.randomUUID();
        guard.recordSuccess(villager, 42);
        guard.clearAll();
        assertTrue(guard.mayCommit(villager, 42));
    }

    @Test void expiryDropsOldUuidEntries() {
        CommitGuard guard=new CommitGuard(); UUID old=UUID.randomUUID(), current=UUID.randomUUID();
        guard.recordSuccess(old,10); guard.recordSuccess(current,20); guard.expireBefore(20);
        assertEquals(1,guard.size()); assertTrue(guard.mayCommit(old,10)); assertFalse(guard.mayCommit(current,20));
    }
}

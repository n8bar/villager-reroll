package online.n8bar.villagerreroll;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}

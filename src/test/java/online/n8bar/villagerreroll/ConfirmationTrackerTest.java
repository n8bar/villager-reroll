package online.n8bar.villagerreroll;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfirmationTrackerTest {
    private final UUID player = UUID.randomUUID();
    private final UUID villager = UUID.randomUUID();
    private final ConfirmationTracker<String> tracker = new ConfirmationTracker<>();

    @Test
    void sameVillagerAndDimensionConfirmsInsideDeadline() {
        assertEquals(ConfirmationTracker.Result.ARMED,
                tracker.armOrConfirm(player, villager, "overworld", 100, 200));
        assertEquals(ConfirmationTracker.Result.CONFIRMED,
                tracker.armOrConfirm(player, villager, "overworld", 300, 200));
        assertEquals(0, tracker.size());
    }

    @Test
    void expiredConfirmationArmsAgain() {
        tracker.armOrConfirm(player, villager, "overworld", 100, 200);
        assertEquals(ConfirmationTracker.Result.ARMED,
                tracker.armOrConfirm(player, villager, "overworld", 301, 200));
        assertEquals(1, tracker.size());
    }

    @Test
    void differentVillagerOrDimensionReplacesPending() {
        tracker.armOrConfirm(player, villager, "overworld", 100, 200);
        assertEquals(ConfirmationTracker.Result.ARMED,
                tracker.armOrConfirm(player, UUID.randomUUID(), "overworld", 101, 200));
        assertEquals(ConfirmationTracker.Result.ARMED,
                tracker.armOrConfirm(player, villager, "nether", 102, 200));
    }

    @Test
    void clearAndSweepRemoveState() {
        tracker.armOrConfirm(player, villager, "overworld", 100, 200);
        tracker.expire(301);
        assertEquals(0, tracker.size());
        tracker.armOrConfirm(player, villager, "overworld", 400, 200);
        tracker.clear(player);
        assertEquals(0, tracker.size());
    }

    @Test
    void clearVillagerInvalidatesEveryPlayersConfirmation() {
        UUID otherPlayer = UUID.randomUUID();
        tracker.armOrConfirm(player, villager, "overworld", 100, 200);
        tracker.armOrConfirm(otherPlayer, villager, "overworld", 100, 200);

        tracker.clearVillager(villager);

        assertEquals(0, tracker.size());
        assertEquals(ConfirmationTracker.Result.ARMED,
                tracker.armOrConfirm(otherPlayer, villager, "overworld", 101, 200));
    }

    @Test
    void clearAllRemovesEveryConfirmation() {
        tracker.armOrConfirm(player, villager, "overworld", 100, 200);
        tracker.armOrConfirm(UUID.randomUUID(), UUID.randomUUID(), "nether", 100, 200);
        tracker.clearAll();
        assertEquals(0, tracker.size());
    }
}

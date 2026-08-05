package online.n8bar.villagerreroll;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfirmationTrackerTest {
    private final ConfirmationTracker<String, String> tracker = new ConfirmationTracker<>();
    private final UUID player=UUID.randomUUID(), villager=UUID.randomUUID();
    @Test void takesExactPreparedPlanOnce() {
        tracker.prepare(player,villager,"overworld",100,200,"plan");
        assertEquals("plan",tracker.take(player,villager,"overworld",300).orElseThrow());
        assertTrue(tracker.take(player,villager,"overworld",300).isEmpty());
    }
    @Test void rejectsExpiredWrongVillagerAndDimension() {
        tracker.prepare(player,villager,"overworld",100,200,"plan");
        assertTrue(tracker.take(player,villager,"overworld",301).isEmpty());
        tracker.prepare(player,villager,"overworld",400,200,"plan");
        assertTrue(tracker.take(player,UUID.randomUUID(),"overworld",401).isEmpty());
        tracker.prepare(player,villager,"overworld",400,200,"plan");
        assertTrue(tracker.take(player,villager,"nether",401).isEmpty());
    }
    @Test void clearVillagerAndAllInvalidatePlans() {
        UUID other=UUID.randomUUID();
        tracker.prepare(player,villager,"overworld",1,20,"a");
        tracker.prepare(other,villager,"overworld",1,20,"b");
        tracker.clearVillager(villager); assertEquals(0,tracker.size());
        tracker.prepare(player,villager,"overworld",1,20,"a");
        tracker.clearAll(); assertEquals(0,tracker.size());
    }
    @Test void expireAndPlayerClearWork() {
        tracker.prepare(player,villager,"overworld",1,20,"a"); tracker.expire(22); assertEquals(0,tracker.size());
        tracker.prepare(player,villager,"overworld",1,20,"a"); tracker.clear(player); assertEquals(0,tracker.size());
    }
}

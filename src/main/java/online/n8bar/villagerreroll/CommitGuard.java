package online.n8bar.villagerreroll;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Stops reentrant or sequential commits against one villager in the same server tick. */
final class CommitGuard {
    private final Map<UUID, Long> lastSuccessTick = new HashMap<>();

    boolean mayCommit(UUID villager, long now) {
        return lastSuccessTick.getOrDefault(villager, Long.MIN_VALUE) != now;
    }

    void recordSuccess(UUID villager, long now) {
        lastSuccessTick.put(villager, now);
    }

    void expireBefore(long oldestTick) {
        lastSuccessTick.values().removeIf(tick -> tick < oldestTick);
    }

    int size() { return lastSuccessTick.size(); }

    void clearAll() {
        lastSuccessTick.clear();
    }
}

package online.n8bar.villagerreroll;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** In-memory, non-persistent two-click confirmation state. */
final class ConfirmationTracker<D> {
    private final Map<UUID, Pending<D>> pending = new HashMap<>();

    Result armOrConfirm(UUID player, UUID villager, D dimension, long now, long timeout) {
        Pending<D> current = pending.get(player);
        if (current != null && current.matches(villager, dimension) && now <= current.expiresAt()) {
            pending.remove(player);
            return Result.CONFIRMED;
        }
        pending.put(player, new Pending<>(villager, dimension, now + timeout));
        return Result.ARMED;
    }

    void clear(UUID player) {
        pending.remove(player);
    }

    void clearVillager(UUID villager) {
        pending.values().removeIf(value -> value.villager().equals(villager));
    }

    void clearAll() {
        pending.clear();
    }

    void expire(long now) {
        pending.values().removeIf(value -> now > value.expiresAt());
    }

    int size() {
        return pending.size();
    }

    enum Result { ARMED, CONFIRMED }

    private record Pending<D>(UUID villager, D dimension, long expiresAt) {
        boolean matches(UUID expectedVillager, D expectedDimension) {
            return villager.equals(expectedVillager) && dimension.equals(expectedDimension);
        }
    }
}

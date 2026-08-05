package online.n8bar.villagerreroll;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory prepared transaction state; never persisted across a server stop. */
final class ConfirmationTracker<D, P> {
    private final Map<UUID, Pending<D, P>> pending = new HashMap<>();

    void prepare(UUID player, UUID villager, D dimension, long now, long timeout, P plan) {
        pending.put(player, new Pending<>(villager, dimension, now + timeout, plan));
    }

    Optional<P> take(UUID player, UUID villager, D dimension, long now) {
        Pending<D, P> current = pending.remove(player);
        return current != null && current.matches(villager, dimension) && now <= current.expiresAt()
                ? Optional.of(current.plan()) : Optional.empty();
    }

    void clear(UUID player) { pending.remove(player); }
    void clearVillager(UUID villager) {
        pending.values().removeIf(value -> value.villager().equals(villager));
    }
    void clearAll() { pending.clear(); }
    void expire(long now) { pending.values().removeIf(value -> now > value.expiresAt()); }
    int size() { return pending.size(); }

    private record Pending<D, P>(UUID villager, D dimension, long expiresAt, P plan) {
        boolean matches(UUID expectedVillager, D expectedDimension) {
            return villager.equals(expectedVillager) && dimension.equals(expectedDimension);
        }
    }
}

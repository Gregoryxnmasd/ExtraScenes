package com.extrascenes.scene;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Entity;

public class SessionEntityTracker {
    private final Set<UUID> trackedEntityIds = new HashSet<>();

    public void register(Entity entity) {
        if (entity != null) {
            trackedEntityIds.add(entity.getUniqueId());
        }
    }

    public void unregister(Entity entity) {
        if (entity != null) {
            trackedEntityIds.remove(entity.getUniqueId());
        }
    }

    public Set<UUID> getTrackedEntityIds() {
        return Collections.unmodifiableSet(trackedEntityIds);
    }

    public void clear() {
        trackedEntityIds.clear();
    }
}

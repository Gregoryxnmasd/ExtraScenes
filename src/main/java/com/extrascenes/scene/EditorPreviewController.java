package com.extrascenes.scene;

import com.extrascenes.CitizensAdapter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class EditorPreviewController {
    private final CitizensAdapter citizensAdapter;
    private final Map<UUID, Map<String, SessionActorHandle>> handlesByViewer = new HashMap<>();

    public EditorPreviewController(CitizensAdapter citizensAdapter) {
        this.citizensAdapter = citizensAdapter;
    }

    public Map<String, SessionActorHandle> handlesFor(Player viewer) {
        return handlesByViewer.computeIfAbsent(viewer.getUniqueId(), key -> new HashMap<>());
    }

    public void register(Player viewer, SessionActorHandle handle) {
        if (viewer == null || handle == null || handle.getActorId() == null) {
            return;
        }
        handlesFor(viewer).put(handle.getActorId().toLowerCase(Locale.ROOT), handle);
    }

    public SessionActorHandle getHandle(Player viewer, String actorId) {
        if (viewer == null || actorId == null) {
            return null;
        }
        return handlesFor(viewer).get(actorId.toLowerCase(Locale.ROOT));
    }

    public List<SessionActorHandle> listHandles(UUID viewerId) {
        if (viewerId == null) {
            return List.of();
        }
        Map<String, SessionActorHandle> handles = handlesByViewer.get(viewerId);
        if (handles == null || handles.isEmpty()) {
            return List.of();
        }
        return List.copyOf(handles.values());
    }

    public void cleanup(Player viewer) {
        if (viewer == null) {
            return;
        }
        cleanup(viewer.getUniqueId());
    }

    public void cleanup(UUID viewerId) {
        if (viewerId == null) {
            return;
        }
        Map<String, SessionActorHandle> handles = handlesByViewer.remove(viewerId);
        if (handles == null) {
            return;
        }
        for (SessionActorHandle handle : handles.values()) {
            if (handle.getCitizensNpc() != null && citizensAdapter.isAvailable()) {
                citizensAdapter.destroy(handle.getCitizensNpc());
            }
            Entity entity = handle.getEntity();
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
    }

    public void cleanupAll() {
        for (UUID viewerId : java.util.List.copyOf(handlesByViewer.keySet())) {
            cleanup(viewerId);
        }
    }
}

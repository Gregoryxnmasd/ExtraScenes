package com.extrascenes.scene;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditorSessionManager {
    private final Map<UUID, EditorSession> sessions = new HashMap<>();

    public EditorSession createSession(UUID playerId, Scene scene) {
        EditorSession session = new EditorSession(playerId, scene);
        sessions.put(playerId, session);
        return session;
    }

    public EditorSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    public void removeSession(UUID playerId) {
        sessions.remove(playerId);
    }

    public java.util.Map<UUID, EditorSession> getSessionsView() {
        return java.util.Collections.unmodifiableMap(sessions);
    }

    public void clear() {
        sessions.clear();
    }
}

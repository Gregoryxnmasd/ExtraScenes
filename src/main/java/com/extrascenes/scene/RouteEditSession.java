package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class RouteEditSession {
    private final UUID playerId;
    private final String routeId;
    private final List<Transform> points = new ArrayList<>();

    public RouteEditSession(UUID playerId, String routeId) {
        this.playerId = playerId;
        this.routeId = routeId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getRouteId() {
        return routeId;
    }

    public void addPoint(Transform transform) {
        if (transform != null) {
            points.add(transform);
        }
    }

    public List<Transform> getPoints() {
        return Collections.unmodifiableList(points);
    }
}

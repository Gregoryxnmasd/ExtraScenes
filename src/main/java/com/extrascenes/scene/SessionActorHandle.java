package com.extrascenes.scene;

import org.bukkit.entity.Entity;

public class SessionActorHandle {
    private final String actorId;
    private final Object citizensNpc;
    private final Entity entity;
    private boolean spawned;
    private Transform lastTransform;

    public SessionActorHandle(String actorId, Object citizensNpc, Entity entity) {
        this.actorId = actorId;
        this.citizensNpc = citizensNpc;
        this.entity = entity;
        this.spawned = true;
    }

    public String getActorId() {
        return actorId;
    }

    public Object getCitizensNpc() {
        return citizensNpc;
    }

    public Entity getEntity() {
        return entity;
    }

    public boolean isSpawned() {
        return spawned;
    }

    public void setSpawned(boolean spawned) {
        this.spawned = spawned;
    }

    public Transform getLastTransform() {
        return lastTransform;
    }

    public void setLastTransform(Transform lastTransform) {
        this.lastTransform = lastTransform;
    }
}

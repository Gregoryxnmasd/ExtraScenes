package com.extrascenes.scene;

import org.bukkit.entity.Entity;

public class SessionActorHandle {
    private final String actorId;
    private final Object citizensNpc;
    private final Entity entity;

    public SessionActorHandle(String actorId, Object citizensNpc, Entity entity) {
        this.actorId = actorId;
        this.citizensNpc = citizensNpc;
        this.entity = entity;
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
}

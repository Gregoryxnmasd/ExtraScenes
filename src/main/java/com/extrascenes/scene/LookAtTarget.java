package com.extrascenes.scene;

import java.util.UUID;

public class LookAtTarget {
    public enum Mode {
        NONE,
        POSITION,
        ENTITY
    }

    private Mode mode;
    private Transform position;
    private UUID entityId;

    public LookAtTarget(Mode mode, Transform position, UUID entityId) {
        this.mode = mode;
        this.position = position;
        this.entityId = entityId;
    }

    public static LookAtTarget none() {
        return new LookAtTarget(Mode.NONE, null, null);
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Transform getPosition() {
        return position;
    }

    public void setPosition(Transform position) {
        this.position = position;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }
}

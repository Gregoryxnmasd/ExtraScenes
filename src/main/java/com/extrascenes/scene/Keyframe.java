package com.extrascenes.scene;

import java.util.UUID;

public abstract class Keyframe {
    private final UUID id;
    private int timeTicks;

    protected Keyframe(UUID id, int timeTicks) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.timeTicks = timeTicks;
    }

    public UUID getId() {
        return id;
    }

    public int getTimeTicks() {
        return timeTicks;
    }

    public void setTimeTicks(int timeTicks) {
        this.timeTicks = timeTicks;
    }

    public abstract SceneTrackType getTrackType();

    public abstract String getType();
}

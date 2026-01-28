package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SceneKeyframe {
    private final int timeTicks;
    private final String type;
    private final SmoothingMode smoothingMode;
    private final boolean instant;
    private final String lookAt;
    private final List<String> commands;
    private final UUID refId;

    public SceneKeyframe(int timeTicks, String type, SmoothingMode smoothingMode, boolean instant, String lookAt,
                         List<String> commands, UUID refId) {
        this.timeTicks = timeTicks;
        this.type = type;
        this.smoothingMode = smoothingMode;
        this.instant = instant;
        this.lookAt = lookAt;
        this.commands = commands == null ? new ArrayList<>() : new ArrayList<>(commands);
        this.refId = refId;
    }

    public int getTimeTicks() {
        return timeTicks;
    }

    public String getType() {
        return type;
    }

    public SmoothingMode getSmoothingMode() {
        return smoothingMode;
    }

    public boolean isInstant() {
        return instant;
    }

    public String getLookAt() {
        return lookAt;
    }

    public List<String> getCommands() {
        return new ArrayList<>(commands);
    }

    public UUID getRefId() {
        return refId;
    }
}

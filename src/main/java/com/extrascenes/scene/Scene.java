package com.extrascenes.scene;

import java.util.EnumMap;
import java.util.Map;

public class Scene {
    private final String name;
    private int durationTicks;
    private final int formatVersion;
    private final Map<SceneTrackType, Track<? extends Keyframe>> tracks;
    private final SceneTimeline timeline;
    private SmoothingMode defaultSmoothing;
    private SmoothingQuality smoothingQuality;
    private String cameraMode;
    private boolean freezePlayer;
    private boolean allowGlobalCommands;
    private EndTeleportMode endTeleportMode;
    private SceneLocation endLocation;
    private boolean dirty;

    public Scene(String name, int durationTicks, int formatVersion, Map<SceneTrackType, Track<? extends Keyframe>> tracks) {
        this.name = name;
        this.durationTicks = durationTicks;
        this.formatVersion = formatVersion;
        this.tracks = new EnumMap<>(SceneTrackType.class);
        if (tracks != null) {
            this.tracks.putAll(tracks);
        }
        this.timeline = new SceneTimeline(this);
        this.defaultSmoothing = SmoothingMode.EASE_IN_OUT_QUINT;
        this.smoothingQuality = SmoothingQuality.NORMAL;
        this.cameraMode = "SPECTATOR";
        this.freezePlayer = true;
        this.allowGlobalCommands = false;
        this.endTeleportMode = EndTeleportMode.RETURN_TO_START;
        this.endLocation = null;
        this.dirty = false;
    }

    public String getName() {
        return name;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public void setDurationTicks(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    public Map<SceneTrackType, Track<? extends Keyframe>> getTracks() {
        return tracks;
    }

    @SuppressWarnings("unchecked")
    public <T extends Keyframe> Track<T> getTrack(SceneTrackType type) {
        return (Track<T>) tracks.get(type);
    }

    public SceneTimeline getTimeline() {
        return timeline;
    }

    public SmoothingMode getDefaultSmoothing() {
        return defaultSmoothing;
    }

    public void setDefaultSmoothing(SmoothingMode defaultSmoothing) {
        this.defaultSmoothing = defaultSmoothing == null ? SmoothingMode.EASE_IN_OUT_QUINT : defaultSmoothing;
    }

    public SmoothingQuality getSmoothingQuality() {
        return smoothingQuality;
    }

    public void setSmoothingQuality(SmoothingQuality smoothingQuality) {
        this.smoothingQuality = smoothingQuality == null ? SmoothingQuality.NORMAL : smoothingQuality;
    }

    public String getCameraMode() {
        return cameraMode;
    }

    public void setCameraMode(String cameraMode) {
        this.cameraMode = cameraMode == null || cameraMode.isBlank() ? "SPECTATOR" : cameraMode;
    }

    public boolean isFreezePlayer() {
        return freezePlayer;
    }

    public void setFreezePlayer(boolean freezePlayer) {
        this.freezePlayer = freezePlayer;
    }

    public boolean isAllowGlobalCommands() {
        return allowGlobalCommands;
    }

    public void setAllowGlobalCommands(boolean allowGlobalCommands) {
        this.allowGlobalCommands = allowGlobalCommands;
    }

    public EndTeleportMode getEndTeleportMode() {
        return endTeleportMode;
    }

    public void setEndTeleportMode(EndTeleportMode endTeleportMode) {
        this.endTeleportMode = endTeleportMode == null ? EndTeleportMode.RETURN_TO_START : endTeleportMode;
    }

    public SceneLocation getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(SceneLocation endLocation) {
        this.endLocation = endLocation;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}

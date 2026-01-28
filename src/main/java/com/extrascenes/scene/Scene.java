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
    private String cameraMode;
    private boolean freezePlayer;
    private boolean allowGlobalCommands;

    public Scene(String name, int durationTicks, int formatVersion, Map<SceneTrackType, Track<? extends Keyframe>> tracks) {
        this.name = name;
        this.durationTicks = durationTicks;
        this.formatVersion = formatVersion;
        this.tracks = new EnumMap<>(SceneTrackType.class);
        if (tracks != null) {
            this.tracks.putAll(tracks);
        }
        this.timeline = new SceneTimeline(this);
        this.defaultSmoothing = SmoothingMode.EASE_IN_OUT;
        this.cameraMode = "SPECTATOR";
        this.freezePlayer = true;
        this.allowGlobalCommands = false;
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
        this.defaultSmoothing = defaultSmoothing == null ? SmoothingMode.EASE_IN_OUT : defaultSmoothing;
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
}

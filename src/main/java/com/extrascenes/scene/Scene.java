package com.extrascenes.scene;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Scene {
    private final String sceneId;
    private final String name;
    private int durationTicks;
    private final int formatVersion;
    private final Map<SceneTrackType, Track<? extends Keyframe>> tracks;
    private final SceneTimeline timeline;
    private final Map<String, SceneModelEntry> modelLibrary;
    private final Map<String, SceneActorTemplate> actorTemplates;
    private SmoothingMode defaultSmoothing;
    private SmoothingQuality smoothingQuality;
    private boolean freezePlayer;
    private boolean allowGlobalCommands;
    private EndTeleportMode endTeleportMode;
    private SceneLocation endLocation;
    private boolean dirty;

    public Scene(String sceneId, String name, int durationTicks, int formatVersion, Map<SceneTrackType, Track<? extends Keyframe>> tracks) {
        this.sceneId = sceneId == null || sceneId.isBlank() ? java.util.UUID.randomUUID().toString() : sceneId;
        this.name = name;
        this.durationTicks = durationTicks;
        this.formatVersion = formatVersion;
        this.tracks = new EnumMap<>(SceneTrackType.class);
        if (tracks != null) {
            this.tracks.putAll(tracks);
        }
        this.timeline = new SceneTimeline(this);
        this.modelLibrary = new LinkedHashMap<>();
        this.actorTemplates = new LinkedHashMap<>();
        this.defaultSmoothing = SmoothingMode.SMOOTH;
        this.smoothingQuality = SmoothingQuality.SMOOTH;
        this.freezePlayer = true;
        this.allowGlobalCommands = false;
        this.endTeleportMode = EndTeleportMode.RETURN_TO_START;
        this.endLocation = null;
        this.dirty = false;
    }

    public String getSceneId() {
        return sceneId;
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

    public Map<String, SceneModelEntry> getModelLibrary() {
        return modelLibrary;
    }

    public SceneModelEntry getModelEntry(String name) {
        if (name == null) {
            return null;
        }
        return modelLibrary.get(name.toLowerCase());
    }

    public void putModelEntry(SceneModelEntry entry) {
        if (entry == null || entry.getName() == null) {
            return;
        }
        modelLibrary.put(entry.getName().toLowerCase(), entry);
    }

    public void removeModelEntry(String name) {
        if (name != null) {
            modelLibrary.remove(name.toLowerCase());
        }
    }


    public Map<String, SceneActorTemplate> getActorTemplates() {
        return actorTemplates;
    }

    public SceneActorTemplate getActorTemplate(String actorId) {
        if (actorId == null) {
            return null;
        }
        return actorTemplates.get(actorId.toLowerCase());
    }

    public void putActorTemplate(SceneActorTemplate template) {
        if (template == null || template.getActorId() == null) {
            return;
        }
        actorTemplates.put(template.getActorId().toLowerCase(), template);
    }

    public void removeActorTemplate(String actorId) {
        if (actorId != null) {
            actorTemplates.remove(actorId.toLowerCase());
        }
    }

    public SmoothingMode getDefaultSmoothing() {
        return defaultSmoothing;
    }

    public void setDefaultSmoothing(SmoothingMode defaultSmoothing) {
        this.defaultSmoothing = defaultSmoothing == null ? SmoothingMode.SMOOTH : defaultSmoothing;
    }

    public SmoothingQuality getSmoothingQuality() {
        return smoothingQuality;
    }

    public void setSmoothingQuality(SmoothingQuality smoothingQuality) {
        this.smoothingQuality = smoothingQuality == null ? SmoothingQuality.SMOOTH : smoothingQuality;
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

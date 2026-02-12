package com.extrascenes.scene;

import java.util.Map;
import java.util.TreeMap;
import org.bukkit.entity.EntityType;

public class SceneActorTemplate {
    private final String actorId;
    private EntityType entityType;
    private String displayName;
    private String skinName;
    private double scale;
    private ActorPlaybackMode playbackMode;
    private final Map<Integer, ActorTransformTick> transformTicks;

    public SceneActorTemplate(String actorId) {
        this.actorId = actorId;
        this.entityType = EntityType.PLAYER;
        this.scale = 1.0D;
        this.playbackMode = ActorPlaybackMode.EXACT;
        this.transformTicks = new TreeMap<>();
    }

    public String getActorId() {
        return actorId;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType == null ? EntityType.PLAYER : entityType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getSkinName() {
        return skinName;
    }

    public void setSkinName(String skinName) {
        this.skinName = skinName;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public ActorPlaybackMode getPlaybackMode() {
        return playbackMode;
    }

    public void setPlaybackMode(ActorPlaybackMode playbackMode) {
        this.playbackMode = playbackMode == null ? ActorPlaybackMode.EXACT : playbackMode;
    }

    public Map<Integer, ActorTransformTick> getTransformTicks() {
        return transformTicks;
    }

    public void putTransformTick(ActorTransformTick tick) {
        if (tick != null) {
            transformTicks.put(tick.getTick(), tick);
        }
    }

    public ActorTransformTick getTransformTick(int tick) {
        return transformTicks.get(tick);
    }
}

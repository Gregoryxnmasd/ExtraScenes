package com.extrascenes.scene;

import java.util.Map;
import java.util.TreeMap;
import org.bukkit.entity.EntityType;

public class SceneActorTemplate {
    private final String actorId;
    private EntityType entityType;
    private String displayName;
    private String skinName;
    private String skinSignature;
    private String skinTexture;
    private String skinCacheKey;
    private double scale;
    private ActorPlaybackMode playbackMode;
    private boolean previewEnabled;
    private final Map<Integer, ActorTransformTick> transformTicks;
    private final Map<Integer, ActorTickAction> tickActions;

    public SceneActorTemplate(String actorId) {
        this.actorId = actorId;
        this.entityType = EntityType.PLAYER;
        this.scale = 1.0D;
        this.playbackMode = ActorPlaybackMode.EXACT;
        this.previewEnabled = true;
        this.transformTicks = new TreeMap<>();
        this.tickActions = new TreeMap<>();
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

    public String getSkinSignature() {
        return skinSignature;
    }

    public void setSkinSignature(String skinSignature) {
        this.skinSignature = skinSignature;
    }

    public String getSkinTexture() {
        return skinTexture;
    }

    public void setSkinTexture(String skinTexture) {
        this.skinTexture = skinTexture;
    }

    public String getSkinCacheKey() {
        return skinCacheKey;
    }

    public void setSkinCacheKey(String skinCacheKey) {
        this.skinCacheKey = skinCacheKey;
    }

    public boolean isPreviewEnabled() {
        return previewEnabled;
    }

    public void setPreviewEnabled(boolean previewEnabled) {
        this.previewEnabled = previewEnabled;
    }

    public void putTransformTick(ActorTransformTick tick) {
        if (tick != null) {
            transformTicks.put(tick.getTick(), tick);
        }
    }

    public ActorTransformTick getTransformTick(int tick) {
        return transformTicks.get(tick);
    }

    public Map<Integer, ActorTickAction> getTickActions() {
        return tickActions;
    }

    public ActorTickAction getOrCreateTickAction(int tick) {
        return tickActions.computeIfAbsent(Math.max(0, tick), ActorTickAction::new);
    }

    public ActorTickAction getTickAction(int tick) {
        return tickActions.get(Math.max(0, tick));
    }
}

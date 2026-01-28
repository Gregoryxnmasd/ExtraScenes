package com.extrascenes.scene;

import java.util.UUID;

public class ModelKeyframe extends Keyframe {
    public enum Action {
        SPAWN,
        ANIM,
        STOP,
        DESPAWN
    }

    private Action action;
    private String modelId;
    private String entityRef;
    private String animationId;
    private boolean loop;
    private double speed;
    private Transform spawnTransform;

    public ModelKeyframe(UUID id, int timeTicks, Action action) {
        super(id, timeTicks);
        this.action = action == null ? Action.SPAWN : action;
    }

    @Override
    public SceneTrackType getTrackType() {
        return SceneTrackType.MODEL;
    }

    @Override
    public String getType() {
        return "model";
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getEntityRef() {
        return entityRef;
    }

    public void setEntityRef(String entityRef) {
        this.entityRef = entityRef;
    }

    public String getAnimationId() {
        return animationId;
    }

    public void setAnimationId(String animationId) {
        this.animationId = animationId;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public Transform getSpawnTransform() {
        return spawnTransform;
    }

    public void setSpawnTransform(Transform spawnTransform) {
        this.spawnTransform = spawnTransform;
    }
}

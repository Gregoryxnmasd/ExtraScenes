package com.extrascenes.scene;

import java.util.UUID;

public class ParticleKeyframe extends Keyframe {
    private String particleId;
    private Transform transform;

    public ParticleKeyframe(UUID id, int timeTicks, String particleId, Transform transform) {
        super(id, timeTicks);
        this.particleId = particleId;
        this.transform = transform;
    }

    @Override
    public SceneTrackType getTrackType() {
        return SceneTrackType.PARTICLE;
    }

    @Override
    public String getType() {
        return "particle";
    }

    public String getParticleId() {
        return particleId;
    }

    public void setParticleId(String particleId) {
        this.particleId = particleId;
    }

    public Transform getTransform() {
        return transform;
    }

    public void setTransform(Transform transform) {
        this.transform = transform;
    }
}

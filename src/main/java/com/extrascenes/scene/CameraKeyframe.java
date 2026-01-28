package com.extrascenes.scene;

import java.util.UUID;

public class CameraKeyframe extends Keyframe {
    private Transform transform;
    private SmoothingMode smoothingMode;
    private boolean instant;
    private LookAtTarget lookAt;

    public CameraKeyframe(UUID id, int timeTicks, Transform transform, SmoothingMode smoothingMode, boolean instant,
                          LookAtTarget lookAt) {
        super(id, timeTicks);
        this.transform = transform;
        this.smoothingMode = smoothingMode == null ? SmoothingMode.LINEAR : smoothingMode;
        this.instant = instant;
        this.lookAt = lookAt == null ? LookAtTarget.none() : lookAt;
    }

    @Override
    public SceneTrackType getTrackType() {
        return SceneTrackType.CAMERA;
    }

    @Override
    public String getType() {
        return "camera";
    }

    public Transform getTransform() {
        return transform;
    }

    public void setTransform(Transform transform) {
        this.transform = transform;
    }

    public SmoothingMode getSmoothingMode() {
        return smoothingMode;
    }

    public void setSmoothingMode(SmoothingMode smoothingMode) {
        this.smoothingMode = smoothingMode;
    }

    public boolean isInstant() {
        return instant;
    }

    public void setInstant(boolean instant) {
        this.instant = instant;
    }

    public LookAtTarget getLookAt() {
        return lookAt;
    }

    public void setLookAt(LookAtTarget lookAt) {
        this.lookAt = lookAt;
    }
}

package com.extrascenes.scene;

public class ActorTransformTick {
    private final int tick;
    private final Transform transform;
    private final boolean sneaking;
    private final boolean sprinting;
    private final boolean swimming;
    private final boolean gliding;

    public ActorTransformTick(int tick, Transform transform, boolean sneaking, boolean sprinting,
                              boolean swimming, boolean gliding) {
        this.tick = tick;
        this.transform = transform;
        this.sneaking = sneaking;
        this.sprinting = sprinting;
        this.swimming = swimming;
        this.gliding = gliding;
    }

    public int getTick() {
        return tick;
    }

    public Transform getTransform() {
        return transform;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public boolean isSwimming() {
        return swimming;
    }

    public boolean isGliding() {
        return gliding;
    }
}

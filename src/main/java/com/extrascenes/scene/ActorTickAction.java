package com.extrascenes.scene;

public class ActorTickAction {
    private final int tick;
    private boolean spawn;
    private boolean despawn;
    private boolean manualTransform;
    private String animation;
    private boolean stopAnimation;
    private LookAtTarget lookAtTarget;
    private String command;
    private Double scale;
    private String skinName;

    public ActorTickAction(int tick) {
        this.tick = Math.max(0, tick);
    }

    public int getTick() {
        return tick;
    }

    public boolean isSpawn() {
        return spawn;
    }

    public void setSpawn(boolean spawn) {
        this.spawn = spawn;
    }

    public boolean isDespawn() {
        return despawn;
    }

    public void setDespawn(boolean despawn) {
        this.despawn = despawn;
    }

    public boolean isManualTransform() {
        return manualTransform;
    }

    public void setManualTransform(boolean manualTransform) {
        this.manualTransform = manualTransform;
    }

    public String getAnimation() {
        return animation;
    }

    public void setAnimation(String animation) {
        this.animation = animation;
    }

    public boolean isStopAnimation() {
        return stopAnimation;
    }

    public void setStopAnimation(boolean stopAnimation) {
        this.stopAnimation = stopAnimation;
    }

    public LookAtTarget getLookAtTarget() {
        return lookAtTarget;
    }

    public void setLookAtTarget(LookAtTarget lookAtTarget) {
        this.lookAtTarget = lookAtTarget;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Double getScale() {
        return scale;
    }

    public void setScale(Double scale) {
        this.scale = scale;
    }

    public String getSkinName() {
        return skinName;
    }

    public void setSkinName(String skinName) {
        this.skinName = skinName;
    }
}

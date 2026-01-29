package com.extrascenes.scene;

public class SceneModelEntry {
    private final String name;
    private String modelId;
    private Transform spawnTransform;
    private String defaultAnimation;

    public SceneModelEntry(String name, String modelId, Transform spawnTransform, String defaultAnimation) {
        this.name = name;
        this.modelId = modelId;
        this.spawnTransform = spawnTransform;
        this.defaultAnimation = defaultAnimation;
    }

    public String getName() {
        return name;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public Transform getSpawnTransform() {
        return spawnTransform;
    }

    public void setSpawnTransform(Transform spawnTransform) {
        this.spawnTransform = spawnTransform;
    }

    public String getDefaultAnimation() {
        return defaultAnimation;
    }

    public void setDefaultAnimation(String defaultAnimation) {
        this.defaultAnimation = defaultAnimation;
    }
}

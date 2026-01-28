package com.extrascenes.scene;

public class Scene {
    private final String name;
    private final SceneTimeline timeline;
    private final int formatVersion;

    public Scene(String name, SceneTimeline timeline, int formatVersion) {
        this.name = name;
        this.timeline = timeline;
        this.formatVersion = formatVersion;
    }

    public String getName() {
        return name;
    }

    public SceneTimeline getTimeline() {
        return timeline;
    }

    public int getFormatVersion() {
        return formatVersion;
    }
}

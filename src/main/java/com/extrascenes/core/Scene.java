package com.extrascenes.core;

import java.util.HashMap;
import java.util.Map;

public class Scene {
    private String name;
    private int durationTicks;
    private Map<Integer, CameraPoint> cameraPoints = new HashMap<>();
    private Map<String, ActorTemplate> actors = new HashMap<>();

    public Scene() {}

    public Scene(String name, int durationTicks) {
        this.name = name;
        this.durationTicks = durationTicks;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getDurationTicks() { return durationTicks; }
    public void setDurationTicks(int durationTicks) { this.durationTicks = durationTicks; }
    public Map<Integer, CameraPoint> getCameraPoints() { return cameraPoints; }
    public void setCameraPoints(Map<Integer, CameraPoint> cameraPoints) { this.cameraPoints = cameraPoints == null ? new HashMap<>() : cameraPoints; }
    public Map<String, ActorTemplate> getActors() { return actors; }
    public void setActors(Map<String, ActorTemplate> actors) { this.actors = actors == null ? new HashMap<>() : actors; }
}

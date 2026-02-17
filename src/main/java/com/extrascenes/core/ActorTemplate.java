package com.extrascenes.core;

import java.util.HashMap;
import java.util.Map;

public class ActorTemplate {
    private String actorId;
    private String skin;
    private double scale = 1.0d;
    private Map<Integer, Transform> recording = new HashMap<>();

    public ActorTemplate() {}

    public ActorTemplate(String actorId) {
        this.actorId = actorId;
    }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getSkin() { return skin; }
    public void setSkin(String skin) { this.skin = skin; }
    public double getScale() { return scale; }
    public void setScale(double scale) { this.scale = scale; }
    public Map<Integer, Transform> getRecording() { return recording; }
    public void setRecording(Map<Integer, Transform> recording) { this.recording = recording == null ? new HashMap<>() : recording; }
}

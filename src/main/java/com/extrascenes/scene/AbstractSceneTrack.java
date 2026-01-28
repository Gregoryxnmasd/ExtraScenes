package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractSceneTrack implements SceneTrack {
    private final String name;
    private final List<SceneKeyframe> keyframes = new ArrayList<>();

    protected AbstractSceneTrack(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void addKeyframe(SceneKeyframe keyframe) {
        keyframes.add(keyframe);
    }

    @Override
    public List<SceneKeyframe> getKeyframes() {
        return Collections.unmodifiableList(keyframes);
    }
}

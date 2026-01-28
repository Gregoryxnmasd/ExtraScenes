package com.extrascenes.scene;

import java.util.List;

public interface SceneTrack {
    String getName();

    List<SceneKeyframe> getKeyframes();
}

package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SceneTimeline {
    private final int durationTicks;
    private final List<SceneTrack> tracks;

    public SceneTimeline(int durationTicks, List<SceneTrack> tracks) {
        this.durationTicks = durationTicks;
        this.tracks = tracks == null ? new ArrayList<>() : new ArrayList<>(tracks);
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public List<SceneTrack> getTracks() {
        return Collections.unmodifiableList(tracks);
    }
}

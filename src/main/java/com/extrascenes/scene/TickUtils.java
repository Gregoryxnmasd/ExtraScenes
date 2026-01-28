package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TickUtils {
    private TickUtils() {
    }

    public static <T extends Keyframe> List<T> getKeyframesAtTick(Track<T> track, int tick) {
        if (track == null) {
            return List.of();
        }
        List<T> matches = new ArrayList<>();
        for (T keyframe : track.getKeyframes()) {
            if (keyframe.getTimeTicks() == tick) {
                matches.add(keyframe);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    public static <T extends Keyframe> T getFirstKeyframeAtTick(Track<T> track, int tick) {
        if (track == null) {
            return null;
        }
        for (T keyframe : track.getKeyframes()) {
            if (keyframe.getTimeTicks() == tick) {
                return keyframe;
            }
        }
        return null;
    }
}

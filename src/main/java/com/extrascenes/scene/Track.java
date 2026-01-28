package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Track<T extends Keyframe> {
    private final SceneTrackType type;
    private final List<T> keyframes = new ArrayList<>();
    private int revision;

    public Track(SceneTrackType type) {
        this.type = type;
    }

    public SceneTrackType getType() {
        return type;
    }

    public List<T> getKeyframes() {
        return Collections.unmodifiableList(keyframes);
    }

    public int getRevision() {
        return revision;
    }

    public void addKeyframe(T keyframe) {
        insertKeyframe(keyframe);
    }

    public void insertKeyframe(T keyframe) {
        int index = 0;
        while (index < keyframes.size() && keyframes.get(index).getTimeTicks() <= keyframe.getTimeTicks()) {
            index++;
        }
        keyframes.add(index, keyframe);
        revision++;
    }

    public boolean removeKeyframe(UUID keyframeId) {
        boolean removed = keyframes.removeIf(keyframe -> keyframe.getId().equals(keyframeId));
        if (removed) {
            revision++;
        }
        return removed;
    }

    public void clear() {
        if (!keyframes.isEmpty()) {
            keyframes.clear();
            revision++;
        }
    }

    public void moveKeyframe(UUID keyframeId, int newTimeTicks) {
        T target = null;
        for (T keyframe : keyframes) {
            if (keyframe.getId().equals(keyframeId)) {
                target = keyframe;
                break;
            }
        }
        if (target == null) {
            return;
        }
        keyframes.remove(target);
        target.setTimeTicks(Math.max(0, newTimeTicks));
        insertKeyframe(target);
        revision++;
    }

    public T getKeyframe(UUID keyframeId) {
        for (T keyframe : keyframes) {
            if (keyframe.getId().equals(keyframeId)) {
                return keyframe;
            }
        }
        return null;
    }
}

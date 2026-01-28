package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SceneTimeline {
    private final Scene scene;
    private List<CameraSegment> cameraSegments;
    private int lastCameraRevision = -1;

    public SceneTimeline(Scene scene) {
        this.scene = scene;
    }

    public int getDurationTicks() {
        return scene.getDurationTicks();
    }

    public List<CameraSegment> getCameraSegments() {
        Track<CameraKeyframe> cameraTrack = scene.getTrack(SceneTrackType.CAMERA);
        int revision = cameraTrack == null ? 0 : cameraTrack.getRevision();
        if (cameraSegments == null || revision != lastCameraRevision) {
            cameraSegments = buildCameraSegments(cameraTrack == null ? List.of() : cameraTrack.getKeyframes());
            lastCameraRevision = revision;
        }
        return cameraSegments;
    }

    private List<CameraSegment> buildCameraSegments(List<CameraKeyframe> keyframes) {
        if (keyframes == null || keyframes.isEmpty()) {
            return Collections.emptyList();
        }
        List<CameraSegment> segments = new ArrayList<>();
        CameraKeyframe previous = null;
        for (CameraKeyframe current : keyframes) {
            if (previous != null) {
                segments.add(new CameraSegment(previous, current));
            }
            previous = current;
        }
        return segments;
    }

    public record CameraSegment(CameraKeyframe start, CameraKeyframe end) {
    }
}

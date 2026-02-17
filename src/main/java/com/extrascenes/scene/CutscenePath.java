package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CutscenePath {
    private final int durationTicks;
    private final double stepResolution;
    private final SmoothingMode defaultSmoothing;
    private final List<CameraKeyframe> points;
    private final List<IntRange> playerSegments;

    public CutscenePath(int durationTicks, double stepResolution, SmoothingMode defaultSmoothing,
                        List<CameraKeyframe> points, List<IntRange> playerSegments) {
        this.durationTicks = Math.max(1, durationTicks);
        this.stepResolution = stepResolution <= 0.0D ? 0.35D : stepResolution;
        this.defaultSmoothing = defaultSmoothing == null ? SmoothingMode.SMOOTH : defaultSmoothing;
        this.points = points == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(points));
        this.playerSegments = playerSegments == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(playerSegments));
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public double getStepResolution() {
        return stepResolution;
    }

    public SmoothingMode getDefaultSmoothing() {
        return defaultSmoothing;
    }

    public List<CameraKeyframe> getPoints() {
        return points;
    }

    public boolean isPlayerCameraSegment(int segmentIndex) {
        for (IntRange range : playerSegments) {
            if (range.contains(segmentIndex)) {
                return true;
            }
        }
        return false;
    }

    public record IntRange(int startInclusive, int endInclusive) {
        public boolean contains(int value) {
            return value >= startInclusive && value <= endInclusive;
        }
    }
}

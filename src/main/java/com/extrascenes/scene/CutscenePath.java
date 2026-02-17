package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CutscenePath {
    private final int durationTicks;
    private final double stepResolution;
    private final SmoothingMode defaultSmoothing;
    private final List<CameraKeyframe> points;
    private final List<IntRange> playerSegments;
    private final List<String> startCommands;
    private final Map<Integer, List<String>> segmentCommands;

    public CutscenePath(int durationTicks, double stepResolution, SmoothingMode defaultSmoothing,
                        List<CameraKeyframe> points, List<IntRange> playerSegments,
                        List<String> startCommands, Map<Integer, List<String>> segmentCommands) {
        this.durationTicks = Math.max(1, durationTicks);
        this.stepResolution = stepResolution <= 0.0D ? 0.35D : stepResolution;
        this.defaultSmoothing = defaultSmoothing == null ? SmoothingMode.SMOOTH : defaultSmoothing;
        this.points = points == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(points));
        this.playerSegments = playerSegments == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(playerSegments));
        this.startCommands = startCommands == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(startCommands));
        Map<Integer, List<String>> builtSegmentCommands = new LinkedHashMap<>();
        if (segmentCommands != null) {
            for (Map.Entry<Integer, List<String>> entry : segmentCommands.entrySet()) {
                List<String> commands = entry.getValue() == null ? Collections.emptyList() : entry.getValue();
                builtSegmentCommands.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(commands)));
            }
        }
        this.segmentCommands = Collections.unmodifiableMap(builtSegmentCommands);
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

    public List<String> getStartCommands() {
        return startCommands;
    }

    public List<String> getSegmentCommands(int segmentIndex) {
        return segmentCommands.getOrDefault(segmentIndex, Collections.emptyList());
    }

    public record IntRange(int startInclusive, int endInclusive) {
        public boolean contains(int value) {
            return value >= startInclusive && value <= endInclusive;
        }
    }
}

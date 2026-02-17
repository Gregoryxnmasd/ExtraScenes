package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;

public final class CutsceneTimelineBuilder {
    private CutsceneTimelineBuilder() {
    }

    public static List<CutsceneFrame> build(CutscenePath path) {
        if (path == null || path.getPoints().isEmpty()) {
            return Collections.emptyList();
        }
        List<CameraKeyframe> keyframes = new ArrayList<>(path.getPoints());
        keyframes.sort(java.util.Comparator.comparingInt(CameraKeyframe::getTimeTicks));

        List<CutsceneFrame> timeline = new ArrayList<>();
        int totalTicks = Math.max(path.getDurationTicks(), keyframes.get(keyframes.size() - 1).getTimeTicks() + 1);
        for (int tick = 0; tick < totalTicks; tick++) {
            InterpolatedFrame frame = sampleAtTick(path, keyframes, tick);
            if (frame == null) {
                continue;
            }
            timeline.add(new CutsceneFrame(frame.location(), frame.segmentIndex(), path.isPlayerCameraSegment(frame.segmentIndex())));
        }
        return timeline;
    }

    private static InterpolatedFrame sampleAtTick(CutscenePath path, List<CameraKeyframe> keyframes, int tick) {
        if (keyframes.isEmpty()) {
            return null;
        }
        CameraKeyframe previous = keyframes.get(0);
        int prevIndex = 0;
        CameraKeyframe next = keyframes.get(keyframes.size() - 1);

        for (int i = 0; i < keyframes.size(); i++) {
            CameraKeyframe candidate = keyframes.get(i);
            if (candidate.getTimeTicks() <= tick) {
                previous = candidate;
                prevIndex = i;
            }
            if (candidate.getTimeTicks() >= tick) {
                next = candidate;
                break;
            }
        }

        Transform a = previous.getTransform();
        Transform b = next.getTransform();
        if (a == null || b == null) {
            return null;
        }
        int deltaTicks = Math.max(1, next.getTimeTicks() - previous.getTimeTicks());
        double baseT = Math.max(0.0D, Math.min(1.0D, (tick - previous.getTimeTicks()) / (double) deltaTicks));

        SmoothingMode mode = previous.getSmoothingMode() == null ? path.getDefaultSmoothing() : previous.getSmoothingMode();
        double eased = applySmoothing(mode, baseT);

        double x = a.getX() + (b.getX() - a.getX()) * eased;
        double y = a.getY() + (b.getY() - a.getY()) * eased;
        double z = a.getZ() + (b.getZ() - a.getZ()) * eased;

        float yaw = lerpAngle(a.getYaw(), b.getYaw(), (float) eased);
        float pitch = (float) (a.getPitch() + (b.getPitch() - a.getPitch()) * eased);

        return new InterpolatedFrame(new Location(null, x, y, z, yaw, pitch), prevIndex);
    }

    private record InterpolatedFrame(Location location, int segmentIndex) {
    }

    private static double applySmoothing(SmoothingMode mode, double t) {
        if (mode == null || mode == SmoothingMode.LINEAR) {
            return t;
        }
        if (mode == SmoothingMode.INSTANT) {
            return t < 1.0D ? 0.0D : 1.0D;
        }
        return t * t * (3.0D - 2.0D * t);
    }

    private static float lerpAngle(float from, float to, float t) {
        float delta = ((to - from + 540.0F) % 360.0F) - 180.0F;
        return from + delta * t;
    }
}

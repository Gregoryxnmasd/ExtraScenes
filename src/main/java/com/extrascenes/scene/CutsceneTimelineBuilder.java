package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;

public final class CutsceneTimelineBuilder {
    private CutsceneTimelineBuilder() {
    }

    public static List<CutsceneFrame> build(CutscenePath path) {
        if (path == null || path.getPoints().size() < 1) {
            return Collections.emptyList();
        }
        List<CameraKeyframe> keyframes = new ArrayList<>(path.getPoints());
        keyframes.sort(java.util.Comparator.comparingInt(CameraKeyframe::getTimeTicks));

        List<CutsceneFrame> rawFrames = buildRawFrames(path, keyframes);
        if (rawFrames.isEmpty()) {
            return Collections.emptyList();
        }
        return stretchToDuration(path.getDurationTicks(), rawFrames);
    }

    private static List<CutsceneFrame> buildRawFrames(CutscenePath path, List<CameraKeyframe> keyframes) {
        List<CutsceneFrame> frames = new ArrayList<>();
        for (int index = 0; index < keyframes.size() - 1; index++) {
            CameraKeyframe from = keyframes.get(index);
            CameraKeyframe to = keyframes.get(index + 1);
            Transform a = from.getTransform();
            Transform b = to.getTransform();
            if (a == null || b == null) {
                continue;
            }

            double dx = b.getX() - a.getX();
            double dy = b.getY() - a.getY();
            double dz = b.getZ() - a.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            boolean directToPoint = path.isDirectPoint(index + 1);
            int steps = directToPoint ? 1 : Math.max(1, (int) Math.ceil(distance / path.getStepResolution()));
            SmoothingMode mode = from.getSmoothingMode() == null ? path.getDefaultSmoothing() : from.getSmoothingMode();

            for (int step = 0; step < steps; step++) {
                double t = directToPoint ? 1.0D : step / (double) steps;
                double eased = applySmoothing(mode, t);
                frames.add(buildFrame(index, a, b, eased, path));
            }
        }

        CameraKeyframe last = keyframes.get(keyframes.size() - 1);
        if (last.getTransform() != null) {
            Transform transform = last.getTransform();
            Location location = new Location(null, transform.getX(), transform.getY(), transform.getZ(),
                    transform.getYaw(), transform.getPitch());
            frames.add(new CutsceneFrame(location, Math.max(0, keyframes.size() - 2),
                    path.isPlayerCameraSegment(Math.max(0, keyframes.size() - 2))));
        }
        return frames;
    }

    private static CutsceneFrame buildFrame(int segmentIndex, Transform a, Transform b, double eased, CutscenePath path) {
        double x = a.getX() + (b.getX() - a.getX()) * eased;
        double y = a.getY() + (b.getY() - a.getY()) * eased;
        double z = a.getZ() + (b.getZ() - a.getZ()) * eased;
        float yaw = lerpAngle(a.getYaw(), b.getYaw(), (float) eased);
        float pitch = (float) (a.getPitch() + (b.getPitch() - a.getPitch()) * eased);
        Location location = new Location(null, x, y, z, yaw, pitch);
        return new CutsceneFrame(location, segmentIndex, path.isPlayerCameraSegment(segmentIndex));
    }

    private static List<CutsceneFrame> stretchToDuration(int durationTicks, List<CutsceneFrame> rawFrames) {
        int target = Math.max(1, durationTicks);
        if (rawFrames.size() == target) {
            return rawFrames;
        }
        List<CutsceneFrame> resized = new ArrayList<>(target);
        for (int tick = 0; tick < target; tick++) {
            int index = (int) Math.floor((tick / (double) Math.max(1, target - 1)) * Math.max(0, rawFrames.size() - 1));
            resized.add(rawFrames.get(Math.max(0, Math.min(index, rawFrames.size() - 1))));
        }
        return resized;
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

package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class CutsceneTimelineBuilder {
    private CutsceneTimelineBuilder() {
    }

    public static List<CutsceneFrame> build(CutscenePath path) {
        if (path == null || path.getPoints().isEmpty()) {
            return Collections.emptyList();
        }
        if (path.getPoints().size() == 1) {
            Transform transform = path.getPoints().get(0).getTransform();
            Location location = new Location(null, transform.getX(), transform.getY(), transform.getZ(),
                    transform.getYaw(), transform.getPitch());
            return List.of(new CutsceneFrame(location, 0, path.isPlayerCameraSegment(0)));
        }
        List<CutsceneFrame> timeline = new ArrayList<>();
        for (int i = 0; i < path.getPoints().size() - 1; i++) {
            CameraKeyframe from = path.getPoints().get(i);
            CameraKeyframe to = path.getPoints().get(i + 1);
            Transform a = from.getTransform();
            Transform b = to.getTransform();
            Vector va = new Vector(a.getX(), a.getY(), a.getZ());
            Vector vb = new Vector(b.getX(), b.getY(), b.getZ());
            double distance = va.distance(vb);
            int steps = Math.max(1, (int) Math.ceil(distance / path.getStepResolution()));
            SmoothingMode mode = from.getSmoothingMode() == null ? path.getDefaultSmoothing() : from.getSmoothingMode();
            for (int step = 0; step < steps; step++) {
                double t = step / (double) steps;
                double eased = applySmoothing(mode, t);
                double x = a.getX() + (b.getX() - a.getX()) * eased;
                double y = a.getY() + (b.getY() - a.getY()) * eased;
                double z = a.getZ() + (b.getZ() - a.getZ()) * eased;
                float yaw = lerpAngle(a.getYaw(), b.getYaw(), (float) eased);
                float pitch = (float) (a.getPitch() + (b.getPitch() - a.getPitch()) * eased);
                Location point = new Location(null, x, y, z, yaw, pitch);
                timeline.add(new CutsceneFrame(point, i, path.isPlayerCameraSegment(i)));
            }
        }

        CameraKeyframe last = path.getPoints().get(path.getPoints().size() - 1);
        Transform t = last.getTransform();
        timeline.add(new CutsceneFrame(new Location(null, t.getX(), t.getY(), t.getZ(), t.getYaw(), t.getPitch()),
                path.getPoints().size() - 1,
                path.isPlayerCameraSegment(path.getPoints().size() - 1)));
        return timeline;
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

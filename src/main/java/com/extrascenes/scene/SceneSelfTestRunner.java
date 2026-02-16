package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.Text;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SceneSelfTestRunner {
    private static final int STABILITY_TICKS = 40;
    private static final int SYNTHETIC_DURATION_TICKS = 60;
    private static final double MAX_POSITION_DELTA = 1.75;
    private static final float MAX_YAW_DELTA = 30.0f;

    private final ExtraScenesPlugin plugin;
    private final SceneSessionManager sessionManager;

    public SceneSelfTestRunner(ExtraScenesPlugin plugin, SceneSessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }

    public void run(Player player, String requestedSceneName, Scene loadedScene) {
        if (player == null) {
            return;
        }
        Scene scene = loadedScene;
        boolean synthetic = false;
        String sceneLabel = requestedSceneName == null || requestedSceneName.isBlank()
                ? (loadedScene != null ? loadedScene.getName() : "unknown")
                : requestedSceneName;

        if (scene == null || cameraPointCount(scene) < 2) {
            scene = buildSyntheticScene(player, sceneLabel);
            synthetic = true;
        }

        int endTick = Math.max(STABILITY_TICKS + 4, Math.min(scene.getDurationTicks(), 120));
        SceneSession session = sessionManager.startScene(player, scene, false, 0, endTick);
        UUID rigId = session.getCameraRigId();
        int forcedChunkX = session.getForcedChunkX();
        int forcedChunkZ = session.getForcedChunkZ();
        String forcedChunkWorld = session.getForcedChunkWorld();

        Text.send(player, String.format(Locale.ROOT,
                "&7SELFTEST_HEADER viewer=%s scene=%s sessionId=%s world=%s rigUUID=%s",
                player.getName(),
                scene.getName(),
                session.getSessionId(),
                player.getWorld().getName(),
                rigId));
        Text.send(player, synthetic
                ? "&7selftest mode: synthetic camera path (fallback due to missing/invalid scene path)"
                : "&7selftest mode: scene camera path");

        new BukkitRunnable() {
            int sampledTicks = 0;
            int stableTicks = 0;
            boolean forcedLoadedDuringRun = true;
            boolean spectatorAlways = true;
            boolean rigExists = true;
            boolean rigWorldMatch = true;
            boolean rigMarkerFalse = true;
            boolean spectatorDropObserved = false;
            boolean stopRequested = false;
            int postStopWaitTicks = 0;
            int targetDropTick = -1;
            UUID targetDropTargetId;
            boolean targetDropRigChunkLoaded;
            boolean targetDropRigValid;
            boolean targetDropRigDead;
            int smoothFailTick = -1;
            double smoothFailPosDelta = 0.0d;
            float smoothFailYawDelta = 0.0f;
            Location lastRigLocation;
            final Set<UUID> seenSessionEntities = new LinkedHashSet<>();

            @Override
            public void run() {
                SceneSession active = sessionManager.getSession(player.getUniqueId());
                if (active == null) {
                    if (!stopRequested) {
                        cancel();
                        return;
                    }
                    postStopWaitTicks++;
                    if (postStopWaitTicks < 2) {
                        return;
                    }
                    emitReport(player, session, sceneLabel,
                            forcedChunkWorld, forcedChunkX, forcedChunkZ,
                            sampledTicks, stableTicks,
                            spectatorDropObserved, targetDropTick, targetDropTargetId,
                            targetDropRigChunkLoaded, targetDropRigValid, targetDropRigDead,
                            forcedLoadedDuringRun,
                            spectatorAlways,
                            rigExists, rigWorldMatch, rigMarkerFalse,
                            smoothFailTick, smoothFailPosDelta, smoothFailYawDelta,
                            seenSessionEntities);
                    cancel();
                    return;
                }

                seenSessionEntities.addAll(active.getEntityTracker().getTrackedEntityIds());
                Entity rig = Bukkit.getEntity(rigId);
                if (rig == null || !rig.isValid()) {
                    rigExists = false;
                    sessionManager.stopScene(player, "selftest_rig_missing");
                    stopRequested = true;
                    return;
                }

                boolean worldMatches = rig.getWorld() != null && rig.getWorld().equals(player.getWorld());
                rigWorldMatch &= worldMatches;
                rigMarkerFalse &= !rig.isMarker();

                World forcedWorld = forcedChunkWorld == null ? null : Bukkit.getWorld(forcedChunkWorld);
                if (forcedWorld == null || !forcedWorld.isChunkForceLoaded(forcedChunkX, forcedChunkZ)) {
                    forcedLoadedDuringRun = false;
                }

                if (player.getGameMode() != GameMode.SPECTATOR) {
                    spectatorAlways = false;
                }

                Entity target = player.getSpectatorTarget();
                boolean targetMatchesRig = target != null && target.getUniqueId().equals(rigId);
                if (targetMatchesRig) {
                    stableTicks++;
                } else {
                    spectatorDropObserved = true;
                    if (targetDropTick < 0) {
                        targetDropTick = sampledTicks;
                        targetDropTargetId = target == null ? null : target.getUniqueId();
                        targetDropRigChunkLoaded = rig.getWorld() != null
                                && rig.getWorld().isChunkLoaded(rig.getLocation().getBlockX() >> 4, rig.getLocation().getBlockZ() >> 4);
                        targetDropRigValid = rig.isValid();
                        targetDropRigDead = rig.isDead();
                    }
                }

                if (lastRigLocation != null) {
                    double deltaPos = rig.getLocation().distance(lastRigLocation);
                    float deltaYaw = Math.abs(wrapDegrees(rig.getLocation().getYaw() - lastRigLocation.getYaw()));
                    if (smoothFailTick < 0 && (deltaPos > MAX_POSITION_DELTA || deltaYaw > MAX_YAW_DELTA)) {
                        smoothFailTick = sampledTicks;
                        smoothFailPosDelta = deltaPos;
                        smoothFailYawDelta = deltaYaw;
                    }
                }
                lastRigLocation = rig.getLocation().clone();

                sampledTicks++;
                if (sampledTicks >= STABILITY_TICKS) {
                    sessionManager.stopScene(player, "selftest_complete");
                    stopRequested = true;
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void emitReport(Player player,
                            SceneSession session,
                            String requestedSceneName,
                            String forcedChunkWorld,
                            int forcedChunkX,
                            int forcedChunkZ,
                            int sampledTicks,
                            int stableTicks,
                            boolean spectatorDropObserved,
                            int targetDropTick,
                            UUID targetDropTargetId,
                            boolean targetDropRigChunkLoaded,
                            boolean targetDropRigValid,
                            boolean targetDropRigDead,
                            boolean forcedLoadedDuringRun,
                            boolean spectatorAlways,
                            boolean rigExists,
                            boolean rigWorldMatch,
                            boolean rigMarkerFalse,
                            int smoothFailTick,
                            double smoothFailPosDelta,
                            float smoothFailYawDelta,
                            Set<UUID> seenSessionEntities) {
        List<String> failures = new ArrayList<>();

        boolean rigCheckPass = rigExists && rigWorldMatch && rigMarkerFalse;
        sendCheck(player, rigCheckPass,
                "rig exists, correct world, marker=false",
                rigCheckPass
                        ? "rig valid and world aligned"
                        : "rigExists=" + rigExists + ", worldMatch=" + rigWorldMatch + ", markerFalse=" + rigMarkerFalse);
        if (!rigCheckPass) {
            failures.add("rig");
        }

        World forcedWorld = forcedChunkWorld == null ? null : Bukkit.getWorld(forcedChunkWorld);
        boolean released = forcedWorld == null || !forcedWorld.isChunkForceLoaded(forcedChunkX, forcedChunkZ);
        boolean chunkCheckPass = forcedLoadedDuringRun && released;
        sendCheck(player, chunkCheckPass,
                "chunk forceLoaded true during run, false after teardown",
                "during=" + forcedLoadedDuringRun + ", released=" + released + ", chunk="
                        + forcedChunkWorld + ":" + forcedChunkX + "," + forcedChunkZ);
        if (!chunkCheckPass) {
            failures.add("chunk");
        }

        sendCheck(player, spectatorAlways,
                "gamemode spectator",
                spectatorAlways ? "player remained spectator during run" : "player left spectator at least once");
        if (!spectatorAlways) {
            failures.add("gamemode");
        }

        boolean handshakePass = session.isSpectatorHandshakeComplete() && session.getSpectatorHandshakeAttempts() <= 10;
        sendCheck(player, handshakePass,
                "handshake success within <=10 attempts",
                "complete=" + session.isSpectatorHandshakeComplete() + ", attempts=" + session.getSpectatorHandshakeAttempts());
        if (!handshakePass) {
            failures.add("handshake");
        }

        boolean stabilityPass = stableTicks >= STABILITY_TICKS && !spectatorDropObserved;
        sendCheck(player, stabilityPass,
                "spectatorTargetMatchesRig true for >=40 consecutive ticks",
                "stableTicks=" + stableTicks + "/" + STABILITY_TICKS + ", sampled=" + sampledTicks);
        if (!stabilityPass) {
            failures.add("stability");
        }

        if (spectatorDropObserved) {
            Text.send(player, String.format(Locale.ROOT,
                    "&cFAIL spectator target drop diagnostics: tick=%d target=%s rigChunkLoaded=%s rigValid=%s rigDead=%s",
                    targetDropTick,
                    targetDropTargetId == null ? "null" : targetDropTargetId,
                    targetDropRigChunkLoaded,
                    targetDropRigValid,
                    targetDropRigDead));
        }

        boolean teleportPass = session.getPlaybackTeleportCount() == 0;
        sendCheck(player, teleportPass,
                "playbackTeleportCount == 0 during PLAYING ticks",
                teleportPass
                        ? "no player teleports during playback"
                        : "count=" + session.getPlaybackTeleportCount() + ", caller=" + session.getLastPlaybackTeleportCaller());
        if (!teleportPass) {
            failures.add("teleport");
        }

        boolean smoothPass = smoothFailTick < 0;
        sendCheck(player, smoothPass,
                "camera smooth delta threshold",
                smoothPass
                        ? "no transform spikes detected"
                        : String.format(Locale.ROOT, "tick=%d posDelta=%.4f yawDelta=%.3f",
                        smoothFailTick, smoothFailPosDelta, smoothFailYawDelta));
        if (!smoothPass) {
            failures.add("smooth");
        }

        boolean spectatorCleared = player.getSpectatorTarget() == null;
        sendCheck(player, spectatorCleared,
                "teardown spectator cleared",
                spectatorCleared ? "spectator target is null" : "target=" + player.getSpectatorTarget().getUniqueId());
        if (!spectatorCleared) {
            failures.add("spectator_clear");
        }

        boolean rigRemoved = session.getCameraRigId() == null
                || Bukkit.getEntity(session.getCameraRigId()) == null
                || !Bukkit.getEntity(session.getCameraRigId()).isValid();
        sendCheck(player, rigRemoved,
                "teardown rig removed",
                rigRemoved ? "camera rig removed" : "rig still valid=" + Bukkit.getEntity(session.getCameraRigId()).isValid());
        if (!rigRemoved) {
            failures.add("rig_removed");
        }

        boolean cleanupPass = true;
        for (UUID entityId : seenSessionEntities) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                cleanupPass = false;
                break;
            }
        }
        sendCheck(player, cleanupPass,
                "teardown no preview/runtime entities left",
                cleanupPass ? "all tracked entities removed" : "at least one tracked entity still valid");
        if (!cleanupPass) {
            failures.add("cleanup");
        }

        if (failures.isEmpty()) {
            Text.send(player, "&aSELFTEST_RESULT PASS scene=" + requestedSceneName + " checks=10");
        } else {
            Text.send(player, "&cSELFTEST_RESULT FAIL scene=" + requestedSceneName + " failedChecks=" + String.join(",", failures));
        }
    }

    private void sendCheck(Player player, boolean pass, String check, String reason) {
        String status = pass ? "&aPASS" : "&cFAIL";
        Text.send(player, status + " &7" + check + " &f- " + reason);
    }

    private Scene buildSyntheticScene(Player player, String requestedName) {
        Map<SceneTrackType, Track<? extends Keyframe>> tracks = new EnumMap<>(SceneTrackType.class);
        Track<CameraKeyframe> cameraTrack = new Track<>(SceneTrackType.CAMERA);

        Location base = player.getLocation().clone();
        String worldName = base.getWorld() == null ? player.getWorld().getName() : base.getWorld().getName();
        Transform first = new Transform(base.getX(), base.getY() + 1.6, base.getZ(), base.getYaw(), base.getPitch(), worldName);
        Transform second = new Transform(base.getX() + 2.5, base.getY() + 1.6, base.getZ() + 2.5,
                base.getYaw() + 30.0f, base.getPitch(), worldName);

        cameraTrack.addKeyframe(new CameraKeyframe(UUID.randomUUID(), 0, first, SmoothingMode.SMOOTH, false, LookAtTarget.none()));
        cameraTrack.addKeyframe(new CameraKeyframe(UUID.randomUUID(), SYNTHETIC_DURATION_TICKS, second,
                SmoothingMode.SMOOTH, false, LookAtTarget.none()));
        tracks.put(SceneTrackType.CAMERA, cameraTrack);

        Scene scene = new Scene("selftest-synthetic-" + player.getUniqueId(),
                requestedName + "_synthetic",
                SYNTHETIC_DURATION_TICKS,
                1,
                tracks);
        scene.setSmoothingQuality(SmoothingQuality.SMOOTH);
        scene.setFreezePlayer(true);
        scene.setEndTeleportMode(EndTeleportMode.NONE);
        return scene;
    }

    private int cameraPointCount(Scene scene) {
        if (scene == null) {
            return 0;
        }
        Track<CameraKeyframe> cameraTrack = scene.getTrack(SceneTrackType.CAMERA);
        if (cameraTrack == null) {
            return 0;
        }
        return cameraTrack.getKeyframes().size();
    }

    private float wrapDegrees(float degrees) {
        float result = degrees % 360.0f;
        if (result >= 180.0f) {
            result -= 360.0f;
        }
        if (result < -180.0f) {
            result += 360.0f;
        }
        return result;
    }
}

package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.SceneProtocolAdapter;
import com.extrascenes.visibility.SceneVisibilityController;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SceneRuntimeEngine {
    private final ExtraScenesPlugin plugin;
    private final SceneSessionManager sessionManager;
    private final SceneVisibilityController visibilityController;
    private final SceneProtocolAdapter protocolAdapter;
    private BukkitRunnable task;

    public SceneRuntimeEngine(ExtraScenesPlugin plugin, SceneSessionManager sessionManager,
                              SceneVisibilityController visibilityController,
                              SceneProtocolAdapter protocolAdapter) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.visibilityController = visibilityController;
        this.protocolAdapter = protocolAdapter;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        };
        task.runTaskTimer(plugin, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        for (SceneSession session : sessionManager.getActiveSessions()) {
            if (session.getState() != SceneState.PLAYING) {
                continue;
            }
            Player player = Bukkit.getPlayer(session.getPlayerId());
            if (player == null) {
                continue;
            }

            session.incrementTime();
            int time = session.getTimeTicks();
            int duration = session.getScene().getTimeline().getDurationTicks();

            if (duration > 0 && time >= duration) {
                sessionManager.stopScene(player, "finished");
                continue;
            }

            Entity cameraRig = getCameraRig(session);
            if (cameraRig != null) {
                visibilityController.hideEntityFromAllExcept(cameraRig, player);
                if ("PACKET".equalsIgnoreCase(plugin.getConfig().getString("camera.mode", "SPECTATOR"))) {
                    protocolAdapter.sendCameraPacket(player, cameraRig);
                }
            }

            for (SceneTrack track : session.getScene().getTimeline().getTracks()) {
                for (SceneKeyframe keyframe : track.getKeyframes()) {
                    if (keyframe.getTimeTicks() == time) {
                        handleKeyframe(player, session, track, keyframe, duration);
                    }
                }
            }
        }
    }

    private Entity getCameraRig(SceneSession session) {
        if (session.getCameraRigId() == null) {
            return null;
        }
        Player player = Bukkit.getPlayer(session.getPlayerId());
        if (player == null) {
            return null;
        }
        return player.getWorld().getEntity(session.getCameraRigId());
    }

    private void handleKeyframe(Player player, SceneSession session, SceneTrack track,
                                SceneKeyframe keyframe, int durationTicks) {
        if (track instanceof SceneCommandTrack) {
            executeCommands(player, session, keyframe, durationTicks);
        } else if (track instanceof SceneCameraTrack) {
            applyCameraKeyframe(player, session, keyframe);
        }
        Bukkit.getPluginManager().callEvent(new SceneKeyframeEvent(player, session.getScene(), keyframe));
    }

    private void applyCameraKeyframe(Player player, SceneSession session, SceneKeyframe keyframe) {
        Entity cameraRig = getCameraRig(session);
        if (cameraRig == null) {
            return;
        }
        Location location = player.getLocation().clone();
        cameraRig.teleport(location);
        session.setLastCameraLocation(location);
    }

    private void executeCommands(Player player, SceneSession session, SceneKeyframe keyframe, int durationTicks) {
        List<String> commands = keyframe.getCommands();
        boolean allowGlobal = plugin.getConfig().getBoolean("commands.allowGlobalDefault", false);
        for (String command : commands) {
            String resolved = SceneTextFormatter.replacePlaceholders(plugin, player, session, command, durationTicks);
            if (resolved.startsWith("console:")) {
                boolean allowForKeyframe = resolved.contains("allowGlobal=true") || allowGlobal;
                if (allowForKeyframe) {
                    String sanitized = resolved.replace("allowGlobal=true", "").trim();
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), sanitized.substring("console:".length()).trim());
                }
            } else {
                Bukkit.dispatchCommand(player, resolved.startsWith("player:")
                        ? resolved.substring("player:".length()).trim()
                        : resolved);
            }
        }
    }
}

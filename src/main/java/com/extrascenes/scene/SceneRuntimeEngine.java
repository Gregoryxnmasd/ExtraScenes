package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.SceneModelTrackAdapter;
import com.extrascenes.SceneProtocolAdapter;
import com.extrascenes.visibility.SceneVisibilityController;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
            int duration = session.getScene().getDurationTicks();

            if (duration > 0 && time >= duration) {
                sessionManager.stopScene(player, "finished");
                continue;
            }

            updateCamera(player, session, time);
            handleKeyframes(player, session, time, duration);
        }
    }

    private void updateCamera(Player player, SceneSession session, int timeTicks) {
        Entity cameraRig = getCameraRig(session, player);
        if (cameraRig == null) {
            return;
        }
        visibilityController.hideEntityFromAllExcept(cameraRig, player);
        if ("PACKET".equalsIgnoreCase(session.getScene().getCameraMode())) {
            protocolAdapter.sendCameraPacket(player, cameraRig);
        }

        Track<CameraKeyframe> cameraTrack = session.getScene().getTrack(SceneTrackType.CAMERA);
        if (cameraTrack == null || cameraTrack.getKeyframes().isEmpty()) {
            return;
        }
        Transform transform = interpolateCamera(cameraTrack.getKeyframes(), timeTicks);
        if (transform == null) {
            return;
        }
        Location location = player.getLocation().clone();
        transform.applyTo(location);
        cameraRig.teleport(location);
        session.setLastCameraLocation(location);
    }

    private Transform interpolateCamera(List<CameraKeyframe> keyframes, int timeTicks) {
        if (keyframes.isEmpty()) {
            return null;
        }
        CameraKeyframe previous = null;
        CameraKeyframe next = null;
        for (CameraKeyframe keyframe : keyframes) {
            if (keyframe.getTimeTicks() <= timeTicks) {
                previous = keyframe;
            }
            if (keyframe.getTimeTicks() >= timeTicks) {
                next = keyframe;
                break;
            }
        }
        if (previous == null) {
            previous = keyframes.get(0);
        }
        if (next == null) {
            next = keyframes.get(keyframes.size() - 1);
        }
        if (previous == next || previous.isInstant()) {
            return previous.getTransform();
        }
        int startTime = previous.getTimeTicks();
        int endTime = Math.max(startTime + 1, next.getTimeTicks());
        float t = (timeTicks - startTime) / (float) (endTime - startTime);
        t = applySmoothing(previous.getSmoothingMode(), t);
        return lerpTransform(previous.getTransform(), next.getTransform(), t);
    }

    private float applySmoothing(SmoothingMode mode, float t) {
        return switch (mode) {
            case NONE -> 0.0f;
            case LINEAR -> t;
            case SMOOTHSTEP -> t * t * (3.0f - 2.0f * t);
            case EASE_IN -> t * t;
            case EASE_OUT -> t * (2.0f - t);
            case EASE_IN_OUT -> t < 0.5f ? 2.0f * t * t : -1.0f + (4.0f - 2.0f * t) * t;
            case CATMULL_ROM -> t * t * (2.0f - t);
        };
    }

    private Transform lerpTransform(Transform from, Transform to, float t) {
        if (from == null || to == null) {
            return from != null ? from : to;
        }
        double x = from.getX() + (to.getX() - from.getX()) * t;
        double y = from.getY() + (to.getY() - from.getY()) * t;
        double z = from.getZ() + (to.getZ() - from.getZ()) * t;
        float yaw = from.getYaw() + (to.getYaw() - from.getYaw()) * t;
        float pitch = from.getPitch() + (to.getPitch() - from.getPitch()) * t;
        return new Transform(x, y, z, yaw, pitch);
    }

    private void handleKeyframes(Player player, SceneSession session, int time, int durationTicks) {
        for (Track<? extends Keyframe> track : session.getScene().getTracks().values()) {
            for (Keyframe keyframe : track.getKeyframes()) {
                if (keyframe.getTimeTicks() == time) {
                    handleKeyframe(player, session, keyframe, durationTicks);
                }
            }
        }
    }

    private void handleKeyframe(Player player, SceneSession session, Keyframe keyframe, int durationTicks) {
        if (keyframe instanceof CommandKeyframe command) {
            executeCommands(player, session, command, durationTicks);
        } else if (keyframe instanceof ModelKeyframe model) {
            handleModelKeyframe(player, session, model);
        } else if (keyframe instanceof ParticleKeyframe particle) {
            handleParticleKeyframe(player, particle);
        } else if (keyframe instanceof SoundKeyframe sound) {
            handleSoundKeyframe(player, sound);
        } else if (keyframe instanceof BlockIllusionKeyframe block) {
            handleBlockIllusionKeyframe(player, block);
        }
        Bukkit.getPluginManager().callEvent(new SceneKeyframeEvent(player, session.getScene(), keyframe));
    }

    private void executeCommands(Player player, SceneSession session, CommandKeyframe keyframe, int durationTicks) {
        List<String> commands = keyframe.getCommands();
        boolean allowGlobalDefault = session.getScene().isAllowGlobalCommands();
        for (String command : commands) {
            String resolved = SceneTextFormatter.replacePlaceholders(plugin, player, session, command, durationTicks);
            if (keyframe.getExecutorMode() == CommandKeyframe.ExecutorMode.CONSOLE) {
                boolean allowConsole = keyframe.isAllowGlobal() || allowGlobalDefault;
                if (session.isPreview() && !keyframe.isAllowGlobal()) {
                    continue;
                }
                if (allowConsole) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved.replaceFirst("^console:", "").trim());
                }
            } else {
                String sanitized = resolved.replaceFirst("^player:", "").trim();
                Bukkit.dispatchCommand(player, sanitized);
            }
        }
    }

    private void handleModelKeyframe(Player player, SceneSession session, ModelKeyframe keyframe) {
        SceneModelTrackAdapter adapter = plugin.getModelTrackAdapter();
        if (adapter == null || !adapter.isModelEngineAvailable()) {
            player.sendMessage(ChatColor.RED + "ModelEngine not installed; model keyframe skipped.");
            return;
        }
        switch (keyframe.getAction()) {
            case SPAWN -> {
                Transform transform = keyframe.getSpawnTransform();
                Location location = player.getLocation().clone();
                if (transform != null) {
                    transform.applyTo(location);
                }
                Entity base = adapter.spawnModelBase(player, location);
                adapter.bindModel(base, keyframe.getModelId());
                session.registerEntity(base);
                sessionManager.registerSceneEntity(session, base);
                if (keyframe.getEntityRef() != null) {
                    session.registerModelRef(keyframe.getEntityRef(), base.getUniqueId());
                }
            }
            case ANIM -> {
                UUID entityId = session.getModelEntityId(keyframe.getEntityRef());
                if (entityId != null) {
                    Entity entity = player.getWorld().getEntity(entityId);
                    if (entity != null) {
                        adapter.playAnimation(entity, keyframe.getAnimationId(), keyframe.isLoop(), keyframe.getSpeed());
                    }
                }
            }
            case STOP -> {
                UUID entityId = session.getModelEntityId(keyframe.getEntityRef());
                if (entityId != null) {
                    Entity entity = player.getWorld().getEntity(entityId);
                    if (entity != null) {
                        adapter.stopAnimation(entity, keyframe.getAnimationId());
                    }
                }
            }
            case DESPAWN -> {
                UUID entityId = session.getModelEntityId(keyframe.getEntityRef());
                if (entityId != null) {
                    Entity entity = player.getWorld().getEntity(entityId);
                    if (entity != null) {
                        session.unregisterEntity(entity);
                        sessionManager.unregisterSceneEntity(entity);
                        entity.remove();
                    }
                }
            }
            default -> {
            }
        }
    }

    private void handleParticleKeyframe(Player player, ParticleKeyframe keyframe) {
        if (keyframe.getParticleId() == null) {
            return;
        }
        Transform transform = keyframe.getTransform();
        Location location = player.getLocation().clone();
        if (transform != null) {
            transform.applyTo(location);
        }
        try {
            Particle particle = Particle.valueOf(keyframe.getParticleId().toUpperCase());
            player.spawnParticle(particle, location, 10, 0.2, 0.2, 0.2);
        } catch (IllegalArgumentException ex) {
            // ignore invalid particle
        }
    }

    private void handleSoundKeyframe(Player player, SoundKeyframe keyframe) {
        if (keyframe.getSoundId() == null) {
            return;
        }
        Transform transform = keyframe.getTransform();
        Location location = player.getLocation().clone();
        if (transform != null) {
            transform.applyTo(location);
        }
        try {
            Sound sound = Sound.valueOf(keyframe.getSoundId().toUpperCase());
            player.playSound(location, sound, keyframe.getVolume(), keyframe.getPitch());
        } catch (IllegalArgumentException ex) {
            player.playSound(location, keyframe.getSoundId(), keyframe.getVolume(), keyframe.getPitch());
        }
    }

    private void handleBlockIllusionKeyframe(Player player, BlockIllusionKeyframe keyframe) {
        Transform transform = keyframe.getTransform();
        if (transform == null) {
            return;
        }
        Location location = player.getLocation().clone();
        transform.applyTo(location);
        Material material = keyframe.getMaterial();
        player.sendBlockChange(location, material.createBlockData());
    }

    private Entity getCameraRig(SceneSession session, Player player) {
        if (session.getCameraRigId() == null) {
            return null;
        }
        return player.getWorld().getEntity(session.getCameraRigId());
    }
}

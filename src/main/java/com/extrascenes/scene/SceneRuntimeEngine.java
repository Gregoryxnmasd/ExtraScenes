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
import org.bukkit.scheduler.BukkitTask;

public class SceneRuntimeEngine {
    private final ExtraScenesPlugin plugin;
    private final SceneSessionManager sessionManager;
    private final SceneVisibilityController visibilityController;
    private final SceneProtocolAdapter protocolAdapter;

    public SceneRuntimeEngine(ExtraScenesPlugin plugin, SceneSessionManager sessionManager,
                              SceneVisibilityController visibilityController,
                              SceneProtocolAdapter protocolAdapter) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.visibilityController = visibilityController;
        this.protocolAdapter = protocolAdapter;
    }

    public void start() {
        // per-session tasks handle playback
    }

    public void stop() {
        for (SceneSession session : sessionManager.getActiveSessions()) {
            stopSession(session);
        }
    }

    public void startSession(SceneSession session) {
        if (session.getRuntimeTask() != null) {
            return;
        }
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                tickSession(session);
            }
        }.runTaskTimer(plugin, 1L, 1L);
        session.setRuntimeTask(task);
    }

    public void stopSession(SceneSession session) {
        if (session == null) {
            return;
        }
        BukkitTask task = session.getRuntimeTask();
        if (task != null) {
            task.cancel();
            session.setRuntimeTask(null);
        }
    }

    private void tickSession(SceneSession session) {
        if (session.getState() != SceneState.PLAYING) {
            return;
        }
        Player player = Bukkit.getPlayer(session.getPlayerId());
        if (player == null) {
            return;
        }

        int time = session.getTimeTicks();
        int duration = session.getScene().getDurationTicks();
        int endTick = session.getEndTick();
        if (duration > 0) {
            endTick = Math.min(endTick, duration);
        }

        if (time >= endTick) {
            sessionManager.stopScene(player, "finished");
            return;
        }

        updateCamera(player, session, time);
        handleKeyframes(player, session, time, duration);
        tickActionBar(player, session, time);
        session.incrementTime();
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
        Transform transform = interpolateCamera(player, session, cameraTrack.getKeyframes(), timeTicks);
        if (transform == null) {
            return;
        }
        Location location = player.getLocation().clone();
        transform.applyTo(location);
        cameraRig.teleport(location);
        session.setLastCameraLocation(location);
    }

    private Transform interpolateCamera(Player player, SceneSession session, List<CameraKeyframe> keyframes, int timeTicks) {
        if (keyframes.isEmpty()) {
            return null;
        }
        SmoothingQuality quality = session.getScene().getSmoothingQuality();
        int subSamples = Math.max(1, quality.getSubSamples());
        Transform result = null;
        for (int i = 0; i < subSamples; i++) {
            float sampleTime = timeTicks + ((i + 1f) / subSamples);
            result = sampleCameraTransform(player, session, keyframes, sampleTime, quality);
        }
        return result;
    }

    private Transform sampleCameraTransform(Player player, SceneSession session, List<CameraKeyframe> keyframes,
                                            float timeTicks, SmoothingQuality quality) {
        CameraKeyframe previous = null;
        CameraKeyframe next = null;
        int prevIndex = 0;
        int nextIndex = keyframes.size() - 1;
        for (int i = 0; i < keyframes.size(); i++) {
            CameraKeyframe keyframe = keyframes.get(i);
            if (keyframe.getTimeTicks() <= timeTicks) {
                previous = keyframe;
                prevIndex = i;
            }
            if (keyframe.getTimeTicks() >= timeTicks) {
                next = keyframe;
                nextIndex = i;
                break;
            }
        }
        if (previous == null) {
            previous = keyframes.get(0);
            prevIndex = 0;
        }
        if (next == null) {
            next = keyframes.get(keyframes.size() - 1);
            nextIndex = keyframes.size() - 1;
        }
        if (previous == next || previous.getSmoothingMode() == SmoothingMode.INSTANT) {
            return previous.getTransform();
        }

        int startTime = previous.getTimeTicks();
        int endTime = Math.max(startTime + 1, next.getTimeTicks());
        float t = (timeTicks - startTime) / (float) (endTime - startTime);
        t = applySmoothing(previous.getSmoothingMode(), t);

        Transform position = quality.isSplinePosition()
                ? catmullRomTransform(keyframes, prevIndex, nextIndex, t)
                : lerpTransform(previous.getTransform(), next.getTransform(), t);
        if (position == null) {
            return null;
        }

        float yaw = lerpAngle(previous.getTransform().getYaw(), next.getTransform().getYaw(), t);
        float pitch = lerpAngle(previous.getTransform().getPitch(), next.getTransform().getPitch(), t);

        LookAtTarget lookAt = previous.getLookAt();
        if (lookAt != null && lookAt.getMode() != LookAtTarget.Mode.NONE) {
            float[] lookAngles = resolveLookAt(player, session, position, lookAt);
            yaw = dampAngle(yaw, lookAngles[0], 0.85f);
            pitch = dampAngle(pitch, lookAngles[1], 0.85f);
            pitch = clampPitch(pitch);
        } else if (quality.isLookAhead()) {
            Transform ahead = sampleCameraTransform(player, session, keyframes, timeTicks + quality.getLookAheadTicks(),
                    SmoothingQuality.NORMAL);
            if (ahead != null) {
                float[] lookAngles = lookAtAngles(position, ahead);
                yaw = dampAngle(yaw, lookAngles[0], 0.85f);
                pitch = dampAngle(pitch, lookAngles[1], 0.85f);
            }
        }

        pitch = clampPitch(pitch);
        return new Transform(position.getX(), position.getY(), position.getZ(), yaw, pitch);
    }

    private float applySmoothing(SmoothingMode mode, float t) {
        return switch (mode) {
            case INSTANT -> 0.0f;
            case LINEAR -> t;
            case SMOOTH -> t < 0.5f
                    ? 16.0f * t * t * t * t * t
                    : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 5) / 2.0f;
        };
    }

    private Transform lerpTransform(Transform from, Transform to, float t) {
        if (from == null || to == null) {
            return from != null ? from : to;
        }
        double x = from.getX() + (to.getX() - from.getX()) * t;
        double y = from.getY() + (to.getY() - from.getY()) * t;
        double z = from.getZ() + (to.getZ() - from.getZ()) * t;
        float yaw = lerpAngle(from.getYaw(), to.getYaw(), t);
        float pitch = lerpAngle(from.getPitch(), to.getPitch(), t);
        return new Transform(x, y, z, yaw, pitch);
    }

    private Transform catmullRomTransform(List<CameraKeyframe> keyframes, int prevIndex, int nextIndex, float t) {
        Transform p0 = keyframes.get(Math.max(prevIndex - 1, 0)).getTransform();
        Transform p1 = keyframes.get(prevIndex).getTransform();
        Transform p2 = keyframes.get(nextIndex).getTransform();
        Transform p3 = keyframes.get(Math.min(nextIndex + 1, keyframes.size() - 1)).getTransform();
        if (p0 == null || p1 == null || p2 == null || p3 == null) {
            return lerpTransform(p1, p2, t);
        }
        double t2 = t * t;
        double t3 = t2 * t;
        double x = 0.5 * ((2.0 * p1.getX())
                + (-p0.getX() + p2.getX()) * t
                + (2.0 * p0.getX() - 5.0 * p1.getX() + 4.0 * p2.getX() - p3.getX()) * t2
                + (-p0.getX() + 3.0 * p1.getX() - 3.0 * p2.getX() + p3.getX()) * t3);
        double y = 0.5 * ((2.0 * p1.getY())
                + (-p0.getY() + p2.getY()) * t
                + (2.0 * p0.getY() - 5.0 * p1.getY() + 4.0 * p2.getY() - p3.getY()) * t2
                + (-p0.getY() + 3.0 * p1.getY() - 3.0 * p2.getY() + p3.getY()) * t3);
        double z = 0.5 * ((2.0 * p1.getZ())
                + (-p0.getZ() + p2.getZ()) * t
                + (2.0 * p0.getZ() - 5.0 * p1.getZ() + 4.0 * p2.getZ() - p3.getZ()) * t2
                + (-p0.getZ() + 3.0 * p1.getZ() - 3.0 * p2.getZ() + p3.getZ()) * t3);
        return new Transform(x, y, z, p1.getYaw(), p1.getPitch());
    }

    private float lerpAngle(float from, float to, float t) {
        float delta = normalizeAngleDelta(to - from);
        return from + delta * t;
    }

    private float normalizeAngleDelta(float delta) {
        while (delta > 180.0f) {
            delta -= 360.0f;
        }
        while (delta < -180.0f) {
            delta += 360.0f;
        }
        return delta;
    }

    private float dampAngle(float from, float to, float factor) {
        float delta = normalizeAngleDelta(to - from);
        return from + delta * factor;
    }

    private float clampPitch(float pitch) {
        return Math.max(-89.9f, Math.min(89.9f, pitch));
    }

    private float[] resolveLookAt(Player player, SceneSession session, Transform position, LookAtTarget target) {
        Transform targetTransform = target.getPosition();
        if (target.getMode() == LookAtTarget.Mode.ENTITY && target.getEntityId() != null) {
            Entity entity = player.getWorld().getEntity(target.getEntityId());
            if (entity != null) {
                targetTransform = Transform.fromLocation(entity.getLocation());
            }
        }
        if (targetTransform == null) {
            return lookAtAngles(position, position);
        }
        return lookAtAngles(position, targetTransform);
    }

    private float[] lookAtAngles(Transform from, Transform to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double distanceXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, distanceXZ));
        return new float[]{yaw, pitch};
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
        } else if (keyframe instanceof ActionBarKeyframe actionBar) {
            activateActionBar(session, actionBar);
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
        SceneModelEntry entry = session.getScene().getModelEntry(keyframe.getModelEntry());
        String modelId = keyframe.getModelId();
        if (entry != null && entry.getModelId() != null && !entry.getModelId().isBlank()) {
            modelId = entry.getModelId();
        }
        switch (keyframe.getAction()) {
            case SPAWN -> {
                Transform transform = keyframe.getSpawnTransform();
                if (transform == null && entry != null) {
                    transform = entry.getSpawnTransform();
                }
                Location location = player.getLocation().clone();
                if (transform != null) {
                    transform.applyTo(location);
                }
                if (modelId == null || modelId.isBlank()) {
                    player.sendMessage(ChatColor.RED + "Model entry missing modelId; spawn skipped.");
                    return;
                }
                Entity base = adapter.spawnModelBase(player, location);
                adapter.bindModel(base, modelId);
                session.registerEntity(base);
                sessionManager.registerSceneEntity(session, base);
                visibilityController.hideEntityFromAllExcept(base, player);
                String handle = keyframe.getEntityRef();
                if (handle == null || handle.isBlank()) {
                    handle = UUID.randomUUID().toString();
                    keyframe.setEntityRef(handle);
                }
                session.registerModelRef(handle, base.getUniqueId());
                session.setLastModelHandle(handle);
                if (entry != null) {
                    session.setLastModelHandleForEntry(entry.getName(), handle);
                    if (entry.getDefaultAnimation() != null && !entry.getDefaultAnimation().isBlank()) {
                        adapter.playAnimation(base, modelId, entry.getDefaultAnimation(), true, 1.0);
                    }
                }
            }
            case ANIM -> {
                UUID entityId = session.getModelEntityId(resolveHandle(session, keyframe));
                if (entityId != null) {
                    Entity entity = player.getWorld().getEntity(entityId);
                    if (entity != null) {
                        adapter.playAnimation(entity, modelId, keyframe.getAnimationId(), keyframe.isLoop(), keyframe.getSpeed());
                    }
                }
            }
            case STOP -> {
                UUID entityId = session.getModelEntityId(resolveHandle(session, keyframe));
                if (entityId != null) {
                    Entity entity = player.getWorld().getEntity(entityId);
                    if (entity != null) {
                        adapter.stopAnimation(entity, modelId, keyframe.getAnimationId());
                    }
                }
            }
            case DESPAWN -> {
                UUID entityId = session.getModelEntityId(resolveHandle(session, keyframe));
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

    private String resolveHandle(SceneSession session, ModelKeyframe keyframe) {
        String handle = keyframe.getEntityRef();
        if (handle == null || handle.isBlank() || "last".equalsIgnoreCase(handle)) {
            String entry = keyframe.getModelEntry();
            String entryHandle = session.getLastModelHandleForEntry(entry);
            return entryHandle != null ? entryHandle : session.getLastModelHandle();
        }
        return handle;
    }

    private void activateActionBar(SceneSession session, ActionBarKeyframe keyframe) {
        session.setActiveActionBarText(keyframe.getText());
        session.setActionBarUntilTick(keyframe.getTimeTicks() + Math.max(1, keyframe.getDurationTicks()));
    }

    private void tickActionBar(Player player, SceneSession session, int time) {
        if (session.getActiveActionBarText() == null) {
            return;
        }
        if (time > session.getActionBarUntilTick()) {
            session.setActiveActionBarText(null);
            return;
        }
        String text = SceneTextFormatter.colorizeActionBar(session.getActiveActionBarText());
        player.sendActionBar(text);
    }
}

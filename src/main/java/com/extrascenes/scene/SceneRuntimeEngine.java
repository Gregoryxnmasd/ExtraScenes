package com.extrascenes.scene;

import com.extrascenes.Text;

import com.extrascenes.CitizensAdapter;
import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.ScaleAttributeResolver;
import com.extrascenes.SceneModelTrackAdapter;
import com.extrascenes.SceneProtocolAdapter;
import com.extrascenes.visibility.SceneVisibilityController;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attributable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class SceneRuntimeEngine {
    private final ExtraScenesPlugin plugin;
    private final SceneSessionManager sessionManager;
    private final SceneVisibilityController visibilityController;
    private final SceneProtocolAdapter protocolAdapter;
    private final CitizensAdapter citizensAdapter;
    private static final int SPECTATOR_RECOVERY_COOLDOWN_TICKS = 10;
    private final EditorPreviewController editorPreviewController;
    private volatile boolean actorDebugEnabled = false;

    public SceneRuntimeEngine(ExtraScenesPlugin plugin, SceneSessionManager sessionManager,
                              SceneVisibilityController visibilityController,
                              SceneProtocolAdapter protocolAdapter) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.visibilityController = visibilityController;
        this.protocolAdapter = protocolAdapter;
        this.citizensAdapter = plugin.getCitizensAdapter();
        this.editorPreviewController = new EditorPreviewController(this.citizensAdapter);
    }


    public void setActorDebugEnabled(boolean actorDebugEnabled) {
        this.actorDebugEnabled = actorDebugEnabled;
        plugin.getLogger().info("Actor transform debug " + (actorDebugEnabled ? "ENABLED" : "DISABLED"));
    }

    public boolean isActorDebugEnabled() {
        return actorDebugEnabled;
    }

    public void start() {
        // per-session tasks handle playback
    }

    public void stop() {
        editorPreviewController.cleanupAll();
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
                try {
                    tickSession(session);
                } catch (Exception ex) {
                    plugin.getLogger().severe("Cutscene session aborted due to runtime exception for "
                            + session.getPlayerId() + ": " + ex.getMessage());
                    sessionManager.abortSession(session.getPlayerId(), "runtime_exception");
                }
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
            sessionManager.abortSession(session.getPlayerId(), "viewer_missing");
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

        ensureSpectatorTarget(player, session);
        tickSessionActors(player, session, time);
        updateCamera(player, session, time);
        handleKeyframes(player, session, time, duration);
        tickActionBar(player, session, time);
        session.incrementTime();
    }



    private void maybeLogActorTransform(Player viewer, String actorId, int tick, Transform transform, SessionActorHandle handle,
                                        boolean recordingPreview) {
        if (!actorDebugEnabled || transform == null || tick % 20 != 0) {
            return;
        }
        String mode = recordingPreview ? "preview" : "runtime";
        String entityInfo = handle != null && handle.getEntity() != null
                ? handle.getEntity().getUniqueId().toString() + " visible=" + viewer.canSee(handle.getEntity())
                : "missing";
        plugin.getLogger().info(String.format(java.util.Locale.ROOT,
                "Actor %s tick %d -> %.2f %.2f %.2f %.1f %.1f (applied, %s, entity=%s)",
                actorId, tick, transform.getX(), transform.getY(), transform.getZ(), transform.getYaw(), transform.getPitch(),
                mode, entityInfo));
    }

    private void clearNameplate(Entity entity, Object npc) {
        if (entity != null) {
            entity.customName(null);
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
        }
        citizensAdapter.disableNameplate(npc);
    }

    private void tickSessionActors(Player viewer, SceneSession session, int tick) {
        if (!citizensAdapter.isAvailable()) {
            return;
        }
        if (session.getActorHandles().isEmpty()) {
            spawnSessionActors(viewer, session);
        }
        for (SceneActorTemplate template : session.getScene().getActorTemplates().values()) {
            if (session.isPreview() && !template.isPreviewEnabled()) {
                continue;
            }
            if (session.isPreview() && template.getTransformTicks().isEmpty() && template.getTickActions().isEmpty()) {
                continue;
            }
            SessionActorHandle handle = session.getActorHandle(template.getActorId());
            if (handle == null || handle.getEntity() == null || !handle.getEntity().isValid()) {
                spawnSessionActor(viewer, session, template);
                handle = session.getActorHandle(template.getActorId());
                if (handle == null || handle.getEntity() == null || !handle.getEntity().isValid()) {
                    continue;
                }
            }
            ActorTickAction action = template.getTickAction(tick);
            if (action != null && action.isSpawn()) {
                handle.getEntity().setInvisible(false);
                handle.setSpawned(true);
            }
            if (action != null && action.isDespawn()) {
                handle.getEntity().setInvisible(true);
                handle.setSpawned(false);
                continue;
            }
            if (!handle.isSpawned() && tick > 0) {
                continue;
            }

            ActorTransformTick transformTick = resolveTransformTick(template, tick);
            Transform transform = transformTick != null ? transformTick.getTransform() : handle.getLastTransform();
            if (transform == null) {
                continue;
            }
            handle.setLastTransform(transform);
            Location target = handle.getEntity().getLocation().clone();
            transform.applyTo(target);
            if (template.getPlaybackMode() == ActorPlaybackMode.WALK) {
                citizensAdapter.setMoveDestination(handle.getCitizensNpc(), target);
            } else {
                handle.getEntity().teleport(target);
            }
            if (transformTick != null && handle.getEntity() instanceof LivingEntity living) {
                living.setGliding(transformTick.isGliding());
            }
            applyScale(handle.getEntity(), template.getScale());
            maybeLogActorTransform(viewer, template.getActorId(), tick, transform, handle, false);
            applyActorTickAction(viewer, template, handle, action, tick, false);
        }
    }

    private void spawnSessionActors(Player viewer, SceneSession session) {
        for (SceneActorTemplate template : session.getScene().getActorTemplates().values()) {
            if (session.isPreview() && !isPreviewEligible(template)) {
                continue;
            }
            spawnSessionActor(viewer, session, template);
        }
    }

    private void spawnSessionActor(Player viewer, SceneSession session, SceneActorTemplate template) {
        Object npc = citizensAdapter.createNpc(template.getEntityType(), template.getDisplayName());
            if (npc == null) {
                return;
            }
            citizensAdapter.applySkinPersistent(npc, template);
            boolean playerFilterApplied = citizensAdapter.applyPlayerFilter(npc, viewer.getUniqueId());
            if (!playerFilterApplied && !protocolAdapter.isProtocolLibAvailable()) {
                plugin.getLogger().warning("per-viewer actor visibility requires ProtocolLib");
                citizensAdapter.destroy(npc);
                return;
            }
            citizensAdapter.configureNpc(npc);

            Location spawnLocation = viewer.getLocation().clone();
            ActorTransformTick first = template.getTransformTicks().values().stream().findFirst().orElse(null);
            if (first != null && first.getTransform() != null) {
                first.getTransform().applyTo(spawnLocation);
            }
            if (!citizensAdapter.spawn(npc, spawnLocation)) {
                citizensAdapter.destroy(npc);
                return;
            }
            Entity entity = citizensAdapter.getEntity(npc);
            if (entity == null) {
                citizensAdapter.destroy(npc);
                return;
            }
            entity.setSilent(true);
            entity.setInvulnerable(true);
            entity.setGravity(false);
            clearNameplate(entity, npc);
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.setAI(false);
            }
            applyScale(entity, template.getScale());
            session.registerEntity(entity);
            SessionActorHandle handle = new SessionActorHandle(template.getActorId(), npc, entity);
            Integer firstSpawnTick = template.getTickActions().values().stream()
                    .filter(ActorTickAction::isSpawn)
                    .map(ActorTickAction::getTick)
                    .min(Integer::compareTo)
                    .orElse(0);
            if (firstSpawnTick > 0) {
                entity.setInvisible(true);
                handle.setSpawned(false);
            }
            session.registerActorHandle(handle);
            sessionManager.registerSceneEntity(session, entity);
            citizensAdapter.disableNameplate(npc);
            if (!playerFilterApplied) {
                visibilityController.hideEntityFromAllExcept(entity, viewer);
            }
            visibilityController.showEntityToPlayer(entity, viewer);
    }

    private ActorTransformTick resolveTransformTick(SceneActorTemplate template, int tick) {
        ActorTransformTick exact = template.getTransformTick(tick);
        if (exact != null) {
            return exact;
        }
        ActorTransformTick last = null;
        for (ActorTransformTick candidate : template.getTransformTicks().values()) {
            if (candidate.getTick() > tick) {
                break;
            }
            last = candidate;
        }
        return last;
    }

    private void applyActorTickAction(Player viewer, SceneActorTemplate template, SessionActorHandle handle,
                                      ActorTickAction action, int tick, boolean previewMode) {
        if (action == null || handle == null || handle.getEntity() == null) {
            return;
        }
        Entity entity = handle.getEntity();
        if (action.getLookAtTarget() != null) {
            float[] angles = resolveLookAt(viewer, null,
                    Transform.fromLocation(entity.getLocation()), action.getLookAtTarget());
            entity.setRotation(angles[0], angles[1]);
            logActionExecution(previewMode, tick, "look", template.getActorId(), entity.getUniqueId());
        }
        if (action.getAnimation() != null && !action.getAnimation().isBlank() && entity instanceof LivingEntity livingEntity) {
            livingEntity.swingMainHand();
            logActionExecution(previewMode, tick, "anim", template.getActorId(), entity.getUniqueId());
        }
        if (action.getCommand() != null && !action.getCommand().isBlank()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), action.getCommand().replace("{player}", viewer.getName()));
            logActionExecution(previewMode, tick, "command", template.getActorId(), entity.getUniqueId());
        }
        if (action.isSpawn()) {
            logActionExecution(previewMode, tick, "spawn", template.getActorId(), entity.getUniqueId());
        }
        if (action.isDespawn()) {
            logActionExecution(previewMode, tick, "despawn", template.getActorId(), entity.getUniqueId());
        }
    }

    private void logActionExecution(boolean previewMode, int tick, String actionType, String actorId, UUID entityUuid) {
        if (!actorDebugEnabled) {
            return;
        }
        plugin.getLogger().info("On tick " + tick + " executed actor action: " + actionType
                + " actor=" + actorId + " mode=" + (previewMode ? "preview" : "runtime") + " entity=" + entityUuid);
    }

    public void clearRecordingPreview(Player player) {
        if (player == null) {
            return;
        }
        previewCleanup(player, "recording_preview_clear");
    }

    public void previewEnable(Scene scene, Player viewer) {
        if (scene == null || viewer == null) {
            return;
        }
        previewActorsAtTick(viewer, scene, null, 0);
    }

    public void previewDisable(Player viewer) {
        cleanupEditorPreview(viewer, "preview_disable");
    }

    public void previewCleanup(Player viewer, String reason) {
        cleanupEditorPreview(viewer, reason == null ? "preview_cleanup" : reason);
    }

    public void previewActorsAtTick(Player viewer, Scene scene, String recordingActorId, int tick) {
        if (viewer == null || scene == null || !citizensAdapter.isAvailable()) {
            return;
        }
        Map<String, SessionActorHandle> handles = editorPreviewController.handlesFor(viewer);
        for (SceneActorTemplate template : scene.getActorTemplates().values()) {
            if (recordingActorId != null && template.getActorId().equalsIgnoreCase(recordingActorId)) {
                continue;
            }
            if (!isPreviewEligible(template)) {
                continue;
            }
            SessionActorHandle handle = handles.get(template.getActorId().toLowerCase());
            if (handle == null || handle.getEntity() == null || !handle.getEntity().isValid()) {
                Object npc = citizensAdapter.createNpc(template.getEntityType(), template.getDisplayName());
                if (npc == null || !citizensAdapter.spawn(npc, viewer.getLocation())) {
                    continue;
                }
                Entity entity = citizensAdapter.getEntity(npc);
                if (entity == null) {
                    citizensAdapter.destroy(npc);
                    continue;
                }
                clearNameplate(entity, npc);
                entity.setInvisible(true);
                boolean playerFilterApplied = citizensAdapter.applyPlayerFilter(npc, viewer.getUniqueId());
                if (!playerFilterApplied && !protocolAdapter.isProtocolLibAvailable()) {
                    plugin.getLogger().warning("per-viewer actor visibility requires ProtocolLib");
                    citizensAdapter.destroy(npc);
                    continue;
                }
                if (!playerFilterApplied) {
                    visibilityController.hideEntityFromAllExcept(entity, viewer);
                }
                visibilityController.showEntityToPlayer(entity, viewer);
                handle = new SessionActorHandle(template.getActorId(), npc, entity);
                editorPreviewController.register(viewer, handle);
            }
            ActorTransformTick transformTick = resolveTransformTick(template, tick);
            Transform transform = transformTick != null ? transformTick.getTransform() : handle.getLastTransform();
            if (transform != null) {
                Location loc = handle.getEntity().getLocation().clone();
                transform.applyTo(loc);
                handle.getEntity().teleport(loc);
                handle.getEntity().setInvisible(false);
                applyScale(handle.getEntity(), template.getScale());
                handle.setLastTransform(transform);
                maybeLogActorTransform(viewer, template.getActorId(), tick, transform, handle, true);
                ActorTickAction action = template.getTickAction(tick);
                applyActorTickAction(viewer, template, handle, action, tick, true);
            }
        }
    }

    public void cleanupEditorPreview(Player viewer) {
        cleanupEditorPreview(viewer, "manual");
    }

    public void cleanupEditorPreview(Player viewer, String reason) {
        if (viewer == null) {
            return;
        }
        editorPreviewController.cleanup(viewer);
        if (actorDebugEnabled) {
            plugin.getLogger().info("preview.cleanup(" + viewer.getUniqueId() + ") reason=" + reason);
        }
    }

    public SessionActorHandle findViewerActorHandle(Player viewer, String actorId) {
        if (viewer == null || actorId == null) {
            return null;
        }
        SceneSession session = sessionManager.getSession(viewer.getUniqueId());
        if (session != null) {
            SessionActorHandle handle = session.getActorHandle(actorId);
            if (handle != null) {
                return handle;
            }
        }
        return editorPreviewController.getHandle(viewer, actorId);
    }

    private boolean isPreviewEligible(SceneActorTemplate template) {
        if (template == null || !template.isPreviewEnabled()) {
            return false;
        }
        if (!template.getTransformTicks().isEmpty()) {
            return true;
        }
        return template.getTickActions().values().stream()
                .anyMatch(action -> action != null && (action.isSpawn()
                        || (action.getAnimation() != null && !action.getAnimation().isBlank())
                        || action.getLookAtTarget() != null
                        || (action.getCommand() != null && !action.getCommand().isBlank())));
    }

    private void applyScale(Entity entity, double scale) {
        org.bukkit.attribute.Attribute attribute = ScaleAttributeResolver.resolveScaleAttribute();
        if (attribute == null || !(entity instanceof Attributable attributable)
                || attributable.getAttribute(attribute) == null) {
            return;
        }
        attributable.getAttribute(attribute).setBaseValue(scale <= 0.0 ? 1.0 : scale);
    }
    private void ensureSpectatorTarget(Player player, SceneSession session) {
        Entity cameraRig = getCameraRig(session, player);
        if (cameraRig == null) {
            return;
        }
        Entity current = player.getSpectatorTarget();
        boolean targetLost = current == null || !current.getUniqueId().equals(cameraRig.getUniqueId());
        if (!targetLost) {
            return;
        }

        int timeTicks = session.getTimeTicks();
        boolean cooldownReady = timeTicks >= session.getSpectatorRecoveryCooldownUntilTick();
        if (cooldownReady) {
            session.setSpectatorRecoveryCooldownUntilTick(timeTicks + SPECTATOR_RECOVERY_COOLDOWN_TICKS);
            player.teleport(cameraRig.getLocation());
            plugin.getLogger().info("Camera rig target lost; recovering for " + player.getName()
                    + " rig=" + cameraRig.getUniqueId());
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            SceneSession active = sessionManager.getSession(player.getUniqueId());
            if (active == null) {
                return;
            }
            Entity rig = getCameraRig(active, player);
            if (rig == null) {
                return;
            }
            protocolAdapter.applySpectatorCamera(player, rig);
        }, 1L);
    }

    private void updateCamera(Player player, SceneSession session, int timeTicks) {
        Entity cameraRig = getCameraRig(session, player);
        if (cameraRig == null) {
            return;
        }
        visibilityController.hideEntityFromAllExcept(cameraRig, player);
        visibilityController.showEntityToPlayer(cameraRig, player);

        Track<CameraKeyframe> cameraTrack = session.getScene().getTrack(SceneTrackType.CAMERA);
        if (cameraTrack == null || cameraTrack.getKeyframes().isEmpty()) {
            return;
        }
        Transform transform = interpolateCamera(player, session, cameraTrack.getKeyframes(), timeTicks);
        if (transform == null) {
            return;
        }
        Location location = cameraRig.getLocation().clone();
        transform.applyTo(location);
        cameraRig.teleport(location);
        protocolAdapter.applySpectatorCamera(player, cameraRig);
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
        t = Math.max(0.0f, Math.min(1.0f, t));
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
            case SMOOTH -> t * t * (3.0f - 2.0f * t);
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
        for (String command : commands) {
            String resolved = SceneTextFormatter.replacePlaceholders(plugin, player, session, command, durationTicks);
            if (keyframe.getExecutorMode() == CommandKeyframe.ExecutorMode.CONSOLE) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved.replaceFirst("^console:", "").trim());
            } else {
                String sanitized = resolved.replaceFirst("^player:", "").trim();
                Bukkit.dispatchCommand(player, sanitized);
            }
        }
    }

    private void handleModelKeyframe(Player player, SceneSession session, ModelKeyframe keyframe) {
        SceneModelTrackAdapter adapter = plugin.getModelTrackAdapter();
        if (adapter == null || !adapter.isModelEngineAvailable()) {
            Text.send(player, "&c" + "ModelEngine not installed; model keyframe skipped.");
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
                    Text.send(player, "&c" + "Model entry missing modelId; spawn skipped.");
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

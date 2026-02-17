package com.extrascenes.scene;

import com.extrascenes.Text;

import com.extrascenes.CitizensAdapter;
import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.ScaleAttributeResolver;
import com.extrascenes.SceneModelTrackAdapter;
import com.extrascenes.SceneProtocolAdapter;
import com.extrascenes.visibility.SceneVisibilityController;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attributable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class SceneRuntimeEngine {
    private static final double MAX_CAMERA_STEP_DISTANCE = 1.75D;
    private static final String PREVIEW_TAG = "extrascenes_preview";
    private static final String PREVIEW_VIEWER_PREFIX = "extrascenes_viewer_";
    private static final String PREVIEW_ACTOR_PREFIX = "extrascenes_actor_";
    private final ExtraScenesPlugin plugin;
    private final SceneSessionManager sessionManager;
    private final SceneVisibilityController visibilityController;
    private final SceneProtocolAdapter protocolAdapter;
    private final CitizensAdapter citizensAdapter;
    private final EditorPreviewController editorPreviewController;
    private volatile boolean actorDebugEnabled = false;
    private final Set<UUID> debugCameraViewers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> debugPreviewViewers = ConcurrentHashMap.newKeySet();

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

        updateCameraRigTransform(player, session, time);
        ensureSpectatorTarget(player, session);
        tickSessionActors(player, session, time);
        maybeLogDebugCamera(player, session, time);
        handleKeyframes(player, session, time, duration);
        tickActionBar(player, session, time);
        session.incrementTime();
    }

    public void setDebugCameraEnabled(UUID viewerId, boolean enabled) {
        if (viewerId == null) {
            return;
        }
        if (enabled) {
            debugCameraViewers.add(viewerId);
        } else {
            debugCameraViewers.remove(viewerId);
        }
    }

    public boolean isDebugCameraEnabled(UUID viewerId) {
        return viewerId != null && debugCameraViewers.contains(viewerId);
    }

    public void setDebugPreviewEnabled(UUID viewerId, boolean enabled) {
        if (viewerId == null) {
            return;
        }
        if (enabled) {
            debugPreviewViewers.add(viewerId);
        } else {
            debugPreviewViewers.remove(viewerId);
        }
    }

    public boolean isDebugPreviewEnabled(UUID viewerId) {
        return viewerId != null && debugPreviewViewers.contains(viewerId);
    }

    public void emitDebugPreview(Player viewer) {
        if (viewer == null || !isDebugPreviewEnabled(viewer.getUniqueId())) {
            return;
        }
        if (Bukkit.getCurrentTick() % 20 != 0) {
            return;
        }
        List<SessionActorHandle> handles = editorPreviewController.listHandles(viewer.getUniqueId());
        String entities = handles.stream()
                .map(handle -> handle.getEntity() != null ? handle.getEntity().getUniqueId().toString() : "missing")
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");
        plugin.getLogger().info("[debugpreview] viewer=" + viewer.getUniqueId()
                + " previewActors=" + handles.size()
                + " entities=" + entities);
    }

    private void maybeLogDebugCamera(Player player, SceneSession session, int timeTicks) {
        if (!isDebugCameraEnabled(player.getUniqueId()) || timeTicks % 20 != 0) {
            return;
        }
        Entity cameraRig = getCameraRig(session, player);
        Entity spectatorTarget = player.getSpectatorTarget();
        String rigId = cameraRig != null ? cameraRig.getUniqueId().toString() : "null";
        String rigWorld = cameraRig != null && cameraRig.getWorld() != null ? cameraRig.getWorld().getName() : "null";
        boolean rigValid = cameraRig != null && cameraRig.isValid();
        String spectatorTargetId = spectatorTarget != null ? spectatorTarget.getUniqueId().toString() : "null";
        boolean spectatorTargetMatchesRig = cameraRig != null && spectatorTarget != null
                && spectatorTarget.getUniqueId().equals(cameraRig.getUniqueId());
        String transform = "missing";
        String transformDelta = "delta=missing";
        if (cameraRig != null) {
            Location loc = cameraRig.getLocation();
            transform = String.format(java.util.Locale.ROOT,
                    "x=%.3f y=%.3f z=%.3f yaw=%.2f pitch=%.2f",
                    loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            Location prev = session.getLastCameraLocation();
            if (prev != null) {
                transformDelta = String.format(java.util.Locale.ROOT,
                        "from=%.3f,%.3f,%.3f,%.2f,%.2f to=%.3f,%.3f,%.3f,%.2f,%.2f",
                        prev.getX(), prev.getY(), prev.getZ(), prev.getYaw(), prev.getPitch(),
                        loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            }
        }
        boolean rigChunkLoaded = false;
        boolean rigChunkForceLoaded = false;
        if (cameraRig != null && cameraRig.getWorld() != null) {
            int chunkX = cameraRig.getLocation().getBlockX() >> 4;
            int chunkZ = cameraRig.getLocation().getBlockZ() >> 4;
            rigChunkLoaded = cameraRig.getWorld().isChunkLoaded(chunkX, chunkZ);
            rigChunkForceLoaded = cameraRig.getWorld().isChunkForceLoaded(chunkX, chunkZ);
        }
        plugin.getLogger().info("[debugcamera] viewer=" + player.getUniqueId()
                + " viewerName=" + player.getName()
                + " sessionId=" + session.getSessionId()
                + " rig=" + rigId
                + " rigValid=" + rigValid
                + " rigWorld=" + rigWorld
                + " spectatorTarget=" + spectatorTargetId
                + " spectatorTargetMatchesRig=" + spectatorTargetMatchesRig
                + " tick=" + timeTicks
                + " chunkLoaded=" + rigChunkLoaded
                + " chunkForceLoaded=" + rigChunkForceLoaded
                + " " + transform + " " + transformDelta);
    }



    private void maybeLogActorTransform(Player viewer, String actorId, int tick, Transform transform, SessionActorHandle handle,
                                        boolean recordingPreview, List<String> executedActions) {
        if (!actorDebugEnabled || transform == null || tick % 20 != 0) {
            return;
        }
        String mode = recordingPreview ? "preview" : "runtime";
        String entityInfo = handle != null && handle.getEntity() != null
                ? handle.getEntity().getUniqueId().toString() + " visible=" + viewer.canSee(handle.getEntity())
                : "missing";
        String actions = executedActions == null || executedActions.isEmpty() ? "none" : String.join(",", executedActions);
        double scale = readScale(handle != null ? handle.getEntity() : null);
        plugin.getLogger().info(String.format(java.util.Locale.ROOT,
                "[debugactors] actorId=%s tick=%d mode=%s entity=%s transform=%.2f %.2f %.2f %.1f %.1f scale=%.3f actions=%s",
                actorId, tick, mode, entityInfo,
                transform.getX(), transform.getY(), transform.getZ(), transform.getYaw(), transform.getPitch(),
                scale, actions));
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
            clearNameplate(handle.getEntity(), handle.getCitizensNpc());
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
            List<String> executedActions = applyActorTickAction(viewer, template, handle, action, tick, false);
            maybeLogActorTransform(viewer, template.getActorId(), tick, transform, handle, false, executedActions);
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
        SessionActorHandle existing = session.getActorHandle(template.getActorId());
        if (existing != null) {
            if (existing.getCitizensNpc() != null) {
                citizensAdapter.destroy(existing.getCitizensNpc());
            }
            Entity previousEntity = existing.getEntity();
            if (previousEntity != null) {
                session.unregisterEntity(previousEntity);
                sessionManager.unregisterSceneEntity(previousEntity);
                if (previousEntity.isValid()) {
                    previousEntity.remove();
                }
            }
            session.unregisterActorHandle(template.getActorId());
        }
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

    private List<String> applyActorTickAction(Player viewer, SceneActorTemplate template, SessionActorHandle handle,
                                      ActorTickAction action, int tick, boolean previewMode) {
        List<String> executedActions = new ArrayList<>();
        if (action == null || handle == null || handle.getEntity() == null) {
            return executedActions;
        }
        Entity entity = handle.getEntity();
        if (action.getLookAtTarget() != null) {
            float[] angles = resolveLookAt(viewer, null,
                    Transform.fromLocation(entity.getLocation()), action.getLookAtTarget());
            entity.setRotation(angles[0], angles[1]);
            executedActions.add("look-at");
        }
        if (action.getAnimation() != null && !action.getAnimation().isBlank() && entity instanceof LivingEntity livingEntity) {
            livingEntity.swingMainHand();
            executedActions.add("play-animation:" + action.getAnimation());
        }
        if (action.isStopAnimation()) {
            executedActions.add("stop-animation");
        }
        if (action.getScale() != null) {
            applyScale(entity, action.getScale());
            executedActions.add("set-scale:" + action.getScale());
        }
        if (action.getSkinName() != null && !action.getSkinName().isBlank()) {
            citizensAdapter.applySkin(handle.getCitizensNpc(), action.getSkinName());
            executedActions.add("set-skin:" + action.getSkinName());
        }
        if (action.getCommand() != null && !action.getCommand().isBlank()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), action.getCommand().replace("{player}", viewer.getName()));
            executedActions.add("run-command");
        }
        if (action.isSpawn()) {
            executedActions.add("spawn");
        }
        if (action.isDespawn()) {
            executedActions.add("despawn");
        }
        return executedActions;
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
        Set<String> liveActorIds = new HashSet<>();
        for (SceneActorTemplate template : scene.getActorTemplates().values()) {
            if (recordingActorId != null && template.getActorId().equalsIgnoreCase(recordingActorId)) {
                continue;
            }
            if (!isPreviewEligible(template)) {
                continue;
            }
            String actorKey = template.getActorId().toLowerCase(java.util.Locale.ROOT);
            liveActorIds.add(actorKey);
            SessionActorHandle handle = handles.get(actorKey);
            if (handle == null || handle.getEntity() == null || !handle.getEntity().isValid()) {
                Object npc = citizensAdapter.createNpc(template.getEntityType(), template.getDisplayName());
                if (npc == null) {
                    continue;
                }
                citizensAdapter.applySkinPersistent(npc, template);
                citizensAdapter.configureNpc(npc);
                Location spawnLocation = resolvePreviewSpawnLocation(viewer, template, tick);
                if (!citizensAdapter.spawn(npc, spawnLocation)) {
                    citizensAdapter.destroy(npc);
                    continue;
                }
                Entity entity = citizensAdapter.getEntity(npc);
                if (entity == null) {
                    citizensAdapter.destroy(npc);
                    continue;
                }
                clearNameplate(entity, npc);
                entity.setSilent(true);
                entity.setInvulnerable(true);
                entity.setGravity(false);
                entity.setInvisible(true);
                tagPreviewEntity(entity, viewer, actorKey);
                if (entity instanceof LivingEntity livingEntity) {
                    livingEntity.setAI(false);
                }
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
                Integer firstSpawnTick = template.getTickActions().values().stream()
                        .filter(ActorTickAction::isSpawn)
                        .map(ActorTickAction::getTick)
                        .min(Integer::compareTo)
                        .orElse(0);
                if (firstSpawnTick > 0) {
                    handle.setSpawned(false);
                }
                editorPreviewController.register(viewer, handle);
            }
            clearNameplate(handle.getEntity(), handle.getCitizensNpc());
            tagPreviewEntity(handle.getEntity(), viewer, actorKey);
            cleanupDuplicateTaggedPreviewEntities(viewer, actorKey, handle.getEntity());
            ActorTickAction action = template.getTickAction(tick);
            if (action != null && action.isDespawn()) {
                handle.getEntity().setInvisible(true);
                handle.setSpawned(false);
                applyActorTickAction(viewer, template, handle, action, tick, true);
                continue;
            }
            if (action != null && action.isSpawn()) {
                if (handle.getLastTransform() != null) {
                    handle.getEntity().setInvisible(false);
                    handle.setSpawned(true);
                }
            }
            if (!handle.isSpawned() && tick > 0) {
                continue;
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
                List<String> executedActions = applyActorTickAction(viewer, template, handle, action, tick, true);
                maybeLogActorTransform(viewer, template.getActorId(), tick, transform, handle, true, executedActions);
            } else if (action != null) {
                applyActorTickAction(viewer, template, handle, action, tick, true);
                handle.getEntity().setInvisible(true);
            }
        }
        cleanupStalePreviewActors(viewer, handles, liveActorIds);
        emitDebugPreview(viewer);
    }

    private Location resolvePreviewSpawnLocation(Player viewer, SceneActorTemplate template, int tick) {
        Location spawnLocation = viewer.getLocation().clone();
        Transform transform = resolveTransformForPreview(template, tick);
        if (transform == null) {
            return spawnLocation;
        }
        transform.applyTo(spawnLocation);
        return spawnLocation;
    }

    private Transform resolveTransformForPreview(SceneActorTemplate template, int tick) {
        ActorTransformTick nearest = resolveTransformTick(template, tick);
        if (nearest != null && nearest.getTransform() != null) {
            return nearest.getTransform();
        }
        if (template == null || template.getTransformTicks().isEmpty()) {
            return null;
        }
        Map.Entry<Integer, ActorTransformTick> first = template.getTransformTicks().entrySet().stream().findFirst().orElse(null);
        if (first == null || first.getValue() == null) {
            return null;
        }
        return first.getValue().getTransform();
    }

    private void cleanupStalePreviewActors(Player viewer, Map<String, SessionActorHandle> handles, Set<String> liveActorIds) {
        if (viewer == null || handles == null || handles.isEmpty()) {
            return;
        }
        for (Map.Entry<String, SessionActorHandle> entry : new ArrayList<>(handles.entrySet())) {
            if (liveActorIds.contains(entry.getKey())) {
                continue;
            }
            SessionActorHandle handle = entry.getValue();
            if (handle != null) {
                if (handle.getCitizensNpc() != null) {
                    citizensAdapter.destroy(handle.getCitizensNpc());
                }
                Entity entity = handle.getEntity();
                if (entity != null && entity.isValid()) {
                    entity.remove();
                }
            }
            handles.remove(entry.getKey());
            plugin.getLogger().info("[preview-cleanup] removed stale actor handle viewer=" + viewer.getUniqueId()
                    + " actor=" + entry.getKey());
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
        cleanupTaggedPreviewEntities(viewer, null);
        if (actorDebugEnabled) {
            plugin.getLogger().info("preview.cleanup(" + viewer.getUniqueId() + ") reason=" + reason);
        }
    }

    private void tagPreviewEntity(Entity entity, Player viewer, String actorKey) {
        if (entity == null || viewer == null || actorKey == null || actorKey.isBlank()) {
            return;
        }
        entity.addScoreboardTag(PREVIEW_TAG);
        entity.addScoreboardTag(PREVIEW_VIEWER_PREFIX + viewer.getUniqueId().toString().replace("-", ""));
        entity.addScoreboardTag(PREVIEW_ACTOR_PREFIX + actorKey.toLowerCase(java.util.Locale.ROOT));
    }

    private void cleanupDuplicateTaggedPreviewEntities(Player viewer, String actorKey, Entity expectedEntity) {
        cleanupTaggedPreviewEntities(viewer, actorKey, expectedEntity);
    }

    private void cleanupTaggedPreviewEntities(Player viewer, String actorKey) {
        cleanupTaggedPreviewEntities(viewer, actorKey, null);
    }

    private void cleanupTaggedPreviewEntities(Player viewer, String actorKey, Entity expectedEntity) {
        if (viewer == null) {
            return;
        }
        String viewerTag = PREVIEW_VIEWER_PREFIX + viewer.getUniqueId().toString().replace("-", "");
        String actorTag = actorKey == null ? null : PREVIEW_ACTOR_PREFIX + actorKey.toLowerCase(java.util.Locale.ROOT);
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                Set<String> tags = entity.getScoreboardTags();
                if (!tags.contains(PREVIEW_TAG) || !tags.contains(viewerTag)) {
                    continue;
                }
                if (actorTag != null && !tags.contains(actorTag)) {
                    continue;
                }
                if (expectedEntity != null && entity.getUniqueId().equals(expectedEntity.getUniqueId())) {
                    continue;
                }
                if (entity.isValid()) {
                    entity.remove();
                }
            }
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
                        || (action.getCommand() != null && !action.getCommand().isBlank())
                        || action.getScale() != null
                        || (action.getSkinName() != null && !action.getSkinName().isBlank())));
    }

    private void applyScale(Entity entity, double scale) {
        org.bukkit.attribute.Attribute attribute = ScaleAttributeResolver.resolveScaleAttribute();
        if (attribute == null || !(entity instanceof Attributable attributable)
                || attributable.getAttribute(attribute) == null) {
            return;
        }
        attributable.getAttribute(attribute).setBaseValue(scale <= 0.0 ? 1.0 : scale);
    }

    private double readScale(Entity entity) {
        org.bukkit.attribute.Attribute attribute = ScaleAttributeResolver.resolveScaleAttribute();
        if (attribute == null || !(entity instanceof Attributable attributable)
                || attributable.getAttribute(attribute) == null) {
            return 1.0;
        }
        return attributable.getAttribute(attribute).getBaseValue();
    }
    private void ensureSpectatorTarget(Player player, SceneSession session) {
        Entity cameraRig = getCameraRig(session, player);
        if (cameraRig == null) {
            plugin.getLogger().severe("Camera rig unavailable for lock enforcement viewer=" + player.getName()
                    + " session=" + session.getSessionId()
                    + " rigId=" + session.getCameraRigId()
                    + " rigWorld=" + session.getCameraRigWorld());
            sessionManager.abortSession(session.getPlayerId(), "camera_rig_missing");
            return;
        }
        if (player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
            plugin.getLogger().warning("[spectator-enforce] restoring spectator mode for " + player.getName()
                    + " session=" + session.getSessionId());
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        }
        Entity current = player.getSpectatorTarget();
        boolean targetLost = current == null || !current.getUniqueId().equals(cameraRig.getUniqueId());
        if (targetLost) {
            plugin.getLogger().warning("Spectator target drift detected for " + player.getName()
                    + " session=" + session.getSessionId()
                    + " current=" + (current == null ? "null" : current.getUniqueId())
                    + " expected=" + cameraRig.getUniqueId());
        }
        protocolAdapter.applySpectatorCamera(player, cameraRig);
        Entity rebound = player.getSpectatorTarget();
        if (rebound == null || !rebound.getUniqueId().equals(cameraRig.getUniqueId())) {
            plugin.getLogger().severe("[spectator-enforce] unable to bind spectator target for " + player.getName()
                    + " session=" + session.getSessionId()
                    + " expected=" + cameraRig.getUniqueId()
                    + " actual=" + (rebound == null ? "null" : rebound.getUniqueId()));
        }
    }

    private void updateCameraRigTransform(Player player, SceneSession session, int timeTicks) {
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
        Location from = cameraRig.getLocation().clone();
        Location location = from.clone();
        transform.applyTo(location);
        clampCameraDelta(from, location, session, timeTicks);
        cameraRig.teleport(location);
        if (isDebugCameraEnabled(player.getUniqueId()) && timeTicks % 20 == 0) {
            plugin.getLogger().info(String.format(java.util.Locale.ROOT,
                    "[debugcamera-rigstep] viewer=%s sessionId=%s rig=%s from=%.3f %.3f %.3f %.2f %.2f to=%.3f %.3f %.3f %.2f %.2f",
                    player.getUniqueId(), session.getSessionId(), cameraRig.getUniqueId(),
                    from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch(),
                    location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch()));
        }
        session.setLastCameraLocation(from);
    }

    private void clampCameraDelta(Location from, Location to, SceneSession session, int tick) {
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null
                || !from.getWorld().equals(to.getWorld())) {
            return;
        }
        double distance = from.distance(to);
        if (distance <= MAX_CAMERA_STEP_DISTANCE || distance <= 0.0D) {
            return;
        }
        double ratio = MAX_CAMERA_STEP_DISTANCE / distance;
        to.setX(from.getX() + (to.getX() - from.getX()) * ratio);
        to.setY(from.getY() + (to.getY() - from.getY()) * ratio);
        to.setZ(from.getZ() + (to.getZ() - from.getZ()) * ratio);
        if (tick % 20 == 0) {
            plugin.getLogger().warning(String.format(java.util.Locale.ROOT,
                    "[camera-clamp] session=%s tick=%d delta=%.3f clampedTo=%.3f",
                    session.getSessionId(), tick, distance, MAX_CAMERA_STEP_DISTANCE));
        }
    }

    private Transform interpolateCamera(Player player, SceneSession session, List<CameraKeyframe> keyframes, int timeTicks) {
        if (keyframes.isEmpty()) {
            return null;
        }
        SmoothingQuality quality = session.getScene().getSmoothingQuality();
        int subSamples = Math.max(1, quality.getSubSamples());
        if (subSamples <= 1) {
            return sampleCameraTransform(player, session, keyframes, timeTicks, quality);
        }
        Transform accumulated = null;
        for (int i = 0; i < subSamples; i++) {
            float sampleTime = timeTicks + (i / (float) subSamples);
            Transform sample = sampleCameraTransform(player, session, keyframes, sampleTime, quality);
            if (sample == null) {
                continue;
            }
            if (accumulated == null) {
                accumulated = new Transform(sample.getX(), sample.getY(), sample.getZ(), sample.getYaw(), sample.getPitch());
            } else {
                accumulated.setX(accumulated.getX() + sample.getX());
                accumulated.setY(accumulated.getY() + sample.getY());
                accumulated.setZ(accumulated.getZ() + sample.getZ());
                accumulated.setYaw(accumulated.getYaw() + sample.getYaw());
                accumulated.setPitch(accumulated.getPitch() + sample.getPitch());
            }
        }
        if (accumulated == null) {
            return null;
        }
        accumulated.setX(accumulated.getX() / subSamples);
        accumulated.setY(accumulated.getY() / subSamples);
        accumulated.setZ(accumulated.getZ() / subSamples);
        accumulated.setYaw(accumulated.getYaw() / subSamples);
        accumulated.setPitch(accumulated.getPitch() / subSamples);
        return accumulated;
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
            boolean globalRequested = resolved.startsWith("global:");
            String normalized = globalRequested ? resolved.substring("global:".length()).trim() : resolved;
            if (globalRequested && !session.getScene().isAllowGlobalCommands() && !keyframe.isAllowGlobal()) {
                plugin.getLogger().warning("[scene-command] blocked global command because scene/keyframe policy denied it"
                        + " viewer=" + player.getName() + " session=" + session.getSessionId()
                        + " tick=" + session.getTimeTicks() + " command=" + normalized);
                continue;
            }
            if (keyframe.getExecutorMode() == CommandKeyframe.ExecutorMode.CONSOLE) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), normalized.replaceFirst("^console:", "").trim());
            } else {
                String sanitized = normalized.replaceFirst("^player:", "").trim();
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
        Particle particle = findParticleByName(keyframe.getParticleId());
        if (particle == null) {
            return;
        }
        player.spawnParticle(particle, location, 10, 0.2, 0.2, 0.2);
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
        Sound sound = findSoundByName(keyframe.getSoundId());
        if (sound != null) {
            player.playSound(location, sound, keyframe.getVolume(), keyframe.getPitch());
            return;
        }
        player.playSound(location, keyframe.getSoundId(), keyframe.getVolume(), keyframe.getPitch());
    }


    private Particle findParticleByName(String particleId) {
        NamespacedKey key = parseNamespacedKey(particleId);
        if (key == null) {
            return null;
        }
        return Registry.PARTICLE_TYPE.get(key);
    }

    private Sound findSoundByName(String soundId) {
        NamespacedKey key = parseNamespacedKey(soundId);
        if (key == null) {
            return null;
        }
        return Registry.SOUNDS.get(key);
    }

    private NamespacedKey parseNamespacedKey(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(id.toLowerCase(java.util.Locale.ROOT));
        if (key != null) {
            return key;
        }
        return NamespacedKey.minecraft(id.toLowerCase(java.util.Locale.ROOT));
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
        if (session.getCameraRigId() == null || player == null) {
            return null;
        }
        Location fallback = session.getLastCameraLocation();
        if (fallback == null) {
            fallback = player.getLocation();
        }
        return sessionManager.ensureCameraRig(session, player, fallback);
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

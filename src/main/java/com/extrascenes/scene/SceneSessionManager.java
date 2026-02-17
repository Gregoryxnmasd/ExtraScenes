package com.extrascenes.scene;

import com.extrascenes.CitizensAdapter;
import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.MovementSpeedAttributeResolver;
import com.extrascenes.SceneProtocolAdapter;
import com.extrascenes.visibility.SceneVisibilityController;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SceneSessionManager {
    private final ExtraScenesPlugin plugin;
    private final SceneVisibilityController visibilityController;
    private final SceneProtocolAdapter protocolAdapter;
    private final CitizensAdapter citizensAdapter;
    private final Map<UUID, SceneSession> sessions = new HashMap<>();
    private final Map<UUID, UUID> sceneEntityToPlayer = new HashMap<>();
    private final Map<UUID, SceneSession> pendingRestores = new HashMap<>();
    private final NamespacedKey movementLockKey;

    public SceneSessionManager(ExtraScenesPlugin plugin, SceneVisibilityController visibilityController,
                               SceneProtocolAdapter protocolAdapter) {
        this.plugin = plugin;
        this.visibilityController = visibilityController;
        this.protocolAdapter = protocolAdapter;
        this.citizensAdapter = plugin.getCitizensAdapter();
        this.movementLockKey = new NamespacedKey(plugin, "cutscene_movement_lock");
    }

    public SceneSession startScene(Player player, Scene scene) {
        return startScene(player, scene, false);
    }

    public SceneSession startScene(Player player, Scene scene, boolean preview) {
        return startScene(player, scene, preview, 0, scene.getDurationTicks());
    }

    public SceneSession startScene(Player player, Scene scene, boolean preview, int startTick, int endTick) {
        SceneSession existing = sessions.get(player.getUniqueId());
        if (existing != null) {
            stopScene(player, "restart");
        }

        SceneSession session = new SceneSession(player, scene, preview, startTick, endTick);
        sessions.put(player.getUniqueId(), session);
        session.setStartLocation(player.getLocation().clone());
        plugin.getLogger().info("[scene-session] start viewer=" + player.getName()
                + " session=" + session.getSessionId()
                + " preview=" + preview
                + " startTick=" + startTick
                + " endTick=" + endTick);

        if (scene.isFreezePlayer()) {
            player.setWalkSpeed(0.0f);
            player.setFlySpeed(0.0f);
        }

        session.setBlockingInventory(plugin.getConfig().getBoolean("player.blockInventoryDuringScene", true));

        CutscenePath cutscenePath = buildCutscenePath(scene);
        session.setCutscenePath(cutscenePath);
        session.resetSegmentCommandExecution();
        java.util.List<CutsceneFrame> timeline = CutsceneTimelineBuilder.build(cutscenePath);
        if (timeline.isEmpty()) {
            sessions.remove(player.getUniqueId());
            plugin.getLogger().severe("Scene " + scene.getName() + " has no camera points, aborting start for " + player.getName());
            return null;
        }
        session.setCutscenePath(cutscenePath);
        session.setCameraTimeline(timeline);

        Location rigStartLocation = timeline.get(0).getLocation().clone();
        rigStartLocation.setWorld(player.getWorld());

        Entity rig = ensureCameraRig(session, player, rigStartLocation);
        if (rig == null) {
            sessions.remove(player.getUniqueId());
            plugin.getLogger().severe("Unable to spawn camera rig for " + player.getName()
                    + " at " + rigStartLocation + "; aborting scene start.");
            return null;
        }
        forceLoadRigChunk(session, rigStartLocation);
        if (plugin.getConfig().getBoolean("camera.hide-others", true)) {
            visibilityController.hideEntityFromAllExcept(rig, player);
        }
        visibilityController.showEntityToPlayer(rig, player);

        session.setRestorePending(false);
        ItemStack originalHelmet = session.getSnapshot().getHelmet();
        session.setOriginalHelmet(originalHelmet == null ? null : originalHelmet.clone());
        if (plugin.getConfig().getBoolean("camera.fake-equip", true)) {
            player.getInventory().setHelmet(protocolAdapter.createMovementLockedPumpkin());
        }
        applyMovementLock(player);
        applyCameraZoomEffect(player);

        player.setGameMode(GameMode.SPECTATOR);
        protocolAdapter.applySpectatorCamera(player, rig);
        startSpectatorHandshake(session, player.getUniqueId(), rig.getUniqueId());

        plugin.getRuntimeEngine().startSession(session);

        Bukkit.getPluginManager().callEvent(new SceneStartEvent(player, scene));
        return session;
    }

    public void stopScene(Player player, String reason) {
        finishSession(player.getUniqueId(), reason, true, true);
    }

    public void handleDisconnect(Player player, String reason) {
        abortSession(player.getUniqueId(), reason);
    }

    public void abortSession(UUID playerId, String reason) {
        finishSession(playerId, reason, false, false);
    }

    public void stopAll(String reason) {
        for (UUID uuid : new HashSet<>(sessions.keySet())) {
            abortSession(uuid, reason);
        }
    }

    private void finishSession(UUID playerId, String reason, boolean teleportOnEnd, boolean openPreviewDashboard) {
        SceneSession session = sessions.remove(playerId);
        if (session == null) {
            return;
        }
        plugin.getLogger().info("[scene-session] finish session=" + session.getSessionId()
                + " viewer=" + playerId
                + " reason=" + reason
                + " teleportOnEnd=" + teleportOnEnd
                + " openPreviewDashboard=" + openPreviewDashboard);
        plugin.getRuntimeEngine().stopSession(session);

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            pendingRestores.remove(playerId);
            restorePlayerState(player, session);
            if (teleportOnEnd) {
                teleportOnEnd(player, session);
            }
            clearActionBar(player, session);
        } else if (!session.isRestorePending()) {
            pendingRestores.remove(playerId);
        }

        for (org.bukkit.scheduler.BukkitTask owned : session.getOwnedTasks()) {
            owned.cancel();
        }
        session.clearOwnedTasks();
        releaseForcedChunk(session);
        cleanupSessionEntities(session);

        if (session.getPlaybackTeleportCount() > 0) {
            plugin.getLogger().severe("Playback teleports detected for session=" + session.getSessionId()
                    + " viewer=" + session.getPlayerId()
                    + " count=" + session.getPlaybackTeleportCount()
                    + " lastCaller=" + session.getLastPlaybackTeleportCaller());
        }

        if (player != null) {
            Bukkit.getPluginManager().callEvent(new SceneEndEvent(player, session.getScene(), reason));
            if (openPreviewDashboard && session.isPreview() && plugin.getEditorEngine() != null) {
                EditorSession editorSession = plugin.getEditorEngine().getEditorSessionManager().getSession(player.getUniqueId());
                if (editorSession != null) {
                    editorSession.setPreviewPlaying(false);
                    plugin.getEditorEngine().openDashboard(player, editorSession, false);
                }
            }
        }
    }

    public SceneSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    public Collection<SceneSession> getActiveSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    public boolean isSceneEntity(UUID entityId) {
        return sceneEntityToPlayer.containsKey(entityId);
    }

    public SceneSession getSessionByEntity(UUID entityId) {
        UUID playerId = sceneEntityToPlayer.get(entityId);
        if (playerId == null) {
            return null;
        }
        return sessions.get(playerId);
    }

    public void registerSceneEntity(SceneSession session, Entity entity) {
        sceneEntityToPlayer.put(entity.getUniqueId(), session.getPlayerId());
    }

    public void unregisterSceneEntity(Entity entity) {
        sceneEntityToPlayer.remove(entity.getUniqueId());
        visibilityController.clearEntity(entity.getUniqueId());
    }

    public Entity ensureCameraRig(SceneSession session, Player viewer, Location preferredLocation) {
        if (session == null || viewer == null || !viewer.isOnline()) {
            return null;
        }
        Entity existing = resolveCameraRigEntity(session, viewer);
        if (existing != null && existing.isValid()) {
            return existing;
        }
        Location spawnLocation = preferredLocation == null ? viewer.getLocation().clone() : preferredLocation.clone();
        if (spawnLocation.getWorld() == null) {
            spawnLocation.setWorld(viewer.getWorld());
        }
        if (spawnLocation.getWorld() == null) {
            return null;
        }
        Entity rig = spawnCameraRig(spawnLocation);
        if (rig == null) {
            return null;
        }
        session.registerEntity(rig);
        registerSceneEntity(session, rig);
        session.setCameraRigId(rig.getUniqueId());
        session.setCameraRigWorld(rig.getWorld().getName());
        if (plugin.getConfig().getBoolean("camera.hide-others", true)) {
            visibilityController.hideEntityFromAllExcept(rig, viewer);
        }
        visibilityController.showEntityToPlayer(rig, viewer);
        plugin.getLogger().warning("Recreated missing camera rig for " + viewer.getName()
                + " session=" + session.getSessionId()
                + " rig=" + rig.getUniqueId());
        return rig;
    }

    private Entity resolveCameraRigEntity(SceneSession session, Player viewer) {
        UUID rigId = session.getCameraRigId();
        if (rigId == null) {
            return null;
        }
        Entity direct = Bukkit.getEntity(rigId);
        if (direct != null && direct.isValid()) {
            return direct;
        }
        if (session.getCameraRigWorld() != null) {
            World world = Bukkit.getWorld(session.getCameraRigWorld());
            if (world != null) {
                Entity byWorld = world.getEntity(rigId);
                if (byWorld != null && byWorld.isValid()) {
                    return byWorld;
                }
            }
        }
        if (viewer.getWorld() != null) {
            Entity byViewerWorld = viewer.getWorld().getEntity(rigId);
            if (byViewerWorld != null && byViewerWorld.isValid()) {
                return byViewerWorld;
            }
        }
        return null;
    }

    private Entity spawnCameraRig(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        try {
            Entity camera = CameraEntityFactory.spawn(plugin, location);
            return camera != null && camera.isValid() ? camera : null;
        } catch (Throwable throwable) {
            plugin.getLogger().severe("Failed to spawn camera entity: " + throwable.getMessage());
            return null;
        }
    }

    public void reapplyVisibility(Player player) {
        SceneSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        for (Entity entity : session.getSceneEntities()) {
            visibilityController.hideEntityFromAllExcept(entity, player);
        }
    }

    public void restoreIfPending(Player player) {
        SceneSession session = pendingRestores.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        session.setRestorePending(false);
        restorePlayerState(player, session);
    }

    public void markRestorePending(UUID playerId) {
        SceneSession session = sessions.get(playerId);
        if (session != null) {
            session.setRestorePending(true);
            pendingRestores.put(playerId, session);
        }
    }

    private void restorePlayerState(Player player, SceneSession session) {
        protocolAdapter.clearSpectatorCamera(player);
        player.setGameMode(session.getSnapshot().getGameMode());
        removeMovementLock(player);
        player.getInventory().setHelmet(session.getOriginalHelmet());
        player.getInventory().setContents(session.getSnapshot().getInventoryContents());
        player.getInventory().setArmorContents(session.getSnapshot().getArmorContents());
        player.getInventory().setItemInOffHand(session.getSnapshot().getOffHand());

        if (session.getScene().isFreezePlayer()) {
            player.setWalkSpeed(session.getSnapshot().getWalkSpeed());
            player.setFlySpeed(session.getSnapshot().getFlySpeed());
        }
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        session.getSnapshot().getPotionEffects().forEach(player::addPotionEffect);
        player.setFlying(session.getSnapshot().isFlying());
        Location original = session.getSnapshot().getLocation();
        if (original != null && original.getWorld() != null) {
            player.teleport(original);
        }
    }

    private void teleportOnEnd(Player player, SceneSession session) {
        EndTeleportMode mode = session.getScene().getEndTeleportMode();
        if (mode == EndTeleportMode.NONE) {
            return;
        }
        if (mode == EndTeleportMode.TELEPORT_TO_END && session.getScene().getEndLocation() != null) {
            Location target = session.getScene().getEndLocation().toBukkitLocation();
            if (target != null) {
                teleportPlayerWithDebug(player, target, "scene_end_target");
            }
            return;
        }
        Location start = session.getStartLocation();
        if (start != null) {
            teleportPlayerWithDebug(player, start, "scene_end_start");
        }
    }

    private void forceLoadRigChunk(SceneSession session, Location location) {
        if (session == null || location == null || location.getWorld() == null) {
            return;
        }
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        location.getWorld().setChunkForceLoaded(chunkX, chunkZ, true);
        session.setForcedChunk(location.getWorld().getName(), chunkX, chunkZ);
        plugin.getLogger().info("Camera rig chunk force-loaded for session=" + session.getSessionId()
                + " world=" + session.getForcedChunkWorld()
                + " chunkX=" + chunkX
                + " chunkZ=" + chunkZ);
    }

    private void releaseForcedChunk(SceneSession session) {
        if (session == null || session.getForcedChunkWorld() == null) {
            return;
        }
        World world = Bukkit.getWorld(session.getForcedChunkWorld());
        if (world != null) {
            world.setChunkForceLoaded(session.getForcedChunkX(), session.getForcedChunkZ(), false);
            plugin.getLogger().info("Camera rig chunk force-load released for session=" + session.getSessionId()
                    + " world=" + session.getForcedChunkWorld()
                    + " chunkX=" + session.getForcedChunkX()
                    + " chunkZ=" + session.getForcedChunkZ());
        }
    }

    private void teleportPlayerWithDebug(Player player, Location target, String reason) {
        if (player == null || target == null) {
            return;
        }
        SceneSession session = sessions.get(player.getUniqueId());
        String caller = resolveTeleportCaller();
        int tick = session != null ? session.getTimeTicks() : -1;
        if (session != null && session.getState() == SceneState.PLAYING
                && !reason.startsWith("start_scene")
                && !reason.startsWith("scene_end")) {
            session.incrementPlaybackTeleportCount(caller);
        }
        plugin.getLogger().info("[scene-teleport] viewer=" + player.getName()
                + " player=" + player.getUniqueId()
                + " tick=" + tick
                + " reason=" + reason
                + " caller=" + caller);
        player.teleport(target);
    }

    private String resolveTeleportCaller() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            if (!element.getClassName().startsWith("com.extrascenes")) {
                continue;
            }
            if (element.getMethodName().equals("teleportPlayerWithDebug")) {
                continue;
            }
            return element.getClassName() + "#" + element.getMethodName();
        }
        return "unknown";
    }

    private void clearActionBar(Player player, SceneSession session) {
        session.setActiveActionBarText(null);
        session.setActionBarUntilTick(0);
        player.sendActionBar("");
    }

    private void cleanupSessionEntities(SceneSession session) {
        for (SessionActorHandle actorHandle : session.getActorHandles().values()) {
            if (actorHandle.getCitizensNpc() != null && citizensAdapter.isAvailable()) {
                citizensAdapter.destroy(actorHandle.getCitizensNpc());
            }
        }
        for (Entity entity : session.getSceneEntities()) {
            unregisterSceneEntity(entity);
            if (entity.isValid()) {
                entity.remove();
            }
        }
        Player player = Bukkit.getPlayer(session.getPlayerId());
        if (player != null) {
            for (UUID entityId : session.getEntityTracker().getTrackedEntityIds()) {
                Entity tracked = player.getWorld().getEntity(entityId);
                if (tracked != null && tracked.isValid()) {
                    unregisterSceneEntity(tracked);
                    tracked.remove();
                }
            }
        }
        session.clearActorHandles();
        session.clearSceneEntities();
    }

    private void startSpectatorHandshake(SceneSession ownerSession, UUID playerId, UUID rigId) {
        org.bukkit.scheduler.BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                SceneSession session = sessions.get(playerId);
                if (session == null) {
                    cancel();
                    return;
                }
                Player player = Bukkit.getPlayer(playerId);
                if (player == null) {
                    cancel();
                    return;
                }
                Entity rig = Bukkit.getEntity(rigId);
                if (rig == null || !rig.isValid()) {
                    plugin.getLogger().warning("Camera rig missing before spectator lock completed for " + player.getName());
                    cancel();
                    return;
                }

                if (tick == 0) {
                    player.setGameMode(GameMode.SPECTATOR);
                    tick++;
                    return;
                }
                if (tick == 1) {
                    protocolAdapter.applySpectatorCamera(player, rig);
                    session.incrementSpectatorHandshakeAttempts();
                    tick++;
                    return;
                }

                Entity current = player.getSpectatorTarget();
                boolean matched = current != null && current.getUniqueId().equals(rigId);
                if (matched) {
                    session.setSpectatorHandshakeComplete(true);
                    plugin.getLogger().info("Spectator lock handshake succeeded for " + player.getName()
                            + " after attempts=" + session.getSpectatorHandshakeAttempts());
                    cancel();
                    return;
                }

                if (tick <= 10) {
                    protocolAdapter.applySpectatorCamera(player, rig);
                    session.incrementSpectatorHandshakeAttempts();
                    plugin.getLogger().warning("Spectator lock handshake retry tick=" + tick
                            + " for " + player.getName());
                    tick++;
                    return;
                }

                plugin.getLogger().severe("Spectator lock handshake failed for " + player.getName()
                        + " after attempts=" + session.getSpectatorHandshakeAttempts());
                cancel();
                abortSession(playerId, "spectator_handshake_failed");
            }
        }.runTaskTimer(plugin, 0L, 1L);
        ownerSession.registerOwnedTask(task);
    }

    private Location resolveRigStartLocation(Player player, Scene scene) {
        Track<CameraKeyframe> cameraTrack = scene.getTrack(SceneTrackType.CAMERA);
        if (cameraTrack != null && !cameraTrack.getKeyframes().isEmpty()) {
            CameraKeyframe keyframe = cameraTrack.getKeyframes().get(0);
            Transform transform = keyframe.getTransform();
            if (transform != null) {
                Location location = player.getLocation().clone();
                transform.applyTo(location);
                return location;
            }
        }
        return player.getLocation().clone();
    }

    private CutscenePath buildCutscenePath(Scene scene) {
        Track<CameraKeyframe> cameraTrack = scene.getTrack(SceneTrackType.CAMERA);
        java.util.List<CameraKeyframe> points = cameraTrack == null
                ? java.util.Collections.emptyList()
                : new java.util.ArrayList<>(cameraTrack.getKeyframes());
        double stepResolution = plugin.getConfig().getDouble("camera.step-resolution", 0.35D);
        String rawSegments = plugin.getConfig().getString("camera.player-segments", "");
        java.util.List<CutscenePath.IntRange> segments = new java.util.ArrayList<>();
        if (rawSegments != null && !rawSegments.isBlank()) {
            for (String token : rawSegments.split(",")) {
                String value = token.trim();
                if (value.isBlank()) {
                    continue;
                }
                if (value.contains("-")) {
                    String[] split = value.split("-");
                    if (split.length == 2) {
                        try {
                            int start = Integer.parseInt(split[0].trim());
                            int end = Integer.parseInt(split[1].trim());
                            segments.add(new CutscenePath.IntRange(Math.min(start, end), Math.max(start, end)));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                } else {
                    try {
                        int index = Integer.parseInt(value);
                        segments.add(new CutscenePath.IntRange(index, index));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        int durationTicks = scene.getDurationTicks() <= 0
                ? Math.max(1, points.stream().mapToInt(CameraKeyframe::getTimeTicks).max().orElse(0) + 1)
                : scene.getDurationTicks();
        java.util.List<String> startCommands = plugin.getConfig().getStringList("camera.start-commands");
        java.util.Map<Integer, java.util.List<String>> segmentCommands = new java.util.LinkedHashMap<>();
        org.bukkit.configuration.ConfigurationSection section = plugin.getConfig().getConfigurationSection("camera.segment-commands");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int segment = Integer.parseInt(key.trim());
                    segmentCommands.put(segment, section.getStringList(key));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return new CutscenePath(durationTicks, stepResolution, scene.getDefaultSmoothing(), points, segments,
                startCommands, segmentCommands);
    }


    private void applyCameraZoomEffect(Player player) {
        if (player == null) {
            return;
        }
        if (!plugin.getConfig().getBoolean("camera.zoom.enabled", true)) {
            return;
        }
        int amplifier = Math.max(0, plugin.getConfig().getInt("camera.zoom.slowness-level", 6) - 1);
        PotionEffect effect = new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, amplifier, false, false, false);
        player.addPotionEffect(effect, true);
    }

    private void applyMovementLock(Player player) {
        Attribute movementSpeed = MovementSpeedAttributeResolver.resolveMovementSpeedAttribute();
        if (movementSpeed == null) {
            return;
        }
        AttributeInstance attribute = player.getAttribute(movementSpeed);
        if (attribute == null) {
            return;
        }
        AttributeModifier existing = com.extrascenes.AttributeModifiers.findModifier(attribute, movementLockKey);
        if (existing != null) {
            return;
        }
        AttributeModifier modifier = com.extrascenes.AttributeModifiers.newModifier(movementLockKey, -10.0,
                AttributeModifier.Operation.ADD_NUMBER, org.bukkit.inventory.EquipmentSlotGroup.ANY);
        attribute.addModifier(modifier);
    }

    private void removeMovementLock(Player player) {
        Attribute movementSpeed = MovementSpeedAttributeResolver.resolveMovementSpeedAttribute();
        if (movementSpeed == null) {
            return;
        }
        AttributeInstance attribute = player.getAttribute(movementSpeed);
        if (attribute == null) {
            return;
        }
        com.extrascenes.AttributeModifiers.removeModifier(attribute, movementLockKey);
    }

}

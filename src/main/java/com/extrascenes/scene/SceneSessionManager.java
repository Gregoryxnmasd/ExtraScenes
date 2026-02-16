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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

        Location rigStartLocation = resolveRigStartLocation(player, scene);
        if (rigStartLocation.getWorld() == null || !rigStartLocation.getWorld().equals(player.getWorld())) {
            rigStartLocation.setWorld(player.getWorld());
            plugin.getLogger().warning("Camera rig world mismatch corrected for " + player.getName());
        }
        ArmorStand rig = (ArmorStand) rigStartLocation.getWorld().spawnEntity(rigStartLocation, EntityType.ARMOR_STAND);
        rig.setInvisible(true);
        rig.setMarker(true);
        rig.setGravity(false);
        rig.setSilent(true);
        rig.setInvulnerable(true);
        rig.setCollidable(false);
        rig.setSmall(true);
        session.registerEntity(rig);
        registerSceneEntity(session, rig);
        forceLoadRigChunk(session, rigStartLocation);
        visibilityController.hideEntityFromAllExcept(rig, player);
        visibilityController.showEntityToPlayer(rig, player);
        session.setCameraRigId(rig.getUniqueId());
        session.setCameraRigWorld(rig.getWorld().getName());
        plugin.getLogger().info("Camera rig spawned for " + player.getName() + " rig=" + rig.getUniqueId()
                + " marker=" + rig.isMarker());

        session.setRestorePending(false);
        ItemStack originalHelmet = session.getSnapshot().getHelmet();
        session.setOriginalHelmet(originalHelmet == null ? null : originalHelmet.clone());
        player.getInventory().setHelmet(protocolAdapter.createMovementLockedPumpkin());
        applyMovementLock(player);
        applyPlaybackZoomEffect(player);

        teleportPlayerWithDebug(player, rigStartLocation, "start_scene_rig");
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
        pendingRestores.remove(playerId);
        plugin.getRuntimeEngine().stopSession(session);

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            restorePlayerState(player, session);
            if (teleportOnEnd) {
                teleportOnEnd(player, session);
            }
            clearActionBar(player, session);
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
        restorePlayerState(player, session);
    }

    public void markRestorePending(UUID playerId) {
        SceneSession session = sessions.get(playerId);
        if (session != null) {
            pendingRestores.put(playerId, session);
        }
    }

    private void restorePlayerState(Player player, SceneSession session) {
        protocolAdapter.clearSpectatorCamera(player);
        player.setGameMode(session.getSnapshot().getGameMode());
        removeMovementLock(player);
        clearPlaybackZoomEffect(player);
        player.getInventory().setHelmet(session.getOriginalHelmet());

        if (session.getScene().isFreezePlayer()) {
            player.setWalkSpeed(session.getSnapshot().getWalkSpeed());
            player.setFlySpeed(session.getSnapshot().getFlySpeed());
        }
        player.setFlying(session.getSnapshot().isFlying());
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

    private void applyPlaybackZoomEffect(Player player) {
        PotionEffectType type = PotionEffectType.SLOWNESS;
        PotionEffect effect = new PotionEffect(type, Integer.MAX_VALUE, 4, false, false, false);
        player.addPotionEffect(effect);
        plugin.getLogger().info("[scene-zoom] applied slowness zoom effect to " + player.getName()
                + " amplifier=" + effect.getAmplifier());
    }

    private void clearPlaybackZoomEffect(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
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

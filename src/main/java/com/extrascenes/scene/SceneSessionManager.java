package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.SceneProtocolAdapter;
import com.extrascenes.visibility.SceneVisibilityController;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SceneSessionManager {
    private final ExtraScenesPlugin plugin;
    private final SceneVisibilityController visibilityController;
    private final SceneProtocolAdapter protocolAdapter;
    private final NamespacedKey movementLockKey;
    private final Map<UUID, SceneSession> sessions = new HashMap<>();
    private final Map<UUID, UUID> sceneEntityToPlayer = new HashMap<>();
    private final Map<UUID, SceneSession> pendingRestores = new HashMap<>();

    public SceneSessionManager(ExtraScenesPlugin plugin, SceneVisibilityController visibilityController,
                               SceneProtocolAdapter protocolAdapter) {
        this.plugin = plugin;
        this.visibilityController = visibilityController;
        this.protocolAdapter = protocolAdapter;
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

        if (scene.isFreezePlayer()) {
            player.setWalkSpeed(0.0f);
            player.setFlySpeed(0.0f);
        }

        session.setBlockingInventory(plugin.getConfig().getBoolean("player.blockInventoryDuringScene", true));

        Location rigStartLocation = resolveRigStartLocation(player, scene);
        ArmorStand rig = (ArmorStand) rigStartLocation.getWorld().spawnEntity(rigStartLocation, EntityType.ARMOR_STAND);
        rig.setInvisible(true);
        rig.setMarker(false);
        rig.setGravity(false);
        rig.setSilent(true);
        rig.setInvulnerable(true);
        rig.setCollidable(false);
        rig.setSmall(true);
        session.registerEntity(rig);
        registerSceneEntity(session, rig);
        visibilityController.hideEntityFromAllExcept(rig, player);
        visibilityController.showEntityToPlayer(rig, player);
        session.setCameraRigId(rig.getUniqueId());
        plugin.getLogger().info("Camera rig spawned for " + player.getName() + " rig=" + rig.getUniqueId()
                + " marker=" + rig.isMarker());

        session.setRestorePending(false);
        ItemStack originalHelmet = session.getSnapshot().getHelmet();
        session.setOriginalHelmet(originalHelmet == null ? null : originalHelmet.clone());
        player.getInventory().setHelmet(new ItemStack(Material.CARVED_PUMPKIN));
        applyMovementLock(player);

        player.teleport(rigStartLocation);
        player.setGameMode(GameMode.SPECTATOR);
        scheduleSpectatorApply(player.getUniqueId(), rig.getUniqueId(), 1L, "camera +1");
        scheduleSpectatorApply(player.getUniqueId(), rig.getUniqueId(), 2L, "camera +2");

        plugin.getRuntimeEngine().startSession(session);

        Bukkit.getPluginManager().callEvent(new SceneStartEvent(player, scene));
        return session;
    }

    public void stopScene(Player player, String reason) {
        SceneSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        plugin.getRuntimeEngine().stopSession(session);
        restorePlayerState(player, session);
        teleportOnEnd(player, session);
        clearActionBar(player, session);
        cleanupSessionEntities(session);
        Bukkit.getPluginManager().callEvent(new SceneEndEvent(player, session.getScene(), reason));

        if (session.isPreview() && plugin.getEditorEngine() != null) {
            EditorSession editorSession = plugin.getEditorEngine().getEditorSessionManager().getSession(player.getUniqueId());
            if (editorSession != null) {
                editorSession.setPreviewPlaying(false);
                plugin.getEditorEngine().openDashboard(player, editorSession, false);
            }
        }
    }

    public void handleDisconnect(Player player, String reason) {
        SceneSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        plugin.getRuntimeEngine().stopSession(session);
        restorePlayerState(player, session);
        cleanupSessionEntities(session);
        Bukkit.getPluginManager().callEvent(new SceneEndEvent(player, session.getScene(), reason));
    }

    public void stopAll(String reason) {
        for (UUID uuid : new HashSet<>(sessions.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                stopScene(player, reason);
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
                player.teleport(target);
            }
            return;
        }
        Location start = session.getStartLocation();
        if (start != null) {
            player.teleport(start);
        }
    }

    private void clearActionBar(Player player, SceneSession session) {
        session.setActiveActionBarText(null);
        session.setActionBarUntilTick(0);
        player.sendActionBar("");
    }

    private void cleanupSessionEntities(SceneSession session) {
        for (Entity entity : session.getSceneEntities()) {
            unregisterSceneEntity(entity);
            entity.remove();
        }
        session.clearSceneEntities();
    }

    private void scheduleSpectatorApply(UUID playerId, UUID rigId, long delayTicks, String label) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            SceneSession session = sessions.get(playerId);
            if (session == null) {
                return;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                return;
            }
            Entity rig = player.getWorld().getEntity(rigId);
            if (rig == null) {
                return;
            }
            protocolAdapter.applySpectatorCamera(player, rig);
            Entity current = player.getSpectatorTarget();
            boolean matched = current != null && current.getUniqueId().equals(rig.getUniqueId());
            plugin.getLogger().info("Camera rig target check (" + label + ") for " + player.getName()
                    + " matched=" + matched);
        }, delayTicks);
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

    private void applyMovementLock(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        Key key = Key.key(movementLockKey.toString());
        AttributeModifier existing = attribute.getModifier(key);
        if (existing != null) {
            return;
        }
        AttributeModifier modifier = new AttributeModifier(movementLockKey, -10.0, AttributeModifier.Operation.ADD_NUMBER);
        attribute.addModifier(modifier);
    }

    private void removeMovementLock(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        Key key = Key.key(movementLockKey.toString());
        if (attribute.getModifier(key) != null) {
            attribute.removeModifier(key);
        }
    }

}

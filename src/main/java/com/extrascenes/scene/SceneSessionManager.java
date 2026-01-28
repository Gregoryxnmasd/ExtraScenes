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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SceneSessionManager {
    private final ExtraScenesPlugin plugin;
    private final SceneVisibilityController visibilityController;
    private final SceneProtocolAdapter protocolAdapter;
    private final Map<UUID, SceneSession> sessions = new HashMap<>();
    private final Map<UUID, UUID> sceneEntityToPlayer = new HashMap<>();
    private final Map<UUID, SceneSession> pendingRestores = new HashMap<>();

    public SceneSessionManager(ExtraScenesPlugin plugin, SceneVisibilityController visibilityController,
                               SceneProtocolAdapter protocolAdapter) {
        this.plugin = plugin;
        this.visibilityController = visibilityController;
        this.protocolAdapter = protocolAdapter;
    }

    public SceneSession startScene(Player player, Scene scene) {
        return startScene(player, scene, false);
    }

    public SceneSession startScene(Player player, Scene scene, boolean preview) {
        SceneSession existing = sessions.get(player.getUniqueId());
        if (existing != null) {
            stopScene(player, "restart");
        }

        SceneSession session = new SceneSession(player, scene, preview);
        sessions.put(player.getUniqueId(), session);

        if (scene.isFreezePlayer()) {
            player.setWalkSpeed(0.0f);
            player.setFlySpeed(0.0f);
        }

        session.setBlockingInventory(plugin.getConfig().getBoolean("player.blockInventoryDuringScene", true));

        ArmorStand rig = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
        rig.setInvisible(true);
        rig.setMarker(true);
        rig.setGravity(false);
        rig.setSilent(true);
        rig.setInvulnerable(true);
        session.registerEntity(rig);
        registerSceneEntity(session, rig);
        visibilityController.hideEntityFromAllExcept(rig, player);
        session.setCameraRigId(rig.getUniqueId());

        session.setRestorePending(false);
        ItemStack fakeHelmet = new ItemStack(Material.CARVED_PUMPKIN);
        protocolAdapter.sendFakeHelmet(player, fakeHelmet);

        String cameraMode = scene.getCameraMode();
        boolean usePacket = "PACKET".equalsIgnoreCase(cameraMode) && protocolAdapter.isProtocolLibAvailable();
        if (!usePacket) {
            protocolAdapter.applySpectatorCamera(player, rig);
        } else {
            protocolAdapter.sendCameraPacket(player, rig);
        }

        Bukkit.getPluginManager().callEvent(new SceneStartEvent(player, scene));
        return session;
    }

    public void stopScene(Player player, String reason) {
        SceneSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        restorePlayerState(player, session);
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
        session.setRestorePending(true);
        pendingRestores.put(player.getUniqueId(), session);
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
        String cameraMode = session.getScene().getCameraMode();
        boolean usePacket = "PACKET".equalsIgnoreCase(cameraMode) && protocolAdapter.isProtocolLibAvailable();
        if (!usePacket) {
            protocolAdapter.clearSpectatorCamera(player);
            player.setGameMode(session.getSnapshot().getGameMode());
        }

        if (session.getScene().isFreezePlayer()) {
            player.setWalkSpeed(session.getSnapshot().getWalkSpeed());
            player.setFlySpeed(session.getSnapshot().getFlySpeed());
        }
        player.setFlying(session.getSnapshot().isFlying());

        protocolAdapter.sendFakeEquipmentRestore(player, session.getSnapshot().getHelmet() == null
                ? new ItemStack(Material.AIR)
                : session.getSnapshot().getHelmet());
    }

    private void cleanupSessionEntities(SceneSession session) {
        for (Entity entity : session.getSceneEntities()) {
            unregisterSceneEntity(entity);
            entity.remove();
        }
        session.clearSceneEntities();
    }
}

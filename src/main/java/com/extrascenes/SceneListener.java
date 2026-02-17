package com.extrascenes;

import com.extrascenes.scene.EditorInputManager;
import com.extrascenes.scene.ActorRecordingService;
import com.extrascenes.scene.EditorSessionManager;
import com.extrascenes.scene.EditorSession;
import com.extrascenes.scene.SceneEditorEngine;
import com.extrascenes.scene.SceneWand;
import com.extrascenes.scene.SceneSession;
import com.extrascenes.scene.SceneState;
import com.extrascenes.scene.SceneSessionManager;
import com.extrascenes.visibility.SceneVisibilityController;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

public class SceneListener implements Listener {
    private final SceneSessionManager sessionManager;
    private final SceneVisibilityController visibilityController;
    private final EditorSessionManager editorSessionManager;
    private final EditorInputManager inputManager;
    private final SceneEditorEngine editorEngine;
    private final ActorRecordingService actorRecordingService;

    public SceneListener(SceneSessionManager sessionManager, SceneVisibilityController visibilityController,
                         EditorSessionManager editorSessionManager, EditorInputManager inputManager,
                         SceneEditorEngine editorEngine, ActorRecordingService actorRecordingService) {
        this.sessionManager = sessionManager;
        this.visibilityController = visibilityController;
        this.editorSessionManager = editorSessionManager;
        this.inputManager = inputManager;
        this.editorEngine = editorEngine;
        this.actorRecordingService = actorRecordingService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        visibilityController.hideAllSceneEntities(event.getPlayer());
        sessionManager.restoreIfPending(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        actorRecordingService.stopRecording(event.getPlayer(), true);
        editorEngine.getPlugin().getRuntimeEngine().previewCleanup(event.getPlayer(), "player_quit");
        sessionManager.markRestorePending(event.getPlayer().getUniqueId());
        sessionManager.abortSession(event.getPlayer().getUniqueId(), "player_quit");
        if (editorSessionManager != null) {
            EditorSession editorSession = editorSessionManager.getSession(event.getPlayer().getUniqueId());
            if (editorSession != null && editorEngine.hasArmedPlacement(editorSession)) {
                editorEngine.cancelPlacementSilent(event.getPlayer(), editorSession);
            }
            editorSessionManager.removeSession(event.getPlayer().getUniqueId());
        }
        if (inputManager != null) {
            inputManager.clearPrompt(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        actorRecordingService.stopRecording(event.getPlayer(), true);
        editorEngine.getPlugin().getRuntimeEngine().previewCleanup(event.getPlayer(), "player_kick");
        sessionManager.markRestorePending(event.getPlayer().getUniqueId());
        sessionManager.abortSession(event.getPlayer().getUniqueId(), "player_kick");
        if (editorSessionManager != null) {
            EditorSession editorSession = editorSessionManager.getSession(event.getPlayer().getUniqueId());
            if (editorSession != null && editorEngine.hasArmedPlacement(editorSession)) {
                editorEngine.cancelPlacementSilent(event.getPlayer(), editorSession);
            }
            editorSessionManager.removeSession(event.getPlayer().getUniqueId());
        }
        if (inputManager != null) {
            inputManager.clearPrompt(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        editorEngine.getPlugin().getRuntimeEngine().previewCleanup(event.getPlayer(), "world_change");
        actorRecordingService.stopRecording(event.getPlayer(), true);
        visibilityController.hideAllSceneEntities(event.getPlayer());
        SceneSession session = sessionManager.getSession(event.getPlayer().getUniqueId());
        if (session != null) {
            sessionManager.stopScene(event.getPlayer(), "world_change");
            return;
        }
        sessionManager.reapplyVisibility(event.getPlayer());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        visibilityController.hideAllSceneEntities(event.getPlayer());
        sessionManager.reapplyVisibility(event.getPlayer());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        Entity target = event.getEntity();
        if (sessionManager.isSceneEntity(target.getUniqueId())) {
            SceneSession owner = sessionManager.getSessionByEntity(target.getUniqueId());
            if (owner == null || !owner.getPlayerId().equals(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        SceneSession session = sessionManager.getSession(event.getPlayer().getUniqueId());
        if (session != null && session.getState() == SceneState.PLAYING) {
            event.setCancelled(true);
            return;
        }
        Entity target = event.getRightClicked();
        if (sessionManager.isSceneEntity(target.getUniqueId())) {
            SceneSession owner = sessionManager.getSessionByEntity(target.getUniqueId());
            if (owner == null || !owner.getPlayerId().equals(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        SceneSession session = sessionManager.getSession(event.getPlayer().getUniqueId());
        if (session != null && session.getState() == SceneState.PLAYING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        SceneSession session = sessionManager.getSession(event.getPlayer().getUniqueId());
        if (session != null && session.getState() == SceneState.PLAYING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        SceneSession session = sessionManager.getSession(event.getPlayer().getUniqueId());
        if (session == null || session.getState() != SceneState.PLAYING) {
            return;
        }
        if (!editorEngine.getPlugin().getConfig().getBoolean("locks.movement", true)) {
            return;
        }
        if (event.getPlayer().getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }
        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
            org.bukkit.Location locked = event.getFrom().clone();
            locked.setYaw(event.getFrom().getYaw());
            locked.setPitch(event.getFrom().getPitch());
            event.setTo(locked);
            return;
        }
        if (event.getFrom().getYaw() != event.getTo().getYaw()
                || event.getFrom().getPitch() != event.getTo().getPitch()) {
            org.bukkit.Location locked = event.getTo().clone();
            locked.setYaw(event.getFrom().getYaw());
            locked.setPitch(event.getFrom().getPitch());
            event.setTo(locked);
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        SceneSession session = sessionManager.getSession(event.getPlayer().getUniqueId());
        if (session != null && session.getState() == SceneState.PLAYING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSwapHands(PlayerSwapHandItemsEvent event) {
        SceneSession session = sessionManager.getSession(event.getPlayer().getUniqueId());
        if (session != null && session.getState() == SceneState.PLAYING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        SceneSession session = sessionManager.getSession(player.getUniqueId());
        if (session != null
                && session.getState() == SceneState.PLAYING
                && session.isBlockingInventory()) {
            if (event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR
                    && event.getSlot() == 39) {
                event.setCancelled(true);
                editorEngine.getPlugin().getLogger().fine("[scene-inventory-lock] blocked helmet interaction for "
                        + player.getName() + " session=" + session.getSessionId());
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && SceneWand.isRecordingWand(item)
                && actorRecordingService.isRecording(player)) {
            actorRecordingService.stopRecording(player, true);
            player.getInventory().remove(item);
            player.sendActionBar("§aRecording stopped");
            event.setCancelled(true);
            return;
        }
        EditorSession editorSession = editorSessionManager.getSession(player.getUniqueId());
        if (editorSession != null && editorEngine.hasArmedPlacement(editorSession)) {
            if (SceneWand.isWand(item)) {
                if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    editorEngine.confirmPlacement(player, editorSession);
                } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    editorEngine.cancelPlacement(player, editorSession);
                }
                event.setCancelled(true);
                return;
            }
        }

        SceneSession session = sessionManager.getSession(player.getUniqueId());
        if (session != null && session.getState() == SceneState.PLAYING) {
            event.setCancelled(true);
        }
    }
}

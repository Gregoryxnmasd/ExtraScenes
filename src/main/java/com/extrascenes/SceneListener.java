package com.extrascenes;

import com.extrascenes.scene.EditorInputManager;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

public class SceneListener implements Listener {
    private final SceneSessionManager sessionManager;
    private final SceneVisibilityController visibilityController;
    private final EditorSessionManager editorSessionManager;
    private final EditorInputManager inputManager;
    private final SceneEditorEngine editorEngine;

    public SceneListener(SceneSessionManager sessionManager, SceneVisibilityController visibilityController,
                         EditorSessionManager editorSessionManager, EditorInputManager inputManager,
                         SceneEditorEngine editorEngine) {
        this.sessionManager = sessionManager;
        this.visibilityController = visibilityController;
        this.editorSessionManager = editorSessionManager;
        this.inputManager = inputManager;
        this.editorEngine = editorEngine;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        visibilityController.hideAllSceneEntities(event.getPlayer());
        sessionManager.restoreIfPending(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessionManager.handleDisconnect(event.getPlayer(), "player_quit");
        if (editorSessionManager != null) {
            EditorSession editorSession = editorSessionManager.getSession(event.getPlayer().getUniqueId());
            if (editorSession != null && editorSession.getArmedTick() != null) {
                editorEngine.cancelCameraPlacementSilent(event.getPlayer(), editorSession);
            }
            editorSessionManager.removeSession(event.getPlayer().getUniqueId());
        }
        if (inputManager != null) {
            inputManager.clearPrompt(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        visibilityController.hideAllSceneEntities(event.getPlayer());
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
        Entity target = event.getRightClicked();
        if (sessionManager.isSceneEntity(target.getUniqueId())) {
            SceneSession owner = sessionManager.getSessionByEntity(target.getUniqueId());
            if (owner == null || !owner.getPlayerId().equals(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        SceneSession session = sessionManager.getSession(player.getUniqueId());
        if (session != null && session.isBlockingInventory()) {
            if (event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR
                    && event.getSlot() == 39) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        EditorSession editorSession = editorSessionManager.getSession(player.getUniqueId());
        if (editorSession != null && editorSession.getArmedTick() != null) {
            ItemStack item = event.getItem();
            if (SceneWand.isWand(item)) {
                if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    editorEngine.confirmCameraPlacement(player, editorSession);
                } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    editorEngine.cancelCameraPlacement(player, editorSession);
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

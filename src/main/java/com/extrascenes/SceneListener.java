package com.extrascenes;

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

public class SceneListener implements Listener {
    private final SceneSessionManager sessionManager;
    private final SceneVisibilityController visibilityController;

    public SceneListener(SceneSessionManager sessionManager, SceneVisibilityController visibilityController) {
        this.sessionManager = sessionManager;
        this.visibilityController = visibilityController;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        visibilityController.hideAllSceneEntities(event.getPlayer());
        sessionManager.restoreIfPending(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessionManager.handleDisconnect(event.getPlayer(), "player_quit");
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
        SceneSession session = sessionManager.getSession(event.getPlayer().getUniqueId());
        if (session != null && session.getState() == SceneState.PLAYING) {
            event.setCancelled(true);
        }
    }
}

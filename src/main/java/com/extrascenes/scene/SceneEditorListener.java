package com.extrascenes.scene;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class SceneEditorListener implements Listener {
    private final SceneEditorEngine editorEngine;
    private final EditorSessionManager editorSessionManager;

    public SceneEditorListener(SceneEditorEngine editorEngine, EditorSessionManager editorSessionManager) {
        this.editorEngine = editorEngine;
        this.editorSessionManager = editorSessionManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (!title.startsWith(GuiUtils.TITLE_PREFIX)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        EditorSession session = editorSessionManager.getSession(player.getUniqueId());
        if (session == null) {
            return;
        }
        Scene scene = session.getScene();
        editorEngine.handleClick(player, scene, session, event.getRawSlot(), event.isRightClick(),
                event.isShiftClick(), event.getCurrentItem());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (!title.startsWith(GuiUtils.TITLE_PREFIX)) {
            return;
        }
        EditorSession session = editorSessionManager.getSession(player.getUniqueId());
        if (session != null) {
            session.clearHistory();
        }
    }
}

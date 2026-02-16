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
        if (editorEngine.isMainMenuTitle(title)) {
            editorEngine.handleMainMenuClick(player, event.getRawSlot());
            return;
        }
        if (editorEngine.isMainMenuDeleteConfirmTitle(title)) {
            editorEngine.handleMainMenuDeleteConfirmClick(player, event.getRawSlot());
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
            editorEngine.getPlugin().getServer().getScheduler().runTask(editorEngine.getPlugin(), () -> {
                if (player.getOpenInventory() == null
                        || !player.getOpenInventory().getTitle().startsWith(GuiUtils.TITLE_PREFIX)) {
                    editorEngine.getPlugin().getRuntimeEngine().cleanupEditorPreview(player);
                    editorEngine.getPlugin().getActorRecordingService().stopRecording(player, true);
                    if (session.isPreviewPlaying()) {
                        editorEngine.getPlugin().getSessionManager().stopScene(player, "editor_inventory_close");
                        session.setPreviewPlaying(false);
                    }
                }
            });
        }
    }
}

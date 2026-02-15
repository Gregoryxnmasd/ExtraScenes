package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class EditorChatListener implements Listener {
    private final ExtraScenesPlugin plugin;
    private final EditorInputManager inputManager;
    private final SceneEditorEngine editorEngine;

    public EditorChatListener(ExtraScenesPlugin plugin, EditorInputManager inputManager, SceneEditorEngine editorEngine) {
        this.plugin = plugin;
        this.inputManager = inputManager;
        this.editorEngine = editorEngine;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        boolean hasEditorPrompt = inputManager.hasPrompt(event.getPlayer().getUniqueId());
        boolean hasMainPrompt = editorEngine.hasMainMenuPrompt(event.getPlayer().getUniqueId());
        if (!hasEditorPrompt && !hasMainPrompt) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (hasMainPrompt) {
                editorEngine.handleMainMenuChat(event.getPlayer(), message);
            }
            if (hasEditorPrompt) {
                inputManager.handleChat(event.getPlayer(), message);
            }
        });
    }
}

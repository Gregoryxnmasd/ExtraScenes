package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class EditorChatListener implements Listener {
    private final ExtraScenesPlugin plugin;
    private final EditorInputManager inputManager;

    public EditorChatListener(ExtraScenesPlugin plugin, EditorInputManager inputManager) {
        this.plugin = plugin;
        this.inputManager = inputManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!inputManager.hasPrompt(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        plugin.getServer().getScheduler().runTask(plugin, () -> inputManager.handleChat(event.getPlayer(), message));
    }
}

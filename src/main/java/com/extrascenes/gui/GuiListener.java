package com.extrascenes.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.extrascenes.runtime.CutsceneRuntime;

public class GuiListener implements Listener {
    private final CutsceneRuntime runtime;

    public GuiListener(JavaPlugin plugin, CutsceneRuntime runtime) {
        this.runtime = runtime;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (runtime.isInPlayback(player) || isPluginGuiInventory(event.getInventory().getHolder())) {
            event.setCancelled(true);
        }
    }

    private boolean isPluginGuiInventory(Object holder) {
        return holder instanceof PluginGuiHolder;
    }
}

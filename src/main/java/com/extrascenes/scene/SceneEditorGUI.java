package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class SceneEditorGUI {
    private final ExtraScenesPlugin plugin;
    private final Scene scene;

    public SceneEditorGUI(ExtraScenesPlugin plugin, Scene scene) {
        this.plugin = plugin;
        this.scene = scene;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 54, ChatColor.DARK_AQUA + "Scene Editor: " + scene.getName());
        player.openInventory(inventory);
    }
}

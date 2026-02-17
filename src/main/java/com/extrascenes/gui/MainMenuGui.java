package com.extrascenes.gui;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.extrascenes.core.Scene;

public class MainMenuGui implements PluginGuiHolder {
    private final Inventory inventory;

    public MainMenuGui(Map<String, Scene> scenes) {
        this.inventory = Bukkit.createInventory(this, 54, "Scene Main Menu");
        int slot = 0;
        for (Scene scene : scenes.values()) {
            if (slot >= 45) break;
            ItemStack item = new ItemStack(Material.COMPASS);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§a" + scene.getName());
            item.setItemMeta(meta);
            inventory.setItem(slot++, item);
        }
        inventory.setItem(53, new ItemStack(Material.ANVIL));
    }

    public void open(Player player) { player.openInventory(inventory); }

    @Override
    public Inventory getInventory() { return inventory; }
}

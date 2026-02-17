package com.extrascenes.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.extrascenes.core.Scene;

public class SceneEditorGui implements PluginGuiHolder {
    private final Inventory inventory;

    public SceneEditorGui(Scene scene) {
        this.inventory = Bukkit.createInventory(this, 27, "Edit: " + scene.getName());
        inventory.setItem(11, new ItemStack(Material.CLOCK));
        inventory.setItem(13, new ItemStack(Material.ENDER_EYE));
        inventory.setItem(15, new ItemStack(Material.PLAYER_HEAD));
    }

    @Override public Inventory getInventory() { return inventory; }
}

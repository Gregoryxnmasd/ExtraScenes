package com.extrascenes.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CameraEditorGui implements PluginGuiHolder {
    private final Inventory inventory = Bukkit.createInventory(this, 27, "Camera Editor");

    public CameraEditorGui() {
        inventory.setItem(10, new ItemStack(Material.GREEN_WOOL));
        inventory.setItem(12, new ItemStack(Material.RED_WOOL));
        inventory.setItem(14, new ItemStack(Material.COMPASS));
    }

    @Override public Inventory getInventory() { return inventory; }
}

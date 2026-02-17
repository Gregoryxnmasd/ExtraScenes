package com.extrascenes.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ActorRecordGui implements PluginGuiHolder {
    private final Inventory inventory = Bukkit.createInventory(this, 27, "Actor Recording");

    public ActorRecordGui() {
        inventory.setItem(11, new ItemStack(Material.CLOCK));
        inventory.setItem(13, new ItemStack(Material.LIME_DYE));
        inventory.setItem(15, new ItemStack(Material.RED_DYE));
    }

    @Override public Inventory getInventory() { return inventory; }
}

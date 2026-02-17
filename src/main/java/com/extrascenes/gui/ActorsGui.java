package com.extrascenes.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ActorsGui implements PluginGuiHolder {
    private final Inventory inventory = Bukkit.createInventory(this, 27, "Actors");

    public ActorsGui() {
        inventory.setItem(11, new ItemStack(Material.PLAYER_HEAD));
        inventory.setItem(13, new ItemStack(Material.NAME_TAG));
        inventory.setItem(15, new ItemStack(Material.BARRIER));
    }

    @Override public Inventory getInventory() { return inventory; }
}

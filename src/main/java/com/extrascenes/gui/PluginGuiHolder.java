package com.extrascenes.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface PluginGuiHolder extends InventoryHolder {
    @Override
    Inventory getInventory();
}

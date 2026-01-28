package com.extrascenes.scene;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class SceneWand {
    public static final String WAND_NAME = ChatColor.AQUA + "Scene Wand";

    private SceneWand() {
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(WAND_NAME);
            meta.setLore(List.of(ChatColor.GRAY + "Left click: set camera point", ChatColor.GRAY + "Right click: cancel"));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && WAND_NAME.equals(meta.getDisplayName());
    }
}

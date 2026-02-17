package com.extrascenes.runtime;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ZoomController {
    private final JavaPlugin plugin;

    public ZoomController(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void apply(Player player) {
        int amplifier = Math.max(0, plugin.getConfig().getInt("zoom.slownessAmplifier", 6));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, amplifier, false, false, false));
    }

    public void remove(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }
}

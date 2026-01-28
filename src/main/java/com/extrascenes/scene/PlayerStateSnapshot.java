package com.extrascenes.scene;

import java.util.Collection;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class PlayerStateSnapshot {
    private final GameMode gameMode;
    private final boolean flying;
    private final float flySpeed;
    private final float walkSpeed;
    private final double health;
    private final Collection<PotionEffect> potionEffects;
    private final ItemStack helmet;

    public PlayerStateSnapshot(Player player) {
        this.gameMode = player.getGameMode();
        this.flying = player.isFlying();
        this.flySpeed = player.getFlySpeed();
        this.walkSpeed = player.getWalkSpeed();
        Attribute maxHealthAttribute = resolveMaxHealthAttribute();
        this.health = maxHealthAttribute != null && player.getAttribute(maxHealthAttribute) != null
                ? player.getHealth()
                : 20.0;
        this.potionEffects = player.getActivePotionEffects();
        this.helmet = player.getInventory().getHelmet();
    }

    private static Attribute resolveMaxHealthAttribute() {
        try {
            return Attribute.valueOf("GENERIC_MAX_HEALTH");
        } catch (IllegalArgumentException ignored) {
            try {
                return Attribute.valueOf("MAX_HEALTH");
            } catch (IllegalArgumentException fallbackIgnored) {
                return null;
            }
        }
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public boolean isFlying() {
        return flying;
    }

    public float getFlySpeed() {
        return flySpeed;
    }

    public float getWalkSpeed() {
        return walkSpeed;
    }

    public double getHealth() {
        return health;
    }

    public Collection<PotionEffect> getPotionEffects() {
        return potionEffects;
    }

    public ItemStack getHelmet() {
        return helmet;
    }
}

package com.extrascenes.scene;

import java.util.Collection;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.Location;

public class PlayerStateSnapshot {
    private static final NamespacedKey GENERIC_MAX_HEALTH_KEY = NamespacedKey.minecraft("generic.max_health");
    private static final NamespacedKey LEGACY_MAX_HEALTH_KEY = NamespacedKey.minecraft("max_health");
    private final GameMode gameMode;
    private final boolean flying;
    private final float flySpeed;
    private final float walkSpeed;
    private final double health;
    private final Collection<PotionEffect> potionEffects;
    private final ItemStack helmet;
    private final ItemStack[] inventoryContents;
    private final ItemStack[] armorContents;
    private final ItemStack offHand;
    private final Location location;

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
        PlayerInventory inventory = player.getInventory();
        this.helmet = inventory.getHelmet();
        this.inventoryContents = inventory.getContents().clone();
        this.armorContents = inventory.getArmorContents().clone();
        this.offHand = inventory.getItemInOffHand() == null ? null : inventory.getItemInOffHand().clone();
        this.location = player.getLocation().clone();
    }

    private static Attribute resolveMaxHealthAttribute() {
        Attribute attribute = Registry.ATTRIBUTE.get(GENERIC_MAX_HEALTH_KEY);
        if (attribute != null) {
            return attribute;
        }
        return Registry.ATTRIBUTE.get(LEGACY_MAX_HEALTH_KEY);
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

    public ItemStack[] getInventoryContents() {
        return inventoryContents.clone();
    }

    public ItemStack[] getArmorContents() {
        return armorContents.clone();
    }

    public ItemStack getOffHand() {
        return offHand == null ? null : offHand.clone();
    }

    public Location getLocation() {
        return location == null ? null : location.clone();
    }
}

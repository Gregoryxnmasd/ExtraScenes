package com.extrascenes;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;

public class SceneProtocolAdapter {
    private final ExtraScenesPlugin plugin;
    private final boolean protocolLibAvailable;
    private final NamespacedKey cutsceneSpeedLockKey;

    public SceneProtocolAdapter(ExtraScenesPlugin plugin) {
        this.plugin = plugin;
        this.protocolLibAvailable = plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib");
        this.cutsceneSpeedLockKey = new NamespacedKey(plugin, "cutscene_speed_lock");
    }

    public boolean isProtocolLibAvailable() {
        return protocolLibAvailable;
    }

    public void sendCameraPacket(Player player, Entity cameraEntity) {
        if (!protocolLibAvailable) {
            return;
        }
        // Placeholder for ProtocolLib-based camera packet targeting.
        // Intentionally no-op unless ProtocolLib is present.
    }

    public void applySpectatorCamera(Player player, Entity cameraEntity) {
        if (player == null || cameraEntity == null) {
            return;
        }
        if (player.getGameMode() != GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SPECTATOR);
        }
        Entity current = player.getSpectatorTarget();
        if (current != null && current.getUniqueId().equals(cameraEntity.getUniqueId())) {
            return;
        }
        player.setSpectatorTarget(cameraEntity);
    }

    public void clearSpectatorCamera(Player player) {
        player.setSpectatorTarget(null);
    }

    public void sendFakeHelmet(Player player, ItemStack itemStack, double movementSpeedAmount) {
        ensurePumpkinMovementModifier(itemStack, movementSpeedAmount);
        player.sendEquipmentChange(player, EquipmentSlot.HEAD, itemStack);
    }

    public ItemStack createMovementLockedPumpkin(double movementSpeedAmount) {
        ItemStack itemStack = new ItemStack(Material.CARVED_PUMPKIN);
        ensurePumpkinMovementModifier(itemStack, movementSpeedAmount);
        return itemStack;
    }

    public void sendFakeEquipmentRestore(Player player, ItemStack itemStack) {
        player.sendEquipmentChange(player, EquipmentSlot.HEAD, itemStack);
    }

    private void ensurePumpkinMovementModifier(ItemStack itemStack, double movementSpeedAmount) {
        if (itemStack == null || itemStack.getType() != Material.CARVED_PUMPKIN) {
            return;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        Attribute attribute = MovementSpeedAttributeResolver.resolveMovementSpeedAttribute();
        if (attribute == null) {
            return;
        }
        Collection<AttributeModifier> existingModifiers = meta.getAttributeModifiers(attribute);
        boolean hasCorrectModifier = false;
        if (existingModifiers != null && !existingModifiers.isEmpty()) {
            for (AttributeModifier modifier : new ArrayList<>(existingModifiers)) {
                if (!AttributeModifiers.hasKey(modifier, cutsceneSpeedLockKey)) {
                    continue;
                }
                if (modifier.getAmount() == movementSpeedAmount
                        && modifier.getOperation() == AttributeModifier.Operation.ADD_NUMBER
                        && modifier.getSlotGroup() == EquipmentSlotGroup.HEAD) {
                    hasCorrectModifier = true;
                } else {
                    meta.removeAttributeModifier(attribute, modifier);
                }
            }
        }
        if (!hasCorrectModifier) {
            AttributeModifier modifier = AttributeModifiers.newModifier(
                    cutsceneSpeedLockKey,
                    movementSpeedAmount,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlot.HEAD.getGroup()
            );
            meta.addAttributeModifier(attribute, modifier);
        }
        itemStack.setItemMeta(meta);
    }
}

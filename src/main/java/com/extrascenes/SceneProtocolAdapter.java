package com.extrascenes;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class SceneProtocolAdapter {
    private final ExtraScenesPlugin plugin;
    private final boolean protocolLibAvailable;

    public SceneProtocolAdapter(ExtraScenesPlugin plugin) {
        this.plugin = plugin;
        this.protocolLibAvailable = plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib");
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
        player.setGameMode(GameMode.SPECTATOR);
        player.setSpectatorTarget(cameraEntity);
    }

    public void clearSpectatorCamera(Player player) {
        player.setSpectatorTarget(null);
    }

    public void sendFakeHelmet(Player player, ItemStack itemStack) {
        player.sendEquipmentChange(player, EquipmentSlot.HEAD, itemStack);
    }

    public void sendFakeEquipmentRestore(Player player, ItemStack itemStack) {
        player.sendEquipmentChange(player, EquipmentSlot.HEAD, itemStack);
    }
}

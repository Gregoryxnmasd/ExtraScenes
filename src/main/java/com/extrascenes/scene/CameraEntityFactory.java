package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;

public final class CameraEntityFactory {
    private CameraEntityFactory() {
    }

    public static Entity spawn(ExtraScenesPlugin plugin, Location location) {
        String type = plugin.getConfig().getString("camera.camera-entity",
                plugin.getConfig().getString("camera.entity-type", "armor_stand")).toLowerCase(java.util.Locale.ROOT);
        Entity entity = switch (type) {
            case "item_display" -> location.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY);
            case "text_display" -> location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);
            case "interaction" -> location.getWorld().spawnEntity(location, EntityType.INTERACTION);
            case "armorstand", "armor_stand" -> location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
            default -> location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        };
        configure(plugin, entity);
        return entity;
    }

    private static void configure(ExtraScenesPlugin plugin, Entity entity) {
        entity.setSilent(true);
        entity.setInvulnerable(true);
        entity.setGravity(false);
        entity.setPersistent(false);
        entity.setVisibleByDefault(false);

        if (entity instanceof ArmorStand armorStand) {
            boolean invisible = plugin.getConfig().getBoolean("camera.armorstand-invisible", true);
            armorStand.setInvisible(invisible);
            armorStand.setMarker(true);
            armorStand.setBasePlate(false);
            armorStand.setSmall(true);
            armorStand.setCollidable(false);
            return;
        }
        if (entity instanceof Interaction interaction) {
            interaction.setResponsive(false);
            interaction.setInteractionWidth(0.1F);
            interaction.setInteractionHeight(0.1F);
        }
    }
}

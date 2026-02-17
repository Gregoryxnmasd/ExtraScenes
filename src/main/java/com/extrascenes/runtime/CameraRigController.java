package com.extrascenes.runtime;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CameraRigController {
    private final JavaPlugin plugin;

    public CameraRigController(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Entity spawnRig(Location location) {
        World world = location.getWorld();
        if (world == null) return null;
        ArmorStand stand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        stand.setCollidable(false);
        return stand;
    }

    public void lockToRig(Player player, Entity rig) {
        player.setSpectatorTarget(rig);
    }

    public boolean enforceLock(Player player, UUID rigUuid) {
        Entity target = player.getSpectatorTarget();
        if (target == null || !target.getUniqueId().equals(rigUuid)) {
            Entity rig = find(player, rigUuid);
            if (rig == null) return false;
            player.setSpectatorTarget(rig);
        }
        return true;
    }

    public Entity find(Player player, UUID uuid) {
        for (World world : plugin.getServer().getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null) return entity;
        }
        return null;
    }

    public void moveRig(Entity rig, Location location) {
        rig.teleport(location);
    }

    public void destroyRig(UUID rigUuid) {
        for (World world : plugin.getServer().getWorlds()) {
            Entity entity = world.getEntity(rigUuid);
            if (entity != null) {
                entity.remove();
                return;
            }
        }
    }
}

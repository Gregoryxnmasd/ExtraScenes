package com.extrascenes.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.Attributable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.extrascenes.core.ActorTemplate;
import com.extrascenes.core.Transform;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;

public class ActorController {
    private final JavaPlugin plugin;
    private final NPCRegistry registry = CitizensAPI.createNamedNPCRegistry("extrascenes-session-" + UUID.randomUUID(), null);
    private final Map<UUID, NPC> byEntity = new HashMap<>();

    public ActorController(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Map<String, UUID> spawnSessionActors(Player viewer, Map<String, ActorTemplate> templates, Transform fallback) {
        Map<String, UUID> ids = new HashMap<>();
        for (ActorTemplate template : templates.values()) {
            Location spawn = toLocation(firstTransform(template, fallback));
            if (spawn == null) continue;
            NPC npc = registry.createNPC(EntityType.PLAYER, template.getActorId());
            if (template.getSkin() != null && !template.getSkin().isBlank()) {
                npc.getOrAddTrait(SkinTrait.class).setSkinName(template.getSkin());
            }
            npc.spawn(spawn);
            Entity entity = npc.getEntity();
            if (entity instanceof Attributable attributable && attributable.getAttribute(Attribute.SCALE) != null) {
                attributable.getAttribute(Attribute.SCALE).setBaseValue(template.getScale());
            }
            if (entity != null) {
                ids.put(template.getActorId(), entity.getUniqueId());
                byEntity.put(entity.getUniqueId(), npc);
                hideFromOthers(viewer, entity);
            }
        }
        return ids;
    }

    public void tick(Map<String, UUID> actorIds, Map<String, ActorTemplate> templates, int tick) {
        for (Map.Entry<String, UUID> entry : actorIds.entrySet()) {
            ActorTemplate template = templates.get(entry.getKey());
            if (template == null) continue;
            Transform transform = exactOrPrevious(template, tick);
            if (transform == null) continue;
            Entity entity = find(entry.getValue());
            if (entity == null) continue;
            Location loc = toLocation(transform);
            if (loc != null) entity.teleport(loc);
        }
    }

    public void cleanup(Map<String, UUID> actorIds) {
        for (UUID uuid : actorIds.values()) {
            NPC npc = byEntity.remove(uuid);
            if (npc != null) {
                npc.destroy();
                registry.deregister(npc);
                continue;
            }
            Entity entity = find(uuid);
            if (entity != null) entity.remove();
        }
    }

    public void hideFrom(Player player) {
        for (UUID uuid : byEntity.keySet()) {
            Entity entity = find(uuid);
            if (entity != null) player.hideEntity(plugin, entity);
        }
    }

    private Transform firstTransform(ActorTemplate template, Transform fallback) {
        return template.getRecording().entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).findFirst().orElse(fallback);
    }

    private Transform exactOrPrevious(ActorTemplate template, int tick) {
        if (template.getRecording().containsKey(tick)) return template.getRecording().get(tick);
        return template.getRecording().entrySet().stream().filter(e -> e.getKey() <= tick).max(Map.Entry.comparingByKey()).map(Map.Entry::getValue).orElse(null);
    }

    private Location toLocation(Transform t) {
        if (t == null) return null;
        World world = Bukkit.getWorld(t.world());
        if (world == null) return null;
        return new Location(world, t.x(), t.y(), t.z(), t.yaw(), t.pitch());
    }

    private Entity find(UUID uuid) {
        for (World world : Bukkit.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null) return entity;
        }
        return null;
    }

    private void hideFromOthers(Player viewer, Entity entity) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(viewer.getUniqueId())) {
                online.showEntity(plugin, entity);
            } else {
                online.hideEntity(plugin, entity);
            }
        }
    }
}

package com.extrascenes;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CitizensAdapter {
    private final ExtraScenesPlugin plugin;
    private boolean available;
    private Object npcRegistry;
    private Method createNpcMethod;
    private Method npcSpawnMethod;
    private Method npcDestroyMethod;
    private Method npcGetEntityMethod;
    private Method npcGetOrAddTraitMethod;
    private Method npcSetProtectedMethod;
    private Method npcSetUseMinecraftAI;
    private Method npcSetMoveDestinationMethod;
    private Method playerFilterSetMethod;
    private Method skinTraitSetSkinNameMethod;

    public CitizensAdapter(ExtraScenesPlugin plugin) {
        this.plugin = plugin;
        detect();
    }

    public boolean isAvailable() {
        return available;
    }

    public Object createNpc(EntityType type, String displayName) {
        if (!available || type == null) {
            return null;
        }
        try {
            return createNpcMethod.invoke(npcRegistry, type, displayName == null ? "" : displayName);
        } catch (Exception ex) {
            available = false;
            plugin.getLogger().warning("Citizens API unavailable while creating NPC: " + ex.getMessage());
            return null;
        }
    }

    public void applySkin(Object npc, String skinName) {
        if (!available || npc == null || skinName == null || skinName.isBlank()) {
            return;
        }
        try {
            Object skinTrait = npcGetOrAddTraitMethod.invoke(npc, resolveClass("net.citizensnpcs.trait.SkinTrait"));
            if (skinTrait != null) {
                skinTraitSetSkinNameMethod.invoke(skinTrait, skinName);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to apply Citizens skin: " + ex.getMessage());
        }
    }

    public boolean applyPlayerFilter(Object npc, UUID sessionOwner) {
        if (!available || npc == null || sessionOwner == null || playerFilterSetMethod == null) {
            return false;
        }
        try {
            Object playerFilter = npcGetOrAddTraitMethod.invoke(npc,
                    resolveClass("net.citizensnpcs.trait.versioned.PlayerFilter"));
            if (playerFilter == null) {
                return false;
            }
            Predicate<Player> predicate = player -> !player.getUniqueId().equals(sessionOwner);
            playerFilterSetMethod.invoke(playerFilter, predicate);
            return true;
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to apply Citizens PlayerFilter: " + ex.getMessage());
            return false;
        }
    }

    public void configureNpc(Object npc) {
        if (!available || npc == null) {
            return;
        }
        try {
            npcSetProtectedMethod.invoke(npc, true);
            if (npcSetUseMinecraftAI != null) {
                npcSetUseMinecraftAI.invoke(npc, false);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to configure Citizens NPC: " + ex.getMessage());
        }
    }

    public boolean spawn(Object npc, Location location) {
        if (!available || npc == null || location == null) {
            return false;
        }
        try {
            return (boolean) npcSpawnMethod.invoke(npc, location);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to spawn Citizens NPC: " + ex.getMessage());
            return false;
        }
    }

    public Entity getEntity(Object npc) {
        if (!available || npc == null) {
            return null;
        }
        try {
            return (Entity) npcGetEntityMethod.invoke(npc);
        } catch (Exception ex) {
            return null;
        }
    }

    public void setMoveDestination(Object npc, Location location) {
        if (!available || npc == null || location == null || npcSetMoveDestinationMethod == null) {
            return;
        }
        try {
            npcSetMoveDestinationMethod.invoke(npc, location);
        } catch (Exception ignored) {
            // Optional API on some Citizens builds.
        }
    }

    public void destroy(Object npc) {
        if (!available || npc == null) {
            return;
        }
        try {
            npcDestroyMethod.invoke(npc);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to destroy Citizens NPC: " + ex.getMessage());
        }
    }

    private void detect() {
        Plugin citizens = Bukkit.getPluginManager().getPlugin("Citizens");
        this.available = citizens != null && citizens.isEnabled();
        if (!available) {
            return;
        }
        try {
            Class<?> citizensApi = resolveClass("net.citizensnpcs.api.CitizensAPI");
            Class<?> npcRegistryClass = resolveClass("net.citizensnpcs.api.npc.NPCRegistry");
            Class<?> npcClass = resolveClass("net.citizensnpcs.api.npc.NPC");
            Class<?> skinTraitClass = resolveClass("net.citizensnpcs.trait.SkinTrait");

            Method getRegistry = citizensApi.getMethod("getNPCRegistry");
            npcRegistry = getRegistry.invoke(null);
            createNpcMethod = npcRegistryClass.getMethod("createNPC", EntityType.class, String.class);

            npcSpawnMethod = npcClass.getMethod("spawn", Location.class);
            npcDestroyMethod = npcClass.getMethod("destroy");
            npcGetEntityMethod = npcClass.getMethod("getEntity");
            npcGetOrAddTraitMethod = npcClass.getMethod("getOrAddTrait", Class.class);
            npcSetProtectedMethod = npcClass.getMethod("setProtected", boolean.class);
            try {
                npcSetUseMinecraftAI = npcClass.getMethod("setUseMinecraftAI", boolean.class);
            } catch (NoSuchMethodException ignored) {
                npcSetUseMinecraftAI = null;
            }
            try {
                npcSetMoveDestinationMethod = npcClass.getMethod("setMoveDestination", Location.class);
            } catch (NoSuchMethodException ignored) {
                npcSetMoveDestinationMethod = null;
            }

            try {
                Class<?> playerFilterClass = resolveClass("net.citizensnpcs.trait.versioned.PlayerFilter");
                playerFilterSetMethod = playerFilterClass.getMethod("setPlayerFilter", Predicate.class);
            } catch (Exception ignored) {
                playerFilterSetMethod = null;
                plugin.getLogger().info("Citizens PlayerFilter trait not available; using Bukkit visibility fallback.");
            }
            skinTraitSetSkinNameMethod = skinTraitClass.getMethod("setSkinName", String.class);
        } catch (Exception ex) {
            available = false;
            plugin.getLogger().warning("Citizens detected but API bridge failed: " + ex.getMessage());
        }
    }

    private Class<?> resolveClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}

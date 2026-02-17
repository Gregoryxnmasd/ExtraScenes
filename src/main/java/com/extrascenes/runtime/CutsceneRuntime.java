package com.extrascenes.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.extrascenes.core.ActorTemplate;
import com.extrascenes.core.CameraPoint;
import com.extrascenes.core.Scene;
import com.extrascenes.core.Transform;

public class CutsceneRuntime implements Listener {
    private final JavaPlugin plugin;
    private final CameraRigController cameraRigController;
    private final ZoomController zoomController;
    private final ActorController actorController;
    private final Map<UUID, CutsceneSession> sessions = new HashMap<>();
    private final Map<UUID, float[]> wantedLook = new HashMap<>();

    public CutsceneRuntime(JavaPlugin plugin, CameraRigController cameraRigController, ZoomController zoomController, ActorController actorController) {
        this.plugin = plugin;
        this.cameraRigController = cameraRigController;
        this.zoomController = zoomController;
        this.actorController = actorController;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registerLookCancel();
    }

    public boolean isInPlayback(Player player) { return sessions.containsKey(player.getUniqueId()); }

    public boolean start(Player viewer, Scene scene) {
        stop(viewer, "restart");
        CameraPoint startPoint = pointAt(scene, 0);
        if (startPoint == null) return false;
        Location rigLoc = toLocation(startPoint);
        if (rigLoc == null) return false;
        Entity rig = cameraRigController.spawnRig(rigLoc);
        if (rig == null) return false;

        CutsceneSession session = new CutsceneSession(viewer.getUniqueId(), scene.getName());
        session.setStartLocation(viewer.getLocation().clone());
        session.setStartGameMode(viewer.getGameMode());
        session.setRigUuid(rig.getUniqueId());
        viewer.setGameMode(GameMode.SPECTATOR);
        cameraRigController.lockToRig(viewer, rig);
        zoomController.apply(viewer);

        Transform fallback = new Transform(startPoint.world(), startPoint.x(), startPoint.y(), startPoint.z(), startPoint.yaw(), startPoint.pitch());
        session.getActorNpcUuids().putAll(actorController.spawnSessionActors(viewer, scene.getActors(), fallback));

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> tick(viewer.getUniqueId(), scene), 1L, 1L);
        session.setTaskId(taskId);
        sessions.put(viewer.getUniqueId(), session);
        return true;
    }

    private void tick(UUID viewerId, Scene scene) {
        CutsceneSession session = sessions.get(viewerId);
        Player viewer = Bukkit.getPlayer(viewerId);
        if (session == null || viewer == null || !viewer.isOnline()) {
            stop(viewerId, "viewer missing");
            return;
        }
        CameraPoint point = pointAt(scene, session.getTick());
        if (point != null) {
            Location loc = toLocation(point);
            Entity rig = cameraRigController.find(viewer, session.getRigUuid());
            if (loc == null || rig == null) {
                stop(viewerId, "rig or world missing");
                return;
            }
            cameraRigController.moveRig(rig, loc);
            wantedLook.put(viewerId, new float[]{point.yaw(), point.pitch()});
            viewer.teleport(new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), point.yaw(), point.pitch()));
        }

        actorController.tick(session.getActorNpcUuids(), scene.getActors(), session.getTick());

        boolean lockOk = cameraRigController.enforceLock(viewer, session.getRigUuid());
        boolean match = lockOk && viewer.getGameMode() == GameMode.SPECTATOR && viewer.getSpectatorTarget() != null && viewer.getSpectatorTarget().getUniqueId().equals(session.getRigUuid());
        if (plugin.getConfig().getBoolean("debugCore", false) && session.getTick() % 20 == 0) {
            String wanted = wantedLook.containsKey(viewerId) ? wantedLook.get(viewerId)[0] + "," + wantedLook.get(viewerId)[1] : "n/a";
            plugin.getLogger().info("[CoreLock] viewer=" + viewer.getName() + " gm=" + viewer.getGameMode() + " target="
                + (viewer.getSpectatorTarget() == null ? "null" : viewer.getSpectatorTarget().getUniqueId()) + " rig=" + session.getRigUuid()
                + " match=" + match + " rigLoc=" + (viewer.getSpectatorTarget() == null ? "null" : viewer.getSpectatorTarget().getLocation())
                + " playerLoc=" + viewer.getLocation() + " yawPitchWanted=" + wanted);
        }
        if (!match) {
            stop(viewerId, "lock mismatch");
            return;
        }

        session.setTick(session.getTick() + 1);
        if (session.getTick() > scene.getDurationTicks()) stop(viewerId, "end");
    }

    public void stop(Player player, String reason) {
        if (player != null) stop(player.getUniqueId(), reason);
    }

    public void stop(UUID viewerId, String reason) {
        CutsceneSession session = sessions.remove(viewerId);
        if (session == null) return;
        Bukkit.getScheduler().cancelTask(session.getTaskId());
        Player viewer = Bukkit.getPlayer(viewerId);
        if (viewer != null) {
            viewer.setSpectatorTarget(null);
            viewer.setGameMode(session.getStartGameMode() == null ? GameMode.SURVIVAL : session.getStartGameMode());
            if (session.getStartLocation() != null) viewer.teleport(session.getStartLocation());
            zoomController.remove(viewer);
        }
        wantedLook.remove(viewerId);
        actorController.cleanup(session.getActorNpcUuids());
        cameraRigController.destroyRig(session.getRigUuid());
        plugin.getLogger().info("Stopped scene session for " + viewerId + " reason=" + reason);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { stop(event.getPlayer(), "quit"); }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) { stop(event.getPlayer(), "world change"); }
    @EventHandler public void onJoin(PlayerJoinEvent event) { actorController.hideFrom(event.getPlayer()); }

    private void registerLookCancel() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (!sessions.containsKey(player.getUniqueId())) return;
                event.setCancelled(true);
                float[] look = wantedLook.get(player.getUniqueId());
                if (look != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> player.teleport(new Location(player.getWorld(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), look[0], look[1])));
                }
            }
        });
    }

    private CameraPoint pointAt(Scene scene, int tick) {
        if (scene.getCameraPoints().containsKey(tick)) return scene.getCameraPoints().get(tick);
        return scene.getCameraPoints().entrySet().stream().filter(e -> e.getKey() <= tick).max(Map.Entry.comparingByKey()).map(Map.Entry::getValue).orElse(null);
    }

    private Location toLocation(CameraPoint point) {
        return new Location(Bukkit.getWorld(point.world()), point.x(), point.y(), point.z(), point.yaw(), point.pitch());
    }
}

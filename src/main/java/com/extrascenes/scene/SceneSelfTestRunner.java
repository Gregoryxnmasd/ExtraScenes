package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SceneSelfTestRunner {
    private static final int STABILITY_TICKS = 40;

    private final ExtraScenesPlugin plugin;
    private final SceneSessionManager sessionManager;

    public SceneSelfTestRunner(ExtraScenesPlugin plugin, SceneSessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }

    public void run(Player player, Scene scene) {
        if (player == null || scene == null) {
            return;
        }
        int endTick = Math.max(STABILITY_TICKS + 5, Math.min(scene.getDurationTicks(), 200));
        SceneSession session = sessionManager.startScene(player, scene, false, 0, endTick);
        UUID rigId = session.getCameraRigId();
        int forcedChunkX = session.getForcedChunkX();
        int forcedChunkZ = session.getForcedChunkZ();
        String forcedChunkWorld = session.getForcedChunkWorld();

        Text.send(player, "&eRunning /scene selftest for &f" + scene.getName() + "&e ...");

        new BukkitRunnable() {
            int sampledTicks = 0;
            int stableTicks = 0;
            boolean forcedLoadedDuringSession = true;
            boolean targetDropped = false;
            boolean spectatorAlways = true;
            boolean rigExisted = true;
            boolean rigWorldLoaded = true;
            boolean rigMarkerFalse = true;
            boolean stopRequested = false;
            int postStopWaitTicks = 0;

            @Override
            public void run() {
                SceneSession active = sessionManager.getSession(player.getUniqueId());
                if (active == null) {
                    if (!stopRequested) {
                        cancel();
                        return;
                    }
                    postStopWaitTicks++;
                    if (postStopWaitTicks < 2) {
                        return;
                    }
                    emitReport(player, session, forcedChunkWorld, forcedChunkX, forcedChunkZ,
                            stableTicks, sampledTicks, targetDropped, forcedLoadedDuringSession,
                            spectatorAlways, rigExisted, rigWorldLoaded, rigMarkerFalse);
                    cancel();
                    return;
                }

                Entity rig = Bukkit.getEntity(rigId);
                if (rig == null || !rig.isValid()) {
                    rigExisted = false;
                    sessionManager.stopScene(player, "selftest_rig_missing");
                    stopRequested = true;
                    return;
                }
                if (rig.getWorld() == null) {
                    rigWorldLoaded = false;
                    sessionManager.stopScene(player, "selftest_rig_world_missing");
                    stopRequested = true;
                    return;
                }
                if (rig.isMarker()) {
                    rigMarkerFalse = false;
                    sessionManager.stopScene(player, "selftest_rig_marker_true");
                    stopRequested = true;
                    return;
                }
                if (!rig.getWorld().equals(player.getWorld())) {
                    sessionManager.stopScene(player, "selftest_world_mismatch");
                    stopRequested = true;
                    return;
                }
                if (!rig.getWorld().isChunkLoaded(rig.getLocation().getBlockX() >> 4, rig.getLocation().getBlockZ() >> 4)) {
                    sessionManager.stopScene(player, "selftest_rig_chunk_not_loaded");
                    stopRequested = true;
                    return;
                }

                World forcedWorld = Bukkit.getWorld(forcedChunkWorld);
                if (forcedWorld == null || !forcedWorld.isChunkForceLoaded(forcedChunkX, forcedChunkZ)) {
                    forcedLoadedDuringSession = false;
                }

                if (player.getGameMode() != GameMode.SPECTATOR) {
                    spectatorAlways = false;
                    sessionManager.stopScene(player, "selftest_not_spectator");
                    stopRequested = true;
                    return;
                }

                Entity target = player.getSpectatorTarget();
                if (target != null && target.getUniqueId().equals(rigId)) {
                    stableTicks++;
                } else {
                    targetDropped = true;
                }

                sampledTicks++;
                if (sampledTicks >= STABILITY_TICKS) {
                    sessionManager.stopScene(player, "selftest_complete");
                    stopRequested = true;
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void emitReport(Player player, SceneSession session,
                            String forcedChunkWorld, int forcedChunkX, int forcedChunkZ,
                            int stableTicks, int sampledTicks, boolean targetDropped,
                            boolean forcedLoadedDuringSession, boolean spectatorAlways,
                            boolean rigExisted, boolean rigWorldLoaded, boolean rigMarkerFalse) {
        List<String> failures = new ArrayList<>();

        if (!rigExisted) {
            failures.add("rig exists: FAIL (camera rig entity missing or invalid)");
        }
        if (!rigWorldLoaded) {
            failures.add("rig world loaded: FAIL (rig world not available)");
        }
        if (!rigMarkerFalse) {
            failures.add("rig marker=false: FAIL (marker became true)");
        }

        if (!spectatorAlways) {
            failures.add("player gamemode spectator: FAIL (player was not spectator during selftest)");
        }

        if (stableTicks < STABILITY_TICKS) {
            failures.add("spectatorTarget == rig (stable for N ticks): FAIL (stableTicks=" + stableTicks
                    + "/" + STABILITY_TICKS + ")");
        }
        if (targetDropped) {
            failures.add("spectatorTarget stability: FAIL (target dropped at least once during sampling)");
        }

        if (session.getPlaybackTeleportCount() != 0) {
            failures.add("player teleports during playback == 0: FAIL (count=" + session.getPlaybackTeleportCount() + ")");
        }

        if (!session.isSpectatorHandshakeComplete()) {
            failures.add("spectator lock handshake: FAIL (did not complete initial lock handshake)");
        }

        if (stableTicks < STABILITY_TICKS) {
            failures.add("moved too quickly warnings expected == 0: FAIL (lock unstable; target did not remain locked)");
        }

        if (!forcedLoadedDuringSession) {
            failures.add("rig chunk forcedLoaded == true during session: FAIL (force-load dropped while running)");
        }
        World forcedWorld = forcedChunkWorld == null ? null : Bukkit.getWorld(forcedChunkWorld);
        boolean released = forcedWorld == null || !forcedWorld.isChunkForceLoaded(forcedChunkX, forcedChunkZ);
        if (!released) {
            failures.add("rig chunk released at end: FAIL (chunk still force loaded)");
        }

        Text.send(player, "&7--- &bScene Selftest: " + session.getScene().getName() + " &7---");
        Text.send(player, "&f- rig exists, rig world loaded, rig marker=false: "
                + (rigExisted && rigWorldLoaded && rigMarkerFalse ? "&aPASS" : "&cFAIL"));
        Text.send(player, "&f- player gamemode spectator: " + (spectatorAlways ? "&aPASS" : "&cFAIL"));
        Text.send(player, "&f- spectatorTarget == rig (stable for N ticks): "
                + (stableTicks >= STABILITY_TICKS && !targetDropped ? "&aPASS" : "&cFAIL")
                + " &7(stable=" + stableTicks + "/" + STABILITY_TICKS + ", sampled=" + sampledTicks + ")");
        Text.send(player, "&f- player teleports during playback == 0: "
                + (session.getPlaybackTeleportCount() == 0 ? "&aPASS" : "&cFAIL")
                + " &7(count=" + session.getPlaybackTeleportCount() + ")");
        Text.send(player, "&f- moved too quickly warnings expected == 0: "
                + (stableTicks >= STABILITY_TICKS && !targetDropped ? "&aPASS" : "&cFAIL"));
        Text.send(player, "&f- rig chunk forcedLoaded == true during session and released at end: "
                + (forcedLoadedDuringSession && released ? "&aPASS" : "&cFAIL"));

        if (failures.isEmpty()) {
            Text.send(player, "&aSelftest PASS");
        } else {
            Text.send(player, "&cSelftest FAIL (&f" + failures.size() + "&c)");
            for (String failure : failures) {
                Text.send(player, "&c  - " + failure);
            }
        }
    }
}

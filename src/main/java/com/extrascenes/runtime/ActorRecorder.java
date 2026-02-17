package com.extrascenes.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.extrascenes.core.ActorTemplate;
import com.extrascenes.core.Transform;

public class ActorRecorder {
    private final JavaPlugin plugin;
    private final Map<UUID, Integer> activeTasks = new HashMap<>();

    public ActorRecorder(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startRecording(Player player, ActorTemplate actor, int durationTicks) {
        stopRecording(player);
        int[] countdown = {3};
        int countdownTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (countdown[0] <= 0) {
                Bukkit.getScheduler().cancelTask(activeTasks.remove(player.getUniqueId()));
                beginRecordLoop(player, actor, durationTicks);
                return;
            }
            player.sendActionBar("§eRecord starts in " + countdown[0] + "...");
            countdown[0]--;
        }, 0L, 20L);
        activeTasks.put(player.getUniqueId(), countdownTask);
    }

    private void beginRecordLoop(Player player, ActorTemplate actor, int durationTicks) {
        int[] tick = {0};
        int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (tick[0] >= durationTicks) {
                stopRecording(player);
                player.sendActionBar("§aRecording completed");
                return;
            }
            Location loc = player.getLocation();
            actor.getRecording().put(tick[0], new Transform(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));
            player.sendActionBar("§cREC " + (tick[0] / 20) + "/" + (durationTicks / 20) + "s");
            tick[0]++;
        }, 1L, 1L);
        activeTasks.put(player.getUniqueId(), task);
    }

    public void stopRecording(Player player) {
        Integer task = activeTasks.remove(player.getUniqueId());
        if (task != null) Bukkit.getScheduler().cancelTask(task);
    }
}

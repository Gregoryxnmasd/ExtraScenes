package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.ScaleAttributeResolver;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class ActorRecordingService {
    private final ExtraScenesPlugin plugin;
    private final Map<UUID, ActiveRecording> activeRecordings = new HashMap<>();

    public ActorRecordingService(ExtraScenesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean startRecording(Player player, Scene scene, SceneActorTemplate template, int startTick) {
        stopRecording(player, false);
        ActiveRecording recording = new ActiveRecording(scene, template, startTick);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> capture(player, recording), 1L, 1L);
        recording.task = task;
        activeRecordings.put(player.getUniqueId(), recording);
        syncScale(player, template);
        return true;
    }

    public boolean stopRecording(Player player, boolean markDirty) {
        ActiveRecording recording = activeRecordings.remove(player.getUniqueId());
        if (recording == null) {
            return false;
        }
        if (recording.task != null) {
            recording.task.cancel();
        }
        if (markDirty) {
            recording.scene.setDirty(true);
            try {
                plugin.getSceneManager().saveScene(recording.scene);
            } catch (Exception ignored) {
            }
        }
        return true;
    }


    public boolean isRecording(Player player) {
        return player != null && activeRecordings.containsKey(player.getUniqueId());
    }

    public boolean isRecordingActor(Player player, String actorId) {
        if (player == null || actorId == null) {
            return false;
        }
        ActiveRecording active = activeRecordings.get(player.getUniqueId());
        return active != null && active.template.getActorId().equalsIgnoreCase(actorId);
    }

    public void stopAll(boolean markDirty) {
        for (UUID playerId : new java.util.ArrayList<>(activeRecordings.keySet())) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                stopRecording(player, markDirty);
                continue;
            }
            ActiveRecording recording = activeRecordings.remove(playerId);
            if (recording == null) {
                continue;
            }
            if (recording.task != null) {
                recording.task.cancel();
            }
            if (markDirty) {
                recording.scene.setDirty(true);
                try {
                    plugin.getSceneManager().saveScene(recording.scene);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void capture(Player player, ActiveRecording recording) {
        if (!player.isOnline()) {
            stopRecording(player, true);
            return;
        }
        int tick = recording.startTick + recording.relativeTick;
        Location loc = player.getLocation();
        Transform transform = Transform.fromLocation(loc);
        recording.template.putTransformTick(new ActorTransformTick(
                tick,
                transform,
                player.isSneaking(),
                player.isSprinting(),
                player.isSwimming(),
                player.isGliding()));
        player.sendActionBar("§cREC §7• §f" + recording.template.getActorId() + " §7• §f tick " + String.format("%04d", tick));
        recording.relativeTick++;
    }

    private void syncScale(Player player, SceneActorTemplate template) {
        Attribute scaleAttribute = ScaleAttributeResolver.resolveScaleAttribute();
        if (scaleAttribute == null || player.getAttribute(scaleAttribute) == null) {
            return;
        }
        template.setScale(player.getAttribute(scaleAttribute).getValue());
    }

    private static final class ActiveRecording {
        private final Scene scene;
        private final SceneActorTemplate template;
        private final int startTick;
        private int relativeTick;
        private BukkitTask task;

        private ActiveRecording(Scene scene, SceneActorTemplate template, int startTick) {
            this.scene = scene;
            this.template = template;
            this.startTick = Math.max(0, startTick);
        }
    }
}

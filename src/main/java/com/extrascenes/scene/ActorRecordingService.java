package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.ScaleAttributeResolver;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.Component;
import java.time.Duration;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class ActorRecordingService {
    private final ExtraScenesPlugin plugin;
    private final Map<UUID, ActiveRecording> activeRecordings = new HashMap<>();

    public ActorRecordingService(ExtraScenesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean startRecording(Player player, Scene scene, SceneActorTemplate template, int startTick) {
        return startRecording(player, scene, template, startTick, true);
    }

    public boolean startRecording(Player player, Scene scene, SceneActorTemplate template, int startTick, boolean previewOthers) {
        return startRecording(player, scene, template, startTick, previewOthers, 0);
    }

    public boolean startRecording(Player player, Scene scene, SceneActorTemplate template, int startTick,
                                  boolean previewOthers, int durationSeconds) {
        stopRecording(player, false);
        ActiveRecording recording = new ActiveRecording(scene, template, startTick, previewOthers, durationSeconds);
        plugin.getLogger().info("[actor-record] start viewer=" + player.getName()
                + " actor=" + template.getActorId()
                + " startTick=" + startTick
                + " durationSeconds=" + durationSeconds
                + " previewOthers=" + previewOthers);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> capture(player, recording), 1L, 1L);
        recording.task = task;
        activeRecordings.put(player.getUniqueId(), recording);
        syncScale(player, template);
        return true;
    }

    public void startRecordingWithCountdown(Player player, Scene scene, SceneActorTemplate template,
                                            int startTick, boolean previewOthers, int durationSeconds) {
        int boundedDuration = Math.max(1, durationSeconds);
        new BukkitRunnable() {
            int countdown = 3;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (countdown > 0) {
                    player.showTitle(Title.title(Component.text(String.valueOf(countdown)), Component.text("Recording starts"),
                            Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(700), Duration.ofMillis(200))));
                    player.sendActionBar("§eRecording starts in §f" + countdown + "§e...");
                    plugin.getLogger().info("[actor-record] countdown viewer=" + player.getName()
                            + " actor=" + template.getActorId()
                            + " secondsLeft=" + countdown);
                    countdown--;
                    return;
                }
                startRecording(player, scene, template, startTick, previewOthers, boundedDuration);
                player.showTitle(Title.title(Component.text("REC"), Component.text(actorLabel(template) + " • " + boundedDuration + "s"),
                        Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(700), Duration.ofMillis(200))));
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public boolean stopRecording(Player player, boolean markDirty) {
        ActiveRecording recording = activeRecordings.remove(player.getUniqueId());
        if (recording == null) {
            return false;
        }
        if (recording.task != null) {
            recording.task.cancel();
        }
        player.getInventory().removeItem(SceneWand.createRecordingWand());
        plugin.getRuntimeEngine().clearRecordingPreview(player);
        plugin.getLogger().info("[actor-record] stop viewer=" + player.getName()
                + " actor=" + recording.template.getActorId()
                + " ticksCaptured=" + recording.relativeTick
                + " markDirty=" + markDirty);
        if (markDirty) {
            plugin.getSceneManager().markDirty(recording.scene);
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
            plugin.getRuntimeEngine().clearRecordingPreview(player);
        if (markDirty) {
                plugin.getSceneManager().markDirty(recording.scene);
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
        int elapsedSeconds = Math.max(1, recording.relativeTick / 20 + 1);
        String durationText = recording.durationSeconds > 0
                ? elapsedSeconds + "/" + recording.durationSeconds + "s"
                : (elapsedSeconds + "s");
        player.sendActionBar("§cREC §7• §f" + recording.template.getActorId()
                + " §7• §fTick " + String.format("%04d", tick)
                + " §7• §f" + durationText);
        if (recording.previewOthers) {
            plugin.getRuntimeEngine().previewActorsAtTick(player, recording.scene, recording.template.getActorId(), tick);
        }
        if (recording.durationSeconds > 0 && recording.relativeTick + 1 >= recording.durationSeconds * 20) {
            stopRecording(player, true);
        }
        recording.relativeTick++;
    }

    private String actorLabel(SceneActorTemplate template) {
        return template == null ? "Actor" : template.getActorId();
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
        private final boolean previewOthers;
        private final int durationSeconds;

        private ActiveRecording(Scene scene, SceneActorTemplate template, int startTick, boolean previewOthers, int durationSeconds) {
            this.scene = scene;
            this.template = template;
            this.startTick = Math.max(0, startTick);
            this.previewOthers = previewOthers;
            this.durationSeconds = Math.max(0, durationSeconds);
        }
    }
}

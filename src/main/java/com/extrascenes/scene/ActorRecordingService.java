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
    private final Map<UUID, BukkitTask> pendingCountdowns = new HashMap<>();

    public ActorRecordingService(ExtraScenesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean startRecording(Player player, Scene scene, SceneActorTemplate template, int startTick) {
        return startRecording(player, scene, template, startTick, true);
    }

    public boolean startRecording(Player player, Scene scene, SceneActorTemplate template, int startTick, boolean previewOthers) {
        return startRecording(player, scene, template, startTick, previewOthers, 0, RecordingDurationUnit.SECONDS);
    }

    public boolean startRecording(Player player, Scene scene, SceneActorTemplate template, int startTick,
                                  boolean previewOthers, int durationTicks, RecordingDurationUnit durationUnit) {
        stopRecording(player, false);
        ActiveRecording recording = new ActiveRecording(scene, template, startTick, previewOthers, durationTicks, durationUnit);
        plugin.getLogger().info("[actor-record] start viewer=" + player.getName()
                + " actor=" + template.getActorId()
                + " startTick=" + startTick
                + " durationTicks=" + durationTicks
                + " previewOthers=" + previewOthers);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> capture(player, recording), 1L, 1L);
        recording.task = task;
        activeRecordings.put(player.getUniqueId(), recording);
        syncScale(player, template);
        return true;
    }

    public void startRecordingWithCountdown(Player player, Scene scene, SceneActorTemplate template,
                                            int startTick, boolean previewOthers, int durationTicks,
                                            RecordingDurationUnit durationUnit) {
        stopRecording(player, false);
        int boundedDurationTicks = Math.max(1, durationTicks);
        RecordingDurationUnit boundedUnit = durationUnit == null ? RecordingDurationUnit.SECONDS : durationUnit;
        BukkitTask countdownTask = new BukkitRunnable() {
            int countdown = 3;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    pendingCountdowns.remove(player.getUniqueId());
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
                pendingCountdowns.remove(player.getUniqueId());
                startRecording(player, scene, template, startTick, previewOthers, boundedDurationTicks, boundedUnit);
                int displayLimit = boundedUnit == RecordingDurationUnit.SECONDS
                        ? Math.max(1, boundedDurationTicks / 20)
                        : boundedDurationTicks;
                player.showTitle(Title.title(Component.text("REC"), Component.text(actorLabel(template) + " • " + displayLimit + boundedUnit.suffix()),
                        Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(700), Duration.ofMillis(200))));
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 20L);
        pendingCountdowns.put(player.getUniqueId(), countdownTask);
    }

    public boolean stopRecording(Player player, boolean markDirty) {
        BukkitTask countdownTask = pendingCountdowns.remove(player.getUniqueId());
        boolean cancelledCountdown = false;
        if (countdownTask != null) {
            countdownTask.cancel();
            cancelledCountdown = true;
        }
        ActiveRecording recording = activeRecordings.remove(player.getUniqueId());
        if (recording == null) {
            if (cancelledCountdown) {
                player.getInventory().removeItem(SceneWand.createRecordingWand());
                plugin.getRuntimeEngine().clearRecordingPreview(player);
            }
            return cancelledCountdown;
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
        for (BukkitTask countdown : new java.util.ArrayList<>(pendingCountdowns.values())) {
            countdown.cancel();
        }
        pendingCountdowns.clear();
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
        int elapsedTicks = recording.relativeTick + 1;
        String durationText = recording.durationTicks > 0
                ? recording.formatElapsed(elapsedTicks) + "/" + recording.formatLimit()
                : recording.formatElapsed(elapsedTicks);
        player.sendActionBar("§cREC §7• §f" + recording.template.getActorId()
                + " §7• §fTick " + String.format("%04d", tick)
                + " §7• §f" + durationText);
        if (recording.previewOthers) {
            plugin.getRuntimeEngine().previewActorsAtTick(player, recording.scene, recording.template.getActorId(), tick);
        }
        if (recording.durationTicks > 0 && recording.relativeTick + 1 >= recording.durationTicks) {
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
        private final int durationTicks;
        private final RecordingDurationUnit durationUnit;

        private ActiveRecording(Scene scene, SceneActorTemplate template, int startTick, boolean previewOthers, int durationTicks, RecordingDurationUnit durationUnit) {
            this.scene = scene;
            this.template = template;
            this.startTick = Math.max(0, startTick);
            this.previewOthers = previewOthers;
            this.durationTicks = Math.max(0, durationTicks);
            this.durationUnit = durationUnit == null ? RecordingDurationUnit.SECONDS : durationUnit;
        }

        private String formatElapsed(int elapsedTicks) {
            if (durationUnit == RecordingDurationUnit.SECONDS) {
                return String.valueOf(Math.max(1, (elapsedTicks + 19) / 20));
            }
            return String.valueOf(elapsedTicks);
        }

        private String formatLimit() {
            if (durationUnit == RecordingDurationUnit.SECONDS) {
                return Math.max(1, durationTicks / 20) + durationUnit.suffix();
            }
            return durationTicks + durationUnit.suffix();
        }
    }
}

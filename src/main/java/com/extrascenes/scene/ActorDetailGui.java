package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ActorDetailGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public ActorDetailGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        SceneActorTemplate actor = session.getScene().getActorTemplate(session.getSelectedActorId());
        Inventory inventory = GuiUtils.createInventory(27, "Actor • " + (actor == null ? "Unknown" : actor.getActorId()));
        GuiUtils.fillInventory(inventory);
        if (actor == null) {
            inventory.setItem(22, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return.")));
            return inventory;
        }
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        ExtraScenesPlugin plugin = editorEngine.getPlugin();
        ActorRecordingService recordingService = plugin.getActorRecordingService();
        boolean recording = player != null && recordingService.isRecordingActor(player, actor.getActorId());

        int groupStart = ((session.getCurrentTick() - 1) / 9) * 9 + 1;
        List<Integer> ticks = new ArrayList<>();
        for (Map.Entry<Integer, ActorTransformTick> entry : actor.getTransformTicks().entrySet()) {
            if (entry.getKey() >= groupStart && entry.getKey() < groupStart + 9) {
                ticks.add(entry.getKey());
            }
        }

        if (!recording) {
            inventory.setItem(10, GuiUtils.makeItem(Material.LIME_DYE, "Record Start", List.of("Start at selected tick: " + session.getActorRecordingStartTick())));
        }
        if (recording) {
            inventory.setItem(11, GuiUtils.makeItem(Material.RED_DYE, "Record Stop", List.of("Stop active recording.")));
        }
        inventory.setItem(12, GuiUtils.makeItem(actor.isPreviewEnabled() ? Material.ENDER_EYE : Material.ENDER_PEARL,
                "Preview Toggle", List.of("Current: " + (actor.isPreviewEnabled() ? "ON" : "OFF"))));
        inventory.setItem(17, GuiUtils.makeItem(session.isPreviewOtherActors() ? Material.LIME_DYE : Material.GRAY_DYE,
                "Preview other actors", List.of("Current: " + (session.isPreviewOtherActors() ? "ON" : "OFF"))));
        inventory.setItem(13, GuiUtils.makeItem(Material.PLAYER_HEAD, "Skin",
                List.of("L-Click: set skin name", "R-Click: load first from skin library", "Shift+Click: copy selected Citizens NPC")));
        inventory.setItem(14, GuiUtils.makeItem(Material.SLIME_BALL, "Scale: Snap from Player", List.of("Current: " + String.format(Locale.ROOT, "%.3f", actor.getScale()))));
        inventory.setItem(15, GuiUtils.makeItem(Material.BARRIER, "Delete", List.of("Remove this actor template.")));
        inventory.setItem(16, GuiUtils.makeItem(Material.CLOCK, "Timeline Window",
                List.of("Cursor tick: " + session.getCurrentTick(), "Window: " + groupStart + "-" + (groupStart + 8),
                        "Recorded: " + (ticks.isEmpty() ? "none" : ticks.stream().map(String::valueOf).collect(Collectors.joining(", "))),
                        "Click: actors timeline")));
        inventory.setItem(21, GuiUtils.makeItem(Material.REPEATER, "Set Start Tick",
                List.of("Current: " + session.getActorRecordingStartTick(), "Left: -9", "Right: +9", "Shift: sync with cursor")));
        inventory.setItem(22, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to actors.")));
        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) return;
        Scene scene = session.getScene();
        SceneActorTemplate actor = scene.getActorTemplate(session.getSelectedActorId());
        if (actor == null) {
            editorEngine.openActorsList(player, session, false);
            return;
        }
        boolean recording = editorEngine.getPlugin().getActorRecordingService().isRecordingActor(player, actor.getActorId());
        if (ctx.getSlot() == 10) {
            if (recording) {
                player.sendMessage(ChatColor.RED + "Already recording this actor.");
                return;
            }
            editorEngine.getPlugin().getActorRecordingService().startRecording(player, scene, actor, session.getActorRecordingStartTick(), session.isPreviewOtherActors());
            player.sendMessage(ChatColor.GREEN + "Recording actor " + actor.getActorId() + " from tick " + session.getActorRecordingStartTick());
        } else if (ctx.getSlot() == 11) {
            if (!recording) {
                player.sendMessage(ChatColor.RED + "No active recording.");
                return;
            }
            boolean stopped = editorEngine.getPlugin().getActorRecordingService().stopRecording(player, true);
            player.sendMessage(stopped ? ChatColor.GREEN + "Recording stopped." : ChatColor.RED + "No active recording.");
        } else if (ctx.getSlot() == 12) {
            actor.setPreviewEnabled(!actor.isPreviewEnabled());
            editorEngine.markDirty(scene);
        } else if (ctx.getSlot() == 13) {
            if (ctx.isShiftClick()) {
                Integer selected = editorEngine.getPlugin().getCitizensAdapter().resolveSelectedNpcId(player);
                if (selected == null || !editorEngine.getPlugin().getCitizensAdapter().copySkinFromNpc(actor, selected)) {
                    player.sendMessage(ChatColor.RED + "No selected Citizens NPC or skin unavailable.");
                } else {
                    editorEngine.markDirty(scene);
                    player.sendMessage(ChatColor.GREEN + "Skin copied from selected Citizens NPC.");
                }
            } else if (ctx.isRightClick()) {
                var entry = editorEngine.getPlugin().getSkinLibrary().firstEntry();
                if (entry == null) {
                    player.sendMessage(ChatColor.RED + "Skin library is empty.");
                } else {
                    actor.setSkinCacheKey(entry.cacheKey());
                    actor.setSkinName(entry.name());
                    actor.setSkinSignature(entry.signature());
                    actor.setSkinTexture(entry.texture());
                    editorEngine.markDirty(scene);
                    player.sendMessage(ChatColor.GREEN + "Skin loaded: " + entry.name());
                }
            } else {
                actor.setSkinName(player.getName());
                actor.setSkinCacheKey(player.getName());
                actor.setSkinSignature(null);
                actor.setSkinTexture(null);
                editorEngine.markDirty(scene);
                player.sendMessage(ChatColor.YELLOW + "Skin name set to your username.");
            }
        } else if (ctx.getSlot() == 14) {
            editorEngine.snapActorScaleFromPlayer(player, scene, actor);
        } else if (ctx.getSlot() == 16) {
            editorEngine.openActorTimeline(player, session, true);
            return;
        } else if (ctx.getSlot() == 17) {
            session.setPreviewOtherActors(!session.isPreviewOtherActors());
        } else if (ctx.getSlot() == 21) {
            int value = session.getActorRecordingStartTick();
            if (ctx.isShiftClick()) {
                value = session.getCurrentTick();
            } else if (ctx.isRightClick()) {
                value += 9;
            } else {
                value -= 9;
            }
            session.setActorRecordingStartTick(Math.max(1, Math.min(scene.getDurationTicks(), value)));
        } else if (ctx.getSlot() == 15) {
            scene.removeActorTemplate(actor.getActorId());
            editorEngine.markDirty(scene);
            editorEngine.openActorsList(player, session, false);
            return;
        } else if (ctx.getSlot() == 22) {
            editorEngine.navigateBack(player, session);
            return;
        }
        refresh(session);
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}

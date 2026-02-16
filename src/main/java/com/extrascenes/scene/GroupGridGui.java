package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class GroupGridGui implements EditorGui {
    private static final int GROUP_SIZE = 9;
    private final SceneEditorEngine editorEngine;

    public GroupGridGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        Scene scene = session.getScene();
        int group = session.getCurrentGroup();
        int startTick = (group - 1) * GROUP_SIZE + 1;
        int endTick = Math.min(startTick + GROUP_SIZE - 1, scene.getDurationTicks());
        Inventory inventory = GuiUtils.createInventory(54,
                session.getSceneName() + " • Group " + group + " (" + startTick + "-" + endTick + ")");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }

        inventory.setItem(4, GuiUtils.makeItem(Material.MAP, "Tick Group " + group,
                List.of("Ticks " + startTick + " - " + endTick, "Top row = ticks", "Rows below = actions")));
        inventory.setItem(0, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to groups.")));
        inventory.setItem(1, GuiUtils.makeItem(Material.ARROW, "Prev Group", List.of("Open previous group.")));
        inventory.setItem(7, GuiUtils.makeItem(Material.ARROW, "Next Group", List.of("Open next group.")));
        inventory.setItem(8, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));

        for (int col = 0; col < GROUP_SIZE; col++) {
            int tick = startTick + col;
            if (tick > scene.getDurationTicks()) {
                continue;
            }
            boolean hasCamera = TickUtils.getFirstKeyframeAtTick(scene.getTrack(SceneTrackType.CAMERA), tick) != null;
            boolean hasActors = hasActorAction(scene, tick);
            boolean hasCommand = TickUtils.getFirstKeyframeAtTick(scene.getTrack(SceneTrackType.COMMAND), tick) != null;
            boolean hasActionbar = TickUtils.getFirstKeyframeAtTick(scene.getTrack(SceneTrackType.ACTIONBAR), tick) != null;
            boolean edited = hasCamera || hasActors || hasCommand || hasActionbar
                    || !TickUtils.getKeyframesAtTick(scene.getTrack(SceneTrackType.MODEL), tick).isEmpty()
                    || !TickUtils.getKeyframesAtTick(scene.getTrack(SceneTrackType.PARTICLE), tick).isEmpty()
                    || !TickUtils.getKeyframesAtTick(scene.getTrack(SceneTrackType.SOUND), tick).isEmpty()
                    || !TickUtils.getKeyframesAtTick(scene.getTrack(SceneTrackType.BLOCK_ILLUSION), tick).isEmpty();

            inventory.setItem(9 + col, GuiUtils.makeItem(edited ? Material.LIME_WOOL : Material.RED_WOOL,
                    "Tick " + tick, List.of("Click to open tick editor.")));
            inventory.setItem(18 + col, buildCameraItem(scene, tick));
            inventory.setItem(27 + col, buildActorItem(scene, tick));
            inventory.setItem(36 + col, buildCommandItem(scene, tick));
            inventory.setItem(45 + col, buildActionBarItem(scene, tick));
        }

        return inventory;
    }

    private boolean hasActorAction(Scene scene, int tick) {
        for (SceneActorTemplate actor : scene.getActorTemplates().values()) {
            if (actor.getTransformTick(tick) != null) {
                return true;
            }
        }
        return false;
    }

    private org.bukkit.inventory.ItemStack buildCameraItem(Scene scene, int tick) {
        CameraKeyframe camera = TickUtils.getFirstKeyframeAtTick(scene.getTrack(SceneTrackType.CAMERA), tick);
        if (camera == null) {
            return null;
        }
        return GuiUtils.makeItem(Material.ENDER_PEARL, "Camera",
                List.of("Configured", "Smooth: " + camera.getSmoothingMode(), "L-click edit", "R-click clear"));
    }

    private org.bukkit.inventory.ItemStack buildActorItem(Scene scene, int tick) {
        int count = 0;
        for (SceneActorTemplate actor : scene.getActorTemplates().values()) {
            if (actor.getTransformTick(tick) != null) {
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        return GuiUtils.makeItem(Material.PLAYER_HEAD, "Actors",
                List.of("Tracks: " + count, "L-click edit tick", "R-click clear transforms"));
    }

    private org.bukkit.inventory.ItemStack buildCommandItem(Scene scene, int tick) {
        CommandKeyframe cmd = TickUtils.getFirstKeyframeAtTick(scene.getTrack(SceneTrackType.COMMAND), tick);
        if (cmd == null) {
            return null;
        }
        return GuiUtils.makeItem(Material.COMMAND_BLOCK, "Commands",
                List.of("Cmd: " + cmd.getCommands().size(), "L-click edit", "R-click clear"));
    }

    private org.bukkit.inventory.ItemStack buildActionBarItem(Scene scene, int tick) {
        ActionBarKeyframe bar = TickUtils.getFirstKeyframeAtTick(scene.getTrack(SceneTrackType.ACTIONBAR), tick);
        if (bar == null) {
            return null;
        }
        return GuiUtils.makeItem(Material.PAPER, "Actionbar",
                List.of("Bar: \"" + bar.getText() + "\"", "L-click edit", "R-click clear"));
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        int slot = ctx.getSlot();
        if (slot == 0) {
            editorEngine.openGroupSelect(player, session, true);
            return;
        }
        if (slot == 8) {
            editorEngine.closeEditor(player, session);
            return;
        }
        if (slot == 1) {
            session.setCurrentGroup(Math.max(1, session.getCurrentGroup() - 1));
            editorEngine.openGroupGrid(player, session, false);
            return;
        }
        if (slot == 7) {
            int maxGroups = (int) Math.ceil(Math.max(session.getScene().getDurationTicks(), GROUP_SIZE) / (double) GROUP_SIZE);
            session.setCurrentGroup(Math.min(maxGroups, session.getCurrentGroup() + 1));
            editorEngine.openGroupGrid(player, session, false);
            return;
        }
        if (slot >= 9 && slot <= 17) {
            int tick = (session.getCurrentGroup() - 1) * GROUP_SIZE + 1 + (slot - 9);
            session.setCurrentTick(tick);
            editorEngine.openTickActionMenu(player, session, true);
            return;
        }
        if (slot >= 18 && slot <= 53) {
            int row = slot / 9;
            int col = slot % 9;
            int tick = (session.getCurrentGroup() - 1) * GROUP_SIZE + 1 + col;
            session.setCurrentTick(tick);
            if (ctx.isRightClick()) {
                clearByRow(session.getScene(), row, tick);
                editorEngine.markDirty(session.getScene());
                refresh(session);
                return;
            }
            if (row == 2) {
                editorEngine.openCameraOptions(player, session, true);
                return;
            }
            if (row == 3) {
                editorEngine.openActorsList(player, session, true);
                return;
            }
            if (row == 4) {
                editorEngine.openCommandEditorForTick(player, session, tick);
                return;
            }
            if (row == 5) {
                editorEngine.openActionBarEditorForTick(player, session, tick);
                return;
            }
            editorEngine.openTickActionMenu(player, session, true);
        }
    }

    private void clearByRow(Scene scene, int row, int tick) {
        if (row == 2) {
            Track<CameraKeyframe> track = scene.getTrack(SceneTrackType.CAMERA);
            CameraKeyframe frame = TickUtils.getFirstKeyframeAtTick(track, tick);
            if (frame != null) {
                track.removeKeyframe(frame.getId());
            }
            return;
        }
        if (row == 3) {
            for (SceneActorTemplate actor : scene.getActorTemplates().values()) {
                actor.getTransformTicks().remove(tick);
            }
            return;
        }
        if (row == 4) {
            Track<CommandKeyframe> track = scene.getTrack(SceneTrackType.COMMAND);
            CommandKeyframe frame = TickUtils.getFirstKeyframeAtTick(track, tick);
            if (frame != null) {
                track.removeKeyframe(frame.getId());
            }
            return;
        }
        if (row == 5) {
            Track<ActionBarKeyframe> track = scene.getTrack(SceneTrackType.ACTIONBAR);
            ActionBarKeyframe frame = TickUtils.getFirstKeyframeAtTick(track, tick);
            if (frame != null) {
                track.removeKeyframe(frame.getId());
            }
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}

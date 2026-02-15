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
        GuiUtils.fillInventory(inventory);

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
            inventory.setItem(18 + col, buildSubItem(hasCamera, Material.ENDER_PEARL, "Camera action"));
            inventory.setItem(27 + col, buildSubItem(hasActors, Material.PLAYER_HEAD, "Actor action"));
            inventory.setItem(36 + col, buildSubItem(hasCommand, Material.COMMAND_BLOCK, "Command action"));
            inventory.setItem(45 + col, buildSubItem(hasActionbar, Material.PAPER, "Actionbar action"));
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

    private org.bukkit.inventory.ItemStack buildSubItem(boolean present, Material presentIcon, String label) {
        if (!present) {
            return GuiUtils.makeItem(Material.GRAY_STAINED_GLASS_PANE, label, List.of("Not set"));
        }
        return GuiUtils.makeItem(presentIcon, label, List.of("Configured"));
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
            refresh(session);
            return;
        }
        if (slot == 7) {
            int maxGroups = (int) Math.ceil(Math.max(session.getScene().getDurationTicks(), GROUP_SIZE) / (double) GROUP_SIZE);
            session.setCurrentGroup(Math.min(maxGroups, session.getCurrentGroup() + 1));
            refresh(session);
            return;
        }
        if (slot >= 9 && slot <= 17) {
            int tick = (session.getCurrentGroup() - 1) * GROUP_SIZE + 1 + (slot - 9);
            session.setCurrentTick(tick);
            editorEngine.openTickActionMenu(player, session, true);
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}

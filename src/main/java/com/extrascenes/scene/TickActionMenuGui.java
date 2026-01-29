package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class TickActionMenuGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public TickActionMenuGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        Scene scene = session.getScene();
        int tick = session.getCurrentTick();
        Inventory inventory = GuiUtils.createInventory(45, session.getSceneName() + " • Tick " + tick);
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.CLOCK, "Tick " + tick,
                buildSummary(scene, tick)));

        inventory.setItem(20, GuiUtils.makeItem(Material.ENDER_PEARL, "Camera",
                List.of("Arm placement mode.")));
        inventory.setItem(21, GuiUtils.makeItem(Material.COMPASS, "Camera Options",
                List.of("Instant cut, look-at, smoothing.")));
        inventory.setItem(22, GuiUtils.makeItem(Material.ARMOR_STAND, "Models",
                List.of("Edit model actions.")));
        inventory.setItem(29, GuiUtils.makeItem(Material.COMMAND_BLOCK, "Commands",
                List.of("Edit commands.")));
        inventory.setItem(30, GuiUtils.makeItem(Material.PAPER, "Actionbar",
                List.of("Edit actionbar text + duration.")));
        inventory.setItem(31, GuiUtils.makeItem(Material.NOTE_BLOCK, "Effects",
                List.of("Particles, sounds, blocks.")));
        inventory.setItem(33, GuiUtils.makeItem(Material.REDSTONE_BLOCK, "Clear Tick",
                List.of("Remove all actions at this tick.")));

        inventory.setItem(36, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to group grid.")));
        inventory.setItem(40, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));

        return inventory;
    }

    private List<String> buildSummary(Scene scene, int tick) {
        List<String> summary = new ArrayList<>();
        summary.add("Scene: " + scene.getName());
        summary.add("Tick: " + tick);
        CameraKeyframe camera = TickUtils.getFirstKeyframeAtTick(scene.getTrack(SceneTrackType.CAMERA), tick);
        CommandKeyframe command = TickUtils.getFirstKeyframeAtTick(scene.getTrack(SceneTrackType.COMMAND), tick);
        ActionBarKeyframe actionbar = TickUtils.getFirstKeyframeAtTick(scene.getTrack(SceneTrackType.ACTIONBAR), tick);
        List<ModelKeyframe> models = TickUtils.getKeyframesAtTick(scene.getTrack(SceneTrackType.MODEL), tick);
        summary.add("Camera: " + (camera == null ? "empty" : "set"));
        summary.add("Commands: " + (command == null ? 0 : command.getCommands().size()));
        summary.add("Models: " + models.size());
        summary.add("Actionbar: " + (actionbar == null ? "none" : actionbar.getDurationTicks() + "t"));
        summary.add("Effects: " + (TickUtils.getKeyframesAtTick(scene.getTrack(SceneTrackType.PARTICLE), tick).size()
                + TickUtils.getKeyframesAtTick(scene.getTrack(SceneTrackType.SOUND), tick).size()
                + TickUtils.getKeyframesAtTick(scene.getTrack(SceneTrackType.BLOCK_ILLUSION), tick).size()));
        return summary;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        int slot = ctx.getSlot();
        if (slot == 36) {
            editorEngine.openGroupGrid(player, session, true);
            return;
        }
        if (slot == 40) {
            editorEngine.closeEditor(player, session);
            return;
        }
        if (slot == 20) {
            editorEngine.armCameraPlacement(player, session, session.getCurrentTick());
            return;
        }
        if (slot == 21) {
            editorEngine.openCameraOptions(player, session, true);
            return;
        }
        if (slot == 22) {
            editorEngine.openModelTickList(player, session, true);
            return;
        }
        if (slot == 29) {
            editorEngine.openCommandEditorForTick(player, session, session.getCurrentTick());
            return;
        }
        if (slot == 30) {
            editorEngine.openActionBarEditorForTick(player, session, session.getCurrentTick());
            return;
        }
        if (slot == 31) {
            editorEngine.openEffectsMenu(player, session, true);
            return;
        }
        if (slot == 33) {
            editorEngine.openConfirm(player, session, ConfirmAction.CLEAR_TICK, null, null);
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}

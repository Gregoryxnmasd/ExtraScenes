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
                session.getSceneName() + " • Group " + group + " (ticks " + startTick + "–" + endTick + ")");

        inventory.setItem(4, GuiUtils.makeItem(Material.MAP, "Group " + group,
                List.of("Ticks " + startTick + " - " + (startTick + GROUP_SIZE - 1))));

        for (int i = 0; i < GROUP_SIZE; i++) {
            int tick = startTick + i;
            if (tick <= scene.getDurationTicks()) {
                inventory.setItem(i, buildTickItem(scene, tick));
            }
        }

        inventory.setItem(45, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to group select.")));
        inventory.setItem(49, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
        inventory.setItem(47, GuiUtils.makeItem(Material.ARROW, "Prev Group", List.of("Open previous group.")));
        inventory.setItem(51, GuiUtils.makeItem(Material.ARROW, "Next Group", List.of("Open next group.")));
        inventory.setItem(53, GuiUtils.makeItem(Material.REDSTONE_BLOCK, "Shift Range",
                List.of("Shift ticks via chat input.")));

        return inventory;
    }

    private org.bukkit.inventory.ItemStack buildTickItem(Scene scene, int tick) {
        List<String> lore = new ArrayList<>();
        lore.add("Tick " + tick);
        Track<CameraKeyframe> cameraTrack = scene.getTrack(SceneTrackType.CAMERA);
        Track<CommandKeyframe> commandTrack = scene.getTrack(SceneTrackType.COMMAND);
        Track<ModelKeyframe> modelTrack = scene.getTrack(SceneTrackType.MODEL);
        Track<ParticleKeyframe> particleTrack = scene.getTrack(SceneTrackType.PARTICLE);
        Track<SoundKeyframe> soundTrack = scene.getTrack(SceneTrackType.SOUND);
        Track<BlockIllusionKeyframe> blockTrack = scene.getTrack(SceneTrackType.BLOCK_ILLUSION);

        CameraKeyframe camera = TickUtils.getFirstKeyframeAtTick(cameraTrack, tick);
        CommandKeyframe command = TickUtils.getFirstKeyframeAtTick(commandTrack, tick);
        List<ModelKeyframe> models = TickUtils.getKeyframesAtTick(modelTrack, tick);
        List<ParticleKeyframe> particles = TickUtils.getKeyframesAtTick(particleTrack, tick);
        List<SoundKeyframe> sounds = TickUtils.getKeyframesAtTick(soundTrack, tick);
        List<BlockIllusionKeyframe> blocks = TickUtils.getKeyframesAtTick(blockTrack, tick);

        ActionBarKeyframe actionbar = TickUtils.getFirstKeyframeAtTick(scene.getTrack(SceneTrackType.ACTIONBAR), tick);
        lore.add("Camera: " + (camera == null ? "No" : "Yes"));
        lore.add("Models: " + models.size());
        lore.add("Commands: " + (command == null ? 0 : command.getCommands().size()));
        lore.add("Actionbar: " + (actionbar == null ? "No" : "Yes (" + actionbar.getDurationTicks() + "t)"));
        lore.add("Effects: " + (particles.size() + sounds.size() + blocks.size()));
        lore.add("Click: edit");
        lore.add("Shift-click: move tick");

        boolean edited = camera != null || command != null || actionbar != null || !models.isEmpty()
                || !particles.isEmpty() || !sounds.isEmpty() || !blocks.isEmpty();
        Material material = edited ? Material.LIME_WOOL : Material.RED_WOOL;
        return GuiUtils.makeItem(material, "Tick " + tick, lore);
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        int slot = ctx.getSlot();
        if (slot == 45) {
            editorEngine.openGroupSelect(player, session, true);
            return;
        }
        if (slot == 49) {
            editorEngine.closeEditor(player, session);
            return;
        }
        if (slot == 47) {
            session.setCurrentGroup(Math.max(1, session.getCurrentGroup() - 1));
            refresh(session);
            return;
        }
        if (slot == 51) {
            int maxGroups = (int) Math.ceil(Math.max(session.getScene().getDurationTicks(), GROUP_SIZE) / (double) GROUP_SIZE);
            session.setCurrentGroup(Math.min(maxGroups, session.getCurrentGroup() + 1));
            refresh(session);
            return;
        }
        if (slot == 53) {
            player.closeInventory();
            editorEngine.getInputManager().beginShiftRangeInput(player, session.getScene(), session, GuiType.GROUP_GRID);
            return;
        }
        if (slot >= 0 && slot < GROUP_SIZE) {
            int tick = (session.getCurrentGroup() - 1) * GROUP_SIZE + 1 + slot;
            if (ctx.isShiftClick()) {
                player.closeInventory();
                editorEngine.getInputManager().beginMoveTickInput(player, session.getScene(), session, tick,
                        GuiType.GROUP_GRID);
            } else {
                session.setCurrentTick(tick);
                editorEngine.openTickActionMenu(player, session, true);
            }
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}

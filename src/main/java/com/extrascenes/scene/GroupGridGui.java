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
        Inventory inventory = GuiUtils.createInventory(54, title(session, group));
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.MAP, "Group " + group,
                List.of("Ticks " + startTick + " - " + (startTick + GROUP_SIZE - 1))));

        for (int i = 0; i < GROUP_SIZE; i++) {
            int tick = startTick + i;
            inventory.setItem(i, buildTickItem(scene, tick));
        }

        inventory.setItem(45, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to group select.")));
        inventory.setItem(49, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
        inventory.setItem(47, GuiUtils.makeItem(Material.LIME_WOOL, "Prev Group", List.of("Open previous group.")));
        inventory.setItem(51, GuiUtils.makeItem(Material.LIME_WOOL, "Next Group", List.of("Open next group.")));
        inventory.setItem(53, GuiUtils.makeItem(Material.RED_WOOL, "Clear Group", List.of("Clear all ticks in group.")));

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

        lore.add("Camera: " + (camera == null ? "empty" : "set"));
        lore.add("Commands: " + (command == null ? 0 : command.getCommands().size()));
        lore.add("Models: " + models.size());
        lore.add("Effects: " + (particles.size() + sounds.size() + blocks.size()));
        lore.add("Click to edit tick.");

        Material material = camera != null || command != null || !models.isEmpty() || !particles.isEmpty()
                || !sounds.isEmpty() || !blocks.isEmpty()
                ? Material.CLOCK
                : Material.LIGHT_GRAY_STAINED_GLASS_PANE;
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
            editorEngine.openConfirm(player, session, ConfirmAction.CLEAR_GROUP, null, null);
            return;
        }
        if (slot >= 0 && slot < GROUP_SIZE) {
            int tick = (session.getCurrentGroup() - 1) * GROUP_SIZE + 1 + slot;
            session.setCurrentTick(tick);
            editorEngine.openTickActionMenu(player, session, true);
        }
    }

    private String title(EditorSession session, int group) {
        return "Scene: " + session.getSceneName() + " • Group: " + group + " • Tick: -";
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}

package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class KeyframeListGui implements EditorGui {
    private static final List<Integer> KEYFRAME_SLOTS = buildKeyframeSlots();

    private final SceneEditorEngine editorEngine;

    public KeyframeListGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        Scene scene = session.getScene();
        Inventory inventory = GuiUtils.createInventory(54,
                "Scene: " + session.getSceneName() + " • Keyframes • Group: " + session.getCurrentGroup()
                        + " • Tick: " + session.getCurrentTick());
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.NAME_TAG,
                "Track: " + formatTrack(session.getSelectedTrack()) + " | Scene: " + scene.getName(),
                List.of("Keyframe list view.")));
        inventory.setItem(8, GuiUtils.makeItem(Material.CLOCK,
                "Cursor Time: " + session.getCursorTimeTicks() + "t (" + GuiUtils.SECONDS_FORMAT.format(session.getCursorTimeTicks() / 20.0) + "s)",
                List.of("Use Set Cursor to adjust.")));

        renderKeyframes(inventory, scene, session);

        inventory.setItem(45, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to tracks.")));
        inventory.setItem(47, GuiUtils.makeItem(Material.ARROW, "Prev Page",
                List.of("Page " + (session.getKeyframePage() + 1))));
        inventory.setItem(48, GuiUtils.makeItem(Material.CLOCK, "Set Cursor",
                List.of("Left: set to selected", "Right: set by chat")));
        inventory.setItem(49, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
        inventory.setItem(50, GuiUtils.makeItem(Material.EMERALD, "Add Keyframe",
                List.of("Add keyframe for this track.")));
        inventory.setItem(51, GuiUtils.makeItem(Material.ARROW, "Next Page",
                List.of("Page " + (session.getKeyframePage() + 1))));

        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        int slot = ctx.getSlot();
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        if (slot == 45) {
            editorEngine.navigateBack(player, session);
            return;
        }
        if (slot == 47) {
            session.setKeyframePage(Math.max(0, session.getKeyframePage() - 1));
            refresh(session);
            return;
        }
        if (slot == 51) {
            session.setKeyframePage(session.getKeyframePage() + 1);
            refresh(session);
            return;
        }
        if (slot == 49) {
            editorEngine.closeEditor(player, session);
            return;
        }
        if (slot == 50) {
            editorEngine.openAddKeyframe(player, session, true);
            return;
        }
        if (slot == 48) {
            if (ctx.isRightClick()) {
                player.closeInventory();
                editorEngine.getInputManager().beginCursorInput(player, session.getScene(), session, GuiType.KEYFRAME_LIST);
            } else {
                setCursorToSelected(session);
                refresh(session);
            }
            return;
        }

        handleKeyframeClick(session, ctx);
    }

    private void handleKeyframeClick(EditorSession session, ClickContext ctx) {
        ItemStack clicked = ctx.getClickedItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        int slotIndex = KEYFRAME_SLOTS.indexOf(ctx.getSlot());
        if (slotIndex < 0) {
            return;
        }
        Track<? extends Keyframe> track = session.getScene().getTrack(session.getSelectedTrack());
        if (track == null) {
            return;
        }
        int startIndex = session.getKeyframePage() * KEYFRAME_SLOTS.size();
        int keyframeIndex = startIndex + slotIndex;
        if (keyframeIndex >= track.getKeyframes().size()) {
            return;
        }
        Keyframe keyframe = track.getKeyframes().get(keyframeIndex);

        if (ctx.isShiftClick()) {
            int delta = ctx.isRightClick() ? 5 : -5;
            track.moveKeyframe(keyframe.getId(), keyframe.getTimeTicks() + delta);
            refresh(session);
            return;
        }

        session.setSelectedKeyframeId(keyframe.getId());
        if (ctx.isRightClick()) {
            openEditorForKeyframe(session, keyframe);
        } else {
            refresh(session);
        }
    }

    private void openEditorForKeyframe(EditorSession session, Keyframe keyframe) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        if (keyframe instanceof CameraKeyframe) {
            editorEngine.openCameraEditor(player, session, true);
        } else if (keyframe instanceof CommandKeyframe) {
            editorEngine.openCommandEditor(player, session, true);
        } else if (keyframe instanceof ModelKeyframe) {
            editorEngine.openModelEditor(player, session, true);
        }
    }

    private void setCursorToSelected(EditorSession session) {
        UUID selectedId = session.getSelectedKeyframeId();
        Track<? extends Keyframe> track = session.getScene().getTrack(session.getSelectedTrack());
        if (selectedId == null || track == null) {
            return;
        }
        Keyframe keyframe = track.getKeyframe(selectedId);
        if (keyframe != null) {
            session.setCursorTimeTicks(keyframe.getTimeTicks());
        }
    }

    private void renderKeyframes(Inventory inventory, Scene scene, EditorSession session) {
        Track<? extends Keyframe> track = scene.getTrack(session.getSelectedTrack());
        List<? extends Keyframe> keyframes = track == null ? List.of() : track.getKeyframes();
        int pageSize = KEYFRAME_SLOTS.size();
        int startIndex = session.getKeyframePage() * pageSize;
        int endIndex = Math.min(keyframes.size(), startIndex + pageSize);

        if (keyframes.isEmpty() || startIndex >= keyframes.size()) {
            ItemStack empty = GuiUtils.makeItem(Material.BLACK_STAINED_GLASS_PANE, "No Keyframes",
                    List.of("Use Add Keyframe to create one."));
            for (int slot : KEYFRAME_SLOTS) {
                inventory.setItem(slot, empty);
            }
            return;
        }

        for (int i = 0; i < KEYFRAME_SLOTS.size(); i++) {
            int index = startIndex + i;
            if (index >= endIndex) {
                inventory.setItem(KEYFRAME_SLOTS.get(i), GuiUtils.makeGlass());
                continue;
            }
            Keyframe keyframe = keyframes.get(index);
            inventory.setItem(KEYFRAME_SLOTS.get(i), GuiUtils.buildKeyframeItem(keyframe, session.getSelectedKeyframeId()));
        }
    }

    private static List<Integer> buildKeyframeSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 9; slot <= 44; slot++) {
            slots.add(slot);
        }
        return slots;
    }

    private String formatTrack(SceneTrackType type) {
        String name = type.name().toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}

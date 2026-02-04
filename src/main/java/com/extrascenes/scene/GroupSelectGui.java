package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class GroupSelectGui implements EditorGui {
    private static final int GROUP_SIZE = 9;
    private static final int GROUPS_PER_PAGE = 36;
    private final SceneEditorEngine editorEngine;

    public GroupSelectGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        Scene scene = session.getScene();
        int duration = Math.max(scene.getDurationTicks(), GROUP_SIZE);
        int totalGroups = (int) Math.ceil(duration / (double) GROUP_SIZE);
        int totalPages = (int) Math.ceil(totalGroups / (double) GROUPS_PER_PAGE);
        int maxPage = Math.max(0, totalPages - 1);
        int currentPage = Math.min(session.getGroupPage(), maxPage);
        if (currentPage != session.getGroupPage()) {
            session.setGroupPage(currentPage);
        }
        Inventory inventory = GuiUtils.createInventory(54, session.getSceneName() + " • Groups");
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.BOOK, "Groups",
                List.of("Scene: " + scene.getName(),
                        "Duration: " + scene.getDurationTicks() + "t",
                        "Page " + (currentPage + 1) + "/" + Math.max(1, totalPages))));

        int slot = 9;
        int startGroup = currentPage * GROUPS_PER_PAGE + 1;
        int endGroup = Math.min(totalGroups, startGroup + GROUPS_PER_PAGE - 1);
        for (int group = startGroup; group <= endGroup; group++) {
            int startTick = (group - 1) * GROUP_SIZE + 1;
            int endTick = Math.min(group * GROUP_SIZE, scene.getDurationTicks());
            List<String> lore = new ArrayList<>();
            lore.add("Ticks " + startTick + " - " + endTick);
            GroupSummary summary = summarizeGroup(scene, startTick, endTick);
            lore.add("Edited ticks: " + summary.editedTicks);
            if (!summary.hasLabels.isEmpty()) {
                lore.add("Has: " + String.join("/", summary.hasLabels));
            }
            lore.add("Click to open grid.");
            Material material = summary.editedTicks > 0 ? Material.BOOK : Material.PAPER;
            inventory.setItem(slot++, GuiUtils.makeItem(material, "Group " + group, lore));
        }

        inventory.setItem(45, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to dashboard.")));
        if (totalPages > 1) {
            inventory.setItem(46, GuiUtils.makeItem(Material.ARROW, "Previous Page",
                    List.of("Page " + (currentPage + 1) + "/" + totalPages)));
            inventory.setItem(52, GuiUtils.makeItem(Material.ARROW, "Next Page",
                    List.of("Page " + (currentPage + 1) + "/" + totalPages)));
        }
        inventory.setItem(49, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
        inventory.setItem(47, GuiUtils.makeItem(Material.LIME_WOOL, "Extend +9", List.of("Add 9 ticks.")));
        inventory.setItem(48, GuiUtils.makeItem(Material.GREEN_WOOL, "Extend +90", List.of("Add 90 ticks.")));
        inventory.setItem(51, GuiUtils.makeItem(Material.REDSTONE_BLOCK, "Trim Duration", List.of("Remove ticks with confirm.")));
        inventory.setItem(53, GuiUtils.makeItem(Material.COMPASS, "Jump to Group", List.of("Enter group number via chat.")));

        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        int slot = ctx.getSlot();
        Scene scene = session.getScene();
        int duration = Math.max(scene.getDurationTicks(), GROUP_SIZE);
        int totalGroups = (int) Math.ceil(duration / (double) GROUP_SIZE);
        int totalPages = (int) Math.ceil(totalGroups / (double) GROUPS_PER_PAGE);
        int maxPage = Math.max(0, totalPages - 1);
        int currentPage = Math.min(session.getGroupPage(), maxPage);
        if (slot == 45) {
            editorEngine.navigateBack(player, session);
            return;
        }
        if (slot == 46 && totalPages > 1) {
            session.setGroupPage(Math.max(0, currentPage - 1));
            refresh(session);
            return;
        }
        if (slot == 49) {
            editorEngine.closeEditor(player, session);
            return;
        }
        if (slot == 47) {
            scene.setDurationTicks(scene.getDurationTicks() + GROUP_SIZE);
            editorEngine.markDirty(scene);
            refresh(session);
            return;
        }
        if (slot == 48) {
            scene.setDurationTicks(scene.getDurationTicks() + GROUP_SIZE * 10);
            editorEngine.markDirty(scene);
            refresh(session);
            return;
        }
        if (slot == 51) {
            session.setConfirmAction(ConfirmAction.TRIM_DURATION);
            editorEngine.openConfirm(player, session, ConfirmAction.TRIM_DURATION, null, null);
            return;
        }
        if (slot == 52 && totalPages > 1) {
            session.setGroupPage(Math.min(maxPage, currentPage + 1));
            refresh(session);
            return;
        }
        if (slot == 53) {
            player.closeInventory();
            editorEngine.getInputManager().beginGroupJumpInput(player, session.getScene(), session, GuiType.GROUP_SELECT);
            return;
        }

        if (slot >= 9 && slot < 45) {
            int startGroup = currentPage * GROUPS_PER_PAGE + 1;
            int group = startGroup + (slot - 9);
            if (group > totalGroups) {
                return;
            }
            session.setCurrentGroup(group);
            editorEngine.openGroupGrid(player, session, true);
        }
    }

    private GroupSummary summarizeGroup(Scene scene, int startTick, int endTick) {
        int editedTicks = 0;
        boolean hasCamera = false;
        boolean hasModels = false;
        boolean hasCommands = false;
        boolean hasActionbar = false;
        for (int tick = startTick; tick <= endTick; tick++) {
            boolean edited = TickUtils.getFirstKeyframeAtTick(scene.getTrack(SceneTrackType.CAMERA), tick) != null
                    || TickUtils.getFirstKeyframeAtTick(scene.getTrack(SceneTrackType.COMMAND), tick) != null
                    || TickUtils.getFirstKeyframeAtTick(scene.getTrack(SceneTrackType.ACTIONBAR), tick) != null
                    || !TickUtils.getKeyframesAtTick(scene.getTrack(SceneTrackType.MODEL), tick).isEmpty()
                    || !TickUtils.getKeyframesAtTick(scene.getTrack(SceneTrackType.PARTICLE), tick).isEmpty()
                    || !TickUtils.getKeyframesAtTick(scene.getTrack(SceneTrackType.SOUND), tick).isEmpty()
                    || !TickUtils.getKeyframesAtTick(scene.getTrack(SceneTrackType.BLOCK_ILLUSION), tick).isEmpty();
            if (edited) {
                editedTicks++;
            }
            hasCamera |= TickUtils.getFirstKeyframeAtTick(scene.getTrack(SceneTrackType.CAMERA), tick) != null;
            hasCommands |= TickUtils.getFirstKeyframeAtTick(scene.getTrack(SceneTrackType.COMMAND), tick) != null;
            hasActionbar |= TickUtils.getFirstKeyframeAtTick(scene.getTrack(SceneTrackType.ACTIONBAR), tick) != null;
            hasModels |= !TickUtils.getKeyframesAtTick(scene.getTrack(SceneTrackType.MODEL), tick).isEmpty();
        }
        List<String> labels = new ArrayList<>();
        if (hasCamera) {
            labels.add("Camera");
        }
        if (hasModels) {
            labels.add("Models");
        }
        if (hasCommands) {
            labels.add("Commands");
        }
        if (hasActionbar) {
            labels.add("Actionbar");
        }
        return new GroupSummary(editedTicks, labels);
    }

    private record GroupSummary(int editedTicks, List<String> hasLabels) {
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}

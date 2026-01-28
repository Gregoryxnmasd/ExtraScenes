package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class GroupSelectGui implements EditorGui {
    private static final int GROUP_SIZE = 9;
    private final SceneEditorEngine editorEngine;

    public GroupSelectGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        Scene scene = session.getScene();
        int duration = Math.max(scene.getDurationTicks(), GROUP_SIZE);
        int totalGroups = (int) Math.ceil(duration / (double) GROUP_SIZE);
        Inventory inventory = GuiUtils.createInventory(54, title(session, "Group Select"));
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.BOOK, "Groups",
                List.of("Scene: " + scene.getName(), "Duration: " + scene.getDurationTicks() + "t")));

        int slot = 9;
        for (int group = 1; group <= totalGroups; group++) {
            int startTick = (group - 1) * GROUP_SIZE + 1;
            int endTick = Math.min(group * GROUP_SIZE, scene.getDurationTicks());
            List<String> lore = new ArrayList<>();
            lore.add("Ticks " + startTick + " - " + endTick);
            lore.add("Click to open grid.");
            inventory.setItem(slot++, GuiUtils.makeItem(Material.PAPER, "Group " + group, lore));
            if (slot >= 45) {
                break;
            }
        }

        inventory.setItem(45, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to dashboard.")));
        inventory.setItem(49, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
        inventory.setItem(47, GuiUtils.makeItem(Material.LIME_WOOL, "Extend +9", List.of("Add 9 ticks.")));
        inventory.setItem(48, GuiUtils.makeItem(Material.GREEN_WOOL, "Extend +90", List.of("Add 90 ticks.")));
        inventory.setItem(51, GuiUtils.makeItem(Material.RED_WOOL, "Trim Duration", List.of("Remove ticks with confirm.")));
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
        if (slot == 45) {
            editorEngine.navigateBack(player, session);
            return;
        }
        if (slot == 49) {
            editorEngine.closeEditor(player, session);
            return;
        }
        if (slot == 47) {
            scene.setDurationTicks(scene.getDurationTicks() + GROUP_SIZE);
            refresh(session);
            return;
        }
        if (slot == 48) {
            scene.setDurationTicks(scene.getDurationTicks() + GROUP_SIZE * 10);
            refresh(session);
            return;
        }
        if (slot == 51) {
            session.setConfirmAction(ConfirmAction.TRIM_DURATION);
            editorEngine.openConfirm(player, session, ConfirmAction.TRIM_DURATION, null, null);
            return;
        }
        if (slot == 53) {
            player.closeInventory();
            editorEngine.getInputManager().beginGroupJumpInput(player, session.getScene(), session, GuiType.GROUP_SELECT);
            return;
        }

        if (slot >= 9 && slot < 45) {
            int group = slot - 9 + 1;
            session.setCurrentGroup(group);
            editorEngine.openGroupGrid(player, session, true);
        }
    }

    private String title(EditorSession session, String label) {
        return "Scene: " + session.getSceneName() + " • " + label + " • Group: - • Tick: -";
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}

package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ModelLibraryGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public ModelLibraryGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        Inventory inventory = GuiUtils.createInventory(54, session.getSceneName() + " • Models");
        GuiUtils.fillInventory(inventory);

        inventory.setItem(4, GuiUtils.makeItem(Material.BOOK, "Scene Model Library",
                List.of("Create and manage model entries.")));

        List<SceneModelEntry> entries = new ArrayList<>(session.getScene().getModelLibrary().values());
        int slot = 9;
        for (SceneModelEntry entry : entries) {
            inventory.setItem(slot++, GuiUtils.makeItem(Material.ARMOR_STAND, entry.getName(),
                    List.of("ModelId: " + GuiUtils.nullToPlaceholder(entry.getModelId()),
                            "Default Anim: " + GuiUtils.nullToPlaceholder(entry.getDefaultAnimation()),
                            "Spawn: " + (entry.getSpawnTransform() == null ? "unset" : "set"),
                            "Left: edit", "Right: remove")));
            if (slot >= 45) {
                break;
            }
        }

        inventory.setItem(45, GuiUtils.makeItem(Material.ANVIL, "Add Model Entry",
                List.of("Create a reusable model entry.")));
        inventory.setItem(49, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
        inventory.setItem(53, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to models menu.")));
        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        int slot = ctx.getSlot();
        if (slot == 53) {
            editorEngine.openModelTickList(player, session, true);
            return;
        }
        if (slot == 49) {
            editorEngine.closeEditor(player, session);
            return;
        }
        if (slot == 45) {
            player.closeInventory();
            editorEngine.getInputManager().beginModelEntryCreate(player, session.getScene(), session,
                    GuiType.MODEL_LIBRARY);
            return;
        }
        if (slot >= 9 && slot < 45) {
            List<SceneModelEntry> entries = new ArrayList<>(session.getScene().getModelLibrary().values());
            int index = slot - 9;
            if (index >= entries.size()) {
                return;
            }
            SceneModelEntry entry = entries.get(index);
            if (ctx.isRightClick()) {
                session.getScene().removeModelEntry(entry.getName());
                editorEngine.markDirty(session.getScene());
                refresh(session);
                return;
            }
            session.setSelectedModelEntryName(entry.getName());
            editorEngine.openModelEntryEditor(player, session, true);
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}

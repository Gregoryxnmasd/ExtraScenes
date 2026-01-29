package com.extrascenes.scene;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ModelEntryEditorGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public ModelEntryEditorGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        SceneModelEntry entry = session.getScene().getModelEntry(session.getSelectedModelEntryName());
        String title = entry == null ? "Model Entry" : entry.getName();
        Inventory inventory = GuiUtils.createInventory(27, session.getSceneName() + " • " + title);
        GuiUtils.fillInventory(inventory);

        if (entry == null) {
            inventory.setItem(4, GuiUtils.makeItem(Material.BARRIER, "No model selected",
                    List.of("Return to the library.")));
        } else {
            inventory.setItem(4, GuiUtils.makeItem(Material.ARMOR_STAND, entry.getName(),
                    List.of("ModelId: " + GuiUtils.nullToPlaceholder(entry.getModelId()),
                            "Default Anim: " + GuiUtils.nullToPlaceholder(entry.getDefaultAnimation()),
                            "Spawn: " + (entry.getSpawnTransform() == null ? "unset" : "set"))));

            inventory.setItem(10, GuiUtils.makeItem(Material.NAME_TAG, "Edit ModelId",
                    List.of("Chat input.")));
            inventory.setItem(12, GuiUtils.makeItem(Material.PAPER, "Edit Default Animation",
                    List.of("Chat input (optional).")));
            inventory.setItem(14, GuiUtils.makeItem(Material.ENDER_PEARL, "Set Model Spawn Location",
                    List.of("Placement mode.")));
        }

        inventory.setItem(22, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to model library.")));
        inventory.setItem(26, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        int slot = ctx.getSlot();
        if (slot == 22) {
            editorEngine.openModelLibrary(player, session, true);
            return;
        }
        if (slot == 26) {
            editorEngine.closeEditor(player, session);
            return;
        }
        SceneModelEntry entry = session.getScene().getModelEntry(session.getSelectedModelEntryName());
        if (entry == null) {
            return;
        }
        if (slot == 10) {
            player.closeInventory();
            editorEngine.getInputManager().beginModelEntryModelIdInput(player, session.getScene(), session, entry,
                    GuiType.MODEL_ENTRY_EDITOR);
            return;
        }
        if (slot == 12) {
            player.closeInventory();
            editorEngine.getInputManager().beginModelEntryAnimationInput(player, session.getScene(), session, entry,
                    GuiType.MODEL_ENTRY_EDITOR);
            return;
        }
        if (slot == 14) {
            editorEngine.armModelEntryPlacement(player, session, entry.getName(), GuiType.MODEL_ENTRY_EDITOR);
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}

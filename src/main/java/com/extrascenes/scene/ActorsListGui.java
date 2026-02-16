package com.extrascenes.scene;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ActorsListGui implements EditorGui {
    private static final int PAGE_SIZE = 36;
    private final SceneEditorEngine editorEngine;

    public ActorsListGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        List<SceneActorTemplate> actors = new ArrayList<>(session.getScene().getActorTemplates().values());
        actors.sort(Comparator.comparing(SceneActorTemplate::getActorId, String.CASE_INSENSITIVE_ORDER));
        int totalPages = Math.max(1, (int) Math.ceil(actors.size() / (double) PAGE_SIZE));
        int page = Math.min(session.getActorsPage(), totalPages - 1);
        session.setActorsPage(page);

        Inventory inventory = GuiUtils.createInventory(54,
                session.getSceneName() + " • Actors • Page " + (page + 1) + "/" + totalPages);
        GuiUtils.fillInventory(inventory);

        int start = page * PAGE_SIZE;
        int end = Math.min(actors.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            SceneActorTemplate actor = actors.get(i);
            boolean hasRecording = !actor.getTransformTicks().isEmpty();
            boolean selected = actor.getActorId().equalsIgnoreCase(session.getSelectedActorId());
            inventory.setItem(i - start, GuiUtils.makeItem(
                    selected ? Material.GLOWSTONE_DUST : (hasRecording ? Material.LIME_DYE : Material.GRAY_DYE),
                    actor.getActorId(),
                    List.of(
                            "Recorded ticks: " + actor.getTransformTicks().size(),
                            "Preview: " + (actor.isPreviewEnabled() ? "ON" : "OFF"),
                            "Click for details",
                            selected ? "[SELECTED]" : ""
                    )));
        }

        inventory.setItem(45, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return to dashboard.")));
        inventory.setItem(48, GuiUtils.makeItem(Material.NAME_TAG, "Create Actor", List.of("Creates actor_<n>.")));
        inventory.setItem(50, GuiUtils.makeItem(Material.ANVIL, "Rename Actor", List.of(
                "Chat input guided rename.",
                "Format 1: <oldId> <newId>",
                "Format 2: <newId> (uses selected actor)",
                "The menu will close automatically."
        )));
        inventory.setItem(49, GuiUtils.makeItem(Material.BARRIER, "Close", List.of("Exit editor.")));
        if (totalPages > 1) {
            inventory.setItem(46, GuiUtils.makeItem(Material.ARROW, "Prev", List.of("Page " + (page + 1) + "/" + totalPages)));
            inventory.setItem(52, GuiUtils.makeItem(Material.ARROW, "Next", List.of("Page " + (page + 1) + "/" + totalPages)));
        }
        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        List<SceneActorTemplate> actors = new ArrayList<>(session.getScene().getActorTemplates().values());
        actors.sort(Comparator.comparing(SceneActorTemplate::getActorId, String.CASE_INSENSITIVE_ORDER));
        int totalPages = Math.max(1, (int) Math.ceil(actors.size() / (double) PAGE_SIZE));
        int page = Math.min(session.getActorsPage(), totalPages - 1);

        if (ctx.getSlot() == 45) {
            editorEngine.navigateBack(player, session);
            return;
        }
        if (ctx.getSlot() == 49) {
            editorEngine.closeEditor(player, session);
            return;
        }
        if (ctx.getSlot() == 46 && totalPages > 1) {
            session.setActorsPage(Math.max(0, page - 1));
            refresh(session);
            return;
        }
        if (ctx.getSlot() == 52 && totalPages > 1) {
            session.setActorsPage(Math.min(totalPages - 1, page + 1));
            refresh(session);
            return;
        }
        if (ctx.getSlot() == 48) {
            String base = "actor_";
            int idx = 1;
            while (session.getScene().getActorTemplate(base + idx) != null) {
                idx++;
            }
            SceneActorTemplate created = new SceneActorTemplate(base + idx);
            session.getScene().putActorTemplate(created);
            session.setSelectedActorId(created.getActorId());
            editorEngine.markDirty(session.getScene());
            editorEngine.openActorDetail(player, session, true);
            return;
        }
        if (ctx.getSlot() == 50) {
            player.closeInventory();
            editorEngine.getInputManager().beginActorRenameInput(player, session.getScene(), session,
                    GuiType.ACTORS_LIST);
            return;
        }

        int start = page * PAGE_SIZE;
        int end = Math.min(actors.size(), start + PAGE_SIZE);
        int size = end - start;
        if (ctx.getSlot() >= 0 && ctx.getSlot() < size) {
            session.setSelectedActorId(actors.get(start + ctx.getSlot()).getActorId());
            editorEngine.openActorDetail(player, session, true);
        }
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}

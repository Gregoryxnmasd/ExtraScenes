package com.extrascenes.scene;

import com.extrascenes.Text;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ActorTickActionsGui implements EditorGui {
    private final SceneEditorEngine editorEngine;

    public ActorTickActionsGui(SceneEditorEngine editorEngine) {
        this.editorEngine = editorEngine;
    }

    @Override
    public Inventory build(EditorSession session) {
        SceneActorTemplate actor = session.getScene().getActorTemplate(session.getSelectedActorId());
        Inventory inventory = GuiUtils.createInventory(27, "Actor Tick Actions");
        GuiUtils.fillInventory(inventory);
        if (actor == null) {
            return inventory;
        }
        ActorTickAction action = actor.getOrCreateTickAction(session.getCurrentTick());
        inventory.setItem(4, GuiUtils.makeItem(Material.NAME_TAG, "Actor: " + actor.getActorId(),
                List.of("Tick: " + session.getCurrentTick())));
        inventory.setItem(10, GuiUtils.makeItem(action.isSpawn() ? Material.LIME_DYE : Material.GRAY_DYE,
                "Spawn", List.of("Toggle spawn at this tick")));
        inventory.setItem(11, GuiUtils.makeItem(action.isDespawn() ? Material.RED_DYE : Material.GRAY_DYE,
                "Despawn", List.of("Toggle despawn at this tick")));
        inventory.setItem(12, GuiUtils.makeItem(action.isManualTransform() ? Material.CHAIN : Material.LEVER,
                "Transform Source", List.of(action.isManualTransform() ? "Manual" : "Recorded")));
        inventory.setItem(13, GuiUtils.makeItem(Material.PAPER, "Animation", List.of("Left: set demo", "Right: clear")));
        inventory.setItem(14, GuiUtils.makeItem(Material.COMPASS, "Look-at", List.of("Set to player look target")));
        inventory.setItem(15, GuiUtils.makeItem(Material.COMMAND_BLOCK, "Command", List.of("Left: set /say", "Right: clear")));
        inventory.setItem(16, GuiUtils.makeItem(Material.SLIME_BALL, "Set Scale", List.of("Left: use actor scale", "Right: clear")));
        inventory.setItem(17, GuiUtils.makeItem(Material.PLAYER_HEAD, "Set Skin", List.of("Left: use actor skin", "Right: clear")));
        inventory.setItem(22, GuiUtils.makeItem(Material.ARROW, "Back", List.of("Return timeline")));
        return inventory;
    }

    @Override
    public void handleClick(EditorSession session, ClickContext ctx) {
        Player player = Bukkit.getPlayer(session.getPlayerUuid());
        if (player == null) {
            return;
        }
        Scene scene = session.getScene();
        SceneActorTemplate actor = scene.getActorTemplate(session.getSelectedActorId());
        if (actor == null) {
            editorEngine.openActorTimeline(player, session, false);
            return;
        }
        ActorTickAction action = actor.getOrCreateTickAction(session.getCurrentTick());
        boolean changed = false;
        if (ctx.getSlot() == 10) {
            action.setSpawn(!action.isSpawn());
            changed = true;
        } else if (ctx.getSlot() == 11) {
            action.setDespawn(!action.isDespawn());
            changed = true;
        } else if (ctx.getSlot() == 12) {
            action.setManualTransform(!action.isManualTransform());
            changed = true;
        } else if (ctx.getSlot() == 13) {
            action.setAnimation(ctx.isRightClick() ? null : "idle");
            changed = true;
        } else if (ctx.getSlot() == 14) {
            action.setLookAtTarget(new LookAtTarget(
                    LookAtTarget.Mode.POSITION,
                    Transform.fromLocation(player.getLocation()),
                    null));
            changed = true;
        } else if (ctx.getSlot() == 15) {
            action.setCommand(ctx.isRightClick() ? null : "say actor " + actor.getActorId() + " tick " + session.getCurrentTick());
            changed = true;
        } else if (ctx.getSlot() == 16) {
            action.setScale(ctx.isRightClick() ? null : actor.getScale());
            changed = true;
        } else if (ctx.getSlot() == 17) {
            action.setSkinName(ctx.isRightClick() ? null : actor.getSkinName());
            changed = true;
        } else if (ctx.getSlot() == 22) {
            editorEngine.navigateBack(player, session);
            return;
        }
        if (changed) {
            editorEngine.markDirty(scene);
            Text.send(player, "&a" + "Actor tick action saved.");
        }
        refresh(session);
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}

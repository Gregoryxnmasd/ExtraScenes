package com.extrascenes.scene;

import com.extrascenes.Text;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.Vector;

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
                "Spawn", List.of("Toggle spawn at this tick", "Current: " + action.isSpawn())));
        inventory.setItem(11, GuiUtils.makeItem(action.isDespawn() ? Material.RED_DYE : Material.GRAY_DYE,
                "Despawn", List.of("Toggle despawn at this tick", "Current: " + action.isDespawn())));
        inventory.setItem(12, GuiUtils.makeItem(action.isManualTransform() ? Material.CHAIN : Material.LEVER,
                "Transform Source", List.of("Current: " + (action.isManualTransform() ? "Manual" : "Recorded"))));
        inventory.setItem(13, GuiUtils.makeItem(Material.PAPER, "Animation",
                List.of("Current: " + display(action.getAnimation()), "L-click: edit", "R-click: clear")));
        inventory.setItem(14, GuiUtils.makeItem(Material.COMPASS, "Look-at",
                List.of("L-click: set from crosshair", "R-click: clear",
                        "Current: " + (action.getLookAtTarget() == null ? "none" : action.getLookAtTarget().getMode().name()))));
        inventory.setItem(15, GuiUtils.makeItem(Material.COMMAND_BLOCK, "Command",
                List.of("Current: " + display(action.getCommand()), "L-click: edit", "R-click: clear")));
        inventory.setItem(16, GuiUtils.makeItem(Material.SLIME_BALL, "Set Scale",
                List.of("Current: " + displayScale(action.getScale()), "L-click: edit", "R-click: clear")));
        inventory.setItem(17, GuiUtils.makeItem(Material.PLAYER_HEAD, "Set Skin",
                List.of("Current: " + display(action.getSkinName()), "L-click: edit", "R-click: clear")));
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
            if (ctx.isRightClick()) {
                action.setAnimation(null);
                changed = true;
            } else {
                player.closeInventory();
                editorEngine.getInputManager().beginActorTickAnimationInput(player, scene, session,
                        actor.getActorId(), session.getCurrentTick(), GuiType.ACTOR_TICK_ACTIONS);
                return;
            }
        } else if (ctx.getSlot() == 14) {
            if (ctx.isRightClick()) {
                action.setLookAtTarget(null);
            } else {
                action.setLookAtTarget(buildLookAtTarget(player));
            }
            changed = true;
        } else if (ctx.getSlot() == 15) {
            if (ctx.isRightClick()) {
                action.setCommand(null);
                changed = true;
            } else {
                player.closeInventory();
                editorEngine.getInputManager().beginActorTickCommandInput(player, scene, session,
                        actor.getActorId(), session.getCurrentTick(), GuiType.ACTOR_TICK_ACTIONS);
                return;
            }
        } else if (ctx.getSlot() == 16) {
            if (ctx.isRightClick()) {
                action.setScale(null);
                changed = true;
            } else {
                player.closeInventory();
                editorEngine.getInputManager().beginActorTickScaleInput(player, scene, session,
                        actor.getActorId(), session.getCurrentTick(), GuiType.ACTOR_TICK_ACTIONS);
                return;
            }
        } else if (ctx.getSlot() == 17) {
            if (ctx.isRightClick()) {
                action.setSkinName(null);
                changed = true;
            } else {
                player.closeInventory();
                editorEngine.getInputManager().beginActorTickSkinInput(player, scene, session,
                        actor.getActorId(), session.getCurrentTick(), GuiType.ACTOR_TICK_ACTIONS);
                return;
            }
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

    private LookAtTarget buildLookAtTarget(Player player) {
        Block targetBlock = player.getTargetBlockExact(64);
        org.bukkit.Location target;
        if (targetBlock != null) {
            target = targetBlock.getLocation().add(0.5, 0.5, 0.5);
        } else {
            target = player.getEyeLocation().clone();
            Vector direction = target.getDirection().normalize().multiply(8.0);
            target.add(direction);
        }
        return new LookAtTarget(LookAtTarget.Mode.POSITION, Transform.fromLocation(target), null);
    }

    private String display(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private String displayScale(Double scale) {
        return scale == null ? "none" : String.format(Locale.ROOT, "%.3f", scale);
    }

    @Override
    public void refresh(EditorSession session) {
        GuiUtils.refreshInventory(session, build(session));
    }
}

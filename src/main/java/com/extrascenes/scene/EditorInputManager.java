package com.extrascenes.scene;

import com.extrascenes.ExtraScenesPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class EditorInputManager {
    public enum PromptType {
        COMMANDS,
        COMMAND_APPEND,
        COMMANDS_APPEND,
        CURSOR_TIME,
        KEYFRAME_TIME,
        GROUP_JUMP,
        MOVE_TICK,
        COPY_TICK,
        SHIFT_RANGE,
        ACTIONBAR_TEXT,
        ACTIONBAR_DURATION,
        MODEL_FIELD,
        MODEL_ACTION,
        MODEL_MODEL_ID,
        MODEL_ANIMATION_ID,
        MODEL_LOOP,
        MODEL_SPEED,
        MODEL_SPEED_EDIT,
        MODEL_ENTRY_CREATE_NAME,
        MODEL_ENTRY_CREATE_MODEL_ID,
        MODEL_ENTRY_MODEL_ID,
        MODEL_ENTRY_ANIMATION_ID
    }

    public enum ModelField {
        ANIMATION_ID
    }

    public static class PromptState {
        private final UUID playerId;
        private final Scene scene;
        private final EditorSession editorSession;
        private PromptType type;
        private final List<String> commandBuffer = new ArrayList<>();
        private ModelKeyframe modelKeyframe;
        private CommandKeyframe commandTarget;
        private ModelField modelField;
        private SceneModelEntry modelEntry;
        private GuiType returnGui;
        private SceneTrackType trackType;
        private UUID keyframeId;

        public PromptState(UUID playerId, Scene scene, EditorSession editorSession, PromptType type) {
            this.playerId = playerId;
            this.scene = scene;
            this.editorSession = editorSession;
            this.type = type;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public Scene getScene() {
            return scene;
        }

        public EditorSession getEditorSession() {
            return editorSession;
        }

        public PromptType getType() {
            return type;
        }

        public void setType(PromptType type) {
            this.type = type;
        }

        public List<String> getCommandBuffer() {
            return commandBuffer;
        }

        public ModelKeyframe getModelKeyframe() {
            return modelKeyframe;
        }

        public void setModelKeyframe(ModelKeyframe modelKeyframe) {
            this.modelKeyframe = modelKeyframe;
        }

        public CommandKeyframe getCommandTarget() {
            return commandTarget;
        }

        public void setCommandTarget(CommandKeyframe commandTarget) {
            this.commandTarget = commandTarget;
        }

        public ModelField getModelField() {
            return modelField;
        }

        public void setModelField(ModelField modelField) {
            this.modelField = modelField;
        }

        public SceneModelEntry getModelEntry() {
            return modelEntry;
        }

        public void setModelEntry(SceneModelEntry modelEntry) {
            this.modelEntry = modelEntry;
        }

        public GuiType getReturnGui() {
            return returnGui;
        }

        public void setReturnGui(GuiType returnGui) {
            this.returnGui = returnGui;
        }

        public SceneTrackType getTrackType() {
            return trackType;
        }

        public void setTrackType(SceneTrackType trackType) {
            this.trackType = trackType;
        }

        public UUID getKeyframeId() {
            return keyframeId;
        }

        public void setKeyframeId(UUID keyframeId) {
            this.keyframeId = keyframeId;
        }
    }

    private final ExtraScenesPlugin plugin;
    private final SceneManager sceneManager;
    private final SceneEditorEngine editorEngine;
    private final java.util.Map<UUID, PromptState> prompts = new java.util.HashMap<>();

    public EditorInputManager(ExtraScenesPlugin plugin, SceneManager sceneManager, SceneEditorEngine editorEngine) {
        this.plugin = plugin;
        this.sceneManager = sceneManager;
        this.editorEngine = editorEngine;
    }

    public void beginCommandInput(Player player, Scene scene, EditorSession session, GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.COMMANDS);
        state.setReturnGui(returnGui);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter commands one per message. Type 'done' to finish.");
    }

    public void beginCommandAppendInput(Player player, Scene scene, EditorSession session, CommandKeyframe target,
                                        GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.COMMAND_APPEND);
        state.setCommandTarget(target);
        state.setReturnGui(returnGui);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter a command to add. Type 'cancel' to abort.");
    }

    public void beginCommandAppendInputMulti(Player player, Scene scene, EditorSession session, CommandKeyframe target,
                                             GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.COMMANDS_APPEND);
        state.setCommandTarget(target);
        state.setReturnGui(returnGui);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter commands one per message. Type 'done' to finish.");
    }

    public void beginCursorInput(Player player, Scene scene, EditorSession session, GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.CURSOR_TIME);
        state.setReturnGui(returnGui);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter cursor time in ticks. Type 'cancel' to abort.");
    }

    public void beginModelInput(Player player, Scene scene, EditorSession session, GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.MODEL_ACTION);
        state.setReturnGui(returnGui);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter model action (SPAWN/ANIM/STOP/DESPAWN):");
    }

    public void beginModelInput(Player player, Scene scene, EditorSession session, ModelKeyframe.Action action,
                                GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.MODEL_ACTION);
        state.setReturnGui(returnGui);
        state.setModelKeyframe(new ModelKeyframe(null, session.getCursorTimeTicks(), action));
        if (action == ModelKeyframe.Action.SPAWN) {
            state.setType(PromptType.MODEL_MODEL_ID);
            player.sendMessage(ChatColor.AQUA + "Enter model entry name:");
        } else {
            state.setType(PromptType.MODEL_MODEL_ID);
            player.sendMessage(ChatColor.AQUA + "Enter model entry name:");
        }
        prompts.put(player.getUniqueId(), state);
    }

    public void beginModelFieldInput(Player player, Scene scene, EditorSession session, ModelKeyframe target,
                                     ModelField field, GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.MODEL_FIELD);
        state.setModelKeyframe(target);
        state.setModelField(field);
        state.setReturnGui(returnGui);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter value for " + field.name() + ". Type 'cancel' to abort.");
    }

    public void beginModelSpeedInput(Player player, Scene scene, EditorSession session, ModelKeyframe target,
                                     GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.MODEL_SPEED_EDIT);
        state.setModelKeyframe(target);
        state.setReturnGui(returnGui);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter animation speed (e.g. 1.0). Type 'cancel' to abort.");
    }

    public void beginKeyframeTimeInput(Player player, Scene scene, EditorSession session, SceneTrackType trackType,
                                       UUID keyframeId, GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.KEYFRAME_TIME);
        state.setTrackType(trackType);
        state.setKeyframeId(keyframeId);
        state.setReturnGui(returnGui);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter new keyframe time in ticks. Type 'cancel' to abort.");
    }

    public void beginGroupJumpInput(Player player, Scene scene, EditorSession session, GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.GROUP_JUMP);
        state.setReturnGui(returnGui);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter group number to open. Type 'cancel' to abort.");
    }

    public void beginMoveTickInput(Player player, Scene scene, EditorSession session, int fromTick, GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.MOVE_TICK);
        state.setReturnGui(returnGui);
        state.setKeyframeId(null);
        state.getCommandBuffer().add(String.valueOf(fromTick));
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter destination tick. Type 'cancel' to abort.");
    }

    public void beginCopyTickInput(Player player, Scene scene, EditorSession session, int fromTick, GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.COPY_TICK);
        state.setReturnGui(returnGui);
        state.getCommandBuffer().add(String.valueOf(fromTick));
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter destination tick to copy into. Type 'cancel' to abort.");
    }

    public void beginShiftRangeInput(Player player, Scene scene, EditorSession session, GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.SHIFT_RANGE);
        state.setReturnGui(returnGui);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter shift range: <fromTick> <toTick> <delta>. Type 'cancel' to abort.");
    }

    public void beginActionBarTextInput(Player player, Scene scene, EditorSession session, GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.ACTIONBAR_TEXT);
        state.setReturnGui(returnGui);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter actionbar text. Type 'cancel' to abort.");
    }

    public void beginActionBarDurationInput(Player player, Scene scene, EditorSession session, GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.ACTIONBAR_DURATION);
        state.setReturnGui(returnGui);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter actionbar duration in ticks. Type 'cancel' to abort.");
    }

    public void beginModelEntryCreate(Player player, Scene scene, EditorSession session, GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.MODEL_ENTRY_CREATE_NAME);
        state.setReturnGui(returnGui);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter a name for the model entry. Type 'cancel' to abort.");
    }

    public void beginModelEntryModelIdInput(Player player, Scene scene, EditorSession session, SceneModelEntry entry,
                                            GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.MODEL_ENTRY_MODEL_ID);
        state.setModelEntry(entry);
        state.setReturnGui(returnGui);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter modelId for " + entry.getName() + ". Type 'cancel' to abort.");
    }

    public void beginModelEntryAnimationInput(Player player, Scene scene, EditorSession session, SceneModelEntry entry,
                                              GuiType returnGui) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.MODEL_ENTRY_ANIMATION_ID);
        state.setModelEntry(entry);
        state.setReturnGui(returnGui);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter default animation for " + entry.getName()
                + " (or leave empty). Type 'cancel' to abort.");
    }

    public boolean handleChat(Player player, String message) {
        PromptState state = prompts.get(player.getUniqueId());
        if (state == null) {
            return false;
        }
        message = message.trim();
        if (message.equalsIgnoreCase("cancel")) {
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                    state.getReturnGui() == null ? GuiType.SCENE_DASHBOARD : state.getReturnGui()));
            player.sendMessage(ChatColor.YELLOW + "Editor input cancelled.");
            return true;
        }

        switch (state.getType()) {
            case COMMANDS -> handleCommandInput(player, state, message);
            case COMMAND_APPEND -> handleCommandAppend(player, state, message);
            case COMMANDS_APPEND -> handleCommandAppendMulti(player, state, message);
            case CURSOR_TIME -> handleCursorInput(player, state, message);
            case KEYFRAME_TIME -> handleKeyframeTimeInput(player, state, message);
            case GROUP_JUMP -> handleGroupJump(player, state, message);
            case MOVE_TICK -> handleMoveTick(player, state, message);
            case COPY_TICK -> handleCopyTick(player, state, message);
            case SHIFT_RANGE -> handleShiftRange(player, state, message);
            case ACTIONBAR_TEXT -> handleActionBarText(player, state, message);
            case ACTIONBAR_DURATION -> handleActionBarDuration(player, state, message);
            case MODEL_FIELD -> handleModelField(player, state, message);
            case MODEL_ACTION -> handleModelAction(player, state, message);
            case MODEL_MODEL_ID -> handleModelModelId(player, state, message);
            case MODEL_ANIMATION_ID -> handleModelAnimationId(player, state, message);
            case MODEL_LOOP -> handleModelLoop(player, state, message);
            case MODEL_SPEED -> handleModelSpeed(player, state, message);
            case MODEL_SPEED_EDIT -> handleModelSpeedEdit(player, state, message);
            case MODEL_ENTRY_CREATE_NAME -> handleModelEntryCreateName(player, state, message);
            case MODEL_ENTRY_CREATE_MODEL_ID -> handleModelEntryCreateModelId(player, state, message);
            case MODEL_ENTRY_MODEL_ID -> handleModelEntryModelId(player, state, message);
            case MODEL_ENTRY_ANIMATION_ID -> handleModelEntryAnimation(player, state, message);
            default -> {
            }
        }
        return true;
    }

    private void handleCommandInput(Player player, PromptState state, String message) {
        if (message.equalsIgnoreCase("done")) {
            CommandKeyframe keyframe = new CommandKeyframe(null, state.getEditorSession().getCursorTimeTicks(),
                    state.getCommandBuffer(), CommandKeyframe.ExecutorMode.PLAYER, false);
            Track<CommandKeyframe> track = state.getScene().getTrack(SceneTrackType.COMMAND);
            track.addKeyframe(keyframe);
            editorEngine.markDirty(state.getScene());
            prompts.remove(player.getUniqueId());
            state.getEditorSession().setSelectedKeyframeId(keyframe.getId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                    state.getReturnGui() == null ? GuiType.KEYFRAME_LIST : state.getReturnGui()));
            player.sendMessage(ChatColor.GREEN + "Command keyframe added.");
            return;
        }
        state.getCommandBuffer().add(message);
        player.sendMessage(ChatColor.GRAY + "Added command. Type 'done' to finish or more commands.");
    }

    private void handleCommandAppend(Player player, PromptState state, String message) {
        if (state.getCommandTarget() == null) {
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                    state.getReturnGui() == null ? GuiType.COMMAND_EDITOR : state.getReturnGui()));
            return;
        }
        state.getCommandTarget().addCommand(message);
        editorEngine.markDirty(state.getScene());
        prompts.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                state.getReturnGui() == null ? GuiType.COMMAND_EDITOR : state.getReturnGui()));
        player.sendMessage(ChatColor.GREEN + "Command added to keyframe.");
    }

    private void handleCommandAppendMulti(Player player, PromptState state, String message) {
        if (state.getCommandTarget() == null) {
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                    state.getReturnGui() == null ? GuiType.COMMAND_EDITOR : state.getReturnGui()));
            return;
        }
        if (message.equalsIgnoreCase("done")) {
            for (String cmd : state.getCommandBuffer()) {
                state.getCommandTarget().addCommand(cmd);
            }
            editorEngine.markDirty(state.getScene());
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                    state.getReturnGui() == null ? GuiType.COMMAND_EDITOR : state.getReturnGui()));
            player.sendMessage(ChatColor.GREEN + "Commands added to keyframe.");
            return;
        }
        state.getCommandBuffer().add(message);
        player.sendMessage(ChatColor.GRAY + "Added command. Type 'done' to finish or more commands.");
    }
    private void handleCursorInput(Player player, PromptState state, String message) {
        try {
            int ticks = Integer.parseInt(message);
            state.getEditorSession().setCursorTimeTicks(ticks);
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                    state.getReturnGui() == null ? GuiType.KEYFRAME_LIST : state.getReturnGui()));
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Invalid number. Enter cursor time in ticks.");
        }
    }

    private void handleKeyframeTimeInput(Player player, PromptState state, String message) {
        try {
            int ticks = Integer.parseInt(message);
            Track<? extends Keyframe> track = state.getScene().getTrack(state.getTrackType());
            if (track != null && state.getKeyframeId() != null) {
                track.moveKeyframe(state.getKeyframeId(), ticks);
                editorEngine.markDirty(state.getScene());
            }
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                    state.getReturnGui() == null ? GuiType.KEYFRAME_LIST : state.getReturnGui()));
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Invalid number. Enter keyframe time in ticks.");
        }
    }

    private void handleGroupJump(Player player, PromptState state, String message) {
        try {
            int group = Integer.parseInt(message);
            state.getEditorSession().setCurrentGroup(group);
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGroupGrid(player, state.getEditorSession(), false));
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Invalid number. Enter group number.");
        }
    }

    private void handleModelAction(Player player, PromptState state, String message) {
        try {
            ModelKeyframe.Action action = ModelKeyframe.Action.valueOf(message.toUpperCase());
            ModelKeyframe keyframe = new ModelKeyframe(null, state.getEditorSession().getCursorTimeTicks(), action);
            state.setModelKeyframe(keyframe);
            if (action == ModelKeyframe.Action.SPAWN) {
                state.setType(PromptType.MODEL_MODEL_ID);
                player.sendMessage(ChatColor.AQUA + "Enter model entry name:");
            } else {
                state.setType(PromptType.MODEL_MODEL_ID);
                player.sendMessage(ChatColor.AQUA + "Enter model entry name:");
            }
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ChatColor.RED + "Invalid action. Use SPAWN/ANIM/STOP/DESPAWN.");
        }
    }

    private void handleModelField(Player player, PromptState state, String message) {
        ModelKeyframe target = state.getModelKeyframe();
        if (target == null || state.getModelField() == null) {
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                    state.getReturnGui() == null ? GuiType.MODEL_EDITOR : state.getReturnGui()));
            return;
        }
        switch (state.getModelField()) {
            case ANIMATION_ID -> target.setAnimationId(message);
            default -> {
            }
        }
        prompts.remove(player.getUniqueId());
        editorEngine.markDirty(state.getScene());
        Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                state.getReturnGui() == null ? GuiType.MODEL_EDITOR : state.getReturnGui()));
        player.sendMessage(ChatColor.GREEN + "Model keyframe updated.");
    }

    private void handleModelModelId(Player player, PromptState state, String message) {
        SceneModelEntry entry = state.getScene().getModelEntry(message);
        if (entry == null) {
            player.sendMessage(ChatColor.RED + "Model entry not found. Use the model library to create it first.");
            return;
        }
        state.getModelKeyframe().setModelEntry(entry.getName());
        state.getModelKeyframe().setModelId(entry.getModelId());
        if (state.getModelKeyframe().getAction() == ModelKeyframe.Action.SPAWN
                || state.getModelKeyframe().getAction() == ModelKeyframe.Action.DESPAWN) {
            finalizeModelKeyframe(player, state);
            return;
        }
        player.sendMessage(ChatColor.AQUA + "Enter animationId (or leave empty):");
        state.setType(PromptType.MODEL_ANIMATION_ID);
    }

    private void handleModelAnimationId(Player player, PromptState state, String message) {
        state.getModelKeyframe().setAnimationId(message == null || message.isBlank() ? null : message);
        if (state.getModelKeyframe().getAction() == ModelKeyframe.Action.ANIM) {
            player.sendMessage(ChatColor.AQUA + "Loop animation? (true/false)");
            state.setType(PromptType.MODEL_LOOP);
        } else if (state.getModelKeyframe().getAction() == ModelKeyframe.Action.STOP) {
            finalizeModelKeyframe(player, state);
        } else {
            finalizeModelKeyframe(player, state);
        }
    }

    private void handleModelLoop(Player player, PromptState state, String message) {
        state.getModelKeyframe().setLoop(Boolean.parseBoolean(message));
        player.sendMessage(ChatColor.AQUA + "Animation speed (e.g. 1.0):");
        state.setType(PromptType.MODEL_SPEED);
    }

    private void handleModelSpeed(Player player, PromptState state, String message) {
        try {
            state.getModelKeyframe().setSpeed(Double.parseDouble(message));
        } catch (NumberFormatException ex) {
            state.getModelKeyframe().setSpeed(1.0);
        }
        finalizeModelKeyframe(player, state);
    }

    private void finalizeModelKeyframe(Player player, PromptState state) {
        Track<ModelKeyframe> track = state.getScene().getTrack(SceneTrackType.MODEL);
        track.addKeyframe(state.getModelKeyframe());
        editorEngine.markDirty(state.getScene());
        prompts.remove(player.getUniqueId());
        state.getEditorSession().setSelectedKeyframeId(state.getModelKeyframe().getId());
        Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                state.getReturnGui() == null ? GuiType.KEYFRAME_LIST : state.getReturnGui()));
        player.sendMessage(ChatColor.GREEN + "Model keyframe added.");
    }

    private void handleModelSpeedEdit(Player player, PromptState state, String message) {
        if (state.getModelKeyframe() == null) {
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                    state.getReturnGui() == null ? GuiType.MODEL_EDITOR : state.getReturnGui()));
            return;
        }
        try {
            state.getModelKeyframe().setSpeed(Double.parseDouble(message));
        } catch (NumberFormatException ex) {
            state.getModelKeyframe().setSpeed(1.0);
        }
        prompts.remove(player.getUniqueId());
        editorEngine.markDirty(state.getScene());
        Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                state.getReturnGui() == null ? GuiType.MODEL_EDITOR : state.getReturnGui()));
        player.sendMessage(ChatColor.GREEN + "Model speed updated.");
    }

    private void handleMoveTick(Player player, PromptState state, String message) {
        try {
            int toTick = Integer.parseInt(message);
            int fromTick = Integer.parseInt(state.getCommandBuffer().get(0));
            editorEngine.moveTick(state.getScene(), fromTick, toTick, state.getEditorSession());
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                    state.getReturnGui() == null ? GuiType.GROUP_GRID : state.getReturnGui()));
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Invalid number. Enter destination tick.");
        }
    }

    private void handleCopyTick(Player player, PromptState state, String message) {
        try {
            int toTick = Integer.parseInt(message);
            int fromTick = Integer.parseInt(state.getCommandBuffer().get(0));
            editorEngine.duplicateTickActions(state.getScene(), fromTick, toTick, state.getEditorSession());
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                    state.getReturnGui() == null ? GuiType.TICK_TOOLS : state.getReturnGui()));
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Invalid number. Enter destination tick.");
        }
    }

    private void handleShiftRange(Player player, PromptState state, String message) {
        String[] parts = message.split("\\\\s+");
        if (parts.length < 3) {
            player.sendMessage(ChatColor.RED + "Enter: <fromTick> <toTick> <delta>.");
            return;
        }
        try {
            int from = Integer.parseInt(parts[0]);
            int to = Integer.parseInt(parts[1]);
            int delta = Integer.parseInt(parts[2]);
            editorEngine.shiftRange(state.getScene(), from, to, delta, state.getEditorSession());
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                    state.getReturnGui() == null ? GuiType.GROUP_GRID : state.getReturnGui()));
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Invalid numbers. Enter: <fromTick> <toTick> <delta>.");
        }
    }

    private void handleActionBarText(Player player, PromptState state, String message) {
        ActionBarKeyframe keyframe = editorEngine.getSelectedActionBarKeyframe(state.getEditorSession());
        if (keyframe == null) {
            prompts.remove(player.getUniqueId());
            return;
        }
        keyframe.setText(message);
        editorEngine.markDirty(state.getScene());
        prompts.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                state.getReturnGui() == null ? GuiType.ACTIONBAR_EDITOR : state.getReturnGui()));
        player.sendMessage(ChatColor.GREEN + "Actionbar text updated.");
    }

    private void handleActionBarDuration(Player player, PromptState state, String message) {
        ActionBarKeyframe keyframe = editorEngine.getSelectedActionBarKeyframe(state.getEditorSession());
        if (keyframe == null) {
            prompts.remove(player.getUniqueId());
            return;
        }
        try {
            keyframe.setDurationTicks(Integer.parseInt(message));
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Invalid duration. Enter ticks.");
            return;
        }
        editorEngine.markDirty(state.getScene());
        prompts.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                state.getReturnGui() == null ? GuiType.ACTIONBAR_EDITOR : state.getReturnGui()));
        player.sendMessage(ChatColor.GREEN + "Actionbar duration updated.");
    }

    private void handleModelEntryCreateName(Player player, PromptState state, String message) {
        if (message.isBlank()) {
            player.sendMessage(ChatColor.RED + "Entry name cannot be empty.");
            return;
        }
        if (state.getScene().getModelEntry(message) != null) {
            player.sendMessage(ChatColor.RED + "A model entry with that name already exists.");
            return;
        }
        state.setModelEntry(new SceneModelEntry(message, null, null, null));
        state.setType(PromptType.MODEL_ENTRY_CREATE_MODEL_ID);
        player.sendMessage(ChatColor.AQUA + "Enter modelId for " + message + ":");
    }

    private void handleModelEntryCreateModelId(Player player, PromptState state, String message) {
        SceneModelEntry entry = state.getModelEntry();
        if (entry == null) {
            prompts.remove(player.getUniqueId());
            return;
        }
        entry.setModelId(message);
        state.getScene().putModelEntry(entry);
        editorEngine.markDirty(state.getScene());
        prompts.remove(player.getUniqueId());
        state.getEditorSession().setSelectedModelEntryName(entry.getName());
        Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                GuiType.MODEL_ENTRY_EDITOR));
        player.sendMessage(ChatColor.GREEN + "Model entry created. Set its spawn location with placement mode.");
    }

    private void handleModelEntryModelId(Player player, PromptState state, String message) {
        SceneModelEntry entry = state.getModelEntry();
        if (entry == null) {
            prompts.remove(player.getUniqueId());
            return;
        }
        entry.setModelId(message);
        editorEngine.markDirty(state.getScene());
        prompts.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                state.getReturnGui() == null ? GuiType.MODEL_ENTRY_EDITOR : state.getReturnGui()));
        player.sendMessage(ChatColor.GREEN + "ModelId updated.");
    }

    private void handleModelEntryAnimation(Player player, PromptState state, String message) {
        SceneModelEntry entry = state.getModelEntry();
        if (entry == null) {
            prompts.remove(player.getUniqueId());
            return;
        }
        entry.setDefaultAnimation(message == null || message.isBlank() ? null : message);
        editorEngine.markDirty(state.getScene());
        prompts.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openGuiByType(player, state.getEditorSession(),
                state.getReturnGui() == null ? GuiType.MODEL_ENTRY_EDITOR : state.getReturnGui()));
        player.sendMessage(ChatColor.GREEN + "Default animation updated.");
    }

    public void clearPrompt(UUID playerId) {
        prompts.remove(playerId);
    }

    public boolean hasPrompt(UUID playerId) {
        return prompts.containsKey(playerId);
    }
}

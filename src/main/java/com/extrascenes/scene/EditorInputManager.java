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
        CURSOR_TIME,
        MODEL_FIELD,
        MODEL_ACTION,
        MODEL_MODEL_ID,
        MODEL_ENTITY_REF,
        MODEL_ANIMATION_ID,
        MODEL_LOOP,
        MODEL_SPEED
    }

    public enum ModelField {
        ENTITY_REF,
        MODEL_ID,
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

    public void beginCommandInput(Player player, Scene scene, EditorSession session) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.COMMANDS);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter commands one per message. Type 'done' to finish.");
    }

    public void beginCommandAppendInput(Player player, Scene scene, EditorSession session, CommandKeyframe target) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.COMMAND_APPEND);
        state.setCommandTarget(target);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter a command to add. Type 'cancel' to abort.");
    }

    public void beginCursorInput(Player player, Scene scene, EditorSession session) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.CURSOR_TIME);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter cursor time in ticks. Type 'cancel' to abort.");
    }

    public void beginModelInput(Player player, Scene scene, EditorSession session) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.MODEL_ACTION);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter model action (SPAWN/ANIM/STOP/DESPAWN):");
    }

    public void beginModelFieldInput(Player player, Scene scene, EditorSession session, ModelKeyframe target, ModelField field) {
        PromptState state = new PromptState(player.getUniqueId(), scene, session, PromptType.MODEL_FIELD);
        state.setModelKeyframe(target);
        state.setModelField(field);
        prompts.put(player.getUniqueId(), state);
        player.sendMessage(ChatColor.AQUA + "Enter value for " + field.name() + ". Type 'cancel' to abort.");
    }

    public boolean handleChat(Player player, String message) {
        PromptState state = prompts.get(player.getUniqueId());
        if (state == null) {
            return false;
        }
        message = message.trim();
        if (message.equalsIgnoreCase("cancel")) {
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openMainMenu(player, state.getScene(), state.getEditorSession()));
            player.sendMessage(ChatColor.YELLOW + "Editor input cancelled.");
            return true;
        }

        switch (state.getType()) {
            case COMMANDS -> handleCommandInput(player, state, message);
            case COMMAND_APPEND -> handleCommandAppend(player, state, message);
            case CURSOR_TIME -> handleCursorInput(player, state, message);
            case MODEL_FIELD -> handleModelField(player, state, message);
            case MODEL_ACTION -> handleModelAction(player, state, message);
            case MODEL_MODEL_ID -> handleModelModelId(player, state, message);
            case MODEL_ENTITY_REF -> handleModelEntityRef(player, state, message);
            case MODEL_ANIMATION_ID -> handleModelAnimationId(player, state, message);
            case MODEL_LOOP -> handleModelLoop(player, state, message);
            case MODEL_SPEED -> handleModelSpeed(player, state, message);
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
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openMainMenu(player, state.getScene(), state.getEditorSession()));
            player.sendMessage(ChatColor.GREEN + "Command keyframe added.");
            return;
        }
        state.getCommandBuffer().add(message);
        player.sendMessage(ChatColor.GRAY + "Added command. Type 'done' to finish or more commands.");
    }

    private void handleCommandAppend(Player player, PromptState state, String message) {
        if (state.getCommandTarget() == null) {
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openMainMenu(player, state.getScene(), state.getEditorSession()));
            return;
        }
        state.getCommandTarget().addCommand(message);
        prompts.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openEditMenu(player, state.getScene(), state.getEditorSession(), state.getCommandTarget()));
        player.sendMessage(ChatColor.GREEN + "Command added to keyframe.");
    }
    private void handleCursorInput(Player player, PromptState state, String message) {
        try {
            int ticks = Integer.parseInt(message);
            state.getEditorSession().setCursorTimeTicks(ticks);
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openMainMenu(player, state.getScene(), state.getEditorSession()));
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Invalid number. Enter cursor time in ticks.");
        }
    }

    private void handleModelAction(Player player, PromptState state, String message) {
        try {
            ModelKeyframe.Action action = ModelKeyframe.Action.valueOf(message.toUpperCase());
            ModelKeyframe keyframe = new ModelKeyframe(null, state.getEditorSession().getCursorTimeTicks(), action);
            state.setModelKeyframe(keyframe);
            state.setType(action == ModelKeyframe.Action.ANIM ? PromptType.MODEL_ENTITY_REF : PromptType.MODEL_MODEL_ID);
            if (action == ModelKeyframe.Action.ANIM || action == ModelKeyframe.Action.STOP || action == ModelKeyframe.Action.DESPAWN) {
                state.setType(PromptType.MODEL_ENTITY_REF);
                player.sendMessage(ChatColor.AQUA + "Enter entityRef:");
            } else {
                player.sendMessage(ChatColor.AQUA + "Enter modelId:");
            }
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ChatColor.RED + "Invalid action. Use SPAWN/ANIM/STOP/DESPAWN.");
        }
    }

    private void handleModelField(Player player, PromptState state, String message) {
        ModelKeyframe target = state.getModelKeyframe();
        if (target == null || state.getModelField() == null) {
            prompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openMainMenu(player, state.getScene(), state.getEditorSession()));
            return;
        }
        switch (state.getModelField()) {
            case ENTITY_REF -> target.setEntityRef(message);
            case MODEL_ID -> target.setModelId(message);
            case ANIMATION_ID -> target.setAnimationId(message);
            default -> {
            }
        }
        prompts.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openEditMenu(player, state.getScene(), state.getEditorSession(), target));
        player.sendMessage(ChatColor.GREEN + "Model keyframe updated.");
    }

    private void handleModelModelId(Player player, PromptState state, String message) {
        state.getModelKeyframe().setModelId(message);
        player.sendMessage(ChatColor.AQUA + "Enter entityRef:");
        state.setType(PromptType.MODEL_ENTITY_REF);
    }

    private void handleModelEntityRef(Player player, PromptState state, String message) {
        state.getModelKeyframe().setEntityRef(message);
        if (state.getModelKeyframe().getAction() == ModelKeyframe.Action.ANIM
                || state.getModelKeyframe().getAction() == ModelKeyframe.Action.STOP) {
            player.sendMessage(ChatColor.AQUA + "Enter animationId:");
            state.setType(PromptType.MODEL_ANIMATION_ID);
        } else {
            finalizeModelKeyframe(player, state);
        }
    }

    private void handleModelAnimationId(Player player, PromptState state, String message) {
        state.getModelKeyframe().setAnimationId(message);
        if (state.getModelKeyframe().getAction() == ModelKeyframe.Action.ANIM) {
            player.sendMessage(ChatColor.AQUA + "Loop animation? (true/false)");
            state.setType(PromptType.MODEL_LOOP);
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
        if (state.getModelKeyframe().getAction() == ModelKeyframe.Action.SPAWN) {
            state.getModelKeyframe().setSpawnTransform(Transform.fromLocation(player.getLocation()));
        }
        Track<ModelKeyframe> track = state.getScene().getTrack(SceneTrackType.MODEL);
        track.addKeyframe(state.getModelKeyframe());
        prompts.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> editorEngine.openMainMenu(player, state.getScene(), state.getEditorSession()));
        player.sendMessage(ChatColor.GREEN + "Model keyframe added.");
    }

    public void clearPrompt(UUID playerId) {
        prompts.remove(playerId);
    }

    public boolean hasPrompt(UUID playerId) {
        return prompts.containsKey(playerId);
    }
}

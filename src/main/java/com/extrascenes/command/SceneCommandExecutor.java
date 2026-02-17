package com.extrascenes.command;

import com.extrascenes.Text;

import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.scene.ActorRecordingService;
import com.extrascenes.scene.EditorSession;
import com.extrascenes.scene.Scene;
import com.extrascenes.scene.SceneEditorEngine;
import com.extrascenes.scene.SceneLocation;
import com.extrascenes.scene.SceneManager;
import com.extrascenes.scene.SceneSession;
import com.extrascenes.scene.SceneSelfTestRunner;
import com.extrascenes.scene.SceneSessionManager;
import com.extrascenes.scene.SceneActorTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class SceneCommandExecutor implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "main", "edit", "play", "stop", "pause", "resume", "reload", "list",
            "create", "delete", "rename", "duplicate", "group", "tick", "cancel", "here", "setend",
            "debugcamera", "debugpreview", "debugactors", "debugvisibility", "actor", "selftest"
    );

    private final ExtraScenesPlugin plugin;
    private final SceneManager sceneManager;
    private final SceneSessionManager sessionManager;
    private final SceneEditorEngine editorEngine;
    private final ActorRecordingService actorRecordingService;
    private final SceneSelfTestRunner selfTestRunner;

    public SceneCommandExecutor(ExtraScenesPlugin plugin, SceneManager sceneManager,
                                SceneSessionManager sessionManager, SceneEditorEngine editorEngine) {
        this.plugin = plugin;
        this.sceneManager = sceneManager;
        this.sessionManager = sessionManager;
        this.editorEngine = editorEngine;
        this.actorRecordingService = plugin.getActorRecordingService();
        this.selfTestRunner = new SceneSelfTestRunner(plugin, sessionManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                editorEngine.openMainMenu(player);
            } else {
                sendHelp(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "main" -> handleMain(sender);
            case "create" -> handleCreate(sender, args);
            case "edit" -> handleEdit(sender, args);
            case "group" -> handleGroup(sender, args);
            case "tick" -> handleTick(sender, args);
            case "cancel" -> handleCancel(sender);
            case "here" -> handleHere(sender);
            case "setend" -> handleSetEnd(sender, args);
            case "play" -> handlePlay(sender, args);
            case "stop" -> handleStop(sender, args);
            case "pause" -> handlePause(sender, args);
            case "resume" -> handleResume(sender, args);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            case "delete" -> handleDelete(sender, args);
            case "rename" -> handleRename(sender, args);
            case "duplicate" -> handleDuplicate(sender, args);
            case "debugcamera" -> handleDebugCamera(sender, args);
            case "debugpreview" -> handleDebugPreview(sender, args);
            case "debugactors" -> handleDebugActors(sender, args);
            case "debugvisibility" -> handleDebugVisibility(sender, args);
            case "actor" -> handleActor(sender, args);
            case "selftest" -> handleSelfTest(sender, args);
            default -> Text.send(sender, "&c" + "Unknown scene subcommand.");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        Text.send(sender, "&b" + "/scene main");
        Text.send(sender, "&b" + "/scene edit <name>");
        Text.send(sender, "&b" + "/scene play <name> [player] [startTick] [endTick]");
        Text.send(sender, "&b" + "/scene stop [player]");
        Text.send(sender, "&b" + "/scene pause [player]");
        Text.send(sender, "&b" + "/scene resume [player]");
        Text.send(sender, "&b" + "/scene reload");
        Text.send(sender, "&b" + "/scene list");
        Text.send(sender, "&b" + "/scene create <name> [duration]");
        Text.send(sender, "&b" + "/scene delete <name>");
        Text.send(sender, "&b" + "/scene rename <old> <new>");
        Text.send(sender, "&b" + "/scene duplicate <source> <target>");
        Text.send(sender, "&b" + "/scene group <number>");
        Text.send(sender, "&b" + "/scene tick <tick>");
        Text.send(sender, "&b" + "/scene cancel");
        Text.send(sender, "&b" + "/scene here");
        Text.send(sender, "&b" + "/scene setend <here|x y z yaw pitch>");
        Text.send(sender, "&b" + "/scene debugcamera <on|off>");
        Text.send(sender, "&b" + "/scene debugpreview <on|off>");
        Text.send(sender, "&b" + "/scene debugactors <on|off>");
        Text.send(sender, "&b" + "/scene debugvisibility <actorId>");
        Text.send(sender, "&b" + "/scene actor add <scene> <actorId>");
        Text.send(sender, "&b" + "/scene actor rename <scene> <oldId> <newId>");
        Text.send(sender, "&b" + "/scene actor delete <scene> <actorId> confirm");
        Text.send(sender, "&b" + "/scene actor skin <scene> <actorId> <skin>");
        Text.send(sender, "&b" + "/scene actor playback <scene> <actorId> <exact|walk>");
        Text.send(sender, "&b" + "/scene actor scale <scene> <actorId> <value|snap>");
        Text.send(sender, "&b" + "/scene actor record start <scene> <actorId> [tick|start:20] [duration:10s|200t] [preview:on|off]");
        Text.send(sender, "&b" + "/scene actor record stop");
        Text.send(sender, "&b" + "/scene actor record delete <scene> <actorId> [confirm]");
        Text.send(sender, "&b" + "/scene selftest <name>");
    }

    private void handleMain(CommandSender sender) {
        if (sender instanceof Player player) {
            editorEngine.openMainMenu(player);
            return;
        }
        sendHelp(sender);
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scenes.edit")) {
            Text.send(sender, "&c" + "You lack permission to create scenes.");
            return;
        }
        if (args.length < 2) {
            Text.send(sender, "&c" + "Usage: /scene create <name> [duration]");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        int duration = 200;
        if (args.length >= 3) {
            try {
                duration = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                Text.send(sender, "&c" + "Invalid duration; using 200 ticks.");
            }
        }
        Scene scene = sceneManager.createScene(name, duration);
        try {
            sceneManager.saveScene(scene);
            Text.send(sender, "&a" + "Scene created: " + name);
        } catch (Exception ex) {
            Text.send(sender, "&c" + "Failed to save scene.");
        }
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scenes.edit")) {
            Text.send(sender, "&c" + "You lack permission to edit scenes.");
            return;
        }
        if (!(sender instanceof Player player)) {
            Text.send(sender, "&c" + "Only players can edit scenes.");
            return;
        }
        if (args.length < 2) {
            Text.send(sender, "&c" + "Usage: /scene edit <name>");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        Scene scene = sceneManager.loadScene(name);
        if (scene == null) {
            scene = sceneManager.createScene(name, 200);
        }
        sceneManager.cacheScene(scene);
        editorEngine.openEditor(player, scene);
    }

    private void handleGroup(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.send(sender, "&c" + "Only players can use editor commands.");
            return;
        }
        EditorSession session = editorEngine.getEditorSessionManager().getSession(player.getUniqueId());
        int groupIndex = 1;
        if (args.length >= 2 && session != null) {
            try {
                groupIndex = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                // allow scene name + group
            }
        }
        if (args.length >= 3) {
            Scene scene = sceneManager.loadScene(args[1].toLowerCase(Locale.ROOT));
            if (scene == null) {
                Text.send(sender, "&c" + "Scene not found.");
                return;
            }
            session = editorEngine.getEditorSessionManager().createSession(player.getUniqueId(), scene);
            session.clearHistory();
            try {
                groupIndex = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                Text.send(sender, "&c" + "Invalid group number.");
                return;
            }
        } else if (session == null) {
            Text.send(sender, "&c" + "No active editor session.");
            return;
        }
        session.setCurrentGroup(groupIndex);
        session.clearHistory();
        editorEngine.openGroupGrid(player, session, false);
    }

    private void handleTick(CommandSender sender, String[] args) {
        EditorSession session = requireEditorSession(sender);
        if (session == null) {
            return;
        }
        if (args.length < 2) {
            Text.send(sender, "&c" + "Usage: /scene tick <tick>");
            return;
        }
        try {
            int tick = Integer.parseInt(args[1]);
            session.setCurrentTick(tick);
            session.setCurrentGroup((tick - 1) / 9 + 1);
            session.clearHistory();
            editorEngine.openTickActionMenu((Player) sender, session, false);
        } catch (NumberFormatException ex) {
            Text.send(sender, "&c" + "Invalid tick number.");
        }
    }

    private void handleCancel(CommandSender sender) {
        EditorSession session = requireEditorSession(sender);
        if (session == null) {
            return;
        }
        editorEngine.cancelPlacement((Player) sender, session);
    }

    private void handleHere(CommandSender sender) {
        EditorSession session = requireEditorSession(sender);
        if (session == null) {
            return;
        }
        editorEngine.confirmPlacement((Player) sender, session);
    }

    private void handleSetEnd(CommandSender sender, String[] args) {
        EditorSession session = requireEditorSession(sender);
        if (session == null) {
            return;
        }
        if (!(sender instanceof Player player)) {
            Text.send(sender, "&c" + "Only players can set end locations.");
            return;
        }
        if (args.length < 2) {
            Text.send(sender, "&c" + "Usage: /scene setend <here|x y z yaw pitch>");
            return;
        }
        if (args[1].equalsIgnoreCase("here")) {
            session.getScene().setEndLocation(SceneLocation.fromLocation(player.getLocation()));
            session.getScene().setEndTeleportMode(com.extrascenes.scene.EndTeleportMode.TELEPORT_TO_END);
            editorEngine.markDirty(session.getScene());
            Text.send(sender, "&a" + "Scene end location set to your current position.");
            return;
        }
        if (args.length < 6) {
            Text.send(sender, "&c" + "Usage: /scene setend <here|x y z yaw pitch>");
            return;
        }
        try {
            double x = Double.parseDouble(args[1]);
            double y = Double.parseDouble(args[2]);
            double z = Double.parseDouble(args[3]);
            float yaw = Float.parseFloat(args[4]);
            float pitch = Float.parseFloat(args[5]);
            SceneLocation location = new SceneLocation(player.getWorld().getName(), x, y, z, yaw, pitch);
            session.getScene().setEndLocation(location);
            session.getScene().setEndTeleportMode(com.extrascenes.scene.EndTeleportMode.TELEPORT_TO_END);
            editorEngine.markDirty(session.getScene());
            Text.send(sender, "&a" + "Scene end location updated.");
        } catch (NumberFormatException ex) {
            Text.send(sender, "&c" + "Invalid coordinates.");
        }
    }

    private void handlePlay(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scenes.play")) {
            Text.send(sender, "&c" + "You lack permission to play scenes.");
            return;
        }
        if (args.length < 2) {
            Text.send(sender, "&c" + "Usage: /scene play <name> [player] [startTick] [endTick]");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        Scene scene = sceneManager.loadScene(name);
        if (scene == null) {
            Text.send(sender, "&c" + "Scene not found.");
            return;
        }
        Player target = sender instanceof Player player ? player : null;
        if (args.length >= 3) {
            Player specified = Bukkit.getPlayer(args[2]);
            if (specified != null) {
                target = specified;
            }
        }
        if (target == null) {
            Text.send(sender, "&c" + "Player not found.");
            return;
        }
        int startTick = 0;
        int endTick = scene.getDurationTicks();
        if (args.length >= 4) {
            try {
                startTick = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                Text.send(sender, "&c" + "Invalid start tick.");
                return;
            }
        }
        if (args.length >= 5) {
            try {
                endTick = Integer.parseInt(args[4]);
            } catch (NumberFormatException ex) {
                Text.send(sender, "&c" + "Invalid end tick.");
                return;
            }
        }
        sessionManager.startScene(target, scene, false, startTick, endTick);
        Text.send(sender, "&a" + "Scene playing for " + target.getName());
    }

    private void handleStop(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args);
        if (target == null) {
            Text.send(sender, "&c" + "Player not found.");
            return;
        }
        sessionManager.stopScene(target, "command_stop");
        Text.send(sender, "&a" + "Scene stopped for " + target.getName());
    }

    private void handlePause(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args);
        if (target == null) {
            Text.send(sender, "&c" + "Player not found.");
            return;
        }
        SceneSession session = sessionManager.getSession(target.getUniqueId());
        if (session != null) {
            session.setState(com.extrascenes.scene.SceneState.PAUSED);
            Text.send(sender, "&e" + "Scene paused for " + target.getName());
        }
    }

    private void handleResume(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args);
        if (target == null) {
            Text.send(sender, "&c" + "Player not found.");
            return;
        }
        SceneSession session = sessionManager.getSession(target.getUniqueId());
        if (session != null) {
            session.setState(com.extrascenes.scene.SceneState.PLAYING);
            Text.send(sender, "&e" + "Scene resumed for " + target.getName());
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sceneManager.reloadAll();
        Text.send(sender, "&a" + "Scenes reloaded.");
    }

    private void handleList(CommandSender sender) {
        List<String> scenes = sceneManager.listScenes();
        Text.send(sender, "&b" + "Scenes: " + (scenes.isEmpty() ? "(none)" : String.join(", ", scenes)));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scenes.edit")) {
            Text.send(sender, "&c" + "You lack permission to delete scenes.");
            return;
        }
        if (args.length < 2) {
            Text.send(sender, "&c" + "Usage: /scene delete <name>");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            Text.send(sender, "&e" + "Confirm deletion with: /scene delete " + name + " confirm");
            return;
        }
        boolean deleted = sceneManager.deleteScene(name);
        if (deleted) {
            editorEngine.forceCloseEditorsForScene(name);
        }
        Text.send(sender, deleted ? "&a" + "Scene deleted." : "&c" + "Scene not found.");
    }

    private void handleRename(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Text.send(sender, "&c" + "Usage: /scene rename <old> <new>");
            return;
        }
        boolean renamed = sceneManager.renameScene(args[1].toLowerCase(Locale.ROOT), args[2].toLowerCase(Locale.ROOT));
        if (renamed) {
            editorEngine.forceCloseEditorsForScene(args[1]);
            editorEngine.clearLastSelectedSceneReferences(args[1]);
        }
        Text.send(sender, renamed ? "&a" + "Scene renamed." : "&c" + "Rename failed.");
    }

    private void handleDuplicate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Text.send(sender, "&c" + "Usage: /scene duplicate <source> <target>");
            return;
        }
        boolean duplicated = sceneManager.duplicateScene(args[1].toLowerCase(Locale.ROOT), args[2].toLowerCase(Locale.ROOT));
        Text.send(sender, duplicated ? "&a" + "Scene duplicated." : "&c" + "Duplicate failed.");
    }

    private void handleDebugActors(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Text.send(sender, "&c" + "Usage: /scene debugactors <on|off>");
            return;
        }
        boolean enabled = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
        plugin.getRuntimeEngine().setActorDebugEnabled(enabled);
        Text.send(sender, "&e" + "Actor transform debug " + (enabled ? "enabled" : "disabled") + ".");
    }

    private void handleDebugCamera(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.send(sender, "&c" + "Only players can toggle camera debug.");
            return;
        }
        if (args.length < 2) {
            Text.send(sender, "&c" + "Usage: /scene debugcamera <on|off>");
            return;
        }
        boolean enabled = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
        plugin.getRuntimeEngine().setDebugCameraEnabled(player.getUniqueId(), enabled);
        Text.send(sender, "&e" + "Camera debug " + (enabled ? "enabled" : "disabled") + ".");
    }

    private void handleDebugPreview(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.send(sender, "&c" + "Only players can toggle preview debug.");
            return;
        }
        if (args.length < 2) {
            Text.send(sender, "&c" + "Usage: /scene debugpreview <on|off>");
            return;
        }
        boolean enabled = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
        plugin.getRuntimeEngine().setDebugPreviewEnabled(player.getUniqueId(), enabled);
        Text.send(sender, "&e" + "Preview debug " + (enabled ? "enabled" : "disabled") + ".");
    }

    private void handleDebugVisibility(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.send(sender, "&cOnly players can inspect visibility.");
            return;
        }
        if (args.length < 2) {
            Text.send(sender, "&cUsage: /scene debugvisibility <actorId>");
            return;
        }
        com.extrascenes.scene.SessionActorHandle handle = plugin.getRuntimeEngine().findViewerActorHandle(player, args[1]);
        if (handle == null || handle.getEntity() == null) {
            Text.send(sender, "&cNo actor handle found for viewer/session.");
            return;
        }
        java.util.UUID entityId = handle.getEntity().getUniqueId();
        Text.send(sender, "&eviewer UUID: &f" + player.getUniqueId());
        Text.send(sender, "&eactor entity UUID: &f" + entityId);
        java.util.Set<java.util.UUID> hidden = plugin.getVisibilityController().getHiddenPlayers(entityId);
        java.util.Set<java.util.UUID> shown = plugin.getVisibilityController().getShownPlayers(entityId);
        StringBuilder lines = new StringBuilder();
        for (Player online : Bukkit.getOnlinePlayers()) {
            String state = shown.contains(online.getUniqueId()) ? "shown"
                    : hidden.contains(online.getUniqueId()) ? "hidden" : "unknown";
            lines.append(online.getName()).append("(").append(online.getUniqueId()).append(")=").append(state).append(" ");
        }
        Text.send(sender, "&eplayers: &f" + lines.toString().trim());
    }

    private EditorSession requireEditorSession(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Text.send(sender, "&c" + "Only players can use editor commands.");
            return null;
        }
        EditorSession session = editorEngine.getEditorSessionManager().getSession(player.getUniqueId());
        if (session == null) {
            Text.send(sender, "&c" + "No active editor session.");
        }
        return session;
    }

    private Player resolveTarget(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            return Bukkit.getPlayer(args[1]);
        }
        if (sender instanceof Player player) {
            return player;
        }
        return null;
    }

    private void handleActor(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.send(sender, "&c" + "Only players can manage actors.");
            return;
        }
        if (args.length < 2) {
            Text.send(sender, "&c" + "Usage: /scene actor <add|rename|delete|skin|playback|scale|record> ...");
            return;
        }
        String mode = args[1].toLowerCase(Locale.ROOT);
        switch (mode) {
            case "add" -> {
                if (args.length < 4) {
                    Text.send(sender, "&c" + "Usage: /scene actor add <scene> <actorId>");
                    return;
                }
                Scene scene = sceneManager.loadScene(args[2].toLowerCase(Locale.ROOT));
                if (scene == null) {
                    Text.send(sender, "&c" + "Scene not found.");
                    return;
                }
                SceneActorTemplate template = new SceneActorTemplate(args[3]);
                template.setDisplayName(args[3]);
                scene.putActorTemplate(template);
                scene.setDirty(true);
                try {
                    sceneManager.saveScene(scene);
                    Text.send(sender, "&a" + "Actor template created: " + args[3]);
                } catch (Exception ex) {
                    Text.send(sender, "&c" + "Failed to save scene.");
                }
            }
            case "rename" -> {
                if (args.length < 5) {
                    Text.send(sender, "&c" + "Usage: /scene actor rename <scene> <oldId> <newId>");
                    return;
                }
                Scene scene = sceneManager.loadScene(args[2].toLowerCase(Locale.ROOT));
                if (scene == null) {
                    Text.send(sender, "&c" + "Scene not found.");
                    return;
                }
                SceneActorTemplate template = scene.getActorTemplate(args[3]);
                if (template == null) {
                    Text.send(sender, "&c" + "Actor not found.");
                    return;
                }
                scene.removeActorTemplate(args[3]);
                SceneActorTemplate renamed = new SceneActorTemplate(args[4]);
                renamed.setDisplayName(args[4]);
                renamed.setEntityType(template.getEntityType());
                renamed.setSkinName(template.getSkinName());
                renamed.setSkinSignature(template.getSkinSignature());
                renamed.setSkinTexture(template.getSkinTexture());
                renamed.setSkinCacheKey(template.getSkinCacheKey());
                renamed.setScale(template.getScale());
                renamed.setPlaybackMode(template.getPlaybackMode());
                renamed.setPreviewEnabled(template.isPreviewEnabled());
                renamed.getTransformTicks().putAll(template.getTransformTicks());
                renamed.getTickActions().putAll(template.getTickActions());
                scene.putActorTemplate(renamed);
                sceneManager.markDirty(scene);
                Text.send(sender, "&a" + "Actor renamed.");
            }
            case "delete" -> {
                if (args.length < 5 || !"confirm".equalsIgnoreCase(args[4])) {
                    Text.send(sender, "&e" + "Usage: /scene actor delete <scene> <actorId> confirm");
                    return;
                }
                Scene scene = sceneManager.loadScene(args[2].toLowerCase(Locale.ROOT));
                if (scene == null) {
                    Text.send(sender, "&c" + "Scene not found.");
                    return;
                }
                scene.removeActorTemplate(args[3]);
                sceneManager.markDirty(scene);
                Text.send(sender, "&a" + "Actor deleted.");
            }
            case "skin" -> {
                if (args.length < 5) {
                    Text.send(sender, "&c" + "Usage: /scene actor skin <scene> <actorId> <skin>");
                    return;
                }
                Scene scene = sceneManager.loadScene(args[2].toLowerCase(Locale.ROOT));
                if (scene == null) {
                    Text.send(sender, "&c" + "Scene not found.");
                    return;
                }
                SceneActorTemplate template = scene.getActorTemplate(args[3]);
                if (template == null) {
                    Text.send(sender, "&c" + "Actor not found.");
                    return;
                }
                template.setSkinName(args[4]);
                scene.setDirty(true);
                try {
                    sceneManager.saveScene(scene);
                    Text.send(sender, "&a" + "Actor skin updated.");
                } catch (Exception ex) {
                    Text.send(sender, "&c" + "Failed to save scene.");
                }
            }
            case "playback" -> handleActorPlayback(sender, args);
            case "scale" -> handleActorScale(sender, player, args);
            case "record" -> handleActorRecord(sender, player, args);
            default -> Text.send(sender, "&c" + "Unknown actor subcommand.");
        }
    }


    private void handleActorPlayback(CommandSender sender, String[] args) {
        if (args.length < 5) {
            Text.send(sender, "&c" + "Usage: /scene actor playback <scene> <actorId> <exact|walk>");
            return;
        }
        Scene scene = sceneManager.loadScene(args[2].toLowerCase(Locale.ROOT));
        if (scene == null) {
            Text.send(sender, "&c" + "Scene not found.");
            return;
        }
        SceneActorTemplate template = scene.getActorTemplate(args[3]);
        if (template == null) {
            Text.send(sender, "&c" + "Actor not found.");
            return;
        }
        String mode = args[4].toUpperCase(Locale.ROOT);
        try {
            template.setPlaybackMode(com.extrascenes.scene.ActorPlaybackMode.valueOf(mode));
            scene.setDirty(true);
            sceneManager.saveScene(scene);
            Text.send(sender, "&a" + "Actor playback mode updated to " + mode + ".");
        } catch (IllegalArgumentException ex) {
            Text.send(sender, "&c" + "Playback mode must be exact or walk.");
        } catch (Exception ex) {
            Text.send(sender, "&c" + "Failed to save scene.");
        }
    }

    private void handleActorScale(CommandSender sender, Player player, String[] args) {
        if (args.length < 5) {
            Text.send(sender, "&c" + "Usage: /scene actor scale <scene> <actorId> <value|snap>");
            return;
        }
        Scene scene = sceneManager.loadScene(args[2].toLowerCase(Locale.ROOT));
        if (scene == null) {
            Text.send(sender, "&c" + "Scene not found.");
            return;
        }
        SceneActorTemplate template = scene.getActorTemplate(args[3]);
        if (template == null) {
            Text.send(sender, "&c" + "Actor not found.");
            return;
        }
        if ("snap".equalsIgnoreCase(args[4])) {
            org.bukkit.attribute.Attribute attribute = com.extrascenes.ScaleAttributeResolver.resolveScaleAttribute();
            if (attribute == null || player.getAttribute(attribute) == null) {
                Text.send(sender, "&c" + "Scale attribute not available on this server build.");
                return;
            }
            template.setScale(player.getAttribute(attribute).getValue());
        } else {
            try {
                template.setScale(Double.parseDouble(args[4]));
            } catch (NumberFormatException ex) {
                Text.send(sender, "&c" + "Invalid scale value.");
                return;
            }
        }
        scene.setDirty(true);
        try {
            sceneManager.saveScene(scene);
            Text.send(sender, "&a" + "Actor scale updated to " + String.format(Locale.ROOT, "%.3f", template.getScale()) + ".");
        } catch (Exception ex) {
            Text.send(sender, "&c" + "Failed to save scene.");
        }
    }

    private void handleActorRecord(CommandSender sender, Player player, String[] args) {
        if (args.length < 3) {
            Text.send(sender, "&c" + "Usage: /scene actor record <start|stop|delete> ...");
            return;
        }
        if ("stop".equalsIgnoreCase(args[2])) {
            boolean stopped = actorRecordingService.stopRecording(player, true);
            Text.send(sender, stopped ? "&a" + "Actor recording stopped and saved."
                    : "&c" + "No active actor recording.");
            return;
        }
        if ("delete".equalsIgnoreCase(args[2])) {
            handleActorRecordDelete(sender, args);
            return;
        }
        if (!"start".equalsIgnoreCase(args[2]) || args.length < 5) {
            Text.send(sender, "&c" + "Usage: /scene actor record start <scene> <actorId> [tick|start:<tick>] [duration:10s|200t] [preview:on|off]");
            return;
        }
        Scene scene = sceneManager.loadScene(args[3].toLowerCase(Locale.ROOT));
        if (scene == null) {
            Text.send(sender, "&c" + "Scene not found.");
            return;
        }
        SceneActorTemplate template = scene.getActorTemplate(args[4]);
        if (template == null) {
            Text.send(sender, "&c" + "Actor not found.");
            return;
        }
        int startTick = 1;
        int durationTicks = 15 * 20;
        com.extrascenes.scene.RecordingDurationUnit durationUnit = com.extrascenes.scene.RecordingDurationUnit.SECONDS;
        boolean previewOthers = true;
        boolean startTickSet = false;

        for (int i = 5; i < args.length; i++) {
            String token = args[i].trim();
            if (token.isBlank()) {
                continue;
            }
            String lower = token.toLowerCase(Locale.ROOT);
            if (!startTickSet && lower.matches("\\d+")) {
                startTick = Integer.parseInt(lower);
                startTickSet = true;
                continue;
            }
            if (lower.startsWith("start:")) {
                Integer parsedStart = parsePositiveInt(lower.substring("start:".length()));
                if (parsedStart == null) {
                    Text.send(sender, "&c" + "Invalid start tick token '" + token + "'; using current value.");
                } else {
                    startTick = parsedStart;
                    startTickSet = true;
                }
                continue;
            }
            if (lower.startsWith("preview:")) {
                String raw = lower.substring("preview:".length());
                if (raw.equals("on") || raw.equals("true") || raw.equals("yes")) {
                    previewOthers = true;
                } else if (raw.equals("off") || raw.equals("false") || raw.equals("no")) {
                    previewOthers = false;
                } else {
                    Text.send(sender, "&c" + "Invalid preview token '" + token + "'; expected preview:on|off.");
                }
                continue;
            }
            String durationRaw = lower.startsWith("duration:")
                    ? lower.substring("duration:".length())
                    : lower;
            if (looksLikeDuration(durationRaw) || durationRaw.matches("\\d+")) {
                int[] parsed = parseDurationTicks(durationRaw);
                if (parsed == null) {
                    Text.send(sender, "&c" + "Invalid duration token '" + token + "'; keeping current duration.");
                } else {
                    durationTicks = parsed[0];
                    durationUnit = parsed[1] == 1
                            ? com.extrascenes.scene.RecordingDurationUnit.TICKS
                            : com.extrascenes.scene.RecordingDurationUnit.SECONDS;
                }
                continue;
            }
            Text.send(sender, "&e" + "Unknown recording option ignored: " + token);
        }
        if (scene.getDurationTicks() > 0) {
            startTick = Math.max(1, Math.min(scene.getDurationTicks(), startTick));
        } else {
            startTick = Math.max(1, startTick);
        }
        player.getInventory().addItem(com.extrascenes.scene.SceneWand.createRecordingWand());
        actorRecordingService.startRecordingWithCountdown(player, scene, template, startTick, previewOthers, durationTicks, durationUnit);
        int displayDuration = durationUnit == com.extrascenes.scene.RecordingDurationUnit.SECONDS
                ? Math.max(1, (durationTicks + 19) / 20)
                : durationTicks;
        Text.send(sender, "&a" + "Recording actor " + template.getActorId() + " from tick " + startTick
                + " for " + displayDuration + durationUnit.suffix()
                + " (preview others: " + (previewOthers ? "on" : "off") + ", countdown 3..2..1).");
    }


    private void handleActorRecordDelete(CommandSender sender, String[] args) {
        if (args.length < 5) {
            Text.send(sender, "&e" + "Usage: /scene actor record delete <scene> <actorId> [confirm]");
            return;
        }
        Scene scene = sceneManager.loadScene(args[3].toLowerCase(Locale.ROOT));
        if (scene == null) {
            Text.send(sender, "&c" + "Scene not found.");
            return;
        }
        SceneActorTemplate template = scene.getActorTemplate(args[4]);
        if (template == null) {
            Text.send(sender, "&c" + "Actor not found.");
            return;
        }
        Integer fromTick = null;
        Integer toTick = null;
        if (args.length >= 7) {
            fromTick = parsePositiveInt(args[5]);
            if (fromTick == null) {
                Text.send(sender, "&c" + "Invalid fromTick value.");
                return;
            }
        }
        if (args.length >= 8) {
            toTick = parsePositiveInt(args[6]);
            if (toTick == null) {
                Text.send(sender, "&c" + "Invalid toTick value.");
                return;
            }
        }
        if (fromTick != null && toTick != null && toTick < fromTick) {
            int swap = fromTick;
            fromTick = toTick;
            toTick = swap;
        }

        int removedTicks;
        if (fromTick == null) {
            removedTicks = template.getTransformTicks().size();
            template.getTransformTicks().clear();
        } else {
            final int start = fromTick;
            final int end = toTick == null ? fromTick : toTick;
            removedTicks = (int) template.getTransformTicks().keySet().stream()
                    .filter(tick -> tick >= start && tick <= end)
                    .count();
            template.getTransformTicks().keySet().removeIf(tick -> tick >= start && tick <= end);
        }

        if (removedTicks == 0) {
            Text.send(sender, "&e" + "No recorded transform ticks matched the requested range.");
            return;
        }
        scene.setDirty(true);
        try {
            sceneManager.saveScene(scene);
            if (fromTick == null) {
                Text.send(sender, "&a" + "Deleted " + removedTicks + " recorded tick(s) from actor " + template.getActorId() + ".");
            } else {
                int effectiveEnd = toTick == null ? fromTick : toTick;
                Text.send(sender, "&a" + "Deleted " + removedTicks + " recorded tick(s) from actor " + template.getActorId()
                        + " in range " + fromTick + "-" + effectiveEnd + ".");
            }
        } catch (Exception ex) {
            Text.send(sender, "&c" + "Failed to save scene.");
        }
    }

    private Integer parsePositiveInt(String raw) {
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean looksLikeDuration(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        char last = value.charAt(value.length() - 1);
        return last == 's' || last == 't';
    }

    private int[] parseDurationTicks(String raw) {
        try {
            if (raw.endsWith("t")) {
                return new int[]{Math.max(1, Integer.parseInt(raw.substring(0, raw.length() - 1))), 1};
            }
            if (raw.endsWith("s")) {
                return new int[]{Math.max(1, Integer.parseInt(raw.substring(0, raw.length() - 1)) * 20), 0};
            }
            return new int[]{Math.max(1, Integer.parseInt(raw) * 20), 0};
        } catch (NumberFormatException ex) {
            return null;
        }
    }


    private void handleSelfTest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.send(sender, "&c" + "Only players can run selftests.");
            return;
        }
        if (args.length < 2) {
            Text.send(sender, "&c" + "Usage: /scene selftest <name>");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        Scene scene = sceneManager.loadScene(name);
        if (scene == null) {
            Text.send(sender, "&e" + "Scene not found; selftest will use a synthetic in-memory camera path.");
        }
        selfTestRunner.run(player, name, scene);
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (List.of("edit", "play", "delete", "group", "rename", "duplicate", "selftest").contains(sub)) {
                return filterPrefix(sceneManager.listScenes(), args[1]);
            }
            if (sub.equals("tick")) {
                return filterPrefix(validTickSuggestions(sender), args[1]);
            }
            if (List.of("stop", "pause", "resume").contains(sub)) {
                return filterPrefix(onlinePlayerNames(), args[1]);
            }
            if (List.of("debugcamera", "debugpreview", "debugactors").contains(sub)) {
                return filterPrefix(List.of("on", "off"), args[1]);
            }
        }
        if ((sub.equals("rename") || sub.equals("duplicate")) && args.length == 3) {
            return List.of();
        }
        if (sub.equals("play")) {
            if (args.length == 3) {
                return filterPrefix(onlinePlayerNames(), args[2]);
            }
            if (args.length == 4) {
                return filterPrefix(List.of("0", "1", "10", "20"), args[3]);
            }
            if (args.length == 5) {
                return filterPrefix(List.of("10", "20", "40", "100"), args[4]);
            }
        }
        if (sub.equals("group")) {
            if (args.length == 3) {
                Scene scene = sceneManager.loadScene(args[1].toLowerCase(Locale.ROOT));
                if (scene != null) {
                    return filterPrefix(validGroupSuggestions(scene), args[2]);
                }
            }
        }
        if (sub.equals("actor")) {
            if (args.length == 2) {
                return filterPrefix(List.of("add", "rename", "delete", "skin", "playback", "scale", "record"), args[1]);
            }
            if (args.length == 3 && "record".equalsIgnoreCase(args[1])) {
                return filterPrefix(List.of("start", "stop", "delete"), args[2]);
            }
            if (args.length == 3 && List.of("add", "rename", "delete", "skin", "playback", "scale").contains(args[1].toLowerCase(Locale.ROOT))) {
                return filterPrefix(sceneManager.listScenes(), args[2]);
            }
            if (args.length == 4 && List.of("rename", "delete", "skin", "playback", "scale").contains(args[1].toLowerCase(Locale.ROOT))) {
                Scene scene = sceneManager.loadScene(args[2].toLowerCase(Locale.ROOT));
                if (scene != null) {
                    return filterPrefix(new ArrayList<>(scene.getActorTemplates().keySet()), args[3]);
                }
            }
            if (args.length == 5 && "delete".equalsIgnoreCase(args[1])) {
                return filterPrefix(List.of("confirm"), args[4]);
            }
            if (args.length == 5 && "playback".equalsIgnoreCase(args[1])) {
                return filterPrefix(List.of("exact", "walk"), args[4]);
            }
            if (args.length == 5 && "scale".equalsIgnoreCase(args[1])) {
                return filterPrefix(List.of("snap", "1.0", "0.5", "1.5"), args[4]);
            }
            if (args.length == 4 && "record".equalsIgnoreCase(args[1]) && List.of("start", "delete").contains(args[2].toLowerCase(Locale.ROOT))) {
                return filterPrefix(sceneManager.listScenes(), args[3]);
            }
            if (args.length == 5 && "record".equalsIgnoreCase(args[1]) && List.of("start", "delete").contains(args[2].toLowerCase(Locale.ROOT))) {
                Scene sc = sceneManager.loadScene(args[3].toLowerCase(Locale.ROOT));
                if (sc != null) {
                    return filterPrefix(new ArrayList<>(sc.getActorTemplates().keySet()), args[4]);
                }
            }
            if (args.length == 6 && "record".equalsIgnoreCase(args[1]) && "delete".equalsIgnoreCase(args[2])) {
                return filterPrefix(List.of("confirm", "1", "20", "100"), args[5]);
            }
            if (args.length == 7 && "record".equalsIgnoreCase(args[1]) && "delete".equalsIgnoreCase(args[2])) {
                return filterPrefix(List.of("confirm", "20", "100", "200"), args[6]);
            }
            if (args.length == 8 && "record".equalsIgnoreCase(args[1]) && "delete".equalsIgnoreCase(args[2])) {
                return filterPrefix(List.of("confirm"), args[7]);
            }
            if (args.length >= 6 && "record".equalsIgnoreCase(args[1]) && "start".equalsIgnoreCase(args[2])) {
                return filterPrefix(List.of("1", "20", "start:20", "duration:10s", "duration:200t", "preview:on", "preview:off"), args[args.length - 1]);
            }
        }
        return List.of();
    }

    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    private List<String> validTickSuggestions(CommandSender sender) {
        if (sender instanceof Player player) {
            EditorSession session = editorEngine.getEditorSessionManager().getSession(player.getUniqueId());
            if (session != null) {
                int duration = Math.max(1, session.getScene().getDurationTicks());
                return rangeSuggestions(1, Math.min(duration, 200));
            }
        }
        return List.of("1", "10", "20", "30");
    }

    private List<String> validGroupSuggestions(Scene scene) {
        int groups = (int) Math.ceil(Math.max(scene.getDurationTicks(), 9) / 9.0);
        return rangeSuggestions(1, groups);
    }

    private List<String> rangeSuggestions(int start, int end) {
        List<String> suggestions = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            suggestions.add(String.valueOf(i));
        }
        return suggestions;
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        String lower = Optional.ofNullable(prefix).orElse("").toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toList());
    }
}

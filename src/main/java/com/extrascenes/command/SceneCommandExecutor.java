package com.extrascenes.command;

import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.scene.ActorRecordingService;
import com.extrascenes.scene.EditorSession;
import com.extrascenes.scene.Scene;
import com.extrascenes.scene.SceneEditorEngine;
import com.extrascenes.scene.SceneLocation;
import com.extrascenes.scene.SceneManager;
import com.extrascenes.scene.SceneSession;
import com.extrascenes.scene.SceneSessionManager;
import com.extrascenes.scene.SceneActorTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class SceneCommandExecutor implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "edit", "play", "stop", "pause", "resume", "reload", "list",
            "create", "delete", "rename", "duplicate", "group", "tick", "cancel", "here", "setend", "debugcamera", "debugactors", "actor"
    );

    private final ExtraScenesPlugin plugin;
    private final SceneManager sceneManager;
    private final SceneSessionManager sessionManager;
    private final SceneEditorEngine editorEngine;
    private final ActorRecordingService actorRecordingService;

    public SceneCommandExecutor(ExtraScenesPlugin plugin, SceneManager sceneManager,
                                SceneSessionManager sessionManager, SceneEditorEngine editorEngine) {
        this.plugin = plugin;
        this.sceneManager = sceneManager;
        this.sessionManager = sessionManager;
        this.editorEngine = editorEngine;
        this.actorRecordingService = plugin.getActorRecordingService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
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
            case "debugactors" -> handleDebugActors(sender, args);
            case "actor" -> handleActor(sender, args);
            default -> sender.sendMessage(ChatColor.RED + "Unknown scene subcommand.");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "/scene edit <name>");
        sender.sendMessage(ChatColor.AQUA + "/scene play <name> [player] [startTick] [endTick]");
        sender.sendMessage(ChatColor.AQUA + "/scene stop [player]");
        sender.sendMessage(ChatColor.AQUA + "/scene pause [player]");
        sender.sendMessage(ChatColor.AQUA + "/scene resume [player]");
        sender.sendMessage(ChatColor.AQUA + "/scene reload");
        sender.sendMessage(ChatColor.AQUA + "/scene list");
        sender.sendMessage(ChatColor.AQUA + "/scene create <name> [duration]");
        sender.sendMessage(ChatColor.AQUA + "/scene delete <name>");
        sender.sendMessage(ChatColor.AQUA + "/scene rename <old> <new>");
        sender.sendMessage(ChatColor.AQUA + "/scene duplicate <source> <target>");
        sender.sendMessage(ChatColor.AQUA + "/scene group <number>");
        sender.sendMessage(ChatColor.AQUA + "/scene tick <tick>");
        sender.sendMessage(ChatColor.AQUA + "/scene cancel");
        sender.sendMessage(ChatColor.AQUA + "/scene here");
        sender.sendMessage(ChatColor.AQUA + "/scene setend <here|x y z yaw pitch>");
        sender.sendMessage(ChatColor.AQUA + "/scene debugcamera <player>");
        sender.sendMessage(ChatColor.AQUA + "/scene debugactors <on|off>");
        sender.sendMessage(ChatColor.AQUA + "/scene actor add <scene> <actorId>");
        sender.sendMessage(ChatColor.AQUA + "/scene actor rename <scene> <oldId> <newId>");
        sender.sendMessage(ChatColor.AQUA + "/scene actor delete <scene> <actorId> confirm");
        sender.sendMessage(ChatColor.AQUA + "/scene actor skin <scene> <actorId> <skin>");
        sender.sendMessage(ChatColor.AQUA + "/scene actor playback <scene> <actorId> <exact|walk>");
        sender.sendMessage(ChatColor.AQUA + "/scene actor scale <scene> <actorId> <value|snap>");
        sender.sendMessage(ChatColor.AQUA + "/scene actor record start <scene> <actorId> [startTick]");
        sender.sendMessage(ChatColor.AQUA + "/scene actor record stop");
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scenes.edit")) {
            sender.sendMessage(ChatColor.RED + "You lack permission to create scenes.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene create <name> [duration]");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        int duration = 200;
        if (args.length >= 3) {
            try {
                duration = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid duration; using 200 ticks.");
            }
        }
        Scene scene = sceneManager.createScene(name, duration);
        try {
            sceneManager.saveScene(scene);
            sender.sendMessage(ChatColor.GREEN + "Scene created: " + name);
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + "Failed to save scene.");
        }
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scenes.edit")) {
            sender.sendMessage(ChatColor.RED + "You lack permission to edit scenes.");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can edit scenes.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene edit <name>");
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
            sender.sendMessage(ChatColor.RED + "Only players can use editor commands.");
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
                sender.sendMessage(ChatColor.RED + "Scene not found.");
                return;
            }
            session = editorEngine.getEditorSessionManager().createSession(player.getUniqueId(), scene);
            session.clearHistory();
            try {
                groupIndex = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid group number.");
                return;
            }
        } else if (session == null) {
            sender.sendMessage(ChatColor.RED + "No active editor session.");
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
            sender.sendMessage(ChatColor.RED + "Usage: /scene tick <tick>");
            return;
        }
        try {
            int tick = Integer.parseInt(args[1]);
            session.setCurrentTick(tick);
            session.setCurrentGroup((tick - 1) / 9 + 1);
            session.clearHistory();
            editorEngine.openTickActionMenu((Player) sender, session, false);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid tick number.");
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
            sender.sendMessage(ChatColor.RED + "Only players can set end locations.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene setend <here|x y z yaw pitch>");
            return;
        }
        if (args[1].equalsIgnoreCase("here")) {
            session.getScene().setEndLocation(SceneLocation.fromLocation(player.getLocation()));
            session.getScene().setEndTeleportMode(com.extrascenes.scene.EndTeleportMode.TELEPORT_TO_END);
            editorEngine.markDirty(session.getScene());
            sender.sendMessage(ChatColor.GREEN + "Scene end location set to your current position.");
            return;
        }
        if (args.length < 6) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene setend <here|x y z yaw pitch>");
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
            sender.sendMessage(ChatColor.GREEN + "Scene end location updated.");
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid coordinates.");
        }
    }

    private void handlePlay(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scenes.play")) {
            sender.sendMessage(ChatColor.RED + "You lack permission to play scenes.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene play <name> [player] [startTick] [endTick]");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        Scene scene = sceneManager.loadScene(name);
        if (scene == null) {
            sender.sendMessage(ChatColor.RED + "Scene not found.");
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
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }
        int startTick = 0;
        int endTick = scene.getDurationTicks();
        if (args.length >= 4) {
            try {
                startTick = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid start tick.");
                return;
            }
        }
        if (args.length >= 5) {
            try {
                endTick = Integer.parseInt(args[4]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid end tick.");
                return;
            }
        }
        sessionManager.startScene(target, scene, false, startTick, endTick);
        sender.sendMessage(ChatColor.GREEN + "Scene playing for " + target.getName());
    }

    private void handleStop(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }
        sessionManager.stopScene(target, "command_stop");
        sender.sendMessage(ChatColor.GREEN + "Scene stopped for " + target.getName());
    }

    private void handlePause(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }
        SceneSession session = sessionManager.getSession(target.getUniqueId());
        if (session != null) {
            session.setState(com.extrascenes.scene.SceneState.PAUSED);
            sender.sendMessage(ChatColor.YELLOW + "Scene paused for " + target.getName());
        }
    }

    private void handleResume(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }
        SceneSession session = sessionManager.getSession(target.getUniqueId());
        if (session != null) {
            session.setState(com.extrascenes.scene.SceneState.PLAYING);
            sender.sendMessage(ChatColor.YELLOW + "Scene resumed for " + target.getName());
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sceneManager.reloadAll();
        sender.sendMessage(ChatColor.GREEN + "Scenes reloaded.");
    }

    private void handleList(CommandSender sender) {
        List<String> scenes = sceneManager.listScenes();
        sender.sendMessage(ChatColor.AQUA + "Scenes: " + (scenes.isEmpty() ? "(none)" : String.join(", ", scenes)));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scenes.edit")) {
            sender.sendMessage(ChatColor.RED + "You lack permission to delete scenes.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene delete <name>");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            sender.sendMessage(ChatColor.YELLOW + "Confirm deletion with: /scene delete " + name + " confirm");
            return;
        }
        boolean deleted = sceneManager.deleteScene(name);
        if (deleted) {
            editorEngine.forceCloseEditorsForScene(name);
        }
        sender.sendMessage(deleted ? ChatColor.GREEN + "Scene deleted." : ChatColor.RED + "Scene not found.");
    }

    private void handleRename(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene rename <old> <new>");
            return;
        }
        boolean renamed = sceneManager.renameScene(args[1].toLowerCase(Locale.ROOT), args[2].toLowerCase(Locale.ROOT));
        if (renamed) {
            editorEngine.forceCloseEditorsForScene(args[1]);
            editorEngine.clearLastSelectedSceneReferences(args[1]);
        }
        sender.sendMessage(renamed ? ChatColor.GREEN + "Scene renamed." : ChatColor.RED + "Rename failed.");
    }

    private void handleDuplicate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene duplicate <source> <target>");
            return;
        }
        boolean duplicated = sceneManager.duplicateScene(args[1].toLowerCase(Locale.ROOT), args[2].toLowerCase(Locale.ROOT));
        sender.sendMessage(duplicated ? ChatColor.GREEN + "Scene duplicated." : ChatColor.RED + "Duplicate failed.");
    }

    private void handleDebugActors(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene debugactors <on|off>");
            return;
        }
        boolean enabled = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
        plugin.getRuntimeEngine().setActorDebugEnabled(enabled);
        sender.sendMessage(ChatColor.YELLOW + "Actor transform debug " + (enabled ? "enabled" : "disabled") + ".");
    }

    private void handleDebugCamera(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene debugcamera <player>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }
        SceneSession session = sessionManager.getSession(target.getUniqueId());
        String gamemode = String.valueOf(target.getGameMode());
        String spectatorTarget = target.getSpectatorTarget() != null
                ? target.getSpectatorTarget().getUniqueId().toString()
                : "null";
        String rigUuid = session != null && session.getCameraRigId() != null
                ? session.getCameraRigId().toString()
                : "null";
        boolean match = session != null && session.getCameraRigId() != null
                && target.getSpectatorTarget() != null
                && session.getCameraRigId().equals(target.getSpectatorTarget().getUniqueId());
        int cooldownLeft = session != null
                ? Math.max(0, session.getSpectatorRecoveryCooldownUntilTick() - session.getTimeTicks())
                : 0;
        sender.sendMessage(ChatColor.YELLOW + "gamemode=" + gamemode
                + " spectatorTarget=" + spectatorTarget
                + " rigUuid=" + rigUuid
                + " match=" + match
                + " recoveryCooldown=" + cooldownLeft);
    }

    private EditorSession requireEditorSession(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use editor commands.");
            return null;
        }
        EditorSession session = editorEngine.getEditorSessionManager().getSession(player.getUniqueId());
        if (session == null) {
            sender.sendMessage(ChatColor.RED + "No active editor session.");
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
            sender.sendMessage(ChatColor.RED + "Only players can manage actors.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene actor <add|rename|delete|skin|playback|scale|record> ...");
            return;
        }
        String mode = args[1].toLowerCase(Locale.ROOT);
        switch (mode) {
            case "add" -> {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /scene actor add <scene> <actorId>");
                    return;
                }
                Scene scene = sceneManager.loadScene(args[2].toLowerCase(Locale.ROOT));
                if (scene == null) {
                    sender.sendMessage(ChatColor.RED + "Scene not found.");
                    return;
                }
                SceneActorTemplate template = new SceneActorTemplate(args[3]);
                template.setDisplayName(args[3]);
                scene.putActorTemplate(template);
                scene.setDirty(true);
                try {
                    sceneManager.saveScene(scene);
                    sender.sendMessage(ChatColor.GREEN + "Actor template created: " + args[3]);
                } catch (Exception ex) {
                    sender.sendMessage(ChatColor.RED + "Failed to save scene.");
                }
            }
            case "rename" -> {
                if (args.length < 5) {
                    sender.sendMessage(ChatColor.RED + "Usage: /scene actor rename <scene> <oldId> <newId>");
                    return;
                }
                Scene scene = sceneManager.loadScene(args[2].toLowerCase(Locale.ROOT));
                if (scene == null) {
                    sender.sendMessage(ChatColor.RED + "Scene not found.");
                    return;
                }
                SceneActorTemplate template = scene.getActorTemplate(args[3]);
                if (template == null) {
                    sender.sendMessage(ChatColor.RED + "Actor not found.");
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
                sender.sendMessage(ChatColor.GREEN + "Actor renamed.");
            }
            case "delete" -> {
                if (args.length < 5 || !"confirm".equalsIgnoreCase(args[4])) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /scene actor delete <scene> <actorId> confirm");
                    return;
                }
                Scene scene = sceneManager.loadScene(args[2].toLowerCase(Locale.ROOT));
                if (scene == null) {
                    sender.sendMessage(ChatColor.RED + "Scene not found.");
                    return;
                }
                scene.removeActorTemplate(args[3]);
                sceneManager.markDirty(scene);
                sender.sendMessage(ChatColor.GREEN + "Actor deleted.");
            }
            case "skin" -> {
                if (args.length < 5) {
                    sender.sendMessage(ChatColor.RED + "Usage: /scene actor skin <scene> <actorId> <skin>");
                    return;
                }
                Scene scene = sceneManager.loadScene(args[2].toLowerCase(Locale.ROOT));
                if (scene == null) {
                    sender.sendMessage(ChatColor.RED + "Scene not found.");
                    return;
                }
                SceneActorTemplate template = scene.getActorTemplate(args[3]);
                if (template == null) {
                    sender.sendMessage(ChatColor.RED + "Actor not found.");
                    return;
                }
                template.setSkinName(args[4]);
                scene.setDirty(true);
                try {
                    sceneManager.saveScene(scene);
                    sender.sendMessage(ChatColor.GREEN + "Actor skin updated.");
                } catch (Exception ex) {
                    sender.sendMessage(ChatColor.RED + "Failed to save scene.");
                }
            }
            case "playback" -> handleActorPlayback(sender, args);
            case "scale" -> handleActorScale(sender, player, args);
            case "record" -> handleActorRecord(sender, player, args);
            default -> sender.sendMessage(ChatColor.RED + "Unknown actor subcommand.");
        }
    }


    private void handleActorPlayback(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene actor playback <scene> <actorId> <exact|walk>");
            return;
        }
        Scene scene = sceneManager.loadScene(args[2].toLowerCase(Locale.ROOT));
        if (scene == null) {
            sender.sendMessage(ChatColor.RED + "Scene not found.");
            return;
        }
        SceneActorTemplate template = scene.getActorTemplate(args[3]);
        if (template == null) {
            sender.sendMessage(ChatColor.RED + "Actor not found.");
            return;
        }
        String mode = args[4].toUpperCase(Locale.ROOT);
        try {
            template.setPlaybackMode(com.extrascenes.scene.ActorPlaybackMode.valueOf(mode));
            scene.setDirty(true);
            sceneManager.saveScene(scene);
            sender.sendMessage(ChatColor.GREEN + "Actor playback mode updated to " + mode + ".");
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + "Playback mode must be exact or walk.");
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + "Failed to save scene.");
        }
    }

    private void handleActorScale(CommandSender sender, Player player, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene actor scale <scene> <actorId> <value|snap>");
            return;
        }
        Scene scene = sceneManager.loadScene(args[2].toLowerCase(Locale.ROOT));
        if (scene == null) {
            sender.sendMessage(ChatColor.RED + "Scene not found.");
            return;
        }
        SceneActorTemplate template = scene.getActorTemplate(args[3]);
        if (template == null) {
            sender.sendMessage(ChatColor.RED + "Actor not found.");
            return;
        }
        if ("snap".equalsIgnoreCase(args[4])) {
            org.bukkit.attribute.Attribute attribute = com.extrascenes.ScaleAttributeResolver.resolveScaleAttribute();
            if (attribute == null || player.getAttribute(attribute) == null) {
                sender.sendMessage(ChatColor.RED + "Scale attribute not available on this server build.");
                return;
            }
            template.setScale(player.getAttribute(attribute).getValue());
        } else {
            try {
                template.setScale(Double.parseDouble(args[4]));
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid scale value.");
                return;
            }
        }
        scene.setDirty(true);
        try {
            sceneManager.saveScene(scene);
            sender.sendMessage(ChatColor.GREEN + "Actor scale updated to " + String.format(Locale.ROOT, "%.3f", template.getScale()) + ".");
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + "Failed to save scene.");
        }
    }

    private void handleActorRecord(CommandSender sender, Player player, String[] args) {
        if (args.length >= 3 && "stop".equalsIgnoreCase(args[2])) {
            boolean stopped = actorRecordingService.stopRecording(player, true);
            sender.sendMessage(stopped ? ChatColor.GREEN + "Actor recording stopped and saved."
                    : ChatColor.RED + "No active actor recording.");
            return;
        }
        if (args.length < 5 || !"start".equalsIgnoreCase(args[2])) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene actor record start <scene> <actorId> [tick]");
            return;
        }
        Scene scene = sceneManager.loadScene(args[3].toLowerCase(Locale.ROOT));
        if (scene == null) {
            sender.sendMessage(ChatColor.RED + "Scene not found.");
            return;
        }
        SceneActorTemplate template = scene.getActorTemplate(args[4]);
        if (template == null) {
            sender.sendMessage(ChatColor.RED + "Actor not found.");
            return;
        }
        int startTick = 0;
        if (args.length >= 6) {
            try {
                startTick = Integer.parseInt(args[5]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid tick; using 0.");
            }
        }
        actorRecordingService.startRecording(player, scene, template, startTick);
        sender.sendMessage(ChatColor.GREEN + "Recording actor " + template.getActorId() + " from tick " + startTick + ".");
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (List.of("edit", "play", "delete", "group", "rename", "duplicate").contains(sub)) {
                return filterPrefix(sceneManager.listScenes(), args[1]);
            }
            if (sub.equals("tick")) {
                return filterPrefix(validTickSuggestions(sender), args[1]);
            }
            if (List.of("stop", "pause", "resume", "debugcamera").contains(sub)) {
                return filterPrefix(onlinePlayerNames(), args[1]);
            }
            if (sub.equals("debugactors")) {
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
                return filterPrefix(List.of("start", "stop"), args[2]);
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
            if (args.length == 4 && "record".equalsIgnoreCase(args[1]) && "start".equalsIgnoreCase(args[2])) {
                return filterPrefix(sceneManager.listScenes(), args[3]);
            }
            if (args.length == 5 && "record".equalsIgnoreCase(args[1]) && "start".equalsIgnoreCase(args[2])) {
                Scene sc = sceneManager.loadScene(args[3].toLowerCase(Locale.ROOT));
                if (sc != null) {
                    return filterPrefix(new ArrayList<>(sc.getActorTemplates().keySet()), args[4]);
                }
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

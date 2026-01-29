package com.extrascenes.command;

import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.scene.EditorSession;
import com.extrascenes.scene.Scene;
import com.extrascenes.scene.SceneEditorEngine;
import com.extrascenes.scene.SceneLocation;
import com.extrascenes.scene.SceneManager;
import com.extrascenes.scene.SceneSession;
import com.extrascenes.scene.SceneSessionManager;
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
            "create", "delete", "group", "tick", "cancel", "here", "setend"
    );

    private final ExtraScenesPlugin plugin;
    private final SceneManager sceneManager;
    private final SceneSessionManager sessionManager;
    private final SceneEditorEngine editorEngine;

    public SceneCommandExecutor(ExtraScenesPlugin plugin, SceneManager sceneManager,
                                SceneSessionManager sessionManager, SceneEditorEngine editorEngine) {
        this.plugin = plugin;
        this.sceneManager = sceneManager;
        this.sessionManager = sessionManager;
        this.editorEngine = editorEngine;
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
        sender.sendMessage(ChatColor.AQUA + "/scene group <number>");
        sender.sendMessage(ChatColor.AQUA + "/scene tick <tick>");
        sender.sendMessage(ChatColor.AQUA + "/scene cancel");
        sender.sendMessage(ChatColor.AQUA + "/scene here");
        sender.sendMessage(ChatColor.AQUA + "/scene setend <here|x y z yaw pitch>");
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
        sender.sendMessage(deleted ? ChatColor.GREEN + "Scene deleted." : ChatColor.RED + "Scene not found.");
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (List.of("edit", "play", "delete", "group").contains(sub)) {
                return filterPrefix(sceneManager.listScenes(), args[1]);
            }
            if (sub.equals("tick")) {
                return filterPrefix(validTickSuggestions(sender), args[1]);
            }
            if (List.of("stop", "pause", "resume").contains(sub)) {
                return filterPrefix(onlinePlayerNames(), args[1]);
            }
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

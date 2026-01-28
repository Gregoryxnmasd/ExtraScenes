package com.extrascenes.command;

import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.scene.Scene;
import com.extrascenes.scene.SceneEditorEngine;
import com.extrascenes.scene.SceneManager;
import com.extrascenes.scene.SceneRuntimeEngine;
import com.extrascenes.scene.SceneSessionManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class SceneCommandExecutor implements CommandExecutor, TabCompleter {
    private final ExtraScenesPlugin plugin;
    private final SceneManager sceneManager;
    private final SceneSessionManager sessionManager;
    private final SceneRuntimeEngine runtimeEngine;
    private final SceneEditorEngine editorEngine;

    public SceneCommandExecutor(ExtraScenesPlugin plugin, SceneManager sceneManager,
                                SceneSessionManager sessionManager, SceneRuntimeEngine runtimeEngine,
                                SceneEditorEngine editorEngine) {
        this.plugin = plugin;
        this.sceneManager = sceneManager;
        this.sessionManager = sessionManager;
        this.runtimeEngine = runtimeEngine;
        this.editorEngine = editorEngine;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.AQUA + "/scene create <name>");
            sender.sendMessage(ChatColor.AQUA + "/scene edit <name>");
            sender.sendMessage(ChatColor.AQUA + "/scene play <name> [player]");
            sender.sendMessage(ChatColor.AQUA + "/scene stop [player]");
            sender.sendMessage(ChatColor.AQUA + "/scene pause [player]");
            sender.sendMessage(ChatColor.AQUA + "/scene resume [player]");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> handleCreate(sender, args);
            case "edit" -> handleEdit(sender, args);
            case "play" -> handlePlay(sender, args);
            case "stop" -> handleStop(sender, args);
            case "pause" -> handlePause(sender, args);
            case "resume" -> handleResume(sender, args);
            default -> sender.sendMessage(ChatColor.RED + "Unknown scene subcommand.");
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scenes.edit")) {
            sender.sendMessage(ChatColor.RED + "You lack permission to create scenes.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene create <name>");
            return;
        }
        String name = args[1].toLowerCase();
        Scene scene = sceneManager.createScene(name, 200);
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
        String name = args[1].toLowerCase();
        Scene scene = sceneManager.loadScene(name);
        if (scene == null) {
            scene = sceneManager.createScene(name, 200);
        }
        editorEngine.openEditor(player, scene);
    }

    private void handlePlay(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scenes.play")) {
            sender.sendMessage(ChatColor.RED + "You lack permission to play scenes.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene play <name> [player]");
            return;
        }
        Scene scene = sceneManager.loadScene(args[1].toLowerCase());
        if (scene == null) {
            sender.sendMessage(ChatColor.RED + "Scene not found.");
            return;
        }
        Player target = sender instanceof Player player ? player : null;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
        }
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Target player not found.");
            return;
        }
        sessionManager.startScene(target, scene, false);
        sender.sendMessage(ChatColor.GREEN + "Scene playing for " + target.getName());
    }

    private void handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scenes.stop")) {
            sender.sendMessage(ChatColor.RED + "You lack permission to stop scenes.");
            return;
        }
        Player target = sender instanceof Player player ? player : null;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
        }
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Target player not found.");
            return;
        }
        sessionManager.stopScene(target, "command_stop");
        sender.sendMessage(ChatColor.GREEN + "Scene stopped for " + target.getName());
    }

    private void handlePause(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scenes.stop")) {
            sender.sendMessage(ChatColor.RED + "You lack permission to pause scenes.");
            return;
        }
        Player target = sender instanceof Player player ? player : null;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
        }
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Target player not found.");
            return;
        }
        if (sessionManager.getSession(target.getUniqueId()) != null) {
            sessionManager.getSession(target.getUniqueId()).setState(com.extrascenes.scene.SceneState.PAUSED);
            sender.sendMessage(ChatColor.YELLOW + "Scene paused for " + target.getName());
        }
    }

    private void handleResume(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scenes.stop")) {
            sender.sendMessage(ChatColor.RED + "You lack permission to resume scenes.");
            return;
        }
        Player target = sender instanceof Player player ? player : null;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
        }
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Target player not found.");
            return;
        }
        if (sessionManager.getSession(target.getUniqueId()) != null) {
            sessionManager.getSession(target.getUniqueId()).setState(com.extrascenes.scene.SceneState.PLAYING);
            sender.sendMessage(ChatColor.YELLOW + "Scene resumed for " + target.getName());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("create", "edit", "play", "stop", "pause", "resume");
        }
        if (args.length == 2 && ("play".equalsIgnoreCase(args[0]) || "edit".equalsIgnoreCase(args[0]))) {
            return sceneManager.listScenes();
        }
        if (args.length == 2 && ("stop".equalsIgnoreCase(args[0])
                || "pause".equalsIgnoreCase(args[0]) || "resume".equalsIgnoreCase(args[0]))) {
            return new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        return List.of();
    }
}

package com.extrascenes.command;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.extrascenes.core.CameraPoint;
import com.extrascenes.core.Scene;
import com.extrascenes.gui.MainMenuGui;
import com.extrascenes.runtime.CutsceneRuntime;
import com.extrascenes.storage.SceneStorage;

public class SceneCommand implements CommandExecutor {
    private final Map<String, Scene> scenes;
    private final SceneStorage storage;
    private final CutsceneRuntime runtime;

    public SceneCommand(Map<String, Scene> scenes, SceneStorage storage, CutsceneRuntime runtime) {
        this.scenes = scenes;
        this.storage = storage;
        this.runtime = runtime;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only");
            return true;
        }
        if (args.length == 0) {
            new MainMenuGui(scenes).open(player);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (args.length < 3) return false;
                String name = args[1].toLowerCase(Locale.ROOT);
                int duration = Integer.parseInt(args[2]);
                Scene scene = new Scene(name, duration);
                scene.getCameraPoints().put(0, new CameraPoint(player.getWorld().getName(), player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch()));
                scenes.put(name, scene);
                storage.save(scenes);
                player.sendMessage("Created scene " + name);
            }
            case "edit" -> {
                if (args.length < 2) return false;
                player.sendMessage("Editor available via /scene menu for " + args[1]);
            }
            case "play" -> {
                if (args.length < 2) return false;
                Scene scene = scenes.get(args[1].toLowerCase(Locale.ROOT));
                if (scene == null) {
                    player.sendMessage("Scene not found");
                    return true;
                }
                runtime.start(player, scene);
                player.sendMessage("Playing " + scene.getName());
            }
            case "stop" -> {
                runtime.stop(player, "manual stop");
                player.sendMessage("Stopped");
            }
            case "delete" -> {
                if (args.length < 2) return false;
                scenes.remove(args[1].toLowerCase(Locale.ROOT));
                storage.save(scenes);
                player.sendMessage("Deleted " + args[1]);
            }
            default -> player.sendMessage("Unknown subcommand. " + Arrays.toString(new String[]{"create", "edit", "play", "stop", "delete"}));
        }
        return true;
    }
}

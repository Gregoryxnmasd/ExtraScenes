package com.extrascenes.command;

import com.extrascenes.ExtraScenesPlugin;
import com.extrascenes.scene.BlockIllusionKeyframe;
import com.extrascenes.scene.CameraKeyframe;
import com.extrascenes.scene.CommandKeyframe;
import com.extrascenes.scene.EditorSession;
import com.extrascenes.scene.ModelKeyframe;
import com.extrascenes.scene.ParticleKeyframe;
import com.extrascenes.scene.Scene;
import com.extrascenes.scene.SceneEditorEngine;
import com.extrascenes.scene.SceneManager;
import com.extrascenes.scene.SceneRuntimeEngine;
import com.extrascenes.scene.SceneSession;
import com.extrascenes.scene.SceneSessionManager;
import com.extrascenes.scene.SceneTrackType;
import com.extrascenes.scene.SoundKeyframe;
import com.extrascenes.scene.Track;
import com.extrascenes.scene.Transform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> handleCreate(sender, args);
            case "edit" -> handleEdit(sender, args);
            case "group" -> handleGroup(sender, args);
            case "tick" -> handleTick(sender, args);
            case "here" -> handleHere(sender);
            case "arm" -> handleArm(sender, args);
            case "cancel" -> handleCancel(sender);
            case "setduration" -> handleSetDuration(sender, args);
            case "extend" -> handleExtend(sender, args);
            case "trim" -> handleTrim(sender, args);
            case "clear" -> handleClear(sender, args);
            case "copytick" -> handleCopyTick(sender, args);
            case "shift" -> handleShift(sender, args);
            case "play" -> handlePlay(sender, args);
            case "stop" -> handleStop(sender, args);
            case "pause" -> handlePause(sender, args);
            case "resume" -> handleResume(sender, args);
            case "preview" -> handlePreview(sender, args);
            case "export" -> handleExport(sender, args);
            case "import" -> handleImport(sender, args);
            default -> sender.sendMessage(ChatColor.RED + "Unknown scene subcommand.");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "/scene create <name>");
        sender.sendMessage(ChatColor.AQUA + "/scene edit <name>");
        sender.sendMessage(ChatColor.AQUA + "/scene group <n>");
        sender.sendMessage(ChatColor.AQUA + "/scene tick <tick>");
        sender.sendMessage(ChatColor.AQUA + "/scene arm <tick>");
        sender.sendMessage(ChatColor.AQUA + "/scene here");
        sender.sendMessage(ChatColor.AQUA + "/scene cancel");
        sender.sendMessage(ChatColor.AQUA + "/scene setduration <ticks>");
        sender.sendMessage(ChatColor.AQUA + "/scene extend <ticks>");
        sender.sendMessage(ChatColor.AQUA + "/scene trim <ticks>");
        sender.sendMessage(ChatColor.AQUA + "/scene clear <tick|group|all> confirm");
        sender.sendMessage(ChatColor.AQUA + "/scene copytick <from> <to>");
        sender.sendMessage(ChatColor.AQUA + "/scene shift <fromTick> <toTick> <delta>");
        sender.sendMessage(ChatColor.AQUA + "/scene play <name> [player] [startTick] [endTick]");
        sender.sendMessage(ChatColor.AQUA + "/scene stop [player]");
        sender.sendMessage(ChatColor.AQUA + "/scene pause [player]");
        sender.sendMessage(ChatColor.AQUA + "/scene resume [player]");
        sender.sendMessage(ChatColor.AQUA + "/scene preview <name>");
        sender.sendMessage(ChatColor.AQUA + "/scene export <name>");
        sender.sendMessage(ChatColor.AQUA + "/scene import <name>");
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

    private void handleGroup(CommandSender sender, String[] args) {
        EditorSession session = requireEditorSession(sender);
        if (session == null) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene group <n>");
            return;
        }
        try {
            int group = Integer.parseInt(args[1]);
            session.setCurrentGroup(group);
            editorEngine.openGroupGrid((Player) sender, session, true);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid group number.");
        }
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
            editorEngine.openTickActionMenu((Player) sender, session, true);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid tick number.");
        }
    }

    private void handleArm(CommandSender sender, String[] args) {
        EditorSession session = requireEditorSession(sender);
        if (session == null) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene arm <tick>");
            return;
        }
        try {
            int tick = Integer.parseInt(args[1]);
            session.setCurrentTick(tick);
            session.setCurrentGroup((tick - 1) / 9 + 1);
            editorEngine.armCameraPlacement((Player) sender, session, tick);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid tick number.");
        }
    }

    private void handleHere(CommandSender sender) {
        EditorSession session = requireEditorSession(sender);
        if (session == null) {
            return;
        }
        editorEngine.confirmCameraPlacement((Player) sender, session);
    }

    private void handleCancel(CommandSender sender) {
        EditorSession session = requireEditorSession(sender);
        if (session == null) {
            return;
        }
        editorEngine.cancelCameraPlacement((Player) sender, session);
    }

    private void handleSetDuration(CommandSender sender, String[] args) {
        EditorSession session = requireEditorSession(sender);
        if (session == null) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene setduration <ticks>");
            return;
        }
        try {
            int ticks = Integer.parseInt(args[1]);
            session.getScene().setDurationTicks(Math.max(0, ticks));
            sender.sendMessage(ChatColor.GREEN + "Scene duration set to " + ticks + " ticks.");
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid tick count.");
        }
    }

    private void handleExtend(CommandSender sender, String[] args) {
        EditorSession session = requireEditorSession(sender);
        if (session == null) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene extend <ticks>");
            return;
        }
        try {
            int ticks = Integer.parseInt(args[1]);
            session.getScene().setDurationTicks(session.getScene().getDurationTicks() + ticks);
            sender.sendMessage(ChatColor.GREEN + "Scene duration extended by " + ticks + " ticks.");
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid tick count.");
        }
    }

    private void handleTrim(CommandSender sender, String[] args) {
        EditorSession session = requireEditorSession(sender);
        if (session == null) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene trim <ticks>");
            return;
        }
        try {
            int ticks = Integer.parseInt(args[1]);
            session.getScene().setDurationTicks(Math.max(0, session.getScene().getDurationTicks() - ticks));
            sender.sendMessage(ChatColor.GREEN + "Scene duration trimmed by " + ticks + " ticks.");
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid tick count.");
        }
    }

    private void handleClear(CommandSender sender, String[] args) {
        EditorSession session = requireEditorSession(sender);
        if (session == null) {
            return;
        }
        if (args.length < 3 || !"confirm".equalsIgnoreCase(args[2])) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene clear <tick|group|all> confirm");
            return;
        }
        String target = args[1].toLowerCase();
        if ("all".equals(target)) {
            clearAll(session.getScene());
            sender.sendMessage(ChatColor.GREEN + "Cleared all ticks.");
            return;
        }
        if ("group".equals(target)) {
            clearGroup(session.getScene(), session.getCurrentGroup());
            sender.sendMessage(ChatColor.GREEN + "Cleared group " + session.getCurrentGroup() + ".");
            return;
        }
        try {
            int tick = Integer.parseInt(target);
            clearTick(session.getScene(), tick);
            sender.sendMessage(ChatColor.GREEN + "Cleared tick " + tick + ".");
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid clear target. Use tick number, group, or all.");
        }
    }

    private void handleCopyTick(CommandSender sender, String[] args) {
        EditorSession session = requireEditorSession(sender);
        if (session == null) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene copytick <from> <to>");
            return;
        }
        try {
            int from = Integer.parseInt(args[1]);
            int to = Integer.parseInt(args[2]);
            copyTick(session.getScene(), from, to);
            sender.sendMessage(ChatColor.GREEN + "Copied tick " + from + " to " + to + ".");
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid tick number.");
        }
    }

    private void handleShift(CommandSender sender, String[] args) {
        EditorSession session = requireEditorSession(sender);
        if (session == null) {
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene shift <fromTick> <toTick> <delta>");
            return;
        }
        try {
            int from = Integer.parseInt(args[1]);
            int to = Integer.parseInt(args[2]);
            int delta = Integer.parseInt(args[3]);
            shiftRange(session.getScene(), from, to, delta);
            sender.sendMessage(ChatColor.GREEN + "Shifted ticks " + from + " - " + to + " by " + delta + ".");
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid tick number.");
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
        Scene scene = sceneManager.loadScene(args[1].toLowerCase());
        if (scene == null) {
            sender.sendMessage(ChatColor.RED + "Scene not found.");
            return;
        }
        Player target = sender instanceof Player player ? player : null;
        int argIndex = 2;
        int startTick = 0;
        int endTick = scene.getDurationTicks();
        if (args.length >= 3) {
            Player named = Bukkit.getPlayer(args[2]);
            if (named != null) {
                target = named;
                argIndex = 3;
            }
        }
        if (args.length > argIndex) {
            try {
                startTick = Integer.parseInt(args[argIndex]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid start tick.");
                return;
            }
            if (args.length > argIndex + 1) {
                try {
                    endTick = Integer.parseInt(args[argIndex + 1]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "Invalid end tick.");
                    return;
                }
            }
        }
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Target player not found.");
            return;
        }
        sessionManager.startScene(target, scene, false, startTick, endTick);
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
        SceneSession session = sessionManager.getSession(target.getUniqueId());
        if (session != null) {
            session.setState(com.extrascenes.scene.SceneState.PAUSED);
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
        SceneSession session = sessionManager.getSession(target.getUniqueId());
        if (session != null) {
            session.setState(com.extrascenes.scene.SceneState.PLAYING);
            sender.sendMessage(ChatColor.YELLOW + "Scene resumed for " + target.getName());
        }
    }

    private void handlePreview(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can preview scenes.");
            return;
        }
        if (!sender.hasPermission("scenes.play")) {
            sender.sendMessage(ChatColor.RED + "You lack permission to preview scenes.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene preview <name>");
            return;
        }
        Scene scene = sceneManager.loadScene(args[1].toLowerCase());
        if (scene == null) {
            sender.sendMessage(ChatColor.RED + "Scene not found.");
            return;
        }
        sessionManager.startScene(player, scene, true, 0, scene.getDurationTicks());
        sender.sendMessage(ChatColor.GREEN + "Preview started for " + scene.getName());
    }

    private void handleExport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scenes.export")) {
            sender.sendMessage(ChatColor.RED + "You lack permission to export scenes.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene export <name>");
            return;
        }
        try {
            boolean success = sceneManager.exportScene(args[1].toLowerCase());
            sender.sendMessage(success
                    ? ChatColor.GREEN + "Scene exported."
                    : ChatColor.RED + "Scene not found.");
        } catch (IOException ex) {
            sender.sendMessage(ChatColor.RED + "Failed to export scene.");
        }
    }

    private void handleImport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scenes.export")) {
            sender.sendMessage(ChatColor.RED + "You lack permission to import scenes.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /scene import <name>");
            return;
        }
        try {
            boolean success = sceneManager.importScene(args[1].toLowerCase());
            sender.sendMessage(success
                    ? ChatColor.GREEN + "Scene imported."
                    : ChatColor.RED + "Export file not found.");
        } catch (IOException ex) {
            sender.sendMessage(ChatColor.RED + "Failed to import scene.");
        }
    }

    private EditorSession requireEditorSession(CommandSender sender) {
        if (!sender.hasPermission("scenes.edit")) {
            sender.sendMessage(ChatColor.RED + "You lack permission to edit scenes.");
            return null;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return null;
        }
        EditorSession session = editorEngine.getEditorSessionManager().getSession(player.getUniqueId());
        if (session == null) {
            sender.sendMessage(ChatColor.RED + "You are not editing a scene. Use /scene edit <name>.");
            return null;
        }
        return session;
    }

    private void clearTick(Scene scene, int tick) {
        for (SceneTrackType type : SceneTrackType.values()) {
            Track<? extends com.extrascenes.scene.Keyframe> track = scene.getTrack(type);
            if (track == null) {
                continue;
            }
            for (com.extrascenes.scene.Keyframe keyframe : List.copyOf(track.getKeyframes())) {
                if (keyframe.getTimeTicks() == tick) {
                    track.removeKeyframe(keyframe.getId());
                }
            }
        }
    }

    private void clearGroup(Scene scene, int group) {
        int startTick = (group - 1) * 9 + 1;
        int endTick = startTick + 8;
        for (int tick = startTick; tick <= endTick; tick++) {
            clearTick(scene, tick);
        }
    }

    private void clearAll(Scene scene) {
        for (SceneTrackType type : SceneTrackType.values()) {
            Track<? extends com.extrascenes.scene.Keyframe> track = scene.getTrack(type);
            if (track != null) {
                track.clear();
            }
        }
    }

    private void copyTick(Scene scene, int from, int to) {
        clearTick(scene, to);
        Track<CameraKeyframe> cameraTrack = scene.getTrack(SceneTrackType.CAMERA);
        CameraKeyframe camera = cameraTrack == null ? null : findCamera(cameraTrack, from);
        if (camera != null) {
            Transform cameraTransform = camera.getTransform() == null
                    ? null
                    : new Transform(camera.getTransform().getX(), camera.getTransform().getY(), camera.getTransform().getZ(),
                            camera.getTransform().getYaw(), camera.getTransform().getPitch());
            com.extrascenes.scene.LookAtTarget lookAt = camera.getLookAt();
            com.extrascenes.scene.LookAtTarget lookAtCopy = lookAt == null
                    ? com.extrascenes.scene.LookAtTarget.none()
                    : new com.extrascenes.scene.LookAtTarget(lookAt.getMode(), lookAt.getPosition(), lookAt.getEntityId());
            CameraKeyframe copy = new CameraKeyframe(null, to,
                    cameraTransform, camera.getSmoothingMode(), camera.isInstant(), lookAtCopy);
            cameraTrack.addKeyframe(copy);
        }

        Track<CommandKeyframe> commandTrack = scene.getTrack(SceneTrackType.COMMAND);
        CommandKeyframe command = commandTrack == null ? null : findCommand(commandTrack, from);
        if (command != null) {
            CommandKeyframe copy = new CommandKeyframe(null, to, command.getCommands(), command.getExecutorMode(), command.isAllowGlobal());
            commandTrack.addKeyframe(copy);
        }

        Track<ModelKeyframe> modelTrack = scene.getTrack(SceneTrackType.MODEL);
        if (modelTrack != null) {
            for (ModelKeyframe model : modelTrack.getKeyframes()) {
                if (model.getTimeTicks() != from) {
                    continue;
                }
                ModelKeyframe copy = new ModelKeyframe(null, to, model.getAction());
                copy.setModelId(model.getModelId());
                copy.setEntityRef(model.getEntityRef());
                copy.setAnimationId(model.getAnimationId());
                copy.setLoop(model.isLoop());
                copy.setSpeed(model.getSpeed());
                if (model.getSpawnTransform() != null) {
                    Transform spawn = model.getSpawnTransform();
                    copy.setSpawnTransform(new Transform(spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getYaw(), spawn.getPitch()));
                }
                modelTrack.addKeyframe(copy);
            }
        }

        Track<ParticleKeyframe> particleTrack = scene.getTrack(SceneTrackType.PARTICLE);
        if (particleTrack != null) {
            for (ParticleKeyframe particle : particleTrack.getKeyframes()) {
                if (particle.getTimeTicks() == from) {
                    Transform transform = particle.getTransform();
                    Transform copyTransform = transform == null ? null
                            : new Transform(transform.getX(), transform.getY(), transform.getZ(), transform.getYaw(), transform.getPitch());
                    particleTrack.addKeyframe(new ParticleKeyframe(null, to, particle.getParticleId(), copyTransform));
                }
            }
        }

        Track<SoundKeyframe> soundTrack = scene.getTrack(SceneTrackType.SOUND);
        if (soundTrack != null) {
            for (SoundKeyframe sound : soundTrack.getKeyframes()) {
                if (sound.getTimeTicks() == from) {
                    Transform transform = sound.getTransform();
                    Transform copyTransform = transform == null ? null
                            : new Transform(transform.getX(), transform.getY(), transform.getZ(), transform.getYaw(), transform.getPitch());
                    soundTrack.addKeyframe(new SoundKeyframe(null, to, sound.getSoundId(), sound.getVolume(),
                            sound.getPitch(), copyTransform));
                }
            }
        }

        Track<BlockIllusionKeyframe> blockTrack = scene.getTrack(SceneTrackType.BLOCK_ILLUSION);
        if (blockTrack != null) {
            for (BlockIllusionKeyframe block : blockTrack.getKeyframes()) {
                if (block.getTimeTicks() == from) {
                    Transform transform = block.getTransform();
                    Transform copyTransform = transform == null ? null
                            : new Transform(transform.getX(), transform.getY(), transform.getZ(), transform.getYaw(), transform.getPitch());
                    blockTrack.addKeyframe(new BlockIllusionKeyframe(null, to, block.getMaterial(), copyTransform));
                }
            }
        }
    }

    private CameraKeyframe findCamera(Track<CameraKeyframe> track, int tick) {
        for (CameraKeyframe keyframe : track.getKeyframes()) {
            if (keyframe.getTimeTicks() == tick) {
                return keyframe;
            }
        }
        return null;
    }

    private CommandKeyframe findCommand(Track<CommandKeyframe> track, int tick) {
        for (CommandKeyframe keyframe : track.getKeyframes()) {
            if (keyframe.getTimeTicks() == tick) {
                return keyframe;
            }
        }
        return null;
    }

    private void shiftRange(Scene scene, int from, int to, int delta) {
        for (SceneTrackType type : SceneTrackType.values()) {
            Track<? extends com.extrascenes.scene.Keyframe> track = scene.getTrack(type);
            if (track == null) {
                continue;
            }
            List<UUID> ids = new ArrayList<>();
            for (com.extrascenes.scene.Keyframe keyframe : track.getKeyframes()) {
                if (keyframe.getTimeTicks() >= from && keyframe.getTimeTicks() <= to) {
                    ids.add(keyframe.getId());
                }
            }
            for (UUID id : ids) {
                com.extrascenes.scene.Keyframe keyframe = track.getKeyframe(id);
                if (keyframe != null) {
                    track.moveKeyframe(id, keyframe.getTimeTicks() + delta);
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("create", "edit", "group", "tick", "arm", "here", "cancel", "setduration", "extend",
                    "trim", "clear", "copytick", "shift", "play", "stop", "pause", "resume", "preview", "export", "import");
        }
        if (args.length == 2 && ("play".equalsIgnoreCase(args[0]) || "edit".equalsIgnoreCase(args[0])
                || "preview".equalsIgnoreCase(args[0]) || "export".equalsIgnoreCase(args[0])
                || "import".equalsIgnoreCase(args[0]))) {
            return sceneManager.listScenes();
        }
        if (args.length == 2 && ("stop".equalsIgnoreCase(args[0])
                || "pause".equalsIgnoreCase(args[0]) || "resume".equalsIgnoreCase(args[0]))) {
            return new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        return List.of();
    }
}

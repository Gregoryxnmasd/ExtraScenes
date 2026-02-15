package com.extrascenes.scene;

import com.extrascenes.Text;

import com.extrascenes.ExtraScenesPlugin;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class SceneEditorEngine {
    private final ExtraScenesPlugin plugin;
    private final SceneManager sceneManager;
    private final EditorSessionManager editorSessionManager;
    private final EditorInputManager inputManager;
    private final Map<GuiType, EditorGui> guis = new EnumMap<>(GuiType.class);
    private final Map<UUID, String> lastSelectedSceneByPlayer = new java.util.HashMap<>();
    private final Map<UUID, MainMenuState> mainMenuStates = new java.util.HashMap<>();
    private final Map<UUID, MainMenuPrompt> mainMenuPrompts = new java.util.HashMap<>();
    private final Map<UUID, String> pendingDeleteSceneByPlayer = new java.util.HashMap<>();

    public SceneEditorEngine(ExtraScenesPlugin plugin, SceneManager sceneManager, EditorSessionManager editorSessionManager) {
        this.plugin = plugin;
        this.sceneManager = sceneManager;
        this.editorSessionManager = editorSessionManager;
        this.inputManager = new EditorInputManager(plugin, sceneManager, this);
        registerGuis();
    }

    private void registerGuis() {
        guis.put(GuiType.SCENE_DASHBOARD, new SceneDashboardGui(this));
        guis.put(GuiType.GROUP_SELECT, new GroupSelectGui(this));
        guis.put(GuiType.GROUP_GRID, new GroupGridGui(this));
        guis.put(GuiType.TICK_ACTION, new TickActionMenuGui(this));
        guis.put(GuiType.TICK_TOOLS, new TickToolsGui(this));
        guis.put(GuiType.TRACK_SELECT, new TrackSelectGui(this));
        guis.put(GuiType.KEYFRAME_LIST, new KeyframeListGui(this));
        guis.put(GuiType.ADD_KEYFRAME, new AddKeyframeGui(this));
        guis.put(GuiType.CAMERA_EDITOR, new CameraKeyframeEditorGui(this));
        guis.put(GuiType.CAMERA_OPTIONS, new CameraOptionsGui(this));
        guis.put(GuiType.COMMAND_EDITOR, new CommandKeyframeEditorGui(this));
        guis.put(GuiType.MODEL_EDITOR, new ModelKeyframeEditorGui(this));
        guis.put(GuiType.MODEL_TICK_LIST, new ModelTickListGui(this));
        guis.put(GuiType.MODEL_LIBRARY, new ModelLibraryGui(this));
        guis.put(GuiType.MODEL_ENTRY_EDITOR, new ModelEntryEditorGui(this));
        guis.put(GuiType.ACTIONBAR_EDITOR, new ActionBarKeyframeEditorGui(this));
        guis.put(GuiType.EFFECTS_MENU, new EffectsTickMenuGui(this));
        guis.put(GuiType.SCENE_SETTINGS, new SceneSettingsGui(this));
        guis.put(GuiType.ACTORS_LIST, new ActorsListGui(this));
        guis.put(GuiType.ACTOR_DETAIL, new ActorDetailGui(this));
        guis.put(GuiType.ACTOR_TIMELINE, new ActorTimelineGui(this));
        guis.put(GuiType.ACTOR_TICK_ACTIONS, new ActorTickActionsGui(this));
        guis.put(GuiType.CONFIRM, new ConfirmGui(this));
    }


    public void openMainMenu(Player player) {
        MainMenuState state = mainMenuStates.computeIfAbsent(player.getUniqueId(), id -> new MainMenuState());
        player.openInventory(buildMainMenuInventory(state));
    }

    public boolean isMainMenuTitle(String title) {
        return title != null && title.startsWith(GuiUtils.TITLE_PREFIX + "Scenes • Page");
    }

    public boolean isMainMenuDeleteConfirmTitle(String title) {
        return title != null && title.startsWith(GuiUtils.TITLE_PREFIX + "Confirm Delete • "
        );
    }

    public void handleMainMenuDeleteConfirmClick(Player player, int slot) {
        String sceneName = pendingDeleteSceneByPlayer.get(player.getUniqueId());
        if (sceneName == null) {
            openMainMenu(player);
            return;
        }
        if (slot == 11) {
            if (sceneManager.deleteScene(sceneName)) {
                forceCloseEditorsForScene(sceneName);
                clearLastSelectedSceneReferences(sceneName);
            }
            pendingDeleteSceneByPlayer.remove(player.getUniqueId());
            openMainMenu(player);
            return;
        }
        if (slot == 15 || slot == 22) {
            pendingDeleteSceneByPlayer.remove(player.getUniqueId());
            openMainMenu(player);
        }
    }

    private void openSceneDeleteConfirm(Player player, String sceneName) {
        Inventory inventory = GuiUtils.createInventory(27, "Confirm Delete • " + sceneName);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }
        inventory.setItem(11, GuiUtils.makeItem(Material.REDSTONE_BLOCK, "Confirm Delete", java.util.List.of(sceneName)));
        inventory.setItem(15, GuiUtils.makeItem(Material.BARRIER, "Cancel", java.util.List.of("Return to main menu.")));
        inventory.setItem(22, GuiUtils.makeItem(Material.BARRIER, "Close", java.util.List.of("Return to main menu.")));
        player.openInventory(inventory);
    }

    public void handleMainMenuClick(Player player, int slot) {
        MainMenuState state = mainMenuStates.computeIfAbsent(player.getUniqueId(), id -> new MainMenuState());
        java.util.List<String> scenes = filteredSceneNames(state.filter());
        int totalPages = Math.max(1, (int) Math.ceil(scenes.size() / 36.0));
        state.setPage(Math.min(state.page(), totalPages - 1));
        if (slot >= 0 && slot < 36) {
            int index = state.page() * 36 + slot;
            if (index >= scenes.size()) {
                return;
            }
            String sceneName = scenes.get(index);
            switch (state.action()) {
                case EDIT -> {
                    Scene scene = sceneManager.loadScene(sceneName);
                    if (scene != null) {
                        openEditor(player, scene);
                    }
                }
                case DELETE -> {
                    pendingDeleteSceneByPlayer.put(player.getUniqueId(), sceneName);
                    openSceneDeleteConfirm(player, sceneName);
                }
                case DUPLICATE -> {
                    beginMainMenuPrompt(player, MainMenuPromptType.DUPLICATE, sceneName);
                    player.closeInventory();
                }
                case RENAME -> {
                    beginMainMenuPrompt(player, MainMenuPromptType.RENAME, sceneName);
                    player.closeInventory();
                }
            }
            return;
        }
        if (slot == 45 && totalPages > 1) {
            state.setPage(Math.max(0, state.page() - 1));
            openMainMenu(player);
            return;
        }
        if (slot == 53 && totalPages > 1) {
            state.setPage(Math.min(totalPages - 1, state.page() + 1));
            openMainMenu(player);
            return;
        }
        if (slot == 46) {
            state.setAction(MainMenuAction.EDIT);
            openMainMenu(player);
            return;
        }
        if (slot == 47) {
            state.setAction(MainMenuAction.CREATE);
            createSceneFromMainMenu(player);
            return;
        }
        if (slot == 48) {
            state.setAction(MainMenuAction.DELETE);
            openMainMenu(player);
            return;
        }
        if (slot == 49) {
            state.setAction(MainMenuAction.RENAME);
            openMainMenu(player);
            return;
        }
        if (slot == 50) {
            state.setAction(MainMenuAction.DUPLICATE);
            openMainMenu(player);
            return;
        }
        if (slot == 51) {
            beginMainMenuPrompt(player, MainMenuPromptType.FILTER, null);
            player.closeInventory();
            return;
        }
        if (slot == 52) {
            plugin.reloadConfig();
            sceneManager.reloadAll();
            openMainMenu(player);
        }
    }

    public boolean hasMainMenuPrompt(UUID playerId) {
        return playerId != null && mainMenuPrompts.containsKey(playerId);
    }

    public boolean handleMainMenuChat(Player player, String message) {
        MainMenuPrompt prompt = mainMenuPrompts.get(player.getUniqueId());
        if (prompt == null) {
            return false;
        }
        if ("cancel".equalsIgnoreCase(message)) {
            mainMenuPrompts.remove(player.getUniqueId());
            openMainMenu(player);
            return true;
        }
        MainMenuState state = mainMenuStates.computeIfAbsent(player.getUniqueId(), id -> new MainMenuState());
        if (prompt.type() == MainMenuPromptType.FILTER) {
            state.setFilter(message.equalsIgnoreCase("clear") ? "" : message);
            state.setPage(0);
        } else if (prompt.type() == MainMenuPromptType.RENAME) {
            if (sceneManager.renameScene(prompt.sceneName(), message)) {
                forceCloseEditorsForScene(prompt.sceneName());
                clearLastSelectedSceneReferences(prompt.sceneName());
            }
        } else if (prompt.type() == MainMenuPromptType.DUPLICATE) {
            sceneManager.duplicateScene(prompt.sceneName(), message);
        }
        mainMenuPrompts.remove(player.getUniqueId());
        openMainMenu(player);
        return true;
    }

    private void createSceneFromMainMenu(Player player) {
        int idx = 1;
        String name;
        do {
            name = "scene_" + idx++;
        } while (sceneManager.sceneExists(name));
        Scene scene = sceneManager.createScene(name, 200);
        sceneManager.markDirty(scene);
        openMainMenu(player);
    }

    private void beginMainMenuPrompt(Player player, MainMenuPromptType type, String sceneName) {
        mainMenuPrompts.put(player.getUniqueId(), new MainMenuPrompt(type, sceneName));
        if (type == MainMenuPromptType.FILTER) {
            Text.send(player, "&b" + "Enter search text (or 'clear'). Type cancel to abort.");
        } else if (type == MainMenuPromptType.RENAME) {
            Text.send(player, "&b" + "Enter new scene name for '" + sceneName + "'. Type cancel to abort.");
        } else if (type == MainMenuPromptType.DUPLICATE) {
            Text.send(player, "&b" + "Enter duplicate name for '" + sceneName + "'. Type cancel to abort.");
        }
    }

    private Inventory buildMainMenuInventory(MainMenuState state) {
        java.util.List<String> scenes = filteredSceneNames(state.filter());
        int totalPages = Math.max(1, (int) Math.ceil(scenes.size() / 36.0));
        int page = Math.min(state.page(), totalPages - 1);
        state.setPage(page);
        Inventory inventory = GuiUtils.createInventory(54, "Scenes • Page " + (page + 1) + "/" + totalPages);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, null);
        }
        int start = page * 36;
        for (int i = 0; i < 36 && start + i < scenes.size(); i++) {
            String name = scenes.get(start + i);
            Scene scene = sceneManager.loadScene(name);
            if (scene == null) {
                continue;
            }
            boolean hasCam = !scene.getTrack(SceneTrackType.CAMERA).getKeyframes().isEmpty();
            boolean hasActors = !scene.getActorTemplates().isEmpty();
            boolean hasCmd = !scene.getTrack(SceneTrackType.COMMAND).getKeyframes().isEmpty();
            boolean hasBar = !scene.getTrack(SceneTrackType.ACTIONBAR).getKeyframes().isEmpty();
            java.util.List<String> lore = java.util.List.of(
                    "Duration: " + scene.getDurationTicks() + " ticks",
                    "Last modified: " + GuiUtils.formatLastSaved(sceneManager.getSceneLastModified(name)),
                    "📷 Camera: " + (hasCam ? "Yes" : "No"),
                    "🧍 Actors: " + (hasActors ? "Yes" : "No"),
                    "⌨ Commands: " + (hasCmd ? "Yes" : "No"),
                    "💬 Actionbars: " + (hasBar ? "Yes" : "No")
            );
            inventory.setItem(i, GuiUtils.makeItem(Material.BOOK, scene.getName(), lore));
        }
        inventory.setItem(45, GuiUtils.makeItem(Material.ARROW, "Prev", java.util.List.of("Previous page")));
        inventory.setItem(46, modeItem(state, MainMenuAction.EDIT, "Edit"));
        inventory.setItem(47, GuiUtils.makeItem(Material.LIME_DYE, "Create Scene", java.util.List.of("Create new scene")));
        inventory.setItem(48, modeItem(state, MainMenuAction.DELETE, "Delete Scene"));
        inventory.setItem(49, modeItem(state, MainMenuAction.RENAME, "Rename Scene"));
        inventory.setItem(50, modeItem(state, MainMenuAction.DUPLICATE, "Duplicate Scene"));
        inventory.setItem(51, GuiUtils.makeItem(Material.COMPASS, "Search/Filter", java.util.List.of("Current: " + (state.filter().isBlank() ? "none" : state.filter()))));
        inventory.setItem(52, GuiUtils.makeItem(Material.REPEATER, "Reload", java.util.List.of("Reload config + scenes")));
        inventory.setItem(53, GuiUtils.makeItem(Material.ARROW, "Next", java.util.List.of("Next page")));
        return inventory;
    }

    private org.bukkit.inventory.ItemStack modeItem(MainMenuState state, MainMenuAction action, String name) {
        Material mat = state.action() == action ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE;
        return GuiUtils.makeItem(mat, name, java.util.List.of("Select action mode."));
    }

    private java.util.List<String> filteredSceneNames(String filter) {
        String f = filter == null ? "" : filter.toLowerCase(java.util.Locale.ROOT);
        return sceneManager.listScenes().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .filter(n -> f.isBlank() || n.toLowerCase(java.util.Locale.ROOT).contains(f))
                .toList();
    }

    public EditorInputManager getInputManager() {
        return inputManager;
    }

    public EditorSessionManager getEditorSessionManager() {
        return editorSessionManager;
    }

    public void openEditor(Player player, Scene scene) {
        if (scene == null) {
            return;
        }
        Scene cached = sceneManager.loadScene(scene.getName());
        if (cached == null) {
            Text.send(player, "&c" + "Scene file is missing; cannot open editor.");
            return;
        }
        sceneManager.cacheScene(cached);
        lastSelectedSceneByPlayer.put(player.getUniqueId(), cached.getName());
        EditorSession session = editorSessionManager.getSession(player.getUniqueId());
        if (session == null) {
            session = editorSessionManager.createSession(player.getUniqueId(), cached);
        } else {
            session.setScene(cached);
        }
        session.clearHistory();
        openDashboard(player, session, false);
    }

    public void openDashboard(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.SCENE_DASHBOARD, pushHistory);
    }

    public void openTrackSelect(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.TRACK_SELECT, pushHistory);
    }

    public void openGroupSelect(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.GROUP_SELECT, pushHistory);
    }

    public void openGroupGrid(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.GROUP_GRID, pushHistory);
    }

    public void openTickActionMenu(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.TICK_ACTION, pushHistory);
    }

    public void openTickTools(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.TICK_TOOLS, pushHistory);
    }

    public void openCameraOptions(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.CAMERA_OPTIONS, pushHistory);
    }

    public void openModelTickList(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.MODEL_TICK_LIST, pushHistory);
    }

    public void openModelLibrary(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.MODEL_LIBRARY, pushHistory);
    }

    public void openModelEntryEditor(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.MODEL_ENTRY_EDITOR, pushHistory);
    }

    public void openEffectsMenu(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.EFFECTS_MENU, pushHistory);
    }

    public void openKeyframeList(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.KEYFRAME_LIST, pushHistory);
    }

    public void openAddKeyframe(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.ADD_KEYFRAME, pushHistory);
    }

    public void openCameraEditor(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.CAMERA_EDITOR, pushHistory);
    }

    public void openCommandEditor(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.COMMAND_EDITOR, pushHistory);
    }

    public void openModelEditor(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.MODEL_EDITOR, pushHistory);
    }

    public void openActionBarEditor(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.ACTIONBAR_EDITOR, pushHistory);
    }

    public void openSceneSettings(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.SCENE_SETTINGS, pushHistory);
    }

    public void openActorsList(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.ACTORS_LIST, pushHistory);
    }

    public void openActorDetail(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.ACTOR_DETAIL, pushHistory);
    }

    public void openActorTimeline(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.ACTOR_TIMELINE, pushHistory);
    }

    public void openActorTickActions(Player player, EditorSession session, boolean pushHistory) {
        openGui(player, session, GuiType.ACTOR_TICK_ACTIONS, pushHistory);
    }

    public ExtraScenesPlugin getPlugin() {
        return plugin;
    }

    public void snapActorScaleFromPlayer(Player player, Scene scene, SceneActorTemplate actor) {
        org.bukkit.attribute.Attribute attribute = com.extrascenes.ScaleAttributeResolver.resolveScaleAttribute();
        if (attribute == null || player.getAttribute(attribute) == null) {
            Text.send(player, "&c" + "Scale attribute not available on this server build.");
            return;
        }
        actor.setScale(player.getAttribute(attribute).getValue());
        markDirty(scene);
        Text.send(player, "&a" + "Actor scale snapped from player.");
    }

    public void openConfirm(Player player, EditorSession session, ConfirmAction action, SceneTrackType track, UUID keyframeId) {
        session.setConfirmAction(action);
        session.setConfirmTrack(track);
        session.setConfirmKeyframeId(keyframeId);
        openGui(player, session, GuiType.CONFIRM, true);
    }

    public void openCommandEditorForTick(Player player, EditorSession session, int tick) {
        CommandKeyframe keyframe = getOrCreateCommandKeyframe(session, tick);
        session.setSelectedKeyframeId(keyframe.getId());
        session.setSelectedTrack(SceneTrackType.COMMAND);
        openCommandEditor(player, session, true);
    }

    public void openActionBarEditorForTick(Player player, EditorSession session, int tick) {
        ActionBarKeyframe keyframe = getOrCreateActionBarKeyframe(session, tick);
        session.setSelectedKeyframeId(keyframe.getId());
        session.setSelectedTrack(SceneTrackType.ACTIONBAR);
        openActionBarEditor(player, session, true);
    }

    public void navigateBack(Player player, EditorSession session) {
        EditorSession.NavigationState previous = session.popHistory();
        if (previous == null) {
            openDashboard(player, session, false);
            return;
        }
        session.setKeyframePage(previous.keyframePage());
        session.setGroupPage(previous.groupPage());
        session.setCurrentGroup(previous.currentGroup());
        session.setCurrentTick(previous.currentTick());
        session.setActorsPage(previous.actorsPage());
        session.setSelectedActorId(previous.selectedActorId());
        openGui(player, session, previous.guiType(), false);
    }

    public void closeEditor(Player player, EditorSession session) {
        player.closeInventory();
        sceneManager.saveSceneImmediate(session.getScene());
        if (hasArmedPlacement(session)) {
            cancelPlacementSilent(player, session);
        }
        editorSessionManager.removeSession(player.getUniqueId());
        session.clearHistory();
        lastSelectedSceneByPlayer.remove(player.getUniqueId());
        inputManager.clearPrompt(player.getUniqueId());
    }

    public void handleClick(Player player, Scene scene, EditorSession session, int slot, boolean rightClick,
                            boolean shiftClick, org.bukkit.inventory.ItemStack clicked) {
        EditorGui gui = guis.get(session.getCurrentGui());
        if (gui == null) {
            return;
        }
        gui.handleClick(session, new ClickContext(slot, rightClick, shiftClick, clicked));
    }

    private void openGui(Player player, EditorSession session, GuiType guiType, boolean pushHistory) {
        if (pushHistory && session.getCurrentGui() != null) {
            session.pushHistory(session.getCurrentGui());
        }
        session.setCurrentGui(guiType);
        EditorGui gui = guis.get(guiType);
        if (gui == null) {
            return;
        }
        Inventory inventory = gui.build(session);
        player.openInventory(inventory);
    }

    public void openGuiByType(Player player, EditorSession session, GuiType guiType) {
        openGui(player, session, guiType, false);
    }

    public void armCameraPlacement(Player player, EditorSession session, int tick) {
        session.setArmedTick(tick);
        session.setArmedGroup(session.getCurrentGroup());
        session.setCursorTimeTicks(tick);
        session.setArmedModelEntryName(null);
        session.setArmedModelKeyframeId(null);
        session.setArmedReturnGui(null);
        player.closeInventory();
        if (session.getWandSlot() == -1) {
            session.setWandSlot(player.getInventory().getHeldItemSlot());
            session.setWandBackup(player.getInventory().getItem(session.getWandSlot()));
            player.getInventory().setItem(session.getWandSlot(), SceneWand.create());
        }
        Text.send(player, "&b" + "Placement mode armed for tick " + tick
                + ". Move and confirm with /scene here or left-click with the wand. Use /scene cancel to abort.");
    }

    public void confirmCameraPlacement(Player player, EditorSession session) {
        Integer tick = session.getArmedTick();
        if (tick == null) {
            Text.send(player, "&c" + "No tick is armed for placement.");
            return;
        }
        CameraKeyframe keyframe = getOrCreateCameraKeyframe(session, tick);
        keyframe.setTransform(Transform.fromLocation(player.getEyeLocation()));
        sceneManager.markDirty(session.getScene());
        session.setArmedTick(null);
        restoreWand(player, session);
        session.setCurrentTick(tick);
        session.setCurrentGroup(session.getArmedGroup());
        openGroupGrid(player, session, false);
        Text.send(player, "&a" + "Camera position set for tick " + tick + ".");
    }

    public void cancelCameraPlacement(Player player, EditorSession session) {
        if (session.getArmedTick() == null) {
            Text.send(player, "&e" + "No placement in progress.");
            return;
        }
        session.setArmedTick(null);
        restoreWand(player, session);
        openGroupGrid(player, session, false);
        Text.send(player, "&e" + "Placement cancelled.");
    }

    public void cancelCameraPlacementSilent(Player player, EditorSession session) {
        session.setArmedTick(null);
        restoreWand(player, session);
    }

    public void armModelEntryPlacement(Player player, EditorSession session, String entryName, GuiType returnGui) {
        session.setArmedTick(null);
        session.setArmedModelEntryName(entryName);
        session.setArmedModelKeyframeId(null);
        session.setArmedReturnGui(returnGui);
        player.closeInventory();
        if (session.getWandSlot() == -1) {
            session.setWandSlot(player.getInventory().getHeldItemSlot());
            session.setWandBackup(player.getInventory().getItem(session.getWandSlot()));
            player.getInventory().setItem(session.getWandSlot(), SceneWand.create());
        }
        Text.send(player, "&b" + "Placement mode armed for model entry " + entryName
                + ". Move and confirm with /scene here or left-click with the wand. Use /scene cancel to abort.");
    }

    public void armModelKeyframePlacement(Player player, EditorSession session, UUID keyframeId, GuiType returnGui) {
        session.setArmedTick(null);
        session.setArmedModelEntryName(null);
        session.setArmedModelKeyframeId(keyframeId);
        session.setArmedReturnGui(returnGui);
        player.closeInventory();
        if (session.getWandSlot() == -1) {
            session.setWandSlot(player.getInventory().getHeldItemSlot());
            session.setWandBackup(player.getInventory().getItem(session.getWandSlot()));
            player.getInventory().setItem(session.getWandSlot(), SceneWand.create());
        }
        Text.send(player, "&b" + "Placement mode armed for model spawn. Move and confirm with /scene here or"
                + " left-click with the wand. Use /scene cancel to abort.");
    }

    public void confirmModelPlacement(Player player, EditorSession session) {
        if (session.getArmedModelEntryName() != null) {
            SceneModelEntry entry = session.getScene().getModelEntry(session.getArmedModelEntryName());
            if (entry != null) {
                entry.setSpawnTransform(Transform.fromLocation(player.getLocation()));
                sceneManager.markDirty(session.getScene());
                Text.send(player, "&a" + "Model entry spawn location updated.");
            }
            GuiType returnGui = session.getArmedReturnGui();
            clearModelPlacement(session);
            restoreWand(player, session);
            if (returnGui != null) {
                openGuiByType(player, session, returnGui);
            } else {
                openGroupGrid(player, session, false);
            }
            return;
        }
        if (session.getArmedModelKeyframeId() != null) {
            Track<ModelKeyframe> track = session.getScene().getTrack(SceneTrackType.MODEL);
            if (track != null) {
                ModelKeyframe keyframe = track.getKeyframe(session.getArmedModelKeyframeId());
                if (keyframe != null) {
                    keyframe.setSpawnTransform(Transform.fromLocation(player.getLocation()));
                    sceneManager.markDirty(session.getScene());
                    Text.send(player, "&a" + "Model spawn override set.");
                }
            }
            GuiType returnGui = session.getArmedReturnGui();
            clearModelPlacement(session);
            restoreWand(player, session);
            if (returnGui != null) {
                openGuiByType(player, session, returnGui);
            } else {
                openGroupGrid(player, session, false);
            }
            return;
        }
        Text.send(player, "&c" + "No placement in progress.");
    }

    public void cancelModelPlacement(Player player, EditorSession session) {
        if (session.getArmedModelEntryName() == null && session.getArmedModelKeyframeId() == null) {
            Text.send(player, "&e" + "No placement in progress.");
            return;
        }
        GuiType returnGui = session.getArmedReturnGui();
        clearModelPlacement(session);
        restoreWand(player, session);
        if (returnGui != null) {
            openGuiByType(player, session, returnGui);
        } else {
            openGroupGrid(player, session, false);
        }
        Text.send(player, "&e" + "Placement cancelled.");
    }

    public void cancelModelPlacementSilent(Player player, EditorSession session) {
        clearModelPlacement(session);
        restoreWand(player, session);
    }

    public void cancelPlacementSilent(Player player, EditorSession session) {
        if (session.getArmedTick() != null) {
            cancelCameraPlacementSilent(player, session);
            return;
        }
        if (session.getArmedModelEntryName() != null || session.getArmedModelKeyframeId() != null) {
            cancelModelPlacementSilent(player, session);
        }
    }

    public boolean hasArmedPlacement(EditorSession session) {
        return session.getArmedTick() != null || session.getArmedModelEntryName() != null
                || session.getArmedModelKeyframeId() != null;
    }

    public void confirmPlacement(Player player, EditorSession session) {
        if (session.getArmedTick() != null) {
            confirmCameraPlacement(player, session);
            return;
        }
        confirmModelPlacement(player, session);
    }

    public void cancelPlacement(Player player, EditorSession session) {
        if (session.getArmedTick() != null) {
            cancelCameraPlacement(player, session);
            return;
        }
        cancelModelPlacement(player, session);
    }

    private void clearModelPlacement(EditorSession session) {
        session.setArmedModelEntryName(null);
        session.setArmedModelKeyframeId(null);
        session.setArmedReturnGui(null);
    }

    private void restoreWand(Player player, EditorSession session) {
        if (session.getWandSlot() >= 0) {
            player.getInventory().setItem(session.getWandSlot(), session.getWandBackup());
            session.setWandSlot(-1);
            session.setWandBackup(null);
        }
    }

    public void saveScene(Player player, EditorSession session) {
        try {
            sceneManager.saveScene(session.getScene());
            session.setLastSavedAt(System.currentTimeMillis());
            Text.send(player, "&a" + "Scene saved.");
        } catch (IOException ex) {
            Text.send(player, "&c" + "Failed to save scene.");
        }
    }

    public void togglePreview(Player player, EditorSession session) {
        if (session.isPreviewPlaying()) {
            plugin.getSessionManager().stopScene(player, "preview_stop");
            session.setPreviewPlaying(false);
        } else {
            plugin.getSessionManager().startScene(player, session.getScene(), true);
            session.setPreviewPlaying(true);
        }
    }

    public void playScene(Player player, EditorSession session) {
        plugin.getSessionManager().startScene(player, session.getScene(), false);
    }

    public CameraKeyframe getSelectedCameraKeyframe(EditorSession session) {
        return getSelectedKeyframe(session, SceneTrackType.CAMERA, CameraKeyframe.class);
    }

    public CommandKeyframe getSelectedCommandKeyframe(EditorSession session) {
        return getSelectedKeyframe(session, SceneTrackType.COMMAND, CommandKeyframe.class);
    }

    public ModelKeyframe getSelectedModelKeyframe(EditorSession session) {
        return getSelectedKeyframe(session, SceneTrackType.MODEL, ModelKeyframe.class);
    }

    public ActionBarKeyframe getSelectedActionBarKeyframe(EditorSession session) {
        return getSelectedKeyframe(session, SceneTrackType.ACTIONBAR, ActionBarKeyframe.class);
    }

    private <T extends Keyframe> T getSelectedKeyframe(EditorSession session, SceneTrackType type, Class<T> clazz) {
        if (session.getSelectedKeyframeId() == null) {
            return null;
        }
        Track<? extends Keyframe> track = session.getScene().getTrack(type);
        if (track == null) {
            return null;
        }
        Keyframe keyframe = track.getKeyframe(session.getSelectedKeyframeId());
        if (clazz.isInstance(keyframe)) {
            return clazz.cast(keyframe);
        }
        return null;
    }

    public CameraKeyframe getOrCreateCameraKeyframe(EditorSession session, int tick) {
        Track<CameraKeyframe> track = session.getScene().getTrack(SceneTrackType.CAMERA);
        CameraKeyframe existing = TickUtils.getFirstKeyframeAtTick(track, tick);
        if (existing != null) {
            return existing;
        }
        Transform fallback = null;
        if (track != null) {
            for (CameraKeyframe keyframe : track.getKeyframes()) {
                if (keyframe.getTimeTicks() <= tick && keyframe.getTransform() != null) {
                    Transform transform = keyframe.getTransform();
                    fallback = new Transform(transform.getX(), transform.getY(), transform.getZ(),
                            transform.getYaw(), transform.getPitch());
                }
            }
        }
        CameraKeyframe created = new CameraKeyframe(null, tick, fallback,
                session.getScene().getDefaultSmoothing(), false, LookAtTarget.none());
        track.addKeyframe(created);
        sceneManager.markDirty(session.getScene());
        return created;
    }

    public CommandKeyframe getOrCreateCommandKeyframe(EditorSession session, int tick) {
        Track<CommandKeyframe> track = session.getScene().getTrack(SceneTrackType.COMMAND);
        CommandKeyframe existing = TickUtils.getFirstKeyframeAtTick(track, tick);
        if (existing != null) {
            return existing;
        }
        CommandKeyframe created = new CommandKeyframe(null, tick, List.of(), CommandKeyframe.ExecutorMode.PLAYER, false);
        track.addKeyframe(created);
        sceneManager.markDirty(session.getScene());
        return created;
    }

    public ActionBarKeyframe getOrCreateActionBarKeyframe(EditorSession session, int tick) {
        Track<ActionBarKeyframe> track = session.getScene().getTrack(SceneTrackType.ACTIONBAR);
        ActionBarKeyframe existing = TickUtils.getFirstKeyframeAtTick(track, tick);
        if (existing != null) {
            return existing;
        }
        ActionBarKeyframe created = new ActionBarKeyframe(null, tick, "", 20);
        track.addKeyframe(created);
        sceneManager.markDirty(session.getScene());
        return created;
    }

    public void duplicateKeyframe(EditorSession session, Keyframe keyframe) {
        if (keyframe == null) {
            return;
        }
        if (keyframe instanceof CameraKeyframe camera) {
            Track<CameraKeyframe> track = session.getScene().getTrack(SceneTrackType.CAMERA);
            if (track == null) {
                return;
            }
            CameraKeyframe copy = new CameraKeyframe(null, camera.getTimeTicks(),
                    cloneTransform(camera.getTransform()),
                    camera.getSmoothingMode(), camera.isInstant(),
                    cloneLookAt(camera.getLookAt()));
            track.addKeyframe(copy);
            sceneManager.markDirty(session.getScene());
        } else if (keyframe instanceof CommandKeyframe command) {
            Track<CommandKeyframe> track = session.getScene().getTrack(SceneTrackType.COMMAND);
            if (track == null) {
                return;
            }
            CommandKeyframe copy = new CommandKeyframe(null, command.getTimeTicks(),
                    List.copyOf(command.getCommands()), command.getExecutorMode(), command.isAllowGlobal());
            track.addKeyframe(copy);
            sceneManager.markDirty(session.getScene());
        } else if (keyframe instanceof ModelKeyframe model) {
            Track<ModelKeyframe> track = session.getScene().getTrack(SceneTrackType.MODEL);
            if (track == null) {
                return;
            }
            ModelKeyframe copy = new ModelKeyframe(null, model.getTimeTicks(), model.getAction());
            copy.setModelId(model.getModelId());
            copy.setModelEntry(model.getModelEntry());
            copy.setEntityRef(null);
            copy.setAnimationId(model.getAnimationId());
            copy.setLoop(model.isLoop());
            copy.setSpeed(model.getSpeed());
            copy.setSpawnTransform(cloneTransform(model.getSpawnTransform()));
            track.addKeyframe(copy);
            sceneManager.markDirty(session.getScene());
        } else if (keyframe instanceof ActionBarKeyframe actionBar) {
            Track<ActionBarKeyframe> track = session.getScene().getTrack(SceneTrackType.ACTIONBAR);
            if (track == null) {
                return;
            }
            ActionBarKeyframe copy = new ActionBarKeyframe(null, actionBar.getTimeTicks(), actionBar.getText(),
                    actionBar.getDurationTicks());
            track.addKeyframe(copy);
            sceneManager.markDirty(session.getScene());
        }
    }

    public void addCameraKeyframe(Player player, EditorSession session, SmoothingMode smoothingMode) {
        SmoothingMode smoothing = smoothingMode == null ? session.getScene().getDefaultSmoothing() : smoothingMode;
        CameraKeyframe keyframe = new CameraKeyframe(null, session.getCursorTimeTicks(),
                Transform.fromLocation(player.getEyeLocation()),
                smoothing, smoothing == SmoothingMode.INSTANT, LookAtTarget.none());
        Track<CameraKeyframe> track = session.getScene().getTrack(SceneTrackType.CAMERA);
        track.addKeyframe(keyframe);
        sceneManager.markDirty(session.getScene());
        session.setSelectedKeyframeId(keyframe.getId());
        openKeyframeList(player, session, false);
    }

    public void addLookAtKeyframe(Player player, EditorSession session) {
        SmoothingMode smoothing = session.getScene().getDefaultSmoothing();
        CameraKeyframe keyframe = new CameraKeyframe(null, session.getCursorTimeTicks(),
                Transform.fromLocation(player.getEyeLocation()),
                smoothing, false,
                new LookAtTarget(LookAtTarget.Mode.POSITION, Transform.fromLocation(player.getEyeLocation()), null));
        Track<CameraKeyframe> track = session.getScene().getTrack(SceneTrackType.CAMERA);
        track.addKeyframe(keyframe);
        sceneManager.markDirty(session.getScene());
        session.setSelectedKeyframeId(keyframe.getId());
        openKeyframeList(player, session, false);
    }

    public void addSoundKeyframe(Player player, EditorSession session) {
        SoundKeyframe keyframe = new SoundKeyframe(null, session.getCursorTimeTicks(), "entity.player.levelup",
                1.0f, 1.0f, Transform.fromLocation(player.getLocation()));
        Track<SoundKeyframe> track = session.getScene().getTrack(SceneTrackType.SOUND);
        track.addKeyframe(keyframe);
        sceneManager.markDirty(session.getScene());
        session.setSelectedKeyframeId(keyframe.getId());
        openKeyframeList(player, session, false);
    }

    public void addSoundKeyframeAtTick(Player player, EditorSession session, int tick) {
        session.setCursorTimeTicks(tick);
        SoundKeyframe keyframe = new SoundKeyframe(null, tick, "entity.player.levelup",
                1.0f, 1.0f, Transform.fromLocation(player.getLocation()));
        Track<SoundKeyframe> track = session.getScene().getTrack(SceneTrackType.SOUND);
        track.addKeyframe(keyframe);
        sceneManager.markDirty(session.getScene());
    }

    public void addParticleKeyframe(Player player, EditorSession session) {
        ParticleKeyframe keyframe = new ParticleKeyframe(null, session.getCursorTimeTicks(), "happy_villager",
                Transform.fromLocation(player.getLocation()));
        Track<ParticleKeyframe> track = session.getScene().getTrack(SceneTrackType.PARTICLE);
        track.addKeyframe(keyframe);
        sceneManager.markDirty(session.getScene());
        session.setSelectedKeyframeId(keyframe.getId());
        openKeyframeList(player, session, false);
    }

    public void addParticleKeyframeAtTick(Player player, EditorSession session, int tick) {
        session.setCursorTimeTicks(tick);
        ParticleKeyframe keyframe = new ParticleKeyframe(null, tick, "happy_villager",
                Transform.fromLocation(player.getLocation()));
        Track<ParticleKeyframe> track = session.getScene().getTrack(SceneTrackType.PARTICLE);
        track.addKeyframe(keyframe);
        sceneManager.markDirty(session.getScene());
    }

    public void addBlockKeyframe(Player player, EditorSession session) {
        BlockIllusionKeyframe keyframe = new BlockIllusionKeyframe(null, session.getCursorTimeTicks(),
                Material.GLASS, Transform.fromLocation(player.getLocation()));
        Track<BlockIllusionKeyframe> track = session.getScene().getTrack(SceneTrackType.BLOCK_ILLUSION);
        track.addKeyframe(keyframe);
        sceneManager.markDirty(session.getScene());
        session.setSelectedKeyframeId(keyframe.getId());
        openKeyframeList(player, session, false);
    }

    public void addBlockKeyframeAtTick(Player player, EditorSession session, int tick) {
        session.setCursorTimeTicks(tick);
        BlockIllusionKeyframe keyframe = new BlockIllusionKeyframe(null, tick,
                Material.GLASS, Transform.fromLocation(player.getLocation()));
        Track<BlockIllusionKeyframe> track = session.getScene().getTrack(SceneTrackType.BLOCK_ILLUSION);
        track.addKeyframe(keyframe);
        sceneManager.markDirty(session.getScene());
    }

    public void handleConfirmAction(Player player, EditorSession session) {
        ConfirmAction action = session.getConfirmAction();
        if (action == null) {
            navigateBack(player, session);
            return;
        }
        if (action == ConfirmAction.DELETE_KEYFRAME) {
            Track<? extends Keyframe> track = session.getScene().getTrack(session.getConfirmTrack());
            if (track != null && session.getConfirmKeyframeId() != null) {
                track.removeKeyframe(session.getConfirmKeyframeId());
                if (session.getConfirmKeyframeId().equals(session.getSelectedKeyframeId())) {
                    session.setSelectedKeyframeId(null);
                }
                sceneManager.markDirty(session.getScene());
            }
            clearConfirm(session);
            session.setKeyframePage(0);
            openKeyframeList(player, session, false);
            return;
        }
        if (action == ConfirmAction.REMOVE_COMMAND) {
            CommandKeyframe keyframe = getSelectedCommandKeyframe(session);
            Integer index = session.getConfirmCommandIndex();
            if (keyframe != null && index != null && index >= 0 && index < keyframe.getCommands().size()) {
                List<String> commands = new java.util.ArrayList<>(keyframe.getCommands());
                commands.remove(index.intValue());
                keyframe.setCommands(commands);
                sceneManager.markDirty(session.getScene());
            }
            clearConfirm(session);
            openCommandEditor(player, session, false);
            return;
        }
        if (action == ConfirmAction.CLEAR_TRACK) {
            Track<? extends Keyframe> track = session.getScene().getTrack(session.getConfirmTrack());
            if (track != null) {
                track.clear();
                sceneManager.markDirty(session.getScene());
            }
            clearConfirm(session);
            session.setSelectedKeyframeId(null);
            session.setKeyframePage(0);
            openKeyframeList(player, session, false);
            return;
        }
        if (action == ConfirmAction.CLEAR_TICK) {
            clearTick(session.getScene(), session.getCurrentTick());
            sceneManager.markDirty(session.getScene());
            clearConfirm(session);
            openTickActionMenu(player, session, false);
            return;
        }
        if (action == ConfirmAction.CLEAR_GROUP) {
            clearGroup(session.getScene(), session.getCurrentGroup());
            sceneManager.markDirty(session.getScene());
            clearConfirm(session);
            openGroupGrid(player, session, false);
            return;
        }
        if (action == ConfirmAction.CLEAR_ALL) {
            clearAll(session.getScene());
            sceneManager.markDirty(session.getScene());
            clearConfirm(session);
            openGroupSelect(player, session, false);
            return;
        }
        if (action == ConfirmAction.CLEAR_EFFECTS) {
            clearEffects(session.getScene(), session.getCurrentTick());
            sceneManager.markDirty(session.getScene());
            clearConfirm(session);
            openEffectsMenu(player, session, false);
            return;
        }
        if (action == ConfirmAction.TRIM_DURATION) {
            session.getScene().setDurationTicks(Math.max(0, session.getScene().getDurationTicks() - 9));
            sceneManager.markDirty(session.getScene());
            clearConfirm(session);
            openGroupSelect(player, session, false);
            return;
        }
        if (action == ConfirmAction.DELETE_ACTOR) {
            String actorId = session.getSelectedActorId();
            if (actorId != null) {
                session.getScene().removeActorTemplate(actorId);
                session.setSelectedActorId(null);
                sceneManager.markDirty(session.getScene());
            }
            clearConfirm(session);
            openActorsList(player, session, false);
            return;
        }
        if (action == ConfirmAction.DELETE_SCENE) {
            String sceneName = session.getScene().getName();
            sceneManager.deleteScene(sceneName);
            clearLastSelectedScene(player.getUniqueId(), sceneName);
            clearLastSelectedSceneReferences(sceneName);
            clearConfirm(session);
            forceCloseEditorsForScene(sceneName);
        }
    }

    public void markDirty(Scene scene) {
        sceneManager.markDirty(scene);
    }

    public List<SceneModelEntry> getModelEntries(Scene scene) {
        if (scene == null) {
            return List.of();
        }
        return new java.util.ArrayList<>(scene.getModelLibrary().values());
    }

    public String nextModelEntry(Scene scene, String current) {
        List<SceneModelEntry> entries = getModelEntries(scene);
        if (entries.isEmpty()) {
            return null;
        }
        if (current == null) {
            return entries.get(0).getName();
        }
        int index = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getName().equalsIgnoreCase(current)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            return entries.get(0).getName();
        }
        return entries.get((index + 1) % entries.size()).getName();
    }

    public void moveTick(Scene scene, int fromTick, int toTick, EditorSession session) {
        if (scene == null || fromTick == toTick) {
            return;
        }
        for (SceneTrackType type : SceneTrackType.values()) {
            Track<? extends Keyframe> track = scene.getTrack(type);
            if (track == null) {
                continue;
            }
            List<? extends Keyframe> keyframes = List.copyOf(track.getKeyframes());
            for (Keyframe keyframe : keyframes) {
                if (keyframe.getTimeTicks() == fromTick) {
                    track.moveKeyframe(keyframe.getId(), toTick);
                } else if (keyframe.getTimeTicks() == toTick) {
                    track.moveKeyframe(keyframe.getId(), fromTick);
                }
            }
        }
        if (session != null) {
            session.setCurrentTick(toTick);
            session.setCurrentGroup((toTick - 1) / 9 + 1);
        }
        sceneManager.markDirty(scene);
    }

    public void shiftRange(Scene scene, int fromTick, int toTick, int delta, EditorSession session) {
        if (scene == null || delta == 0) {
            return;
        }
        int start = Math.min(fromTick, toTick);
        int end = Math.max(fromTick, toTick);
        for (SceneTrackType type : SceneTrackType.values()) {
            Track<? extends Keyframe> track = scene.getTrack(type);
            if (track == null) {
                continue;
            }
            List<? extends Keyframe> keyframes = List.copyOf(track.getKeyframes());
            for (Keyframe keyframe : keyframes) {
                if (keyframe.getTimeTicks() >= start && keyframe.getTimeTicks() <= end) {
                    track.moveKeyframe(keyframe.getId(), keyframe.getTimeTicks() + delta);
                }
            }
        }
        if (session != null) {
            int updated = Math.max(1, session.getCurrentTick() + delta);
            session.setCurrentTick(updated);
            session.setCurrentGroup((updated - 1) / 9 + 1);
        }
        sceneManager.markDirty(scene);
    }

    public void duplicateTickActions(Scene scene, int fromTick, int toTick, EditorSession session) {
        if (scene == null || fromTick == toTick) {
            return;
        }
        for (SceneTrackType type : SceneTrackType.values()) {
            Track<? extends Keyframe> track = scene.getTrack(type);
            if (track == null) {
                continue;
            }
            List<? extends Keyframe> keyframes = List.copyOf(track.getKeyframes());
            for (Keyframe keyframe : keyframes) {
                if (keyframe.getTimeTicks() == fromTick) {
                    Keyframe copy = cloneKeyframeAtTick(keyframe, toTick);
                    if (copy != null) {
                        @SuppressWarnings("unchecked")
                        Track<Keyframe> mutableTrack = (Track<Keyframe>) track;
                        mutableTrack.addKeyframe(copy);
                    }
                }
            }
        }
        if (session != null) {
            session.setCurrentTick(toTick);
            session.setCurrentGroup((toTick - 1) / 9 + 1);
        }
        sceneManager.markDirty(scene);
    }

    private Keyframe cloneKeyframeAtTick(Keyframe keyframe, int timeTicks) {
        if (keyframe instanceof CameraKeyframe camera) {
            return new CameraKeyframe(null, timeTicks, cloneTransform(camera.getTransform()),
                    camera.getSmoothingMode(), camera.isInstant(), cloneLookAt(camera.getLookAt()));
        }
        if (keyframe instanceof CommandKeyframe command) {
            return new CommandKeyframe(null, timeTicks, List.copyOf(command.getCommands()),
                    command.getExecutorMode(), command.isAllowGlobal());
        }
        if (keyframe instanceof ModelKeyframe model) {
            ModelKeyframe copy = new ModelKeyframe(null, timeTicks, model.getAction());
            copy.setModelId(model.getModelId());
            copy.setModelEntry(model.getModelEntry());
            copy.setEntityRef(null);
            copy.setAnimationId(model.getAnimationId());
            copy.setLoop(model.isLoop());
            copy.setSpeed(model.getSpeed());
            copy.setSpawnTransform(cloneTransform(model.getSpawnTransform()));
            return copy;
        }
        if (keyframe instanceof ActionBarKeyframe actionBar) {
            return new ActionBarKeyframe(null, timeTicks, actionBar.getText(), actionBar.getDurationTicks());
        }
        if (keyframe instanceof ParticleKeyframe particle) {
            return new ParticleKeyframe(null, timeTicks, particle.getParticleId(), cloneTransform(particle.getTransform()));
        }
        if (keyframe instanceof SoundKeyframe sound) {
            return new SoundKeyframe(null, timeTicks, sound.getSoundId(), sound.getVolume(), sound.getPitch(),
                    cloneTransform(sound.getTransform()));
        }
        if (keyframe instanceof BlockIllusionKeyframe block) {
            return new BlockIllusionKeyframe(null, timeTicks, block.getMaterial(), cloneTransform(block.getTransform()));
        }
        return null;
    }

    private void clearConfirm(EditorSession session) {
        session.setConfirmAction(null);
        session.setConfirmTrack(null);
        session.setConfirmKeyframeId(null);
        session.setConfirmCommandIndex(null);
    }

    private Transform cloneTransform(Transform transform) {
        if (transform == null) {
            return null;
        }
        return new Transform(transform.getX(), transform.getY(), transform.getZ(), transform.getYaw(), transform.getPitch(),
                transform.getWorldName());
    }

    private LookAtTarget cloneLookAt(LookAtTarget lookAt) {
        if (lookAt == null) {
            return LookAtTarget.none();
        }
        Transform position = cloneTransform(lookAt.getPosition());
        return new LookAtTarget(lookAt.getMode(), position, lookAt.getEntityId());
    }

    private void clearTick(Scene scene, int tick) {
        for (SceneTrackType type : SceneTrackType.values()) {
            Track<? extends Keyframe> track = scene.getTrack(type);
            if (track == null) {
                continue;
            }
            List<? extends Keyframe> keyframes = List.copyOf(track.getKeyframes());
            for (Keyframe keyframe : keyframes) {
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
            Track<? extends Keyframe> track = scene.getTrack(type);
            if (track != null) {
                track.clear();
            }
        }
    }

    private void clearEffects(Scene scene, int tick) {
        clearTrackAtTick(scene.getTrack(SceneTrackType.PARTICLE), tick);
        clearTrackAtTick(scene.getTrack(SceneTrackType.SOUND), tick);
        clearTrackAtTick(scene.getTrack(SceneTrackType.BLOCK_ILLUSION), tick);
    }

    private void clearTrackAtTick(Track<? extends Keyframe> track, int tick) {
        if (track == null) {
            return;
        }
        List<? extends Keyframe> keyframes = List.copyOf(track.getKeyframes());
        for (Keyframe keyframe : keyframes) {
            if (keyframe.getTimeTicks() == tick) {
                track.removeKeyframe(keyframe.getId());
            }
        }
    }

    public void clearLastSelectedScene(UUID playerId, String sceneName) {
        if (playerId == null || sceneName == null) {
            return;
        }
        String selected = lastSelectedSceneByPlayer.get(playerId);
        if (selected != null && selected.equalsIgnoreCase(sceneName)) {
            lastSelectedSceneByPlayer.remove(playerId);
        }
    }


    public void clearLastSelectedSceneReferences(String sceneName) {
        if (sceneName == null) {
            return;
        }
        lastSelectedSceneByPlayer.entrySet().removeIf(e -> e.getValue() != null && e.getValue().equalsIgnoreCase(sceneName));
    }

    public void forceCloseEditorsForScene(String sceneName) {
        for (var entry : new java.util.HashMap<>(editorSessionManager.getSessionsView()).entrySet()) {
            EditorSession session = entry.getValue();
            if (session != null && session.getScene() != null && session.getScene().getName().equalsIgnoreCase(sceneName)) {
                Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    closeEditor(player, session);
                    Text.send(player, "&c" + "Scene was deleted; editor closed.");
                }
                editorSessionManager.removeSession(entry.getKey());
                clearLastSelectedScene(entry.getKey(), sceneName);
            }
        }
    }


    private enum MainMenuAction { EDIT, CREATE, DELETE, RENAME, DUPLICATE }

    private enum MainMenuPromptType { FILTER, RENAME, DUPLICATE }

    private static final class MainMenuState {
        private int page;
        private String filter = "";
        private MainMenuAction action = MainMenuAction.EDIT;
        public int page() { return page; }
        public void setPage(int page) { this.page = Math.max(0, page); }
        public String filter() { return filter == null ? "" : filter; }
        public void setFilter(String filter) { this.filter = filter == null ? "" : filter; }
        public MainMenuAction action() { return action; }
        public void setAction(MainMenuAction action) { this.action = action == null ? MainMenuAction.EDIT : action; }
    }

    private record MainMenuPrompt(MainMenuPromptType type, String sceneName) {}

}

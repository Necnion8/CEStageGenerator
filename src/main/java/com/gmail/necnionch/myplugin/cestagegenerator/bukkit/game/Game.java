package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game;

import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class Game {

    private final String name;
    private final GameManager manager;
    private GameSetting setting;
    private @Nullable World world;
    private @Nullable String editingName;

    public Game(GameManager manager, String name, @Nullable GameSetting setting) {
        name = name.toLowerCase(Locale.ROOT);
        this.name = name;
        this.setting = setting != null ? setting : new GameSetting();
        this.manager = manager;
    }

    public String getName() {
        return name;
    }

    public Logger getLogger() {
        return manager.getLogger();
    }


    public GameSetting getSetting() {
        return setting;
    }

    public void setSetting(@Nullable GameSetting setting) {
        this.setting = setting != null ? setting : new GameSetting();
    }

    public List<String> stageNames() {
        return setting.stageNames();
    }


    public @Nullable World getWorld() {
        return world;
    }

    public void setWorld(@Nullable World world) {
        this.world = world;
    }

    public @Nullable World loadWorld(boolean create) throws IllegalStateException {
        if (world != null)
            unloadWorld();

        this.world = manager.loadWorld(this, create);
        return world;
    }

    public void unloadWorld() {
        manager.unloadWorld(this);
        this.editingName = null;
        this.world = null;
    }

    public @Nullable String getEditingStageName() {
        return editingName;
    }

    public void setEditingStageName(@Nullable String stageName) {
        this.editingName = stageName;
    }

    public boolean isWorldEditing() {
        return editingName != null;
    }

    public File getOpenedWorldFolderFile() {
        return manager.getOpenedWorldFolderFile(this);
    }

    public File getBackupWorldFolderFile(String stageName) {
        return manager.getBackupWorldFolderFile(this, stageName);
    }

    public boolean existsOpenedWorldFolder() {
        return manager.existsOpenedWorldFolder(this);
    }

    public boolean existsBackupWorldFolder(String stageName) {
        return manager.existsBackupWorldFolder(this, stageName);
    }


    public CompletableFuture<Void> cleanOpenedWorld() throws IllegalStateException {
        return manager.cleanOpenedWorld(this);
    }

    public CompletableFuture<Void> backupWorld(String stageName) throws IllegalArgumentException, IllegalStateException {
        return manager.backupWorld(this, stageName);
    }

    public CompletableFuture<Void> restoreWorld(String stageName) throws IllegalArgumentException, IllegalStateException {
        return manager.restoreWorld(this, stageName);
    }

    public void saveWorld() {
        if (world != null)
            manager.saveWorld(this, world);
    }

    public void restoreNPCs() {
        if (world != null)
            manager.restoreNPCs(world);
    }

}

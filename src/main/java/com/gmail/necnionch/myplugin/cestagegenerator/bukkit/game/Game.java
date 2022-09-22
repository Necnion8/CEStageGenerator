package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game;

import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.config.StageConfig;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class Game {

    private final String name;
    private final GameManager manager;
    private GameSetting setting;
    private @Nullable World world;
    private @Nullable StageConfig currentStageConfig;
    private boolean editing;

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

    public void saveSettings() {
        manager.save();
    }

    public List<String> stageNames() {
        return setting.stageNames();
    }


    public @Nullable World getWorld() {
        return world;
    }

    public @Nullable StageConfig getCurrentStageConfig() {
        return currentStageConfig;
    }

    public void setWorld(@Nullable World world) {
        this.world = world;
        if (this.world != null) {
            this.currentStageConfig = new StageConfig(manager.getPlugin(), world.getWorldFolder());
            this.currentStageConfig.load();
        } else {
            this.currentStageConfig = null;
        }
    }

    public @Nullable World loadWorld(boolean create) throws IllegalStateException {
        if (world != null)
            unloadWorld();

        this.world = manager.loadWorld(this, create);
        if (this.world != null) {
            this.currentStageConfig = new StageConfig(manager.getPlugin(), world.getWorldFolder());
            this.currentStageConfig.load();
        }
        return world;
    }

    public void unloadWorld() {
        manager.unloadWorld(this);
        this.editing = false;
        this.world = null;
        this.currentStageConfig = null;
    }

    public void setWorldEditing(boolean editing) {
        this.editing = editing;
    }

    public boolean isWorldEditing() {
        return editing;
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
            manager.saveWorld(this, world, Objects.requireNonNull(currentStageConfig).getStageName());
    }

    public void restoreNPCs() {
        if (world != null)
            manager.restoreNPCs(world);
    }

}

package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game;

import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.StageGeneratorPlugin;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.config.GameConfig;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.config.StageConfig;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.generator.VoidGenerator;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.hooks.CitizensBridge;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GameManager implements Listener {
    private final StageGeneratorPlugin plugin;
    private final CitizensBridge citizens;
    private final GameConfig config;
    private final Map<String, Game> games = Maps.newHashMap();
    private final File worldContainer;
    private final File worldBackupContainer;
    private final Set<String> processingFiles = Sets.newConcurrentHashSet();

    public GameManager(StageGeneratorPlugin plugin, CitizensBridge citizens) {
        this.plugin = plugin;
        this.citizens = citizens;
        this.config = new GameConfig(plugin);
        this.worldContainer = new File(Bukkit.getWorldContainer(), "stgen");
        this.worldBackupContainer = new File(plugin.getDataFolder(), "stage_backups");
    }

    public Logger getLogger() {
        return plugin.getLogger();
    }

    public File getWorldContainer() {
        return worldContainer;
    }

    public File getWorldBackupContainer() {
        return worldBackupContainer;
    }

    public String getWorldContainerWithGameName(String gameName) {
        String[] split = worldContainer.toString().split(Pattern.quote(System.getProperty("file.separator")));
        String worldFullName = String.join("/", split);

        if (worldFullName.startsWith("./"))
            worldFullName = worldFullName.substring(2);
        worldFullName = worldFullName.replaceAll("/+", "/");

        return worldFullName + "/" + gameName;
    }

    public Map<String, Game> games() {
        return games;
    }

    public @Nullable Game getGameByName(String name) {
        return games.get(name.toLowerCase(Locale.ROOT));
    }


    public Game createGame(String name, GameSetting setting) {
        name = name.toLowerCase(Locale.ROOT);
        if (games.containsKey(name))
            throw new IllegalArgumentException("Already exists game name");


        Game game = new Game(this, name, setting);
        games.put(name, game);
        save();
        return game;
    }

    public Game createGame(String name) {
        name = name.toLowerCase(Locale.ROOT);
        if (games.containsKey(name))
            throw new IllegalArgumentException("Already exists game name");

        Game game = new Game(this, name, null);
        games.put(name, game);
        save();
        return game;
    }

    public void deleteGame(String name) {
        Game game = games.remove(name.toLowerCase(Locale.ROOT));
        if (game == null)
            throw new IllegalArgumentException("Not exists game: " + name.toLowerCase(Locale.ROOT));

        try {
            game.unloadWorld();
            game.cleanOpenedWorld();

        } catch (Throwable e) {
            e.printStackTrace();
        }

        save();
    }


    public void init() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void cleanup() {
        games.values().forEach(game -> {
            try {
                game.unloadWorld();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });

        games.clear();
        config.cleanup();
        HandlerList.unregisterAll(this);
    }

    public void save() {
        config.saveGames(games.values().stream()
                .collect(Collectors.toMap(Game::getName, Game::getSetting)));
    }

    public void reload(boolean loadWorlds) {
        reloadGames();
        if (loadWorlds)
            loadOpenedWorlds();
    }

    public void reloadGames() {
        Map<String, Game> newGames = Maps.newHashMap();
        config.loadGames().forEach((name, setting) -> {
            Game game = games.remove(name);

            if (game == null) {  // new
                game = new Game(this, name, setting);
            } else {  // update
                game.setSetting(setting);
            }

            newGames.put(game.getName(), game);
        });

        games.clear();  // delete olds
        games.putAll(newGames);

        getLogger().info("Loaded " + games.size() + " games");
    }

    public void loadOpenedWorlds() {
        Optional.ofNullable(getWorldContainer().listFiles(File::isDirectory)).ifPresent(files -> {
            for (File worldFolder : files) {
                String gameName = worldFolder.getName();

                World world = Bukkit.getWorld(getWorldContainerWithGameName(gameName));
                Game game = games.get(gameName);

                if (game == null) {
                    if (world != null) {
                        getLogger().warning(String.format(
                                "%s world is loaded, but %s game does not exists. Unload it.",
                                world.getName(), gameName
                        ));
                        Bukkit.unloadWorld(world, true);
                    }
                    continue;
                }

                if (world != null) {  // loaded
                    game.setWorld(world);
                } else {  // start load
                    game.loadWorld(false);
                }
            }
        });
    }

    public File getOpenedWorldFolderFile(Game game) {
        return new File(worldContainer, game.getName());
    }

    public File getBackupWorldFolderFile(Game game, String stageName) {
        return Paths.get(worldBackupContainer.toString(), game.getName(), stageName).toFile();
    }

    boolean existsOpenedWorldFolder(Game game) {
        return new File(getOpenedWorldFolderFile(game), "level.dat").isFile();
    }

    boolean existsBackupWorldFolder(Game game, String stageName) {
        return new File(getBackupWorldFolderFile(game, stageName), "level.dat").isFile();
    }

    @Nullable World loadWorld(Game game, boolean create) throws IllegalStateException {
        File worldFolder = getOpenedWorldFolderFile(game);
        boolean exists = existsOpenedWorldFolder(game);
        if (exists || create) {
            String worldFullName = getWorldContainerWithGameName(game.getName());

            World world = Bukkit.getWorld(worldFullName);
            if (world == null) {
                if (processingFiles.contains(worldFolder.toString()))
                    throw new IllegalStateException("Already processing directory");

                if (exists) {
                    getLogger().info("World Loading: " + worldFullName);
                } else {
                    getLogger().info("World Creating: " + worldFullName);
                }

                world = WorldCreator
                        .name(worldFullName)
                        .generator(new VoidGenerator())
                        .generateStructures(false)
                        .createWorld();

                getLogger().info("World Loaded: " + world.getName());

                world.setKeepSpawnInMemory(false);
                world.setAutoSave(false);
                world.setSpawnLocation(0, 64, 0);

                if (!exists) {
                    world.setTime(6000);
                    world.setGameRule(GameRule.COMMAND_BLOCK_OUTPUT, false);
                    world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, false);
                    world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                    world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
                    world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                    world.setGameRule(GameRule.KEEP_INVENTORY, true);
                    world.setGameRule(GameRule.MOB_GRIEFING, false);
                    world.getBlockAt(0, 63, 0).setType(Material.BEDROCK, false);
                    world.save();
                }
            }

            restoreNPCs(world);
            return world;
        }
        return null;
    }

    void unloadWorld(Game game) {
        Optional.ofNullable(game.getWorld()).ifPresent(world -> {
            world.getPlayers().forEach(p -> p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation()));
            Bukkit.unloadWorld(world, true);
            getLogger().info("World Unloaded: " + world.getName());
        });
    }

    CompletableFuture<Void> cleanOpenedWorld(Game game) throws IllegalStateException {
        CompletableFuture<Void> f = new CompletableFuture<>();
        File worldFolder = getOpenedWorldFolderFile(game);

        if (processingFiles.contains(worldFolder.toString()))
            throw new IllegalStateException("Already processing directory");

        processingFiles.add(worldFolder.toString());
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                FileUtils.deleteDirectory(worldFolder);
            } catch (IOException e) {
                getLogger().severe("Failed to clean '" + worldFolder + "' folder: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> f.completeExceptionally(e));
                return;
            } finally {
                processingFiles.remove(worldFolder.toString());
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> f.complete(null));
        });

        return f;
    }

    CompletableFuture<Void> backupWorld(Game game, String stageName) throws IllegalArgumentException, IllegalStateException {
        if (!existsOpenedWorldFolder(game))
            throw new IllegalArgumentException("Not exists source world: " + game.getName());

        CompletableFuture<Void> f = new CompletableFuture<>();
        File source = getOpenedWorldFolderFile(game);
        File dest = getBackupWorldFolderFile(game, stageName);

        if (processingFiles.contains(dest.toString()))
            throw new IllegalStateException("Already processing directory");

        processingFiles.add(dest.toString());
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {

            long delay = System.currentTimeMillis();
            try {
                if (dest.exists())
                    FileUtils.deleteDirectory(dest);
                FileUtils.copyDirectory(source, dest);

            } catch (IOException e) {
                getLogger().severe("Failed to backup '" + source + "' to '" + dest + "': " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> f.completeExceptionally(e));
                return;

            } finally {
                processingFiles.remove(dest.toString());
            }
            delay = System.currentTimeMillis() - delay;
            getLogger().info("Completed stage backup (" + delay + "ms): " + game.getName() + "/" + stageName);
            plugin.getServer().getScheduler().runTask(plugin, () -> f.complete(null));
        });

        return f;
    }

    CompletableFuture<Void> restoreWorld(Game game, String stageName) throws IllegalArgumentException, IllegalStateException {
        if (!existsBackupWorldFolder(game, stageName))
            throw new IllegalArgumentException("Not exists backup stage world: " + game.getName() + "/" + stageName);

        if (Bukkit.getWorlds().contains(game.getWorld()))
            throw new IllegalArgumentException("Not unloaded opened world: " + game.getWorld().getName());

        CompletableFuture<Void> f = new CompletableFuture<>();
        File source = getBackupWorldFolderFile(game, stageName);
        File dest = getOpenedWorldFolderFile(game);

        if (processingFiles.contains(dest.toString()))
            throw new IllegalStateException("Already processing directory");

        processingFiles.add(dest.toString());
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {

            long delay = System.currentTimeMillis();
            try {
                if (dest.exists())
                    FileUtils.deleteDirectory(dest);
                FileUtils.copyDirectory(source, dest);

            } catch (IOException e) {
                getLogger().severe("Failed to backup '" + source + "' to '" + dest + "': " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> f.completeExceptionally(e));
                return;

            } finally {
                processingFiles.remove(dest.toString());
            }
            delay = System.currentTimeMillis() - delay;
            getLogger().info("Completed stage restore (" + delay + "ms): " + game.getName() + "/" + stageName);
            plugin.getServer().getScheduler().runTask(plugin, () -> f.complete(null));
        });

        return f;
    }

    void saveWorld(Game game, World world) {
        if (!world.equals(game.getWorld()))
            throw new IllegalArgumentException("Not game world");

        world.save();
        saveNPCToWorld(world);
    }

    private void saveNPCToWorld(World world) {
        if (!citizens.isAvailable())
            return;

        StageConfig config = new StageConfig(plugin, world.getWorldFolder());
        long count = world.getEntities().stream()
                .map(entity -> citizens.getNPCRegistry().getNPC(entity))
                .filter(Objects::nonNull)
                .peek(config::addNPC)
                .count();
        if (count >= 1) {
            config.save();
        } else if (config.getFilePath().isFile()) {
            config.getFilePath().delete();
        }
    }

    void restoreNPCs(World world) {
        if (!citizens.isAvailable())
            return;
        StageConfig config = new StageConfig(plugin, world.getWorldFolder());
        if (config.getFilePath().isFile())
            config.restoreNPCs(citizens.getNPCRegistry(), world);
    }

}

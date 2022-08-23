package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.config;

import com.gmail.necnionch.myplugin.cestagegenerator.common.BukkitConfigDriver;
import com.google.common.collect.Maps;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class MainConfig extends BukkitConfigDriver {
    private final Map<String, GameEntry> games = Maps.newHashMap();

    public MainConfig(JavaPlugin plugin) {
        super(plugin, "config.yml", "empty.yml");
    }

    @Override
    public boolean onLoaded(FileConfiguration config) {
        if (super.onLoaded(config)) {
            games.clear();
            ConfigurationSection tmp = config.getConfigurationSection("games");
            if (tmp != null) {
                for (String gameName : tmp.getKeys(false)) {
                    GameEntry game;
                    try {
                        game = GameEntry.deserialize(gameName, tmp.getConfigurationSection(gameName));
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    games.put(gameName, game);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean save() {
        config.set("games", null);  // clear
        ConfigurationSection games = config.createSection("games");
        for (GameEntry game : this.games.values()) {
            game.serialize(games.createSection(game.getGameName()));
        }

        return super.save();
    }

    public @Nullable GameEntry getGameByName(String gameName) {
        return games.get(gameName);
    }

    public Map<String, GameEntry> games() {
        return games;
    }

}

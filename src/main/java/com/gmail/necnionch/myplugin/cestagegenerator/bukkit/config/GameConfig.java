package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.config;

import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.GameSetting;
import com.gmail.necnionch.myplugin.cestagegenerator.common.BukkitConfigDriver;
import com.google.common.collect.Maps;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class GameConfig extends BukkitConfigDriver {
    private Map<String, Object> fails;

    public GameConfig(JavaPlugin plugin) {
        super(plugin, "games.yml", "empty.yml");
    }

    public void cleanup() {
        fails = null;
    }

    public Map<String, GameSetting> loadGames() {
        fails = null;
        load();
        Map<String, GameSetting> games = Maps.newHashMap();

        for (String gameName : config.getKeys(false)) {
            GameSetting game;
            try {
                game = GameSetting.deserialize(config.getConfigurationSection(gameName));
            } catch (Exception e) {
                fails.put(gameName, config.get(gameName));
                e.printStackTrace();
                continue;
            }
            games.put(gameName, game);
        }
        return games;
    }

    public void saveGames(Map<String, GameSetting> games) {
        config = new YamlConfiguration();
        games.forEach((name, setting) -> setting.serialize(config.createSection(name)));

        if (fails != null) {
            fails.forEach((key, obj) -> config.set("fails." + key, obj));
        }
        save();
    }

}

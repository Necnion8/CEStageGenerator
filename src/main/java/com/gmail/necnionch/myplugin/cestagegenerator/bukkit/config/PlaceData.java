package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.config;

import com.gmail.necnionch.myplugin.cestagegenerator.common.BukkitConfigDriver;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class PlaceData extends BukkitConfigDriver {
    private final JavaPlugin plugin;
    private boolean changed;
    private final Map<String, Set<String>> blocks = Maps.newHashMap();
    private @Nullable BukkitTask saveTask;

    public PlaceData(JavaPlugin plugin) {
        super(plugin, "placedata.yml", "empty.yml");
        this.plugin = plugin;
    }

    @Override
    public boolean onLoaded(FileConfiguration config) {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
        if (super.onLoaded(config)) {
            blocks.clear();
            for (String gameName : config.getKeys(false)) {
                blocks.put(gameName, Sets.newHashSet(config.getStringList(gameName)));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean save() {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
        changed = false;
        config = new YamlConfiguration();
        this.blocks.forEach((gameName, blocks) -> {
            if (blocks.isEmpty())
                return;
            config.set(gameName, Lists.newArrayList(blocks));
        });
        return super.save();
    }

    public void put(String gameName, Location location) {
        String loc = location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        if (blocks.containsKey(gameName)) {
            blocks.get(gameName).add(loc);
        } else {
            blocks.put(gameName, Sets.newHashSet(loc));
        }
        changed = true;
        queue();
    }

    public void remove(String gameName, Location location) {
        String loc = location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        if (blocks.containsKey(gameName)) {
            blocks.get(gameName).remove(loc);
            changed = true;
            queue();
        }
    }

    public void removeAll(String gameName) {
        if (blocks.remove(gameName) != null) {
            changed = true;
            queue();
        }
    }

    public boolean contains(Location location) {
        String loc = location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        return this.blocks.values().stream().anyMatch(blocks -> blocks.contains(loc));
    }


    private void queue() {
        if (!changed)
            return;

        if (saveTask != null)
            saveTask.cancel();

        saveTask = plugin.getServer().getScheduler().runTaskLater(plugin, this::save, 1 * 60 * 20);
    }

}

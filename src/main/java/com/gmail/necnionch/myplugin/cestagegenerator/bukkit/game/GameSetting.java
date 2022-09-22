package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GameSetting {
    private final List<String> stageNames;
    private final MaterialList placeBlacklists;
    private final MaterialList breakWhitelists;

    public GameSetting(List<String> stageNames) {
        this.stageNames = stageNames;
        this.placeBlacklists = new MaterialList();
        this.breakWhitelists = new MaterialList();
    }

    public GameSetting() {
        this(Lists.newArrayList());
    }

    public List<String> stageNames() {
        return stageNames;
    }

    public MaterialList getPlaceBlacklists() {
        return placeBlacklists;
    }

    public MaterialList getBreakWhitelists() {
        return breakWhitelists;
    }

    public void serialize(ConfigurationSection config) {
        config.set("stages", stageNames);
        config.set("blacklist-places", placeBlacklists.stream().map(Enum::name).collect(Collectors.toList()));
        config.set("whitelist-breaks", breakWhitelists.stream().map(Enum::name).collect(Collectors.toList()));
    }

    public static GameSetting deserialize(ConfigurationSection config) {
        List<String> stages = config.getStringList("stages");
//        boolean protectBlocks = config.getBoolean("protect-blocks");

        Set<Material> places = Sets.newLinkedHashSet();
        config.getStringList("blacklist-places").forEach(mName -> {
            try {
                places.add(Material.valueOf(mName));
            } catch (IllegalArgumentException ignored) {}
        });
        Set<Material> breaks = Sets.newLinkedHashSet();
        config.getStringList("whitelist-breaks").forEach(mName -> {
            try {
                breaks.add(Material.valueOf(mName));
            } catch (IllegalArgumentException ignored) {}
        });

        GameSetting game = new GameSetting(stages);
        game.placeBlacklists.addAll(places);
        game.breakWhitelists.addAll(breaks);
        return game;
    }


    public static class MaterialList extends LinkedHashSet<Material> {
        public boolean isAll() {
            return this.isEmpty();
        }

        public boolean isListed(Material type) {
            return isAll() || contains(type);
        }
    }

}

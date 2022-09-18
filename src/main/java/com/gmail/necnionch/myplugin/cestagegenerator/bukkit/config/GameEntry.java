package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.config;

import com.google.common.collect.Sets;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GameEntry {
    private final String gameName;
    private final List<String> stageNames;
    private @Nullable String ignoreEntityTag;
    private String worldName;
    private Location pos1;
    private Location pos2;
    private @Nullable CuboidRegion cacheRegion;
    private final Set<Material> placeBlacklists;
    private final Set<Material> breakWhitelists;
    private final Set<Material> replaceFilters;
    private boolean protectBlocks;

    public GameEntry(String gameName, String worldName, Location pos1, Location pos2, List<String> stageNames, @Nullable String ignoreEntityTag, boolean protectBlocks) {
        this.gameName = gameName;
        this.worldName = worldName;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.stageNames = stageNames;
        this.ignoreEntityTag = ignoreEntityTag;
        this.placeBlacklists = Sets.newHashSet();
        this.breakWhitelists = Sets.newHashSet();
        this.replaceFilters = Sets.newHashSet();
        this.protectBlocks = protectBlocks;
    }

    public String getGameName() {
        return gameName;
    }

    public String getWorldName() {
        return worldName;
    }

    public @Nullable World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public CuboidRegion getRegion() {
        if (cacheRegion == null)
            cacheRegion = new CuboidRegion(BlockVector3.at(pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ()), BlockVector3.at(pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ()));
        return cacheRegion;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
        cacheRegion = null;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
        cacheRegion = null;
    }

    public List<String> stageNames() {
        return stageNames;
    }

    public @Nullable String getIgnoreEntityTag() {
        return ignoreEntityTag;
    }

    public boolean isBlacklistedPlace(Material material) {
        return placeBlacklists.contains(material);
    }

    public boolean isWhitelistedBreak(Material material) {
        return breakWhitelists.contains(material);
    }

    public void setIgnoreEntityTag(@Nullable String ignoreEntityTag) {
        this.ignoreEntityTag = ignoreEntityTag;
    }

    public Set<Material> placeBlacklists() {
        return placeBlacklists;
    }

    public Set<Material> breakWhitelists() {
        return breakWhitelists;
    }

    public Set<Material> replaceFilters() {
        return replaceFilters;
    }

    public boolean isProtectBlocks() {
        return protectBlocks;
    }

    public void setProtectBlocks(boolean protect) {
        this.protectBlocks = protect;
    }

    public void serialize(ConfigurationSection config) {
        config.set("world", worldName);
        config.set("pos1.x", pos1.getBlockX());
        config.set("pos1.y", pos1.getBlockY());
        config.set("pos1.z", pos1.getBlockZ());
        config.set("pos2.x", pos2.getBlockX());
        config.set("pos2.y", pos2.getBlockY());
        config.set("pos2.z", pos2.getBlockZ());
        config.set("stages", stageNames);
        config.set("ignore-entity-tag", ignoreEntityTag);
        config.set("blacklist-places", placeBlacklists.stream().map(Enum::name).collect(Collectors.toList()));
        config.set("whitelist-breaks", breakWhitelists.stream().map(Enum::name).collect(Collectors.toList()));
        config.set("replace-blocks", replaceFilters.stream().map(Enum::name).collect(Collectors.toList()));
        config.set("protect-blocks", protectBlocks);
    }

    public static GameEntry deserialize(String gameName, ConfigurationSection config) {
        String worldName = config.getString("world");
        Location pos1 = new Location(null, config.getDouble("pos1.x"), config.getDouble("pos1.y"), config.getDouble("pos1.z"));
        Location pos2 = new Location(null, config.getDouble("pos2.x"), config.getDouble("pos2.y"), config.getDouble("pos2.z"));
        List<String> stages = config.getStringList("stages");
        String ignoreEntityTag = config.getString("ignore-entity-tag");
        boolean protectBlocks = config.getBoolean("protect-blocks");

        Set<Material> places = Sets.newHashSet();
        Set<Material> breaks = Sets.newHashSet();
        Set<Material> filters = Sets.newHashSet();
        config.getStringList("blacklist-places").forEach(mName -> {
            try {
                places.add(Material.valueOf(mName));
            } catch (IllegalArgumentException ignored) {}
        });
        config.getStringList("whitelist-breaks").forEach(mName -> {
            try {
                breaks.add(Material.valueOf(mName));
            } catch (IllegalArgumentException ignored) {}
        });
        config.getStringList("replace-blocks").forEach(mName -> {
            try {
                filters.add(Material.valueOf(mName));
            } catch (IllegalArgumentException ignored) {}
        });

        GameEntry game = new GameEntry(gameName, worldName, pos1, pos2, stages, ignoreEntityTag, protectBlocks);
        game.placeBlacklists.addAll(places);
        game.breakWhitelists.addAll(breaks);
        game.replaceFilters.addAll(filters);
        return game;
    }

}

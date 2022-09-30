package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game;

import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class GameSetting {
    private final List<String> stageNames;
    private final MaterialList places;
    private final MaterialList breaks;
    private ExplosionBreakingMode explosionBreakingMode = ExplosionBreakingMode.BREAKABLE;

    public GameSetting(List<String> stageNames) {
        this.stageNames = stageNames;
        this.places = new MaterialList(MaterialList.Action.ALLOW);
        this.breaks = new MaterialList(MaterialList.Action.DENY);
    }

    public GameSetting(List<String> stageNames, MaterialList places, MaterialList breaks, ExplosionBreakingMode mode) {
        this.stageNames = stageNames;
        this.places = places;
        this.breaks = breaks;
        this.explosionBreakingMode = mode;
    }

    public GameSetting() {
        this(Lists.newArrayList());
    }

    public List<String> stageNames() {
        return stageNames;
    }

    public MaterialList getPlacesList() {
        return places;
    }

    public MaterialList getBreaksList() {
        return breaks;
    }

    public ExplosionBreakingMode getExplosionBreakingMode() {
        return explosionBreakingMode;
    }

    public void setExplosionBreakingMode(ExplosionBreakingMode mode) {
        this.explosionBreakingMode = mode;
    }

    public void serialize(ConfigurationSection config) {
        config.set("stages", stageNames);
        config.set("place-types", places.stream().map(Enum::name).collect(Collectors.toList()));
        config.set("place-mode", places.getAction().name().toLowerCase(Locale.ROOT));
        config.set("break-types", breaks.stream().map(Enum::name).collect(Collectors.toList()));
        config.set("break-mode", breaks.getAction().name().toLowerCase(Locale.ROOT));
        config.set("break-by-explosion", explosionBreakingMode.name().toLowerCase(Locale.ROOT));
    }

    public static GameSetting deserialize(ConfigurationSection config) {
        List<String> stages = config.getStringList("stages");

        MaterialList.Action action;
        try {
            action = MaterialList.Action.valueOf(config.getString("place-mode", "").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            action = MaterialList.Action.ALLOW;
        }
        MaterialList places = new MaterialList(action);
        config.getStringList("place-types").forEach(mName -> {
            try {
                places.add(Material.valueOf(mName));
            } catch (IllegalArgumentException ignored) {
            }
        });

        try {
            action = MaterialList.Action.valueOf(config.getString("break-mode", "").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            action = MaterialList.Action.DENY;
        }
        MaterialList breaks = new MaterialList(action);
        config.getStringList("break-types").forEach(mName -> {
            try {
                breaks.add(Material.valueOf(mName));
            } catch (IllegalArgumentException ignored) {
            }
        });

        ExplosionBreakingMode mode;
        try {
            mode = ExplosionBreakingMode.valueOf(config.getString("break-by-explosion", "").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            mode = ExplosionBreakingMode.PROTECT;
        }

        return new GameSetting(stages, places, breaks, mode);
    }


}

package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.hooks;

import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.StageGeneratorPlugin;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.config.StageConfig;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.Game;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public class CitizensBridge {
    private final StageGeneratorPlugin plugin;
    private boolean available;

    public CitizensBridge(StageGeneratorPlugin owner) {
        plugin = owner;
    }

    public boolean hook(@Nullable Plugin plugin) {
        available = false;
        if (plugin != null && plugin.isEnabled()) {
            try {
                Class.forName("net.citizensnpcs.api.CitizensAPI");
            } catch (ClassNotFoundException ignored) {
                return false;
            }
            available = true;
        }

        if (available)
            try {
                onEnable();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        return available;
    }

    public void unhook() {
        available = false;
    }

    public NPCRegistry getNPCRegistry() {
        return (available) ? CitizensAPI.getNPCRegistry() : null;
    }

    public boolean isAvailable() {
        return available;
    }

    public void onEnable() {
        Collection<Game> games = plugin.getGameManager().getGames().values();
        for (Game game : games) {
            if (game.getWorld() != null && game.getCurrentStageConfig() != null) {
                game.getCurrentStageConfig().restoreNPCs(getNPCRegistry(), game.getWorld());
            }
        }
    }


    public void despawnNPCInWorld(World world) {
        getNPCRegistry().forEach(npc -> {
            if (!npc.isSpawned())
                return;
            try {
                Optional.ofNullable(npc.getStoredLocation())
                        .map(Location::getWorld)
                        .ifPresent(w -> {
                            if (world.getName().equals(w.getName()))
                                npc.despawn();
                        });
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    public void addNPCFromWorldToConfig(World world, StageConfig config) {
        world.getEntities().stream()
                .map(entity -> getNPCRegistry().getNPC(entity))
                .filter(Objects::nonNull)
                .forEach(config::addNPC);
    }

}

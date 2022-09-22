package com.gmail.necnionch.myplugin.cestagegenerator.bukkit;

import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.commands.MainCommand;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.GameManager;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.hooks.CitizensBridge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicBoolean;

public final class StageGeneratorPlugin extends JavaPlugin {
    private final CitizensBridge citizensBridge = new CitizensBridge(this);
    private final GameManager gameManager = new GameManager(this, citizensBridge);
    private final MainCommand mainCommand = new MainCommand(this, gameManager);

    @Override
    public void onEnable() {
        if (citizensBridge.hook(getServer().getPluginManager().getPlugin("Citizens")))
            getLogger().info("Hooked Citizens");

        gameManager.init();
        gameManager.reload(false);
        Panel.OWNER = this;
        mainCommand.registerCommands();

        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onEnable(PluginEnableEvent event) {
                if (!event.getPlugin().getName().equals("Citizens"))
                    return;

                HandlerList.unregisterAll(this);  // only ones
                if (citizensBridge.hook(getServer().getPluginManager().getPlugin("Citizens")))
                    getLogger().info("Hooked Citizens");
            }
        }, this);

        AtomicBoolean isLoaded = new AtomicBoolean();
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onEnable(PluginEnableEvent event) {
                if (!event.getPlugin().getName().equals("Multiverse-Core"))
                    return;

                HandlerList.unregisterAll(this);  // only ones

                if (!isLoaded.get()) {
                    isLoaded.set(true);
                    // delay load (load: STARTUP)
                    gameManager.loadOpenedWorlds();
                }
            }
        }, this);

        getServer().getScheduler().runTask(this, () -> {
            if (!isLoaded.get()) {
                isLoaded.set(true);
                gameManager.loadOpenedWorlds();
            }
        });
    }

    @Override
    public void onDisable() {
        Panel.destroyAll();
        mainCommand.unregisterCommands();
        gameManager.cleanup();
        citizensBridge.unhook();
        Panel.OWNER = null;
    }


}

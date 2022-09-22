package com.gmail.necnionch.myplugin.cestagegenerator.bukkit;

import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.GameManager;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.hooks.CitizensBridge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
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

        AtomicBoolean isLoaded = new AtomicBoolean();
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onLoad(WorldInitEvent event) {
                getLogger().warning("first world init");
                HandlerList.unregisterAll(this);  // only ones

                getLogger().info("on load " + isLoaded.get());
                if (!isLoaded.get()) {
                    isLoaded.set(true);
                    // delay load (load: STARTUP)
                    gameManager.loadOpenedWorlds();
                }
            }
        }, this);

        getServer().getScheduler().runTask(this, () -> {
            getLogger().info("on tick " + isLoaded.get());
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

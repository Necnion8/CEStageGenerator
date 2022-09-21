package com.gmail.necnionch.myplugin.cestagegenerator.bukkit;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class StageGeneratorPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

    }


}

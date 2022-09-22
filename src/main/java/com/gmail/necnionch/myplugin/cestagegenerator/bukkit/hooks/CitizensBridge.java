package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.hooks;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class CitizensBridge {
    private boolean available;

    public CitizensBridge(Plugin owner) {
//        this.plugin = owner;
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

}

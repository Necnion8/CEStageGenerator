package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

public class SilentCommandSender implements Closeable {
    private final CommandMinecart commandMinecart;

    public SilentCommandSender(@Nullable Player player) {
        World world;
        Location location;
        if (player != null) {
            world = player.getWorld();
            location = player.getLocation().clone();
            location.setY(-256);
        } else {
            world = Bukkit.getWorlds().get(0);
            location = new Location(world, 0, -256, 0);
        }

        commandMinecart = ((CommandMinecart) world.spawnEntity(location, EntityType.MINECART_COMMAND));
    }

    public boolean dispatchCommand(String commandLine) {
        return Bukkit.dispatchCommand(commandMinecart, commandLine);
    }

    @Override
    public void close() {
        commandMinecart.remove();
    }

    public CommandMinecart getCommandMinecart() {
        return commandMinecart;
    }

}

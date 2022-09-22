package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.config;

import com.gmail.necnionch.myplugin.cestagegenerator.common.BukkitConfigDriver;
import com.google.common.collect.Sets;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Optional;
import java.util.Set;

public class StageConfig extends BukkitConfigDriver {
    public StageConfig(JavaPlugin plugin, File worldFolder) {
        super(plugin, new File(worldFolder, "stage.yml").toString(), "empty.yml", false);
    }

    public void addNPC(NPC npc) {
        config = Optional.ofNullable(config).orElseGet(YamlConfiguration::new);
        Location loc = npc.getStoredLocation();
        config.set("npc." + npc.getId() + ".x", loc.getX());
        config.set("npc." + npc.getId() + ".y", loc.getY());
        config.set("npc." + npc.getId() + ".z", loc.getZ());
        config.set("npc." + npc.getId() + ".yaw", loc.getYaw());
        config.set("npc." + npc.getId() + ".pitch", loc.getPitch());
    }

    public void restoreNPCs(NPCRegistry registry, World world) {
        ConfigurationSection npcSection = config.getConfigurationSection("npc");
        if (npcSection == null)
            return;

        Set<Integer> placedNPCIds = Sets.newHashSet();

        for (String sNPCId : npcSection.getKeys(false)) {
            ConfigurationSection section = npcSection.getConfigurationSection(sNPCId);
            int npcId;
            try {
                npcId = Integer.parseInt(sNPCId);
            } catch (NumberFormatException e) {
                continue;
            }

            NPC npc = registry.getById(npcId);
            if (npc == null) {
                getLogger().warning("NPC " + npcId + " is not found");
                continue;
            }
            npc.despawn();
            npc.spawn(new Location(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"), ((float) section.getDouble("yaw")), ((float) section.getDouble("pitch"))));
            placedNPCIds.add(npcId);
        }

        for (NPC npc : registry) {
            if (placedNPCIds.contains(npc.getId()))
                continue;
            World w = npc.getStoredLocation().getWorld();
            if (w != null && world.getName().equals(w.getName())) {
                npc.despawn();
            }
        }

    }

}

package me.gamercoder215.mcsurvivors;

import me.gamercoder215.mcsurvivors.biome.MCSBiome;
import me.gamercoder215.mcsurvivors.biome.MCSBiomeManager;
import me.gamercoder215.mcsurvivors.commands.MCSCommands;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.logging.Logger;

public final class MCSCore extends JavaPlugin implements Listener {

    public static final class CancelHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return null;
        }
    }

    @NotNull
    public static Logger getPluginLogger() {
        return JavaPlugin.getPlugin(MCSCore.class).getLogger();
    }

    @NotNull
    public static File getPluginDataFolder() {
        return JavaPlugin.getPlugin(MCSCore.class).getDataFolder();
    }

    @NotNull
    public static String prefix() {
        return ChatColor.translateAlternateColorCodes('&',
                "&6[&eMCS-8&6]&r &a");
    }

    @NotNull
    public static File getBiomesFolder() {
        File f = new File(getPluginDataFolder(), "biomes");
        if (!f.exists()) f.mkdir();
        return f;
    }

    public static void print(@NotNull Throwable t) {
        getPluginLogger().severe(t.getClass().getSimpleName());
        getPluginLogger().severe("-----------");
        getPluginLogger().severe(t.getMessage());
        for (StackTraceElement e : t.getStackTrace()) getPluginLogger().severe(e.toString());
    }

    @Override
    public void onEnable() {
        getLogger().info("MCSCore - Created by GamerCoder");
        getLogger().info("Beginning Initialization...");

        saveDefaultConfig();
        saveConfig();

        getLogger().info("Loaded Files...");

        MCSBiomeManager.registerBiomes();
        MCSBiome.getAllBiomes(); // Load Cache

        getLogger().info("Loaded Biomes...");

        new MCSCommands(this);
        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("Loaded Classes...");
        getLogger().info("Done!");
    }

    // Events

    @EventHandler
    public void onClick(@NotNull InventoryClickEvent e) {
        if (e.getClickedInventory().getHolder() instanceof CancelHolder) e.setCancelled(true);
    }

    @EventHandler
    public void onDrag(@NotNull InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof CancelHolder) e.setCancelled(true);
    }

}

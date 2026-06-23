package com.vehector.nextheads;

import com.vehector.nextheads.commands.CabezasCommand;
import com.vehector.nextheads.hooks.LuckPermsHook;
import com.vehector.nextheads.hooks.PlaceholderHook;
import com.vehector.nextheads.listeners.InventoryListener;
import com.vehector.nextheads.managers.ConfigManager;
import com.vehector.nextheads.managers.CooldownManager;
import com.vehector.nextheads.managers.DenyManager;
import com.vehector.nextheads.managers.FavoritesManager;
import com.vehector.nextheads.managers.HeadsManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Nextheads extends JavaPlugin {

    private static Nextheads instance;

    private ConfigManager configManager;
    private CooldownManager cooldownManager;
    private DenyManager denyManager;
    private FavoritesManager favoritesManager;
    private HeadsManager headsManager;
    private LuckPermsHook luckPermsHook;

    @Override
    public void onEnable() {
        instance = this;

        // Save default configs
        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.configManager = new ConfigManager(this);
        this.configManager.reload();

        this.cooldownManager = new CooldownManager(this);
        this.denyManager = new DenyManager(this);
        this.denyManager.load();
        this.favoritesManager = new FavoritesManager(this);
        this.favoritesManager.load();
        this.headsManager = new HeadsManager(this);

        // Hooks
        this.luckPermsHook = new LuckPermsHook(this);
        this.luckPermsHook.init();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI detectado, expansion registrada.");
        }

        // Commands
        CabezasCommand cabezasCommand = new CabezasCommand(this);
        getCommand("cabezas").setExecutor(cabezasCommand);
        getCommand("cabezas").setTabCompleter(cabezasCommand);

        // Listeners
        Bukkit.getPluginManager().registerEvents(new InventoryListener(this), this);

        getLogger().info("");
        getLogger().info("====================================");
        getLogger().info("  Nextheads v" + getDescription().getVersion());
        getLogger().info("  Autor: Vehector");
        getLogger().info("  Paper 1.21.X | Java 21");
        getLogger().info("====================================");
        getLogger().info("");
    }

    @Override
    public void onDisable() {
        if (denyManager != null) denyManager.save();
        if (headsManager != null) headsManager.shutdown();
    }

    public static Nextheads get() {
        return instance;
    }

    public ConfigManager getConfigManager() { return configManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public DenyManager getDenyManager() { return denyManager; }
    public FavoritesManager getFavoritesManager() { return favoritesManager; }
    public HeadsManager getHeadsManager() { return headsManager; }
    public LuckPermsHook getLuckPermsHook() { return luckPermsHook; }
}

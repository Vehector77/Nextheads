package com.vehector.nextheads.managers;

import com.vehector.nextheads.Nextheads;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DenyManager {

    private final Nextheads plugin;
    private final Set<UUID> denied = new HashSet<>();
    private File file;

    public DenyManager(Nextheads plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "denied.yml");
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("No se pudo crear denied.yml: " + e.getMessage());
            }
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        denied.clear();
        for (String s : cfg.getStringList("denied")) {
            try { denied.add(UUID.fromString(s)); } catch (Exception ignored) {}
        }
    }

    public void save() {
        if (file == null) return;
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("denied", denied.stream().map(UUID::toString).toList());
        try { cfg.save(file); } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar denied.yml: " + e.getMessage());
        }
    }

    public boolean isDenied(UUID id) {
        return denied.contains(id);
    }

    public boolean toggle(UUID id) {
        boolean now;
        if (denied.contains(id)) { denied.remove(id); now = false; }
        else { denied.add(id); now = true; }
        save();
        return now;
    }
}

package com.vehector.nextheads.managers;

import com.vehector.nextheads.Nextheads;
import com.vehector.nextheads.managers.HeadsManager.HeadEntry;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores admin-added favorite heads persistently in {@code favorites.yml} inside
 * the plugin data folder. Each entry is identified uniquely by its texture
 * {@code value} (base64). Operations are thread-safe enough for the plugin's
 * single-main-thread access pattern.
 */
public class FavoritesManager {

    private final Nextheads plugin;
    private final File file;
    private final List<HeadEntry> favorites = new ArrayList<>();

    public FavoritesManager(Nextheads plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "favorites.yml");
    }

    public void load() {
        favorites.clear();
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> list = cfg.getMapList("favorites");
        for (Map<?, ?> m : list) {
            Object value = m.get("value");
            if (value == null) continue;
            String name = m.get("name") == null ? "Favorito" : String.valueOf(m.get("name"));
            String uuid = m.get("uuid") == null ? "" : String.valueOf(m.get("uuid"));
            String tags = m.get("tags") == null ? "" : String.valueOf(m.get("tags"));
            favorites.add(new HeadEntry(name, uuid, String.valueOf(value), tags));
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        List<Map<String, String>> list = new ArrayList<>();
        for (HeadEntry h : favorites) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("name", h.name());
            m.put("uuid", h.uuid());
            m.put("value", h.value());
            m.put("tags", h.tags());
            list.add(m);
        }
        cfg.set("favorites", list);
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar favorites.yml: " + e.getMessage());
        }
    }

    /** @return true if added, false if a head with the same texture already exists. */
    public boolean add(HeadEntry entry) {
        for (HeadEntry h : favorites) {
            if (h.value().equals(entry.value())) return false;
        }
        favorites.add(entry);
        save();
        return true;
    }

    /** @return true if removed, false if no head with that texture was stored. */
    public boolean removeByValue(String value) {
        boolean removed = favorites.removeIf(h -> h.value().equals(value));
        if (removed) save();
        return removed;
    }

    public boolean contains(String value) {
        for (HeadEntry h : favorites) {
            if (h.value().equals(value)) return true;
        }
        return false;
    }

    public List<HeadEntry> getAll() {
        return new ArrayList<>(favorites);
    }
}

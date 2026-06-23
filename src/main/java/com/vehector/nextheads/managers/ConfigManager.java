package com.vehector.nextheads.managers;

import com.vehector.nextheads.Nextheads;
import com.vehector.nextheads.utils.ColorUtil;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConfigManager {

    private final Nextheads plugin;
    private FileConfiguration messages;
    private File messagesFile;

    private int cooldownSeconds;
    private int playersMenuSize;
    private int adminMenuSize;
    private int apiCacheSeconds;
    private int maxHeadsPerCategory;
    private String pickupSound;
    private String clickSound;
    /** Pre-resolved Sound enums (avoids Sound.valueOf() on every click). */
    private Sound pickupSoundEnum;
    private Sound clickSoundEnum;
    private boolean debug;

    private String headDisplayName;
    private List<String> headLore;
    private String defaultRank;

    private String prefix;

    // Cached, recomputed on reload(). getCategories() is called on every
    // category-menu click and used to be rebuilt from raw config each time.
    private List<CategoryDef> categoriesCache = Collections.emptyList();
    /** Fast id -> CategoryDef lookup (case-insensitive). Replaces a linear stream scan. */
    private Map<String, CategoryDef> categoriesById = Collections.emptyMap();

    // Cache for translated gui.* / messages.* strings keyed by lookup key.
    // Cleared on reload() so /cabezas reload still picks up changes.
    private final Map<String, String> guiTextCache = new HashMap<>();
    private final Map<String, List<String>> guiListCache = new HashMap<>();
    private final Map<String, String> msgCache = new HashMap<>();
    private final Map<String, String> guiTextRawCache = new HashMap<>();

    public ConfigManager(Nextheads plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();
        cooldownSeconds = c.getInt("settings.cooldown-seconds", 60);
        playersMenuSize = clampSize(c.getInt("settings.players-menu-size", 54));
        adminMenuSize = clampSize(c.getInt("settings.admin-menu-size", 54));
        apiCacheSeconds = c.getInt("settings.api-cache-seconds", 3600);
        maxHeadsPerCategory = c.getInt("max-heads-per-category", 500);
        pickupSound = c.getString("settings.pickup-sound", "BLOCK_NOTE_BLOCK_PLING");
        clickSound = c.getString("settings.click-sound", "UI_BUTTON_CLICK");
        pickupSoundEnum = resolveSound(pickupSound);
        clickSoundEnum = resolveSound(clickSound);
        debug = c.getBoolean("settings.debug", false);

        headDisplayName = c.getString("head-format.display-name", "&aCabeza de {rank} &c{nick}");
        headLore = c.getStringList("head-format.lore");
        defaultRank = c.getString("default-rank", "&7MIEMBRO");

        // messages
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) plugin.saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        prefix = messages.getString("prefix", "&8[&dNextheads&8] &r");

        // Rebuild caches
        guiTextCache.clear();
        guiListCache.clear();
        msgCache.clear();
        guiTextRawCache.clear();
        categoriesCache = buildCategories();
        Map<String, CategoryDef> byId = new HashMap<>(categoriesCache.size() * 2);
        for (CategoryDef cd : categoriesCache) byId.put(cd.id().toLowerCase(Locale.ROOT), cd);
        categoriesById = Collections.unmodifiableMap(byId);
    }

    private static Sound resolveSound(String name) {
        if (name == null) return null;
        try { return Sound.valueOf(name); } catch (Exception ignored) { return null; }
    }

    private int clampSize(int v) {
        if (v < 9) v = 9;
        if (v > 54) v = 54;
        return (v / 9) * 9;
    }

    public static final String FAVORITES_CATEGORY_ID = "favorites";

    public List<CategoryDef> getCategories() {
        return categoriesCache;
    }

    /** O(1) case-insensitive lookup of a category by its configured id. */
    public CategoryDef getCategoryById(String id) {
        if (id == null) return null;
        return categoriesById.get(id.toLowerCase(Locale.ROOT));
    }

    private List<CategoryDef> buildCategories() {
        List<CategoryDef> out = new ArrayList<>();
        for (var raw : plugin.getConfig().getMapList("categories")) {
            String id = String.valueOf(raw.get("id"));
            String name = String.valueOf(raw.get("name"));
            String icon = String.valueOf(raw.get("icon"));
            out.add(new CategoryDef(id, name, icon));
        }
        String favName = plugin.getConfig().getString("favorites-category.name", "&6&lFavoritos");
        String favIcon = plugin.getConfig().getString("favorites-category.icon", "NETHER_STAR");
        out.add(new CategoryDef(FAVORITES_CATEGORY_ID, favName, favIcon));
        return Collections.unmodifiableList(out);
    }

    public String msg(String key) {
        String cached = msgCache.get(key);
        if (cached != null) return cached;
        String raw = messages.getString("messages." + key, "&cMensaje no definido: " + key);
        String result = ColorUtil.color(raw.replace("{prefix}", prefix));
        msgCache.put(key, result);
        return result;
    }

    public String guiText(String key) {
        String cached = guiTextCache.get(key);
        if (cached != null) return cached;
        String result = ColorUtil.color(messages.getString("gui." + key, "&c?"));
        guiTextCache.put(key, result);
        return result;
    }

    /**
     * Same as {@link #guiText(String)} but returns the raw string with un-translated
     * color codes.
     */
    public String guiTextRaw(String key) {
        String cached = guiTextRawCache.get(key);
        if (cached != null) return cached;
        String result = messages.getString("gui." + key, "&c?");
        guiTextRawCache.put(key, result);
        return result;
    }

    public List<String> guiList(String key) {
        List<String> cached = guiListCache.get(key);
        if (cached != null) return cached;
        List<String> result = Collections.unmodifiableList(
                ColorUtil.color(messages.getStringList("gui." + key)));
        guiListCache.put(key, result);
        return result;
    }

    public List<String> helpLines() {
        List<String> raw = messages.getStringList("help");
        List<String> out = new ArrayList<>(raw.size());
        for (String l : raw) out.add(ColorUtil.color(l.replace("{version}", plugin.getDescription().getVersion())));
        return out;
    }

    public void send(CommandSender s, String key, String... repl) {
        String m = msg(key);
        for (int i = 0; i + 1 < repl.length; i += 2) {
            m = m.replace("{" + repl[i] + "}", repl[i + 1]);
        }
        s.sendMessage(m);
    }

    public int getCooldownSeconds() { return cooldownSeconds; }
    public int getPlayersMenuSize() { return playersMenuSize; }
    public int getAdminMenuSize() { return adminMenuSize; }
    public int getApiCacheSeconds() { return apiCacheSeconds; }
    public int getMaxHeadsPerCategory() { return maxHeadsPerCategory; }
    public String getPickupSound() { return pickupSound; }
    public String getClickSound() { return clickSound; }
    public Sound getPickupSoundEnum() { return pickupSoundEnum; }
    public Sound getClickSoundEnum() { return clickSoundEnum; }
    public boolean isDebug() { return debug; }
    public String getHeadDisplayName() { return headDisplayName; }
    public List<String> getHeadLore() { return headLore; }
    public String getDefaultRank() { return defaultRank; }
    public String getPrefix() { return prefix; }

    public record CategoryDef(String id, String name, String icon) {}
}

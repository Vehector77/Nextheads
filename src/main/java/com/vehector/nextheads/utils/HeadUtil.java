package com.vehector.nextheads.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HeadUtil {

    private HeadUtil() {}

    /**
     * Cache of PlayerProfile per texture value. Building a profile is cheap-ish
     * but textureHead() is called for every visible head on every GUI rebuild
     * (paging), so reusing the same profile avoids re-allocating UUIDs and
     * ProfileProperty objects on every page flip.
     */
    private static final ConcurrentHashMap<String, PlayerProfile> PROFILE_CACHE = new ConcurrentHashMap<>(256);

    /**
     * Cache of fully-built ItemStack templates keyed by (texture + displayName + lore).
     * Bukkit's {@code setDisplayName} / {@code setLore} run a regex-heavy color
     * pipeline (visible as {@code CraftChatMessage.fromString} -> {@code Matcher.find}
     * in the profiler). Building a head once and cloning on subsequent paint avoids
     * paying that cost on every page flip / GUI repaint.
     *
     * Bounded with a coarse clear-when-full strategy: the workload is GUI-driven,
     * so the working set is essentially "the heads the player has scrolled past
     * during this session" -- a hard ceiling is enough.
     */
    private static final ConcurrentHashMap<String, ItemStack> ITEM_CACHE = new ConcurrentHashMap<>(512);
    private static final int ITEM_CACHE_MAX = 8192;

    public static ItemStack playerHead(OfflinePlayer player, String displayName, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ColorUtil.color(displayName));
            if (lore != null && !lore.isEmpty()) meta.setLore(ColorUtil.color(lore));
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * Creates a player head from a Base64-encoded texture value (the value field
     * found in minecraft-heads.com API responses) using Paper's PlayerProfile API.
     *
     * Optimizations applied:
     *  - Cached PlayerProfile per texture value (avoid re-creating profiles).
     *  - Dropped the Base64-decode + setSkin(URL) step: setting the "textures"
     *    ProfileProperty alone is enough for the client to render the skin,
     *    and the decode was a measurable cost in the profiler.
     *  - Cached the fully built ItemStack template per (texture, name, lore)
     *    triplet. On a cache hit we just {@code clone()} the stack, skipping the
     *    expensive Bukkit color-translation pipeline entirely (this was the
     *    dominant cost in the profile, via CraftChatMessage.fromString -> regex).
     */
    public static ItemStack textureHead(String texture, String displayName, List<String> lore) {
        if (texture == null || texture.isEmpty()) return new ItemStack(Material.PLAYER_HEAD);

        String cacheKey = buildItemKey(texture, displayName, lore);
        ItemStack cached = ITEM_CACHE.get(cacheKey);
        if (cached != null) return cached.clone();

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        PlayerProfile profile = PROFILE_CACHE.get(texture);
        if (profile == null) {
            try {
                int hash = texture.hashCode();
                UUID id = new UUID(hash, hash);
                profile = Bukkit.createProfile(id, "head_" + Math.abs(hash) % 100000);
                profile.setProperty(new ProfileProperty("textures", texture));
                PROFILE_CACHE.put(texture, profile);
            } catch (Throwable ignored) {
                // fall through; we'll still return a (textureless) head
            }
        }
        if (profile != null) {
            try { meta.setPlayerProfile(profile); } catch (Throwable ignored) {}
        }

        meta.setDisplayName(ColorUtil.color(displayName));
        if (lore != null && !lore.isEmpty()) meta.setLore(ColorUtil.color(lore));
        head.setItemMeta(meta);

        if (ITEM_CACHE.size() >= ITEM_CACHE_MAX) ITEM_CACHE.clear();
        ITEM_CACHE.put(cacheKey, head);
        return head.clone();
    }

    private static String buildItemKey(String texture, String displayName, List<String> lore) {
        // Small, allocation-conscious key. We rely on String hashing inside the map;
        // texture is the dominant unique part so collisions across (name, lore) are
        // a non-issue in practice.
        int loreLen = 0;
        if (lore != null) {
            for (int i = 0; i < lore.size(); i++) loreLen += lore.get(i).length() + 1;
        }
        StringBuilder sb = new StringBuilder(texture.length() + (displayName == null ? 0 : displayName.length()) + loreLen + 4);
        sb.append(texture).append('\u0001');
        if (displayName != null) sb.append(displayName);
        sb.append('\u0001');
        if (lore != null) {
            for (int i = 0; i < lore.size(); i++) {
                sb.append(lore.get(i)).append('\u0002');
            }
        }
        return sb.toString();
    }

    public static ItemStack simple(Material material, String name, List<String> lore) {
        ItemStack it = new ItemStack(material);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            if (lore != null && !lore.isEmpty()) meta.setLore(ColorUtil.color(lore));
            it.setItemMeta(meta);
        }
        return it;
    }
}

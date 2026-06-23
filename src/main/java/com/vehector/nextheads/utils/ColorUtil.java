package com.vehector.nextheads.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Cache of translated strings. Color translation is called many times per
     * GUI build (every item display name, every lore line) and the regex +
     * char-array scan adds up. The cache is bounded to avoid unbounded growth
     * when callers feed in dynamic content (e.g. player chat messages); when
     * the bound is reached the cache is cleared rather than evicting one by
     * one - simple and predictable for a Minecraft GUI workload.
     */
    private static final ConcurrentHashMap<String, String> COLOR_CACHE = new ConcurrentHashMap<>(512);
    private static final int CACHE_MAX = 4096;

    private ColorUtil() {}

    public static String color(String text) {
        if (text == null) return "";
        if (text.isEmpty()) return text;
        String cached = COLOR_CACHE.get(text);
        if (cached != null) return cached;

        String result;
        if (text.indexOf('&') < 0) {
            result = text;
        } else if (text.indexOf("&#") < 0) {
            // No hex codes -> skip the regex entirely
            result = ChatColor.translateAlternateColorCodes('&', text);
        } else {
            Matcher m = HEX_PATTERN.matcher(text);
            StringBuilder sb = new StringBuilder(text.length() + 16);
            while (m.find()) {
                String hex = m.group(1);
                m.appendReplacement(sb, ChatColor.of("#" + hex).toString());
            }
            m.appendTail(sb);
            result = ChatColor.translateAlternateColorCodes('&', sb.toString());
        }

        if (COLOR_CACHE.size() >= CACHE_MAX) COLOR_CACHE.clear();
        COLOR_CACHE.put(text, result);
        return result;
    }

    public static List<String> color(List<String> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        List<String> out = new ArrayList<>(list.size());
        for (String s : list) out.add(color(s));
        return out;
    }

    public static String strip(String text) {
        return ChatColor.stripColor(color(text));
    }
}

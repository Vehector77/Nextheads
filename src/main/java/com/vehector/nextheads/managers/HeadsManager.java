package com.vehector.nextheads.managers;

import com.vehector.nextheads.Nextheads;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HeadsManager {

    private final Nextheads plugin;
    private final Executor executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Nextheads-API");
        t.setDaemon(true);
        return t;
    });

    private final Map<String, CachedCategory> cache = new ConcurrentHashMap<>();
    /** In-flight requests by category id to deduplicate concurrent fetches. */
    private final Map<String, CompletableFuture<List<HeadEntry>>> inFlight = new ConcurrentHashMap<>();

    public HeadsManager(Nextheads plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<List<HeadEntry>> getCategory(String categoryId) {
        CachedCategory c = cache.get(categoryId);
        long now = System.currentTimeMillis();
        if (c != null && (now - c.timestamp) / 1000 < plugin.getConfigManager().getApiCacheSeconds()) {
            return CompletableFuture.completedFuture(c.heads);
        }
        // Deduplicate concurrent fetches for the same category.
        return inFlight.computeIfAbsent(categoryId, id -> {
            CompletableFuture<List<HeadEntry>> f = CompletableFuture.supplyAsync(() -> fetchCategory(id), executor);
            f.whenComplete((r, t) -> inFlight.remove(id));
            return f;
        });
    }

    public List<HeadEntry> searchAll(String query) {
        if (query == null) return List.of();
        String q = query.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) return List.of();
        List<HeadEntry> out = new ArrayList<>();
        for (CachedCategory cc : cache.values()) {
            for (HeadEntry h : cc.heads) {
                String n = h.name();
                String tg = h.tags();
                if ((n != null && n.toLowerCase(Locale.ROOT).contains(q)) ||
                        (tg != null && !tg.isEmpty() && tg.toLowerCase(Locale.ROOT).contains(q))) {
                    out.add(h);
                    if (out.size() >= 500) return out;
                }
            }
        }
        return out;
    }

    private List<HeadEntry> fetchCategory(String categoryId) {
        List<HeadEntry> result = new ArrayList<>();
        try {
            URI uri = URI.create("https://minecraft-heads.com/scripts/api.php?cat=" + categoryId + "&tags=true");
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "Nextheads/1.0 (Paper Plugin by Vehector)");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder(8192);
                char[] buf = new char[4096];
                int read;
                while ((read = br.read(buf)) > 0) sb.append(buf, 0, read);
                parseJsonArray(sb.toString(), result);
            }
        } catch (Throwable t) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("Error al cargar categoria " + categoryId + ": " + t.getMessage());
            }
        }
        int limit = plugin.getConfigManager().getMaxHeadsPerCategory();
        if (result.size() > limit) result = new ArrayList<>(result.subList(0, limit));
        cache.put(categoryId, new CachedCategory(result, System.currentTimeMillis()));
        return result;
    }

    /**
     * Lightweight JSON array parser, enough for the minecraft-heads.com API
     * shape (array of {name, uuid, value, tags}). Avoids extra dependencies.
     */
    private void parseJsonArray(String json, List<HeadEntry> out) {
        if (json == null || json.isBlank()) return;
        int i = json.indexOf('[');
        if (i < 0) return;
        int depth = 0;
        StringBuilder cur = new StringBuilder(256);
        for (int p = i; p < json.length(); p++) {
            char ch = json.charAt(p);
            if (ch == '{') {
                if (depth == 0) cur.setLength(0);
                depth++;
                cur.append(ch);
            } else if (ch == '}') {
                depth--;
                cur.append(ch);
                if (depth == 0) {
                    HeadEntry e = parseObject(cur.toString());
                    if (e != null) out.add(e);
                }
            } else if (depth > 0) {
                cur.append(ch);
            }
        }
    }

    private HeadEntry parseObject(String obj) {
        String name = extract(obj, "name");
        String uuid = extract(obj, "uuid");
        String value = extract(obj, "value");
        String tags = extract(obj, "tags");
        if (value == null) return null;
        if (name == null) name = "Cabeza";
        return new HeadEntry(name, uuid == null ? "" : uuid, value, tags == null ? "" : tags);
    }

    private String extract(String obj, String key) {
        String pat = "\"" + key + "\"";
        int k = obj.indexOf(pat);
        if (k < 0) return null;
        int colon = obj.indexOf(':', k + pat.length());
        if (colon < 0) return null;
        int q1 = obj.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        for (int p = q1 + 1; p < obj.length(); p++) {
            char c = obj.charAt(p);
            if (esc) { sb.append(c); esc = false; continue; }
            if (c == '\\') { esc = true; continue; }
            if (c == '"') return sb.toString();
            sb.append(c);
        }
        return null;
    }

    public void shutdown() {
        cache.clear();
        inFlight.clear();
    }

    public record HeadEntry(String name, String uuid, String value, String tags) {}
    private record CachedCategory(List<HeadEntry> heads, long timestamp) {}
}

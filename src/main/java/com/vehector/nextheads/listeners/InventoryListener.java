package com.vehector.nextheads.listeners;

import com.vehector.nextheads.Nextheads;
import com.vehector.nextheads.gui.AdminCategoriesGUI;
import com.vehector.nextheads.gui.AdminHeadsGUI;
import com.vehector.nextheads.gui.PlayerHeadsGUI;
import com.vehector.nextheads.managers.ConfigManager.CategoryDef;
import com.vehector.nextheads.managers.HeadsManager.HeadEntry;
import com.vehector.nextheads.utils.ColorUtil;
import com.vehector.nextheads.utils.HeadUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InventoryListener implements Listener {

    private final Nextheads plugin;

    /** Players currently waiting for their next chat message to be a search query. */
    private final Map<UUID, SearchContext> awaitingSearch = new HashMap<>();

    public InventoryListener(Nextheads plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        var holder = e.getInventory().getHolder();

        if (holder instanceof PlayerHeadsGUI gui) {
            e.setCancelled(true);
            handlePlayerHeadsClick(p, gui, e);
        } else if (holder instanceof AdminCategoriesGUI gui) {
            e.setCancelled(true);
            handleCategoriesClick(p, gui, e);
        } else if (holder instanceof AdminHeadsGUI gui) {
            e.setCancelled(true);
            handleAdminHeadsClick(p, gui, e);
        }
    }

    /* ============================================================
       Player heads menu
       ============================================================ */

    private void handlePlayerHeadsClick(Player p, PlayerHeadsGUI gui, InventoryClickEvent e) {
        int size = gui.getInventory().getSize();
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= size) return;

        int row = size - 9;
        if (slot == row + 0) {
            new PlayerHeadsGUI(plugin, gui.getPage() - 1).open(p);
            playClick(p);
            return;
        }
        if (slot == row + 8) {
            new PlayerHeadsGUI(plugin, gui.getPage() + 1).open(p);
            playClick(p);
            return;
        }
        if (slot == row + 7) { p.closeInventory(); return; }
        if (slot >= row) return; // info / decoration

        int idx = gui.getPage() * gui.getContentSize() + slot;
        if (idx < 0 || idx >= gui.getTargets().size()) return;
        Player target = gui.getTargets().get(idx);
        if (!target.isOnline()) {
            plugin.getConfigManager().send(p, "not-online");
            return;
        }

        // Self-bypass not needed; even denied players can get their own head
        if (!p.equals(target) && plugin.getDenyManager().isDenied(target.getUniqueId())
                && !p.hasPermission("nextheads.bypass")) {
            plugin.getConfigManager().send(p, "target-denied", "target", target.getName());
            return;
        }
        if (plugin.getCooldownManager().isOnCooldown(p)) {
            plugin.getConfigManager().send(p, "cooldown", "seconds",
                    String.valueOf(plugin.getCooldownManager().secondsLeft(p)));
            return;
        }

        ItemStack head = buildPlayerHead(target);
        if (p.getInventory().firstEmpty() == -1) {
            plugin.getConfigManager().send(p, "inventory-full");
            return;
        }
        p.getInventory().addItem(head);
        plugin.getCooldownManager().apply(p);
        plugin.getConfigManager().send(p, "head-received", "target", target.getName());
        playPickup(p);
    }

    private ItemStack buildPlayerHead(Player target) {
        String rank = plugin.getLuckPermsHook().getPrefix(target);
        String nick = target.getDisplayName();
        String dn = plugin.getConfigManager().getHeadDisplayName()
                .replace("{rank}", rank)
                .replace("{nick}", nick)
                .replace("{player}", target.getName());
        List<String> lore = new ArrayList<>();
        for (String l : plugin.getConfigManager().getHeadLore()) {
            lore.add(l.replace("{rank}", rank)
                    .replace("{nick}", nick)
                    .replace("{player}", target.getName()));
        }
        return HeadUtil.playerHead(target, dn, lore);
    }

    /* ============================================================
       Admin categories menu
       ============================================================ */

    private void handleCategoriesClick(Player p, AdminCategoriesGUI gui, InventoryClickEvent e) {
        int slot = e.getRawSlot();
        int size = gui.getInventory().getSize();
        int row = size - 9;
        if (slot >= size || slot < 0) return;

        if (slot == row + 8) { p.closeInventory(); return; }
        if (slot == row + 4) { startSearch(p, null); return; }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getLore() == null) return;
        String idLine = null;
        for (String l : meta.getLore()) {
            String stripped = ColorUtil.strip(l);
            if (stripped.startsWith("ID:")) { idLine = stripped; break; }
        }
        if (idLine == null) return;
        String catId = idLine.substring(3).trim();
        CategoryDef cat = plugin.getConfigManager().getCategoryById(catId);
        if (cat == null) return;

        playClick(p);
        if (com.vehector.nextheads.managers.ConfigManager.FAVORITES_CATEGORY_ID.equalsIgnoreCase(cat.id())) {
            List<HeadEntry> favs = plugin.getFavoritesManager().getAll();
            new AdminHeadsGUI(plugin, favs, cat.name(), 0).open(p);
            return;
        }
        plugin.getConfigManager().send(p, "api-loading", "category", ColorUtil.strip(cat.name()));
        plugin.getHeadsManager().getCategory(cat.id()).thenAccept(list -> {
            Bukkit.getScheduler().runTask(plugin, () ->
                    new AdminHeadsGUI(plugin, list, cat.name(), 0).open(p));
        }).exceptionally(t -> {
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getConfigManager().send(p, "api-error", "error", t.getMessage()));
            return null;
        });
    }

    /* ============================================================
       Admin heads (paginated) menu
       ============================================================ */

    private void handleAdminHeadsClick(Player p, AdminHeadsGUI gui, InventoryClickEvent e) {
        int size = gui.getInventory().getSize();
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= size) return;
        int row = size - 9;

        if (slot == row + 0) {
            reopen(p, gui, gui.getPage() - 1);
            playClick(p);
            return;
        }
        if (slot == row + 8) {
            reopen(p, gui, gui.getPage() + 1);
            playClick(p);
            return;
        }
        if (slot == row + 7) { p.closeInventory(); return; }
        if (slot == row + 5) { new AdminCategoriesGUI(plugin).open(p); return; }
        if (slot == row + 3) { startSearch(p, gui); return; }
        if (slot >= row) return;

        int idx = gui.getPage() * gui.getContentSize() + slot;
        if (idx < 0 || idx >= gui.getHeads().size()) return;
        HeadEntry h = gui.getHeads().get(idx);

        ClickType type = e.getClick();
        ItemStack head = HeadUtil.textureHead(h.value(), "&f" + h.name(),
                List.of("&7" + (h.tags().isEmpty() ? "Sin etiquetas" : h.tags()),
                        "&8By Nextheads"));
        if (type.isShiftClick()) {
            head.setAmount(64);
        }
        if (p.getInventory().firstEmpty() == -1) {
            plugin.getConfigManager().send(p, "inventory-full");
            return;
        }
        p.getInventory().addItem(head);
        playPickup(p);
        plugin.getConfigManager().send(p, "head-received", "target", h.name());
    }

    private void reopen(Player p, AdminHeadsGUI gui, int newPage) {
        new AdminHeadsGUI(plugin, gui.getHeads(), gui.getCategoryName(), newPage,
                gui.isSearchMode(), "").open(p);
    }

    /* ============================================================
       Chat-based search
       ============================================================ */

    private void startSearch(Player p, AdminHeadsGUI fromGui) {
        plugin.getConfigManager().send(p, "search-prompt");
        // Pre-load all categories so search has data
        for (CategoryDef cat : plugin.getConfigManager().getCategories()) {
            plugin.getHeadsManager().getCategory(cat.id());
        }
        awaitingSearch.put(p.getUniqueId(), new SearchContext());
        Bukkit.getScheduler().runTask(plugin, (Runnable) p::closeInventory);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        SearchContext ctx = awaitingSearch.remove(p.getUniqueId());
        if (ctx == null) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        if (text.equalsIgnoreCase("cancelar") || text.equalsIgnoreCase("cancel")) {
            plugin.getConfigManager().send(p, "search-cancelled");
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            List<HeadEntry> results = plugin.getHeadsManager().searchAll(text);
            if (results.isEmpty()) {
                plugin.getConfigManager().send(p, "search-no-results", "query", text);
                new AdminCategoriesGUI(plugin).open(p);
                return;
            }
            plugin.getConfigManager().send(p, "search-results",
                    "count", String.valueOf(results.size()),
                    "query", text);
            new AdminHeadsGUI(plugin, results, "&bBusqueda", 0, true, text).open(p);
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        // nothing for now
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        awaitingSearch.remove(e.getPlayer().getUniqueId());
    }

    /* ============================================================ */

    private void playClick(Player p) {
        Sound s = plugin.getConfigManager().getClickSoundEnum();
        if (s == null) return;
        p.playSound(p.getLocation(), s, 0.6f, 1f);
    }

    private void playPickup(Player p) {
        Sound s = plugin.getConfigManager().getPickupSoundEnum();
        if (s == null) return;
        p.playSound(p.getLocation(), s, 1f, 1.4f);
    }

    private static class SearchContext { /* placeholder for future state */ }
}

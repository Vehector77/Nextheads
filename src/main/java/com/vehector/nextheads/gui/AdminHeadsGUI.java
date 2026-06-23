package com.vehector.nextheads.gui;

import com.vehector.nextheads.Nextheads;
import com.vehector.nextheads.managers.HeadsManager.HeadEntry;
import com.vehector.nextheads.utils.HeadUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AdminHeadsGUI implements InventoryHolder {

    private final Nextheads plugin;
    private final List<HeadEntry> heads;
    private final String categoryName; // already colored
    private final boolean searchMode;
    private final String query;
    private int page;
    private Inventory inventory;

    public AdminHeadsGUI(Nextheads plugin, List<HeadEntry> heads, String categoryName, int page) {
        this(plugin, heads, categoryName, page, false, "");
    }

    public AdminHeadsGUI(Nextheads plugin, List<HeadEntry> heads, String categoryName, int page,
                         boolean searchMode, String query) {
        this.plugin = plugin;
        this.heads = heads;
        this.categoryName = categoryName;
        this.searchMode = searchMode;
        this.query = query;
        this.page = page;
        build();
    }

    private void build() {
        int size = plugin.getConfigManager().getAdminMenuSize();
        String title;
        if (searchMode) {
            title = plugin.getConfigManager().guiTextRaw("search-title").replace("{query}", query);
        } else {
            title = plugin.getConfigManager().guiTextRaw("category-title").replace("{category}", categoryName);
        }
        // Apply color translation AFTER substitution so the category name's &codes are translated too.
        title = com.vehector.nextheads.utils.ColorUtil.color(title);
        this.inventory = Bukkit.createInventory(this, size, title);

        int contentSize = size - 9;
        int totalPages = Math.max(1, (int) Math.ceil(heads.size() / (double) contentSize));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int from = page * contentSize;
        int to = Math.min(heads.size(), from + contentSize);

        for (int i = from; i < to; i++) {
            HeadEntry h = heads.get(i);
            List<String> lore = new ArrayList<>();
            lore.add("&7" + (h.tags() == null || h.tags().isEmpty() ? "Sin etiquetas" : h.tags()));
            lore.add("");
            lore.add("&eClick &7para obtener esta cabeza.");
            ItemStack head = HeadUtil.textureHead(h.value(), "&f" + h.name(), lore);
            inventory.setItem(i - from, head);
        }

        int row = size - 9;
        inventory.setItem(row + 0, HeadUtil.simple(Material.ARROW,
                plugin.getConfigManager().guiText("prev-page"), List.of()));
        inventory.setItem(row + 8, HeadUtil.simple(Material.ARROW,
                plugin.getConfigManager().guiText("next-page"), List.of()));
        String pageInfo = plugin.getConfigManager().guiText("page-info")
                .replace("{page}", String.valueOf(page + 1))
                .replace("{total}", String.valueOf(totalPages));
        inventory.setItem(row + 4, HeadUtil.simple(Material.PAPER, pageInfo, List.of()));
        inventory.setItem(row + 3, HeadUtil.simple(Material.OAK_SIGN,
                plugin.getConfigManager().guiText("search-button"),
                plugin.getConfigManager().guiList("search-button-lore")));
        inventory.setItem(row + 5, HeadUtil.simple(Material.OAK_DOOR,
                plugin.getConfigManager().guiText("back"), List.of()));
        inventory.setItem(row + 7, HeadUtil.simple(Material.BARRIER,
                plugin.getConfigManager().guiText("close"), List.of()));
    }

    public void open(Player p) { p.openInventory(inventory); }

    public List<HeadEntry> getHeads() { return heads; }
    public int getPage() { return page; }
    public int getContentSize() { return inventory.getSize() - 9; }
    public boolean isSearchMode() { return searchMode; }
    public String getCategoryName() { return categoryName; }

    @Override public Inventory getInventory() { return inventory; }
}

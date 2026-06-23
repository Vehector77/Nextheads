package com.vehector.nextheads.gui;

import com.vehector.nextheads.Nextheads;
import com.vehector.nextheads.managers.ConfigManager.CategoryDef;
import com.vehector.nextheads.utils.HeadUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AdminCategoriesGUI implements InventoryHolder {

    private final Nextheads plugin;
    private final Inventory inventory;
    private final List<CategoryDef> categories;

    public AdminCategoriesGUI(Nextheads plugin) {
        this.plugin = plugin;
        this.categories = plugin.getConfigManager().getCategories();
        this.inventory = Bukkit.createInventory(this, 45,
                plugin.getConfigManager().guiText("categories-title"));
        build();
    }

    private void build() {
        int slot = 10;
        // Categories live in rows 2-3 (slots 10-16 and 19-25). Row 4 (27-35) is
        // intentionally left empty as a visual separator above the bottom action bar.
        int itemsLimit = inventory.getSize() - 18; // stop before the empty separator row
        for (CategoryDef cat : categories) {
            Material mat;
            try { mat = Material.valueOf(cat.icon().toUpperCase()); }
            catch (Exception e) { mat = Material.PLAYER_HEAD; }

            ItemStack icon = HeadUtil.simple(mat, cat.name(),
                    List.of("&7Click para ver las cabezas", "&7de esta categoria.", "",
                            "&8ID: &7" + cat.id()));
            if (slot >= itemsLimit) break;
            inventory.setItem(slot, icon);
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
        }

        int last = inventory.getSize() - 9;
        inventory.setItem(last + 4, HeadUtil.simple(Material.OAK_SIGN,
                plugin.getConfigManager().guiText("search-button"),
                plugin.getConfigManager().guiList("search-button-lore")));
        inventory.setItem(last + 8, HeadUtil.simple(Material.BARRIER,
                plugin.getConfigManager().guiText("close"), List.of()));
    }

    public List<CategoryDef> getCategories() { return categories; }
    public void open(Player p) { p.openInventory(inventory); }
    @Override public Inventory getInventory() { return inventory; }
}

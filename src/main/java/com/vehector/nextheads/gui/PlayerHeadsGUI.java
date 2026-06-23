package com.vehector.nextheads.gui;

import com.vehector.nextheads.Nextheads;
import com.vehector.nextheads.utils.ColorUtil;
import com.vehector.nextheads.utils.HeadUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PlayerHeadsGUI implements InventoryHolder {

    private final Nextheads plugin;
    private Inventory inventory;
    private final List<Player> targets = new ArrayList<>();
    private int page;

    public PlayerHeadsGUI(Nextheads plugin, int page) {
        this.plugin = plugin;
        this.page = page;
        build();
    }

    private void build() {
        int size = plugin.getConfigManager().getPlayersMenuSize();
        String title = plugin.getConfigManager().guiText("players-title");
        this.inventory = Bukkit.createInventory(this, size, title);

        targets.clear();
        targets.addAll(Bukkit.getOnlinePlayers());

        int contentSize = size - 9;
        int totalPages = Math.max(1, (int) Math.ceil(targets.size() / (double) contentSize));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int from = page * contentSize;
        int to = Math.min(targets.size(), from + contentSize);

        for (int i = from; i < to; i++) {
            Player target = targets.get(i);
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
            // Add a small hint line so the user knows it's clickable
            lore.add("");
            lore.add(ColorUtil.color("&eClick &7para obtener esta cabeza"));

            ItemStack head = HeadUtil.playerHead(target, dn, lore);
            inventory.setItem(i - from, head);
        }

        // Bottom navigation bar
        int row = size - 9;
        inventory.setItem(row + 0, navItem(Material.ARROW, "prev-page"));
        inventory.setItem(row + 4, infoItem(totalPages));
        inventory.setItem(row + 8, navItem(Material.ARROW, "next-page"));
        inventory.setItem(row + 7, HeadUtil.simple(Material.BARRIER,
                plugin.getConfigManager().guiText("close"), List.of()));
    }

    private ItemStack navItem(Material mat, String key) {
        return HeadUtil.simple(mat, plugin.getConfigManager().guiText(key), List.of());
    }

    private ItemStack infoItem(int totalPages) {
        String name = plugin.getConfigManager().guiText("page-info")
                .replace("{page}", String.valueOf(page + 1))
                .replace("{total}", String.valueOf(totalPages));
        String online = plugin.getConfigManager().guiText("online-count")
                .replace("{count}", String.valueOf(targets.size()));
        return HeadUtil.simple(Material.PAPER, name, List.of(online));
    }

    public void open(Player p) { p.openInventory(inventory); }

    public List<Player> getTargets() { return targets; }
    public int getPage() { return page; }
    public int getContentSize() { return inventory.getSize() - 9; }

    @Override public Inventory getInventory() { return inventory; }
}

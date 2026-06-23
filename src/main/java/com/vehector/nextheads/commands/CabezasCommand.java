package com.vehector.nextheads.commands;

import com.vehector.nextheads.Nextheads;
import com.vehector.nextheads.gui.AdminCategoriesGUI;
import com.vehector.nextheads.gui.PlayerHeadsGUI;
import com.vehector.nextheads.managers.HeadsManager.HeadEntry;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CabezasCommand implements CommandExecutor, TabCompleter {

    private final Nextheads plugin;

    public CabezasCommand(Nextheads plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                plugin.getConfigManager().send(sender, "player-only");
                return true;
            }
            if (!p.hasPermission("nextheads.use")) {
                plugin.getConfigManager().send(sender, "no-permission");
                return true;
            }
            new PlayerHeadsGUI(plugin, 0).open(p);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "denegar", "deny" -> {
                if (!(sender instanceof Player p)) {
                    plugin.getConfigManager().send(sender, "player-only");
                    return true;
                }
                if (!p.hasPermission("nextheads.deny")) {
                    plugin.getConfigManager().send(sender, "no-permission");
                    return true;
                }
                boolean now = plugin.getDenyManager().toggle(p.getUniqueId());
                plugin.getConfigManager().send(p, now ? "deny-enabled" : "deny-disabled");
            }
            case "admin" -> {
                if (!(sender instanceof Player p)) {
                    plugin.getConfigManager().send(sender, "player-only");
                    return true;
                }
                if (!p.hasPermission("nextheads.admin")) {
                    plugin.getConfigManager().send(sender, "no-permission");
                    return true;
                }
                if (args.length >= 2 && (args[1].equalsIgnoreCase("favorito")
                        || args[1].equalsIgnoreCase("favorite"))) {
                    handleFavorito(p);
                    return true;
                }
                if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
                    new AdminCategoriesGUI(plugin).open(p);
                } else {
                    new AdminCategoriesGUI(plugin).open(p);
                }
            }
            case "reload" -> {
                if (!sender.hasPermission("nextheads.reload")) {
                    plugin.getConfigManager().send(sender, "no-permission");
                    return true;
                }
                plugin.getConfigManager().reload();
                plugin.getConfigManager().send(sender, "reload-success");
            }
            case "help", "?" -> sendHelp(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender s) {
        for (String l : plugin.getConfigManager().helpLines()) s.sendMessage(l);
    }

    /**
     * Toggles the player head currently held in the main hand as a favorite:
     * adds it if not stored yet, removes it otherwise. Persists changes to disk.
     */
    private void handleFavorito(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() != Material.PLAYER_HEAD) {
            plugin.getConfigManager().send(p, "favorite-not-head");
            return;
        }
        SkullMeta meta = (SkullMeta) hand.getItemMeta();
        if (meta == null) {
            plugin.getConfigManager().send(p, "favorite-no-texture");
            return;
        }
        String texture = null;
        PlayerProfile profile = meta.getPlayerProfile();
        if (profile != null) {
            for (ProfileProperty prop : profile.getProperties()) {
                if ("textures".equals(prop.getName())) {
                    texture = prop.getValue();
                    break;
                }
            }
        }
        if (texture == null || texture.isEmpty()) {
            plugin.getConfigManager().send(p, "favorite-no-texture");
            return;
        }
        String name = meta.hasDisplayName()
                ? ChatColor.stripColor(meta.getDisplayName())
                : "Favorito";
        if (name == null || name.isBlank()) name = "Favorito";

        if (plugin.getFavoritesManager().contains(texture)) {
            plugin.getFavoritesManager().removeByValue(texture);
            plugin.getConfigManager().send(p, "favorite-removed", "name", name);
        } else {
            plugin.getFavoritesManager().add(new HeadEntry(name, "", texture, ""));
            plugin.getConfigManager().send(p, "favorite-added", "name", name);
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> base = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("denegar", "admin", "reload", "help")) {
                if (s.startsWith(args[0].toLowerCase())) base.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            for (String s : Arrays.asList("list", "favorito")) {
                if (s.startsWith(args[1].toLowerCase())) base.add(s);
            }
        }
        return base;
    }
}

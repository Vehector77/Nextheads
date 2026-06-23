package com.vehector.nextheads.hooks;

import com.vehector.nextheads.Nextheads;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderHook extends PlaceholderExpansion {

    private final Nextheads plugin;

    public PlaceholderHook(Nextheads plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "nextheads"; }

    @Override
    public @NotNull String getAuthor() { return "Vehector"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        switch (params.toLowerCase()) {
            case "denied":
                return plugin.getDenyManager().isDenied(player.getUniqueId()) ? "true" : "false";
            case "denied_es":
                return plugin.getDenyManager().isDenied(player.getUniqueId()) ? "&aSi" : "&cNo";
            case "cooldown":
                if (!player.isOnline()) return "0";
                return String.valueOf(plugin.getCooldownManager().secondsLeft(player.getPlayer()));
            case "cooldown_max":
                return String.valueOf(plugin.getConfigManager().getCooldownSeconds());
            case "online":
                return String.valueOf(plugin.getServer().getOnlinePlayers().size());
            case "version":
                return plugin.getDescription().getVersion();
            case "author":
                return "Vehector";
        }
        return null;
    }
}

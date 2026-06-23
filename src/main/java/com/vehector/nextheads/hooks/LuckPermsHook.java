package com.vehector.nextheads.hooks;

import com.vehector.nextheads.Nextheads;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LuckPermsHook {

    private final Nextheads plugin;
    private boolean enabled;
    private LuckPerms api;

    public LuckPermsHook(Nextheads plugin) {
        this.plugin = plugin;
    }

    public void init() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                this.api = LuckPermsProvider.get();
                this.enabled = true;
                plugin.getLogger().info("LuckPerms detectado, integracion activa.");
            } catch (Throwable t) {
                this.enabled = false;
            }
        }
    }

    public String getPrefix(Player player) {
        if (!enabled) return plugin.getConfigManager().getDefaultRank();
        try {
            User user = api.getUserManager().getUser(player.getUniqueId());
            if (user == null) return plugin.getConfigManager().getDefaultRank();
            CachedMetaData meta = user.getCachedData().getMetaData();
            String prefix = meta.getPrefix();
            if (prefix == null || prefix.isEmpty()) return plugin.getConfigManager().getDefaultRank();
            return prefix;
        } catch (Throwable t) {
            return plugin.getConfigManager().getDefaultRank();
        }
    }

    public boolean isEnabled() { return enabled; }
}

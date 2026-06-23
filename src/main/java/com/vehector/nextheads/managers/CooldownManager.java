package com.vehector.nextheads.managers;

import com.vehector.nextheads.Nextheads;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Nextheads plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public CooldownManager(Nextheads plugin) {
        this.plugin = plugin;
    }

    public boolean isOnCooldown(Player p) {
        if (p.hasPermission("nextheads.bypass")) return false;
        Long until = cooldowns.get(p.getUniqueId());
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            cooldowns.remove(p.getUniqueId());
            return false;
        }
        return true;
    }

    public long secondsLeft(Player p) {
        Long until = cooldowns.get(p.getUniqueId());
        if (until == null) return 0;
        long left = until - System.currentTimeMillis();
        return left <= 0 ? 0 : (left / 1000) + 1;
    }

    public void apply(Player p) {
        if (p.hasPermission("nextheads.bypass")) return;
        int s = plugin.getConfigManager().getCooldownSeconds();
        cooldowns.put(p.getUniqueId(), System.currentTimeMillis() + s * 1000L);
    }
}

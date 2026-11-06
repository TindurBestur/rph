package net.resourcepackhub.feature;

import net.resourcepackhub.ResourcePackHub;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class TabManager {
    private final ResourcePackHub plugin;
    private BukkitTask task;
    public TabManager(ResourcePackHub plugin) { this.plugin = plugin; }
    public void start() {
        if (!plugin.getConfig().getBoolean("settings.tablist.enabled", true)) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            String header = plugin.getConfig().getString("settings.tablist.header", "");
            String footer = plugin.getConfig().getString("settings.tablist.footer", "");
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.setPlayerListHeaderFooter(format(header, p), format(footer, p));
            }
        }, 1L, 40L);
    }
    private String format(String s, Player p) {
        return ResourcePackHub.color(s.replace("%player%", p.getName())
            .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
            .replace("%ping%", String.valueOf(p.getPing()))
            .replace("\\n", "\n"));
    }
    public void stop() { if (task != null) task.cancel(); }
}

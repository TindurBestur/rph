package net.resourcepackhub.feature;

import net.resourcepackhub.ResourcePackHub;
import org.bukkit.block.CommandBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

import java.util.Locale;

public final class CommandBlockGuard implements Listener {
    private final ResourcePackHub plugin;
    public CommandBlockGuard(ResourcePackHub plugin) { this.plugin = plugin; }

    @EventHandler
    public void onCommandBlock(BlockRedstoneEvent event) {
        if (!(event.getBlock().getState() instanceof CommandBlock block)) return;
        if (!plugin.getConfig().getBoolean("settings.command-block.whitelist-mode", true)) return;
        String command = normalize(block.getCommand());
        if (!allowed(command)) {
            block.setCommand("");
            block.update(true, false);
            plugin.getLogger().warning("Blocked non-whitelisted command block command: " + command);
        }
    }
    private boolean allowed(String command) {
        if (command.isBlank()) return true;
        String root = command.split("\\s+")[0].replaceFirst("^/", "").toLowerCase(Locale.ROOT);
        return plugin.getConfig().getStringList("settings.command-block.allowed-commands").stream()
            .map(s -> s.toLowerCase(Locale.ROOT)).anyMatch(root::equals);
    }
    private String normalize(String command) { return command == null ? "" : command.trim(); }
}

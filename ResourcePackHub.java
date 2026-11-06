package net.resourcepackhub;

import net.resourcepackhub.command.RPHCommand;
import net.resourcepackhub.data.DataStore;
import net.resourcepackhub.feature.CommandBlockGuard;
import net.resourcepackhub.feature.RankManager;
import net.resourcepackhub.feature.TabManager;
import net.resourcepackhub.feature.ResourcePackManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ResourcePackHub extends JavaPlugin {
    private DataStore dataStore;
    private RankManager rankManager;
    private ResourcePackManager resourcePackManager;
    private TabManager tabManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.dataStore = new DataStore(this);
        this.dataStore.load();
        this.rankManager = new RankManager(this);
        this.resourcePackManager = new ResourcePackManager(this);
        this.tabManager = new TabManager(this);

        RPHCommand executor = new RPHCommand(this);
        String[] commands = {
            "resourcepackhub","rp","spawn","setspawn","home","sethome","delhome",
            "warp","setwarp","delwarp","tpa","tpaccept","tpdeny","back","feed","heal",
            "fly","gmc","gms","gma","gmsp","msg","reply","broadcast","ban","unban",
            "kick","mute","unmute","warn","warnings","freeze","unfreeze","vanish",
            "staffchat","rank","commandwhitelist","setmotd"
        };
        for (String name : commands) {
            PluginCommand command = getCommand(name);
            if (command != null) {
                command.setExecutor(executor);
                command.setTabCompleter(executor);
            }
        }

        Bukkit.getPluginManager().registerEvents(new CommandBlockGuard(this), this);
        Bukkit.getPluginManager().registerEvents(executor, this);
        tabManager.start();
        getLogger().info("ResourcePackHub enabled. Loaded " + commands.length + " command routes.");
    }

    @Override
    public void onDisable() {
        if (tabManager != null) tabManager.stop();
        if (dataStore != null) dataStore.saveAll();
    }

    public DataStore data() { return dataStore; }
    public RankManager ranks() { return rankManager; }
    public ResourcePackManager packs() { return resourcePackManager; }
    public TabManager tab() { return tabManager; }

    public String prefix() {
        return color(getConfig().getString("settings.prefix", "&8[&bResourcePackHub&8] &7"));
    }

    public static String color(String input) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}

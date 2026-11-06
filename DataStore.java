package net.resourcepackhub.data;

import net.resourcepackhub.ResourcePackHub;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public final class DataStore {
    private final ResourcePackHub plugin;
    private YamlConfiguration homes, warps, moderation, settings;
    private File homesFile, warpsFile, moderationFile, settingsFile;

    public DataStore(ResourcePackHub plugin) { this.plugin = plugin; }

    public void load() {
        homesFile = file("homes.yml"); warpsFile = file("warps.yml");
        moderationFile = file("moderation.yml"); settingsFile = file("playerdata.yml");
        homes = YamlConfiguration.loadConfiguration(homesFile);
        warps = YamlConfiguration.loadConfiguration(warpsFile);
        moderation = YamlConfiguration.loadConfiguration(moderationFile);
        settings = YamlConfiguration.loadConfiguration(settingsFile);
    }

    private File file(String name) {
        File f = new File(plugin.getDataFolder(), name);
        if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
        try { if (!f.exists()) f.createNewFile(); } catch (IOException e) { plugin.getLogger().severe(e.getMessage()); }
        return f;
    }

    public void saveAll() { save(homes, homesFile); save(warps, warpsFile); save(moderation, moderationFile); save(settings, settingsFile); }
    private void save(YamlConfiguration y, File f) {
        try { y.save(f); } catch (IOException e) { plugin.getLogger().severe("Could not save " + f.getName() + ": " + e.getMessage()); }
    }

    public void setSpawn(Location loc) { settings.set("spawn", loc.serialize()); save(settings, settingsFile); }
    public Location getSpawn() {
        if (!settings.contains("spawn")) return null;
        return Location.deserialize(settings.getConfigurationSection("spawn").getValues(false));
    }

    public void setHome(Player p, String name, Location loc) { homes.set(p.getUniqueId() + "." + name.toLowerCase(), loc.serialize()); save(homes, homesFile); }
    public Location getHome(Player p, String name) {
        String path = p.getUniqueId() + "." + name.toLowerCase();
        if (!homes.contains(path)) return null;
        return Location.deserialize(homes.getConfigurationSection(path).getValues(false));
    }
    public boolean deleteHome(Player p, String name) {
        String path = p.getUniqueId() + "." + name.toLowerCase();
        if (!homes.contains(path)) return false;
        homes.set(path, null); save(homes, homesFile); return true;
    }
    public List<String> homes(Player p) { return homes.getConfigurationSection(p.getUniqueId().toString()) == null ? List.of() : List.copyOf(homes.getConfigurationSection(p.getUniqueId().toString()).getKeys(false)); }

    public void setWarp(String name, Location loc) { warps.set(name.toLowerCase(), loc.serialize()); save(warps, warpsFile); }
    public Location getWarp(String name) {
        if (!warps.contains(name.toLowerCase())) return null;
        return Location.deserialize(warps.getConfigurationSection(name.toLowerCase()).getValues(false));
    }
    public boolean deleteWarp(String name) {
        if (!warps.contains(name.toLowerCase())) return false;
        warps.set(name.toLowerCase(), null); save(warps, warpsFile); return true;
    }
    public List<String> warps() { return warps.getKeys(false).stream().toList(); }

    public void setRecord(String type, UUID uuid, Object value) { moderation.set(type + "." + uuid, value); save(moderation, moderationFile); }
    public boolean hasRecord(String type, UUID uuid) { return moderation.contains(type + "." + uuid); }
    public String getRecord(String type, UUID uuid) { return moderation.getString(type + "." + uuid); }
    public void removeRecord(String type, UUID uuid) { moderation.set(type + "." + uuid, null); save(moderation, moderationFile); }
    public void addWarning(UUID uuid, String warning) {
        List<String> list = moderation.getStringList("warnings." + uuid);
        list.add(warning); moderation.set("warnings." + uuid, list); save(moderation, moderationFile);
    }
    public List<String> warnings(UUID uuid) { return moderation.getStringList("warnings." + uuid); }
}

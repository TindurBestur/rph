package net.resourcepackhub.feature;

import net.resourcepackhub.ResourcePackHub;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class RankManager {
    private final ResourcePackHub plugin;
    private final File file;
    private final YamlConfiguration yaml;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public RankManager(ResourcePackHub plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "ranks.yml");
        if (!file.exists()) try { file.createNewFile(); } catch (IOException ignored) {}
        yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.contains("ranks.default")) {
            yaml.set("ranks.default.prefix", "&7");
            yaml.set("ranks.default.permissions", List.of());
            save();
        }
        Bukkit.getOnlinePlayers().forEach(this::apply);
    }
    private void save() { try { yaml.save(file); } catch (IOException e) { plugin.getLogger().severe(e.getMessage()); } }
    public Set<String> names() { return yaml.getConfigurationSection("ranks") == null ? Set.of() : yaml.getConfigurationSection("ranks").getKeys(false); }
    public boolean create(String name) {
        String p = "ranks." + name.toLowerCase();
        if (yaml.contains(p)) return false;
        yaml.set(p + ".prefix", "&7"); yaml.set(p + ".permissions", new ArrayList<String>()); save(); return true;
    }
    public boolean delete(String name) {
        if (name.equalsIgnoreCase("default") || !yaml.contains("ranks." + name)) return false;
        yaml.set("ranks." + name, null); save(); return true;
    }
    public void assign(UUID uuid, String rank) { yaml.set("players." + uuid, rank.toLowerCase()); save(); Player p = Bukkit.getPlayer(uuid); if (p != null) apply(p); }
    public String getRank(UUID uuid) { return yaml.getString("players." + uuid, plugin.getConfig().getString("settings.default-rank", "default")); }
    public String prefix(UUID uuid) { return ResourcePackHub.color(yaml.getString("ranks." + getRank(uuid) + ".prefix", "&7")); }
    public boolean addPermission(String rank, String permission) {
        String path = "ranks." + rank.toLowerCase() + ".permissions";
        if (!yaml.contains("ranks." + rank.toLowerCase())) return false;
        List<String> list = yaml.getStringList(path);
        if (!list.contains(permission)) list.add(permission);
        yaml.set(path, list); save(); Bukkit.getOnlinePlayers().forEach(this::apply); return true;
    }
    public boolean removePermission(String rank, String permission) {
        String path = "ranks." + rank.toLowerCase() + ".permissions";
        if (!yaml.contains("ranks." + rank.toLowerCase())) return false;
        List<String> list = yaml.getStringList(path); list.remove(permission); yaml.set(path, list); save();
        Bukkit.getOnlinePlayers().forEach(this::apply); return true;
    }
    public void setPrefix(String rank, String prefix) {
        yaml.set("ranks." + rank.toLowerCase() + ".prefix", prefix); save();
        Bukkit.getOnlinePlayers().forEach(this::apply);
    }
    public void apply(Player player) {
        PermissionAttachment old = attachments.remove(player.getUniqueId());
        if (old != null) player.removeAttachment(old);
        PermissionAttachment attachment = player.addAttachment(plugin);
        for (String node : yaml.getStringList("ranks." + getRank(player.getUniqueId()) + ".permissions")) attachment.setPermission(node, true);
        attachments.put(player.getUniqueId(), attachment);
        player.setPlayerListName(prefix(player.getUniqueId()) + player.getName());
    }
    public void remove(Player player) {
        PermissionAttachment a = attachments.remove(player.getUniqueId());
        if (a != null) player.removeAttachment(a);
    }
}

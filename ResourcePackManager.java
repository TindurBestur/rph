package net.resourcepackhub.feature;

import net.resourcepackhub.ResourcePackHub;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

public final class ResourcePackManager {
    private final ResourcePackHub plugin;
    public ResourcePackManager(ResourcePackHub plugin) { this.plugin = plugin; }

    private UUID packId(String name) {
        return UUID.nameUUIDFromBytes((plugin.getName() + ":" + name.toLowerCase(Locale.ROOT)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public boolean send(Player player, String name) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("settings.resource-packs.packs." + name);
        if (section == null) return false;
        String url = section.getString("url");
        String sha = section.getString("sha1");
        if (url == null || !url.startsWith("https://") || sha == null || !sha.matches("(?i)[0-9a-f]{40}")) return false;
        try {
            player.addResourcePack(packId(name), url, HexFormat.of().parseHex(sha),
                "Resource pack: " + name, section.getBoolean("required", false));
            return true;
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid resource pack configuration for " + name + ": " + ex.getMessage());
            return false;
        }
    }

    public boolean disable(Player player, String name) {
        if (plugin.getConfig().getConfigurationSection("settings.resource-packs.packs." + name) == null) return false;
        player.removeResourcePack(packId(name));
        return true;
    }

    public int sendAll() {
        ConfigurationSection packs = plugin.getConfig().getConfigurationSection("settings.resource-packs.packs");
        if (packs == null) return 0;
        int sent = 0;
        for (String name : packs.getKeys(false)) {
            for (Player p : Bukkit.getOnlinePlayers()) if (send(p, name)) sent++;
        }
        return sent;
    }

    public int disableAll() {
        ConfigurationSection packs = plugin.getConfig().getConfigurationSection("settings.resource-packs.packs");
        if (packs == null) return 0;
        int removed = 0;
        for (String name : packs.getKeys(false)) {
            for (Player p : Bukkit.getOnlinePlayers()) if (disable(p, name)) removed++;
        }
        return removed;
    }

    public ItemStack createModelItem(String model, String materialName) {
        Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
        if (material == null || !material.isItem()) return null;
        String rawKey = plugin.getConfig().getString("settings.item-models." + model);
        if (rawKey == null) return null;
        NamespacedKey key = NamespacedKey.fromString(rawKey);
        if (key == null) return null;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        meta.setItemModel(key);
        meta.setDisplayName(ResourcePackHub.color("&f" + model.replace('_', ' ')));
        item.setItemMeta(meta);
        return item;
    }
}

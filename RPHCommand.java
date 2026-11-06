package net.resourcepackhub.command;

import net.resourcepackhub.ResourcePackHub;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.*;

public final class RPHCommand implements CommandExecutor, TabCompleter, Listener {
    private final ResourcePackHub plugin;
    private final Map<UUID, UUID> requests = new HashMap<>();
    private final Map<UUID, UUID> replies = new HashMap<>();
    private final Map<UUID, Location> previous = new HashMap<>();
    private final Set<UUID> frozen = new HashSet<>();
    private final Set<UUID> vanished = new HashSet<>();

    public RPHCommand(ResourcePackHub plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String c = command.getName().toLowerCase(Locale.ROOT);
        if (c.equals("resourcepackhub")) { help(sender); return true; }
        if (c.equals("rp")) return resourcePack(sender, args);
        if (c.equals("rank")) return rank(sender, args);
        if (c.equals("commandwhitelist")) return whitelist(sender, args);
        if (c.equals("setmotd")) { if (args.length == 0) return usage(sender, "/setmotd <message>"); plugin.getConfig().set("settings.motd", String.join(" ", args)); plugin.saveConfig(); msg(sender, "MOTD saved."); return true; }
        if (!(sender instanceof Player p) && needsPlayer(c)) { msg(sender, "This command can only be used by a player."); return true; }

        Player p = sender instanceof Player ? (Player) sender : null;
        switch (c) {
            case "spawn" -> { Location l = plugin.data().getSpawn(); if (l == null) msg(sender, "Spawn has not been set."); else teleport(p, l); }
            case "setspawn" -> { plugin.data().setSpawn(p.getLocation()); msg(sender, "Spawn set."); }
            case "sethome" -> { String n = args.length > 0 ? args[0] : "home"; if (plugin.data().homes(p).size() >= plugin.getConfig().getInt("settings.homes-per-player", 5) && !plugin.data().homes(p).contains(n.toLowerCase())) msg(sender, "You have reached your home limit."); else { plugin.data().setHome(p,n,p.getLocation()); msg(sender,"Home '"+n+"' set."); } }
            case "home" -> { if (args.length == 0) return usage(sender,"/home <name>"); Location l=plugin.data().getHome(p,args[0]); if(l==null) msg(sender,"Home not found."); else teleport(p,l); }
            case "delhome" -> { if(args.length==0) return usage(sender,"/delhome <name>"); msg(sender, plugin.data().deleteHome(p,args[0])?"Home deleted.":"Home not found."); }
            case "setwarp" -> { if(args.length==0) return usage(sender,"/setwarp <name>"); plugin.data().setWarp(args[0],p.getLocation()); msg(sender,"Warp set."); }
            case "warp" -> { if(args.length==0) { msg(sender,"Warps: "+String.join(", ",plugin.data().warps())); } else { Location l=plugin.data().getWarp(args[0]); if(l==null) msg(sender,"Warp not found."); else teleport(p,l); } }
            case "delwarp" -> { if(args.length==0) return usage(sender,"/delwarp <name>"); msg(sender,plugin.data().deleteWarp(args[0])?"Warp deleted.":"Warp not found."); }
            case "back" -> { Location l=previous.get(p.getUniqueId()); if(l==null) msg(sender,"No previous location saved."); else teleport(p,l); }
            case "tpa" -> { if(args.length==0) return usage(sender,"/tpa <player>"); Player target=Bukkit.getPlayerExact(args[0]); if(target==null) msg(sender,"Player not online."); else if(target.equals(p)) msg(sender,"You cannot request teleport to yourself."); else { requests.put(target.getUniqueId(),p.getUniqueId()); target.sendMessage(plugin.prefix()+p.getName()+" wants to teleport to you. Use /tpaccept."); msg(sender,"Teleport request sent."); } }
            case "tpaccept" -> { UUID from=requests.remove(p.getUniqueId()); Player requester=from==null?null:Bukkit.getPlayer(from); if(requester==null) msg(sender,"No pending request."); else { teleport(requester,p.getLocation()); msg(sender,"Request accepted."); } }
            case "tpdeny" -> { UUID from=requests.remove(p.getUniqueId()); if(from==null) msg(sender,"No pending request."); else { Player requester=Bukkit.getPlayer(from); if(requester!=null) msg(requester,"Your teleport request was denied."); msg(sender,"Request denied."); } }
            case "feed" -> { p.setFoodLevel(20); p.setSaturation(20); msg(sender,"Fed."); }
            case "heal" -> { p.setHealth(p.getMaxHealth()); p.setFireTicks(0); msg(sender,"Healed."); }
            case "fly" -> { p.setAllowFlight(!p.getAllowFlight()); if(!p.getAllowFlight()) p.setFlying(false); msg(sender,"Flight "+(p.getAllowFlight()?"enabled":"disabled")+"."); }
            case "gmc" -> p.setGameMode(GameMode.CREATIVE);
            case "gms" -> p.setGameMode(GameMode.SURVIVAL);
            case "gma" -> p.setGameMode(GameMode.ADVENTURE);
            case "gmsp" -> p.setGameMode(GameMode.SPECTATOR);
            case "msg" -> { if(args.length<2) return usage(sender,"/msg <player> <message>"); Player t=Bukkit.getPlayerExact(args[0]); if(t==null) msg(sender,"Player not online."); else { String text=String.join(" ",Arrays.copyOfRange(args,1,args.length)); t.sendMessage("§d[From "+p.getName()+"] "+text); p.sendMessage("§d[To "+t.getName()+"] "+text); replies.put(p.getUniqueId(),t.getUniqueId()); replies.put(t.getUniqueId(),p.getUniqueId()); } }
            case "reply" -> { if(args.length==0) return usage(sender,"/reply <message>"); UUID id=replies.get(p.getUniqueId()); Player t=id==null?null:Bukkit.getPlayer(id); if(t==null) msg(sender,"Nobody to reply to."); else { String text=String.join(" ",args); t.sendMessage("§d[From "+p.getName()+"] "+text); p.sendMessage("§d[To "+t.getName()+"] "+text); } }
            case "broadcast" -> Bukkit.broadcastMessage("§6[Broadcast] §f"+String.join(" ",args));
            case "ban" -> { if(args.length==0) return usage(sender,"/ban <player> [reason]"); OfflinePlayer t=Bukkit.getOfflinePlayer(args[0]); String reason=args.length>1?String.join(" ",Arrays.copyOfRange(args,1,args.length)):"Banned by staff"; Bukkit.getBanList(BanList.Type.NAME).addBan(t.getName(),reason,null,sender.getName()); Player online=t.getPlayer(); if(online!=null) online.kickPlayer(reason); msg(sender,"Player banned."); }
            case "unban" -> { if(args.length==0) return usage(sender,"/unban <player>"); Bukkit.getBanList(BanList.Type.NAME).pardon(args[0]); msg(sender,"Unban attempted."); }
            case "kick" -> { if(args.length==0) return usage(sender,"/kick <player> [reason]"); Player t=Bukkit.getPlayerExact(args[0]); if(t==null) msg(sender,"Player not online."); else { t.kickPlayer(args.length>1?String.join(" ",Arrays.copyOfRange(args,1,args.length)):"Kicked by staff"); msg(sender,"Player kicked."); } }
            case "mute" -> { if(args.length==0) return usage(sender,"/mute <player>"); OfflinePlayer t=Bukkit.getOfflinePlayer(args[0]); plugin.data().setRecord("mutes",t.getUniqueId(),"Muted by "+sender.getName()); msg(sender,"Player muted."); }
            case "unmute" -> { if(args.length==0) return usage(sender,"/unmute <player>"); plugin.data().removeRecord("mutes",Bukkit.getOfflinePlayer(args[0]).getUniqueId()); msg(sender,"Player unmuted."); }
            case "warn" -> { if(args.length<2) return usage(sender,"/warn <player> <reason>"); OfflinePlayer t=Bukkit.getOfflinePlayer(args[0]); plugin.data().addWarning(t.getUniqueId(),Instant.now()+" | "+sender.getName()+" | "+String.join(" ",Arrays.copyOfRange(args,1,args.length))); msg(sender,"Warning recorded."); }
            case "warnings" -> { if(args.length==0) return usage(sender,"/warnings <player>"); OfflinePlayer t=Bukkit.getOfflinePlayer(args[0]); List<String> w=plugin.data().warnings(t.getUniqueId()); msg(sender,"Warnings ("+w.size()+"): "+String.join(" || ",w)); }
            case "freeze" -> { if(args.length==0) return usage(sender,"/freeze <player>"); Player t=Bukkit.getPlayerExact(args[0]); if(t==null) msg(sender,"Player not online."); else { frozen.add(t.getUniqueId()); msg(sender,"Player frozen."); t.sendMessage(plugin.prefix()+"You have been frozen."); } }
            case "unfreeze" -> { if(args.length==0) return usage(sender,"/unfreeze <player>"); Player t=Bukkit.getPlayerExact(args[0]); if(t!=null) frozen.remove(t.getUniqueId()); msg(sender,"Player unfrozen."); }
            case "vanish" -> { if(vanished.add(p.getUniqueId())) { for(Player other:Bukkit.getOnlinePlayers()) if(!other.hasPermission("resourcepackhub.vanish.see")) other.hidePlayer(plugin,p); msg(sender,"Vanish enabled."); } else { vanished.remove(p.getUniqueId()); for(Player other:Bukkit.getOnlinePlayers()) other.showPlayer(plugin,p); msg(sender,"Vanish disabled."); } }
            case "staffchat" -> { if(args.length==0) return usage(sender,"/staffchat <message>"); String line="§b[Staff] §f"+sender.getName()+": "+String.join(" ",args); for(Player online:Bukkit.getOnlinePlayers()) if(online.hasPermission("resourcepackhub.staffchat")) online.sendMessage(line); plugin.getLogger().info(line); }
            default -> { msg(sender,"Unknown command route. Use /resourcepackhub."); }
        }
        return true;
    }

    private boolean resourcePack(CommandSender s,String[] a) {
        if(a.length==0) return usage(s,"/rp <list|enable|disable|reload|giveitem>");
        String sub=a[0].toLowerCase();
        if(sub.equals("list")) {
            var sec=plugin.getConfig().getConfigurationSection("settings.resource-packs.packs");
            msg(s,"Configured packs: "+(sec==null?"none":String.join(", ",sec.getKeys(false)))); return true;
        }
        if(sub.equals("enable") || sub.equals("reload")) {
            if(a.length<2) return usage(s,"/rp "+sub+" <name|all>");
            if(a[1].equalsIgnoreCase("all")) { int count=plugin.packs().sendAll(); msg(s,"Sent configured packs to players ("+count+" deliveries)."); }
            else if(s instanceof Player p) msg(s,plugin.packs().send(p,a[1])?"Sent pack "+a[1]+".":"Pack missing or invalid in config.");
            else { int count=0; for(Player p:Bukkit.getOnlinePlayers()) if(plugin.packs().send(p,a[1])) count++; msg(s,"Sent "+a[1]+" to "+count+" players."); }
            return true;
        }
        if(sub.equals("disable")) {
            if(a.length<2) return usage(s,"/rp disable <name|all>");
            if(a[1].equalsIgnoreCase("all")) { int count=plugin.packs().disableAll(); msg(s,"Removed configured packs ("+count+" removals)."); }
            else { int count=0; for(Player p:Bukkit.getOnlinePlayers()) if(plugin.packs().disable(p,a[1])) count++; msg(s,"Removed pack from "+count+" players."); }
            return true;
        }
        if(sub.equals("giveitem")) {
            if(a.length<4 || !(s instanceof Player p)) return usage(s,"/rp giveitem <pack> <model> <material>");
            if(plugin.getConfig().getConfigurationSection("settings.resource-packs.packs."+a[1])==null) { msg(s,"Unknown resource pack."); return true; }
            ItemStack item=plugin.packs().createModelItem(a[2],a[3]);
            if(item==null) msg(s,"Unknown model or invalid material. Configure settings.item-models."+a[2]+".");
            else { p.getInventory().addItem(item); msg(s,"Custom model item given."); }
            return true;
        }
        return usage(s,"/rp <list|enable|disable|reload|giveitem>");
    }

    private boolean rank(CommandSender s,String[] a) {
        if(a.length==0) return usage(s,"/rank <create|delete|set|list|prefix|permission>");
        switch(a[0].toLowerCase()) {
            case "list" -> msg(s,"Ranks: "+String.join(", ",plugin.ranks().names()));
            case "create" -> { if(a.length<2) return usage(s,"/rank create <name>"); msg(s,plugin.ranks().create(a[1])?"Rank created.":"Rank already exists."); }
            case "delete" -> { if(a.length<2) return usage(s,"/rank delete <name>"); msg(s,plugin.ranks().delete(a[1])?"Rank deleted.":"Cannot delete rank (missing or default)."); }
            case "set" -> { if(a.length<3) return usage(s,"/rank set <player> <rank>"); Player p=Bukkit.getPlayerExact(a[1]); if(p==null) msg(s,"Player must be online."); else if(!plugin.ranks().names().contains(a[2].toLowerCase())) msg(s,"Rank does not exist."); else { plugin.ranks().assign(p.getUniqueId(),a[2]); msg(s,"Rank assigned."); } }
            case "prefix" -> { if(a.length<3) return usage(s,"/rank prefix <rank> <prefix>"); plugin.ranks().setPrefix(a[1],String.join(" ",Arrays.copyOfRange(a,2,a.length))); msg(s,"Prefix updated."); }
            case "permission" -> { if(a.length<4) return usage(s,"/rank permission <add|remove> <rank> <node>"); boolean ok=a[1].equalsIgnoreCase("add")?plugin.ranks().addPermission(a[2],a[3]):a[1].equalsIgnoreCase("remove")&&plugin.ranks().removePermission(a[2],a[3]); msg(s,ok?"Rank permission updated.":"Unknown rank or operation."); }
            default -> msg(s,"Unknown rank subcommand.");
        }
        return true;
    }

    private boolean whitelist(CommandSender s,String[] a) {
        if(a.length==0) return usage(s,"/commandwhitelist <add|remove|list|reload>");
        List<String> allowed=plugin.getConfig().getStringList("settings.command-block.allowed-commands");
        switch(a[0].toLowerCase()) {
            case "list" -> msg(s,"Allowed command roots: "+String.join(", ",allowed));
            case "add" -> { if(a.length<2) return usage(s,"/commandwhitelist add <command>"); if(!allowed.contains(a[1].toLowerCase())) allowed.add(a[1].toLowerCase()); plugin.getConfig().set("settings.command-block.allowed-commands",allowed); plugin.saveConfig(); msg(s,"Command added."); }
            case "remove" -> { if(a.length<2) return usage(s,"/commandwhitelist remove <command>"); allowed.removeIf(x->x.equalsIgnoreCase(a[1])); plugin.getConfig().set("settings.command-block.allowed-commands",allowed); plugin.saveConfig(); msg(s,"Command removed."); }
            case "reload" -> { plugin.reloadConfig(); msg(s,"Configuration reloaded."); }
            default -> msg(s,"Unknown subcommand.");
        }
        return true;
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) {
        plugin.ranks().apply(e.getPlayer());
        String motd=plugin.getConfig().getString("settings.motd");
        if(motd!=null&&!motd.isBlank()) e.getPlayer().sendMessage(ResourcePackHub.color(motd.replace("%player%",e.getPlayer().getName())));
        for(UUID id:vanished) { Player v=Bukkit.getPlayer(id); if(v!=null&&!e.getPlayer().hasPermission("resourcepackhub.vanish.see")) e.getPlayer().hidePlayer(plugin,v); }
    }
    @EventHandler public void onQuit(PlayerQuitEvent e) { plugin.ranks().remove(e.getPlayer()); frozen.remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void onMove(PlayerMoveEvent e) {
        if(frozen.contains(e.getPlayer().getUniqueId()) && e.getTo()!=null &&
            (e.getFrom().getBlockX()!=e.getTo().getBlockX() || e.getFrom().getBlockY()!=e.getTo().getBlockY() || e.getFrom().getBlockZ()!=e.getTo().getBlockZ())) {
            e.setTo(e.getFrom());
        }
    }
    @EventHandler public void onCommand(PlayerCommandPreprocessEvent e) {
        if(plugin.data().hasRecord("mutes",e.getPlayer().getUniqueId()) &&
            (e.getMessage().toLowerCase().startsWith("/say ") || e.getMessage().toLowerCase().startsWith("/me "))) {
            e.setCancelled(true); msg(e.getPlayer(),"You are muted.");
        }
    }
    private void teleport(Player p, Location l) { if(p==null)return; previous.put(p.getUniqueId(),p.getLocation()); p.teleportAsync(l); msg(p,"Teleported."); }
    private boolean needsPlayer(String c) { return Set.of("spawn","setspawn","home","sethome","delhome","warp","setwarp","delwarp","back","tpa","tpaccept","tpdeny","feed","heal","fly","gmc","gms","gma","gmsp","msg","reply","vanish").contains(c); }
    private void msg(CommandSender s,String text) { s.sendMessage(plugin.prefix()+ResourcePackHub.color(text)); }
    private boolean usage(CommandSender s,String text) { msg(s,"Usage: "+text); return true; }
    private void help(CommandSender s) {
        msg(s,"ResourcePackHub "+plugin.getDescription().getVersion());
        msg(s,"Core: /spawn /home /warp /tpa /back /feed /heal /fly");
        msg(s,"Staff: /ban /kick /mute /warn /freeze /vanish /staffchat");
        msg(s,"Systems: /rank /rp /commandwhitelist");
    }

    @Override public List<String> onTabComplete(CommandSender s, Command c, String alias, String[] a) {
        String n=c.getName().toLowerCase();
        if(n.equals("rp") && a.length==1) return filter(List.of("list","enable","disable","reload","giveitem"),a[0]);
        if(n.equals("rank") && a.length==1) return filter(List.of("create","delete","set","list","prefix","permission"),a[0]);
        if(n.equals("commandwhitelist") && a.length==1) return filter(List.of("add","remove","list","reload"),a[0]);
        if(n.equals("warp") && a.length==1) return filter(plugin.data().warps(),a[0]);
        if(n.equals("home") && a.length==1 && s instanceof Player p) return filter(plugin.data().homes(p),a[0]);
        if(Set.of("ban","kick","mute","unmute","warn","warnings","freeze","unfreeze","tpa","msg").contains(n) && a.length==1)
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),a[0]);
        return List.of();
    }
    private List<String> filter(Collection<String> values,String prefix) {
        return values.stream().filter(x->x.toLowerCase().startsWith(prefix.toLowerCase())).sorted().toList();
    }
}

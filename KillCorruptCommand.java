package dev.killcorrupt;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class KillCorruptCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of("add", "remove", "list", "toggle", "reload");

    private final KillCorrupt plugin;

    public KillCorruptCommand(KillCorrupt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("killcorrupt.use")) {
            sender.sendMessage(plugin.msg("no-permission", null, 0));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(plugin.msg("usage", null, 0));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                plugin.reloadAll();
                sender.sendMessage(plugin.msg("reloaded", null, 0));
            }
            case "list" -> {
                var all = plugin.getCorrupted();
                if (all.isEmpty()) {
                    sender.sendMessage(plugin.msg("list-empty", null, 0));
                    return true;
                }
                sender.sendMessage(plugin.msg("list-header", null, all.size()));
                for (UUID uuid : all) {
                    String name = Bukkit.getOfflinePlayer(uuid).getName();
                    sender.sendMessage(net.kyori.adventure.text.Component.text(
                            " - " + (name != null ? name : uuid.toString())));
                }
            }
            case "add", "remove", "toggle" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.msg("usage", null, 0));
                    return true;
                }
                OfflinePlayer target = resolve(args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.msg("unknown-player", args[1], 0));
                    return true;
                }
                String name = target.getName() != null ? target.getName() : args[1];
                UUID uuid = target.getUniqueId();

                if (sub.equals("toggle")) {
                    sub = plugin.isCorrupted(uuid) ? "remove" : "add";
                }
                if (sub.equals("add")) {
                    sender.sendMessage(plugin.addCorrupted(uuid)
                            ? plugin.msg("added", name, 0)
                            : plugin.msg("already", name, 0));
                } else {
                    sender.sendMessage(plugin.removeCorrupted(uuid)
                            ? plugin.msg("removed", name, 0)
                            : plugin.msg("not-on-list", name, 0));
                }
            }
            default -> sender.sendMessage(plugin.msg("usage", null, 0));
        }
        return true;
    }

    private OfflinePlayer resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("killcorrupt.use")) return List.of();
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String s : SUBS) if (s.startsWith(prefix)) out.add(s);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            String prefix = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("remove")) {
                for (UUID uuid : plugin.getCorrupted()) {
                    String n = Bukkit.getOfflinePlayer(uuid).getName();
                    if (n != null && n.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(n);
                }
            } else if (sub.equals("add") || sub.equals("toggle")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(p.getName());
                }
            }
        }
        return out;
    }
}

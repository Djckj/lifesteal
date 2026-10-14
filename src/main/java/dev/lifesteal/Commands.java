package dev.lifesteal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Commands implements CommandExecutor, TabCompleter {
    private final LifestealPlugin plugin;
    private final HeartManager hearts;

    public Commands(LifestealPlugin plugin) {
        this.plugin = plugin;
        this.hearts = plugin.hearts();
    }

    private static void say(CommandSender s, String msg, NamedTextColor c) {
        s.sendMessage(Component.text(msg, c));
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (cmd.getName().equalsIgnoreCase("withdraw")) return withdraw(s, a);
        if (!s.hasPermission("lifesteal.admin")) { say(s, "No permission.", NamedTextColor.RED); return true; }
        if (a.length == 0) return false;

        switch (a[0].toLowerCase()) {
            case "get", "set", "add", "remove" -> hearts(s, a);
            case "revive" -> revive(s, a);
            case "give" -> give(s, a);
            case "reset" -> reset(s, a);
            case "status" -> {
                for (Weapon w : Weapon.values()) {
                    say(s, w.displayName() + ": " + (plugin.data().isCrafted(w) ? "exists right now" : "doesn't exist (can be crafted)"),
                            NamedTextColor.GRAY);
                }
            }
            default -> { return false; }
        }
        return true;
    }

    private boolean withdraw(CommandSender s, String[] a) {
        if (!(s instanceof Player p)) { say(s, "Players only.", NamedTextColor.RED); return true; }
        int amount = 1;
        if (a.length > 0) {
            try { amount = Integer.parseInt(a[0]); } catch (NumberFormatException ex) { return false; }
        }
        int min = plugin.getConfig().getInt("hearts.min-after-withdraw", 1);
        UUID id = p.getUniqueId();
        int cur = hearts.get(id);
        if (amount < 1 || cur - amount < min) {
            say(p, "You must keep at least " + min + " heart(s).", NamedTextColor.RED);
            return true;
        }
        hearts.set(id, cur - amount);
        hearts.give(p, hearts.heartItem(amount));
        say(p, "Withdrew " + amount + " heart(s).", NamedTextColor.RED);
        return true;
    }

    @SuppressWarnings("deprecation")
    private void hearts(CommandSender s, String[] a) {
        if (a.length < 2) { say(s, "Usage: /lifesteal " + a[0] + " <player>" + (a[0].equalsIgnoreCase("get") ? "" : " <n>"), NamedTextColor.RED); return; }
        OfflinePlayer t = Bukkit.getOfflinePlayer(a[1]);
        UUID id = t.getUniqueId();
        int cur = hearts.get(id);
        if (a[0].equalsIgnoreCase("get")) { say(s, a[1] + " has " + cur + " hearts.", NamedTextColor.GRAY); return; }
        if (a.length < 3) { say(s, "Missing number.", NamedTextColor.RED); return; }
        int n;
        try { n = Integer.parseInt(a[2]); } catch (NumberFormatException ex) { say(s, "Not a number.", NamedTextColor.RED); return; }
        int result = switch (a[0].toLowerCase()) {
            case "set" -> n;
            case "add" -> cur + n;
            default -> cur - n;
        };
        result = Math.max(1, result);
        hearts.set(id, result);
        say(s, a[1] + " now has " + result + " hearts.", NamedTextColor.GREEN);
    }

    @SuppressWarnings("deprecation")
    private void revive(CommandSender s, String[] a) {
        if (a.length < 2) { say(s, "Usage: /lifesteal revive <player>", NamedTextColor.RED); return; }
        OfflinePlayer t = Bukkit.getOfflinePlayer(a[1]);
        hearts.set(t.getUniqueId(), hearts.reviveHearts());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pardon " + a[1]);
        say(s, "Revived " + a[1] + " with " + hearts.reviveHearts() + " hearts.", NamedTextColor.GREEN);
    }

    private void give(CommandSender s, String[] a) {
        if (a.length < 3) { say(s, "Usage: /lifesteal give <lifestealer|sword_axe|armor_breaker> <player>", NamedTextColor.RED); return; }
        Weapon w = Weapon.fromId(a[1]);
        Player t = Bukkit.getPlayer(a[2]);
        if (w == null || t == null) { say(s, "Unknown weapon or player offline.", NamedTextColor.RED); return; }
        hearts.give(t, w.create());
        plugin.data().setCrafted(w, "admin:" + s.getName());
        say(s, "Gave " + w.displayName() + " to " + t.getName() + " (marked as existing).", NamedTextColor.GREEN);
    }

    private void reset(CommandSender s, String[] a) {
        Weapon w = a.length > 1 ? Weapon.fromId(a[1]) : null;
        if (w == null) { say(s, "Usage: /lifesteal reset <weapon>", NamedTextColor.RED); return; }
        plugin.data().clearCrafted(w);
        say(s, w.displayName() + " can be crafted again (marked as not existing).", NamedTextColor.GREEN);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (!cmd.getName().equalsIgnoreCase("lifesteal")) return out;
        if (a.length == 1) {
            out.addAll(List.of("get", "set", "add", "remove", "revive", "give", "reset", "status"));
        } else if (a.length == 2 && (a[0].equalsIgnoreCase("give") || a[0].equalsIgnoreCase("reset"))) {
            for (Weapon w : Weapon.values()) out.add(w.id());
        } else if (a.length == 2 || (a.length == 3 && a[0].equalsIgnoreCase("give"))) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        }
        String last = a[a.length - 1].toLowerCase();
        out.removeIf(x -> !x.toLowerCase().startsWith(last));
        return out;
    }
}

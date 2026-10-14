package dev.lifesteal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.UUID;

public final class LifestealListener implements Listener {
    private final LifestealPlugin plugin;
    private final HeartManager hearts;

    public LifestealListener(LifestealPlugin plugin) {
        this.plugin = plugin;
        this.hearts = plugin.hearts();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        if (hearts.get(id) <= 0) hearts.set(id, hearts.reviveHearts()); // was unbanned manually
        hearts.apply(p);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            hearts.apply(p);
        });
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == victim) killer = null;

        int steal = 1;
        if (killer != null && Weapon.of(killer.getInventory().getItemInMainHand()) == Weapon.LIFESTEALER
                && meleeBy(victim, killer)) {
            steal = plugin.getConfig().getInt("weapons.lifestealer.hearts-stolen", 3);
        }

        boolean loseAny = plugin.getConfig().getBoolean("hearts.lose-on-any-death", true);
        int lost = 0;
        boolean eliminated = false;
        if (killer != null || loseAny) {
            UUID vid = victim.getUniqueId();
            int cur = hearts.get(vid);
            lost = Math.min(steal, cur);
            int remaining = cur - lost;
            hearts.set(vid, Math.max(remaining, 0));
            eliminated = remaining <= 0;
        }

        if (killer != null && lost > 0) {
            int gained = hearts.gain(killer, lost);
            killer.sendMessage(Component.text("You stole " + lost + " heart" + (lost == 1 ? "" : "s")
                    + (gained < lost ? " (some came as Heart items)" : "") + "!", NamedTextColor.RED));
        }
        if (eliminated) hearts.eliminate(victim);
    }

    private boolean meleeBy(Player victim, Player killer) {
        EntityDamageEvent d = victim.getLastDamageCause();
        return d instanceof EntityDamageByEntityEvent ed
                && ed.getDamager().equals(killer)
                && (ed.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || ed.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK);
    }

    @EventHandler
    public void onUseHeart(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!hearts.isHeart(e.getItem())) return;
        e.setCancelled(true);

        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        if (hearts.get(id) >= hearts.max()) {
            p.sendMessage(Component.text("You're already at the max of " + hearts.max() + " hearts.", NamedTextColor.RED));
            return;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        hand.setAmount(hand.getAmount() - 1);
        hearts.set(id, hearts.get(id) + 1);
        p.sendMessage(Component.text("+1 heart (" + hearts.get(id) + ")", NamedTextColor.RED));
    }
}

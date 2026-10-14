package dev.lifesteal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WeaponListener implements Listener {
    /** Containers a protected weapon is allowed to sit in (it always returns to the player). */
    private static final Set<InventoryType> OK_TOP =
            EnumSet.of(InventoryType.CRAFTING, InventoryType.ANVIL, InventoryType.GRINDSTONE, InventoryType.ENCHANTING);

    private static final Set<Enchantment> SWORD_EXTRAS = Set.of(
            Enchantment.FIRE_ASPECT, Enchantment.KNOCKBACK, Enchantment.LOOTING, Enchantment.SWEEPING_EDGE);

    private final LifestealPlugin plugin;

    public WeaponListener(LifestealPlugin plugin) {
        this.plugin = plugin;
    }

    /* ------------------------------------------------------------ crafting (once only) */

    private static Weapon recipeWeapon(Recipe r) {
        if (r instanceof Keyed k) {
            for (Weapon w : Weapon.values()) if (w.key().equals(k.getKey())) return w;
        }
        return null;
    }

    private static boolean containsWeapon(ItemStack[] matrix) {
        for (ItemStack i : matrix) if (Weapon.of(i) != null) return true;
        return false;
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent e) {
        Weapon w = recipeWeapon(e.getRecipe());
        if (w == null) return;
        if (plugin.data().isCrafted(w) || containsWeapon(e.getInventory().getMatrix())) {
            e.getInventory().setResult(null);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        Weapon w = recipeWeapon(e.getRecipe());
        if (w == null) return;
        if (!(e.getWhoClicked() instanceof Player p)
                || plugin.data().isCrafted(w)
                || containsWeapon(e.getInventory().getMatrix())
                || (e.isShiftClick() && p.getInventory().firstEmpty() == -1)) {
            e.setCancelled(true);
            return;
        }
        plugin.data().setCrafted(w, p.getName());
        Bukkit.broadcast(Component.text(p.getName() + " has crafted the " + w.displayName() + "!", w.color()));
    }

    @EventHandler
    public void onCrafter(CrafterCraftEvent e) {
        if (recipeWeapon(e.getRecipe()) != null) e.setCancelled(true); // no auto-crafters
    }

    /* ------------------------------------------------------------ protection */

    private static boolean prot(ItemStack i) { return Weapon.blockedFromContainers(i); }

    private static boolean isBundle(ItemStack i) {
        return i != null && !i.getType().isAir() && i.hasItemMeta() && i.getItemMeta() instanceof BundleMeta;
    }

    private static void deny(Cancellable e, Player p) {
        e.setCancelled(true);
        p.sendMessage(Component.text("That weapon can't be stored in containers.", NamedTextColor.RED));
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();
        boolean cursorP = prot(cursor);
        boolean currentP = prot(current);
        if (!cursorP && !currentP && e.getClick() != ClickType.NUMBER_KEY && e.getClick() != ClickType.SWAP_OFFHAND) return;

        if ((cursorP && isBundle(current)) || (currentP && isBundle(cursor))) { deny(e, p); return; }
        ClickType c = e.getClick();

        Inventory top = e.getView().getTopInventory();
        if (OK_TOP.contains(top.getType())) return;
        boolean inTop = e.getRawSlot() >= 0 && e.getRawSlot() < top.getSize();

        if (inTop && cursorP) { deny(e, p); return; }
        if (!inTop && currentP && e.isShiftClick()) { deny(e, p); return; }
        if (inTop && c == ClickType.NUMBER_KEY && prot(p.getInventory().getItem(e.getHotbarButton()))) { deny(e, p); return; }
        if (inTop && c == ClickType.SWAP_OFFHAND && prot(p.getInventory().getItemInOffHand())) deny(e, p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !prot(e.getOldCursor())) return;
        Inventory top = e.getView().getTopInventory();
        if (OK_TOP.contains(top.getType())) return;
        for (int s : e.getRawSlots()) {
            if (s < top.getSize()) { deny(e, p); return; }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (!prot(e.getPlayer().getInventory().getItem(e.getHand()))) return;
        Entity t = e.getRightClicked();
        if (t instanceof ItemFrame || t instanceof Allay) deny(e, e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent e) {
        if (prot(e.getPlayerItem())) deny(e, e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPot(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null
                && e.getClickedBlock().getType() == Material.DECORATED_POT && prot(e.getItem())) {
            deny(e, e.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopperPickup(InventoryPickupItemEvent e) {
        if (prot(e.getItem().getItemStack())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMobPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player) && prot(e.getItem().getItemStack())) e.setCancelled(true);
    }

    /* ------------------------------------------------------------ destroyed -> craftable again */

    private void destroyed(Weapon w, String how) {
        if (!plugin.data().isCrafted(w)) return;
        plugin.data().clearCrafted(w); // "crafted" flag really means "currently exists"
        Bukkit.broadcast(Component.text("The " + w.displayName() + " was " + how + " and can be crafted again!", w.color()));
    }

    /** Dropped weapon burned / exploded / despawned / fell into the void. */
    @EventHandler
    public void onItemRemoved(EntityRemoveEvent e) {
        if (!(e.getEntity() instanceof Item it)) return;
        EntityRemoveEvent.Cause c = e.getCause();
        if (c != EntityRemoveEvent.Cause.DEATH && c != EntityRemoveEvent.Cause.DESPAWN
                && c != EntityRemoveEvent.Cause.OUT_OF_WORLD) return;
        Weapon w = Weapon.of(it.getItemStack());
        if (w != null) destroyed(w, "destroyed");
    }

    /** Weapon ran out of durability. */
    @EventHandler
    public void onBreak(PlayerItemBreakEvent e) {
        Weapon w = Weapon.of(e.getBrokenItem());
        if (w != null) destroyed(w, "broken");
    }

    /* ------------------------------------------------------------ armor breaker axe */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity victim)) return;
        if (e.getCause() != DamageCause.ENTITY_ATTACK && e.getCause() != DamageCause.ENTITY_SWEEP_ATTACK) return;
        if (Weapon.of(p.getInventory().getItemInMainHand()) != Weapon.ARMOR_BREAKER) return;
        if (e.getFinalDamage() <= 0) return; // fully blocked
        EntityEquipment eq = victim.getEquipment();
        if (eq == null) return;

        int mult = plugin.getConfig().getInt("weapons.armor_breaker.durability-multiplier", 3);
        int vanilla = Math.max(1, (int) (e.getDamage() / 4.0));
        int extra = vanilla * (mult - 1); // vanilla already dealt 1x

        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack piece = eq.getItem(slot);
            if (piece == null || piece.getType().isAir()) continue;
            ItemMeta meta = piece.getItemMeta();
            int max = piece.getType().getMaxDurability();
            if (!(meta instanceof Damageable d) || meta.isUnbreakable() || max <= 0) continue;
            int nd = d.getDamage() + extra;
            if (nd >= max) {
                eq.setItem(slot, null);
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            } else {
                d.setDamage(nd);
                piece.setItemMeta(d);
                eq.setItem(slot, piece);
            }
        }
    }

    /* ------------------------------------------------------------ sword axe enchants */

    /** Lets the Sword Axe take Fire Aspect / Knockback / Looting / Sweeping Edge from books in an anvil. */
    @EventHandler
    public void onAnvil(PrepareAnvilEvent e) {
        ItemStack left = e.getInventory().getItem(0);
        ItemStack right = e.getInventory().getItem(1);
        if (Weapon.of(left) != Weapon.SWORD_AXE || right == null || right.getType().isAir()) return;

        Map<Enchantment, Integer> add = right.getItemMeta() instanceof EnchantmentStorageMeta sm
                ? sm.getStoredEnchants() : right.getEnchantments();
        if (add.isEmpty()) return;

        ItemStack result = (e.getResult() != null && !e.getResult().getType().isAir()) ? e.getResult().clone() : left.clone();
        ItemMeta meta = result.getItemMeta();
        boolean changed = false;
        int cost = 0;
        for (Map.Entry<Enchantment, Integer> en : add.entrySet()) {
            Enchantment ench = en.getKey();
            if (!SWORD_EXTRAS.contains(ench)) continue;
            int have = meta.getEnchantLevel(ench);
            int want = en.getValue();
            int nl = (have == want) ? Math.min(want + 1, ench.getMaxLevel()) : Math.max(have, want);
            if (nl == have) continue;
            meta.addEnchant(ench, nl, true);
            cost += nl;
            changed = true;
        }
        if (!changed) return;
        result.setItemMeta(meta);
        e.setResult(result);
        e.getView().setRepairCost(Math.max(1, cost));
    }
}

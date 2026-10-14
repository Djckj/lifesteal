package dev.lifesteal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public final class HeartManager {
    private final LifestealPlugin plugin;
    private final NamespacedKey heartKey;

    public HeartManager(LifestealPlugin plugin) {
        this.plugin = plugin;
        this.heartKey = new NamespacedKey(plugin, "heart");
    }

    public int get(UUID id) { return plugin.data().hearts(id, plugin.getConfig().getInt("hearts.start", 10)); }
    public int max() { return plugin.getConfig().getInt("hearts.max", 20); }
    public int reviveHearts() { return plugin.getConfig().getInt("hearts.revive-hearts", 5); }

    public void set(UUID id, int hearts) {
        plugin.data().setHearts(id, hearts);
        Player p = Bukkit.getPlayer(id);
        if (p != null) apply(p);
    }

    /** Push the stored heart count onto the player's max health. */
    public void apply(Player p) {
        int h = get(p.getUniqueId());
        if (h <= 0) return;
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) attr.setBaseValue(h * 2.0);
        if (p.getHealth() > h * 2.0) p.setHealth(h * 2.0);
    }

    /** Gives hearts up to the cap; anything over the cap is handed over as Heart items. Returns hearts actually added. */
    public int gain(Player p, int amount) {
        int cur = get(p.getUniqueId());
        int add = Math.max(0, Math.min(amount, max() - cur));
        if (add > 0) set(p.getUniqueId(), cur + add);
        int extra = amount - add;
        if (extra > 0) give(p, heartItem(extra));
        return add;
    }

    public void eliminate(Player p) {
        String reason = plugin.getConfig().getString("elimination.ban-message", "You ran out of hearts!");
        String name = p.getName();
        Bukkit.getScheduler().runTask(plugin,
                () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + name + " " + reason));
    }

    public ItemStack heartItem(int amount) {
        ItemStack item = new ItemStack(Material.NETHER_STAR, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.itemName(Component.text("Heart", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Right-click to gain a heart.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(heartKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public NamespacedKey recipeKey() { return new NamespacedKey(plugin, "heart_recipe"); }

    /** D N D / N T N / D N D  (D = diamond block, N = netherite ingot, T = totem of undying) */
    public ShapedRecipe heartRecipe() {
        ShapedRecipe r = new ShapedRecipe(recipeKey(), heartItem(1));
        r.shape("DND", "NTN", "DND");
        r.setIngredient('D', Material.DIAMOND_BLOCK);
        r.setIngredient('N', Material.NETHERITE_INGOT);
        r.setIngredient('T', Material.TOTEM_OF_UNDYING);
        return r;
    }

    public boolean isHeart(ItemStack item) {
        return item != null && !item.getType().isAir() && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(heartKey, PersistentDataType.BYTE);
    }

    /** Add to inventory; whatever doesn't fit is dropped at the player's feet. */
    public void give(Player p, ItemStack item) {
        for (ItemStack left : p.getInventory().addItem(item).values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), left);
        }
    }
}

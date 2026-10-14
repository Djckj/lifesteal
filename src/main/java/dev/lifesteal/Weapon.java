package dev.lifesteal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public enum Weapon {
    LIFESTEALER("lifestealer", "Lifestealer Axe", NamedTextColor.DARK_RED,
            List.of("Steals extra hearts when it lands the kill.", "Only one will ever exist.")),
    SWORD_AXE("sword_axe", "Sword Axe", NamedTextColor.AQUA,
            List.of("An axe with sword speed.", "Accepts sword enchants (via anvil books).", "Only one will ever exist.")),
    ARMOR_BREAKER("armor_breaker", "Armor Breaker Axe", NamedTextColor.GOLD,
            List.of("Chews through armor durability.", "Only one will ever exist."));

    private static LifestealPlugin plugin;
    private static NamespacedKey KEY;

    private final String id;
    private final String displayName;
    private final NamedTextColor color;
    private final List<String> lore;

    Weapon(String id, String displayName, NamedTextColor color, List<String> lore) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.lore = lore;
    }

    static void init(LifestealPlugin p) {
        plugin = p;
        KEY = new NamespacedKey(p, "weapon");
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public NamedTextColor color() { return color; }
    public NamespacedKey key() { return new NamespacedKey(plugin, id); }

    public static Weapon fromId(String s) {
        for (Weapon w : values()) if (w.id.equalsIgnoreCase(s)) return w;
        return null;
    }

    public static Weapon of(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        String v = item.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
        return v == null ? null : fromId(v);
    }

    /** True if the item is one of our weapons and containers are blocked (config: weapons.block-containers). */
    public static boolean blockedFromContainers(ItemStack item) {
        return of(item) != null && plugin.getConfig().getBoolean("weapons.block-containers", true);
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.itemName(Component.text(displayName, color).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream()
                .map(s -> Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .toList());
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, id);
        if (plugin.getConfig().getBoolean("weapons.unbreakable", false)) meta.setUnbreakable(true);

        if (this == SWORD_AXE) {
            // Netherite axe: 10 damage, 1.0 attack speed. Sword cooldown: 1.6 attack speed.
            // Setting modifiers replaces the item's defaults, so both are set explicitly.
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                    new NamespacedKey(plugin, "sword_axe_damage"), 9.0,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                    new NamespacedKey(plugin, "sword_axe_speed"), -2.4,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        }
        item.setItemMeta(meta);
        return item;
    }

    public ShapedRecipe recipe() {
        ShapedRecipe r = new ShapedRecipe(key(), create());
        r.setIngredient('D', Material.DIAMOND_BLOCK);
        r.setIngredient('A', Material.NETHERITE_AXE);
        switch (this) {
            case LIFESTEALER -> {
                r.shape("DED", "EAE", "DED");
                r.setIngredient('E', Material.ENCHANTED_GOLDEN_APPLE);
            }
            case SWORD_AXE -> {
                r.shape("DDD", "SAT", "DDD");
                r.setIngredient('S', Material.NETHERITE_SWORD);
                r.setIngredient('T', Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
            }
            case ARMOR_BREAKER -> {
                r.shape("DND", "EAE", "DND");
                r.setIngredient('N', Material.NETHERITE_INGOT);
                r.setIngredient('E', Material.ENCHANTED_GOLDEN_APPLE);
            }
        }
        return r;
    }
}

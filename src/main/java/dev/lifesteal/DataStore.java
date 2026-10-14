package dev.lifesteal;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Tiny YAML store: hearts per player, which weapons were crafted, items waiting to be handed back. */
public final class DataStore {
    private final File file;
    private final YamlConfiguration yml;

    public DataStore(JavaPlugin plugin) {
        plugin.getDataFolder().mkdirs();
        file = new File(plugin.getDataFolder(), "data.yml");
        yml = YamlConfiguration.loadConfiguration(file);
    }

    public int hearts(UUID id, int def) { return yml.getInt("hearts." + id, def); }

    public void setHearts(UUID id, int n) {
        yml.set("hearts." + id, n);
        save();
    }

    public boolean isCrafted(Weapon w) { return yml.contains("crafted." + w.id()); }

    public void setCrafted(Weapon w, String by) {
        yml.set("crafted." + w.id(), by);
        save();
    }

    public void clearCrafted(Weapon w) {
        yml.set("crafted." + w.id(), null);
        save();
    }

    public void addPending(UUID id, ItemStack item) {
        List<ItemStack> list = readPending(id);
        list.add(item);
        yml.set("pending." + id, list);
        save();
    }

    public List<ItemStack> takePending(UUID id) {
        List<ItemStack> list = readPending(id);
        if (!list.isEmpty()) {
            yml.set("pending." + id, null);
            save();
        }
        return list;
    }

    private List<ItemStack> readPending(UUID id) {
        List<ItemStack> list = new ArrayList<>();
        List<?> raw = yml.getList("pending." + id, List.of());
        for (Object o : raw) if (o instanceof ItemStack s) list.add(s);
        return list;
    }

    public synchronized void save() {
        try {
            yml.save(file);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

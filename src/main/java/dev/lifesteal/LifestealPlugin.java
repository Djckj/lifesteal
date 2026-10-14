package dev.lifesteal;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LifestealPlugin extends JavaPlugin {
    private DataStore data;
    private HeartManager hearts;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        data = new DataStore(this);
        Weapon.init(this);
        hearts = new HeartManager(this);

        WeaponListener weapons = new WeaponListener(this);
        Bukkit.getPluginManager().registerEvents(weapons, this);
        Bukkit.getPluginManager().registerEvents(new LifestealListener(this), this);

        for (Weapon w : Weapon.values()) {
            Bukkit.removeRecipe(w.key());
            Bukkit.addRecipe(w.recipe());
        }

        Bukkit.removeRecipe(hearts.recipeKey());
        Bukkit.addRecipe(hearts.heartRecipe());

        Commands cmds = new Commands(this);
        for (String n : new String[]{"withdraw", "lifesteal"}) {
            PluginCommand c = getCommand(n);
            if (c != null) {
                c.setExecutor(cmds);
                c.setTabCompleter(cmds);
            }
        }
    }

    @Override
    public void onDisable() {
        if (data != null) data.save();
    }

    public DataStore data() { return data; }
    public HeartManager hearts() { return hearts; }
}

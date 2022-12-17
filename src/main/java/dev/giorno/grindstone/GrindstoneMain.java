package dev.giorno.grindstone;

import org.bukkit.plugin.java.JavaPlugin;

public final class GrindstoneMain extends JavaPlugin {

    public static GrindstoneMain instance;

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(new GrindStoneInventory(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

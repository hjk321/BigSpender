package com.hjk321.bigspender;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class BigSpender extends JavaPlugin {
    
    protected Config config;
    private Listener preprocessor;
    
    public void onEnable() {
        this.saveDefaultConfig();
        this.reloadConfig();
        this.config = new Config(this);
        if (!this.config.valid) {
            this.getLogger().severe("Config file had errors, disabling plugin.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        preprocessor = new CommandPreprocessor(this);
        this.getServer().getPluginManager().registerEvents(preprocessor, this);
        this.getLogger().info("Enabled!");
    }
    
    public void onDisable() {
        if (preprocessor != null)
            HandlerList.unregisterAll(preprocessor);
        this.getLogger().info("Disabled!");
    }
}

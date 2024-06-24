package com.hjk321.bigspender;

import org.bstats.bukkit.Metrics;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class BigSpender extends JavaPlugin {
    
    protected Config config;
    private Listener preprocessor;
    private static final int BSTATS_ID = 22381;
    private Metrics metrics;
    
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

        this.metrics = new Metrics(this, BSTATS_ID);
        this.getLogger().info("Enabled!");
    }
    
    public void onDisable() {
        if (preprocessor != null)
            HandlerList.unregisterAll(preprocessor);
        metrics.shutdown();
        this.getLogger().info("Disabled!");
    }
}

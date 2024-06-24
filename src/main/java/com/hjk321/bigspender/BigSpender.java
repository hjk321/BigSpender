package com.hjk321.bigspender;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class BigSpender extends JavaPlugin {
    
    protected Config config;
    private Listener preprocessor;
    private static final int BSTATS_ID = 22381;
    private Metrics metrics;
    private PlaceholderManager placeholderManager;
    
    public void onEnable() {
        this.saveDefaultConfig();
        this.reloadConfig();
        this.config = new Config(this);
        if (!this.config.valid) {
            this.getLogger().severe("Config file had errors, disabling plugin.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.getLogger().info("Found PlaceholderAPI, enabling support!");
            try {
                placeholderManager = new PlaceholderManager(this);
                if (!placeholderManager.register())
                    this.getLogger().warning("Couldn't register placeholders. Check PAPI logs for more info.");
            } catch (Exception e) {
                if (this.config.verbose) {
                    this.getLogger().log(Level.SEVERE,
                            "VERBOSE: Couldn't register placeholders, stacktrace is as follows:", e);
                } else {
                    this.getLogger().severe("Couldn't register placeholders. Run in verbose mode for stacktrace.");
                }
                placeholderManager = null;
            }
        } else if (this.config.verbose) {
            this.getLogger().info("VERBOSE: PlaceholderAPI was not found.");
        }

        preprocessor = new CommandPreprocessor(this);
        this.getServer().getPluginManager().registerEvents(preprocessor, this);

        this.metrics = new Metrics(this, BSTATS_ID);
        this.getLogger().info("Enabled!");
    }
    
    public void onDisable() {
        if (preprocessor != null)
            HandlerList.unregisterAll(preprocessor);
        if (placeholderManager != null && placeholderManager.isRegistered())
            placeholderManager.unregister();
        metrics.shutdown();
        this.getLogger().info("Disabled!");
    }
}

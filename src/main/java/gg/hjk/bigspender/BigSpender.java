package gg.hjk.bigspender;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BigSpender extends JavaPlugin {
    private static final int BSTATS_ID = 22381;
    private static final Pattern numberPattern = Pattern.compile("^(\\d*?\\.?\\d+)$");
    private static final Pattern abbreviationPattern = Pattern.compile("^(\\d*?\\.?\\d+)([a-zA-Z]+)$");

    Config config;
    private Listener preprocessor;
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

    protected void logVerbose(String msg) {
        if (config != null && config.verbose)
            this.getLogger().info("VERBOSE: " + msg);
    }

    protected @Nullable BigDecimal parseAbbreviation(String abbreviation) {
        // Check if already pure number
        Matcher matcher = numberPattern.matcher(abbreviation);
        if (matcher.matches()) {
            logVerbose("Abbreviation \"" + abbreviation + "\" is already a plain number with no suffix");
            return new BigDecimal(matcher.group(1)).stripTrailingZeros(); // should never fail due to the regex earlier
        }
        // Get the number and the suffix if the argument has it
        matcher = abbreviationPattern.matcher(abbreviation);
        if (!matcher.matches()) {
            logVerbose("Abbreviation \"" + abbreviation + "\" could not be parsed, not a number plus a suffix.");
            return null;
        }
        BigDecimal number = new BigDecimal(matcher.group(1)); // should never fail due to the regex earlier
        String suffix = matcher.group(2);

        logVerbose("Suffix case-sensitivity is " + (config.caseSensitive ? "on" : "off"));
        if (!config.caseSensitive)
            suffix = suffix.toLowerCase();

        BigDecimal multiplier = config.abbreviations.get(suffix);
        if (multiplier == null) {
            logVerbose("Abbreviation \"" + abbreviation
                    + "\" could not be parsed, suffix \"" + suffix + "\" not recognized.");
            return null;
        }
        number = number.multiply(multiplier).stripTrailingZeros();
        logVerbose("Parsed \"" + abbreviation + "\" into " + number.toPlainString());
        return number;
    }

    private static final BigDecimal ONE = new BigDecimal(1);
    protected @NotNull String formatNumber(@NotNull BigDecimal num, int scale) {
        for (BigDecimal mul : this.config.multipliers.keySet())
            if (num.compareTo(mul) >= 0)
                return num.divide(mul, scale, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
                        + this.config.multipliers.get(mul);
        return num.divide(ONE, scale, RoundingMode.DOWN).stripTrailingZeros().toPlainString();
    }
}

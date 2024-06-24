package com.hjk321.bigspender;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public class PlaceholderManager extends PlaceholderExpansion {
    private final BigSpender plugin;

    public PlaceholderManager(BigSpender plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "bigspender";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        params = params.trim().toLowerCase();
        if (params.isEmpty())
            return null;

        String[] split = params.split("_");
        switch (split.length) {
            case 2:
                if (split[0].equals("parse")) {
                    BigDecimal parse = plugin.parseAbbreviation(split[1]);
                    if (parse != null)
                        return parse.toPlainString();
                }
            default: return null;
        }
    }
}

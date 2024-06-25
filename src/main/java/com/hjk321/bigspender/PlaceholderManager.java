package com.hjk321.bigspender;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

class PlaceholderManager extends PlaceholderExpansion {
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
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        params = params.trim().toLowerCase();
        if (params.isEmpty())
            return null;
        String subParams = getStringAfter(params,"parse_");
        if (subParams != null)
            return doParse(player, subParams);
        subParams = getStringAfter(params,"format_");
        if (subParams != null)
            return doFormat(player, subParams);

        return null;
    }

    private @Nullable String getStringAfter(@NotNull String input, String startsWith) {
        if (!input.startsWith(startsWith))
            return null;
        return input.substring(startsWith.length());
    }

    private @Nullable String doParse(OfflinePlayer player, @NotNull String params) {
        String input = PlaceholderAPI.setBracketPlaceholders(player, params);
        BigDecimal parse = plugin.parseAbbreviation(input);
        if (parse != null)
            return parse.toPlainString();
        return null;
    }

    private @Nullable String doFormat(OfflinePlayer player, @NotNull String params) {
        String input = PlaceholderAPI.setBracketPlaceholders(player, params);
        BigDecimal number;
        try {
            number = new BigDecimal(input);
        } catch (NumberFormatException ex) {
            return null;
        }
        return plugin.formatNumber(number);
    }
}

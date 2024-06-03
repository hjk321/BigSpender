package com.hjk321.bigspender;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;

public class Config {
    Map<String, List<Integer>> commands = new HashMap<>();
    Map<String, BigDecimal> abbreviations = new HashMap<>();
    boolean caseSensitive = false, verbose = false, valid = true;

    public Config(BigSpender plugin) {
        FileConfiguration config = plugin.getConfig();
        Logger log = plugin.getLogger();
        
        // Verbose?
        this.verbose = config.getBoolean("verbose", false);
        // Case Sensitive?
        this.caseSensitive = config.getBoolean("case-sensitive", false);

        // Abbreviations
        for (String line : config.getStringList("abbreviations")) {
            line = line.trim();
            String[] split = line.split(" ");
            if (split.length != 2) {
                log.warning("CONFIG WARNING: Abbreviation \"" + line + "\" skipped. "
                    + "Must be a suffix and multiplier value seperated by a space.");
                continue;
            }
            String suffix = split[0];
            // TODO - confirm suffix only letters
            if (this.caseSensitive)
                suffix = suffix.toLowerCase();
            if (this.abbreviations.containsKey(suffix)) {
                log.warning("CONFIG WARNING: Abbreviation \"" + line + "\" skipped. "
                    + "Suffix \"" + suffix + "\" already exists.");
                continue;
            }
            BigDecimal multiplier;
            try {
                multiplier = new BigDecimal(split[1]);
            } catch (NumberFormatException ex) {
                log.warning("CONFIG WARNING: Abbreviation \"" + line + "\" skipped. "
                    + "Multiplier is not a valid number.");
                continue;
            }
            this.abbreviations.put(suffix, multiplier);
        }
        if (abbreviations.isEmpty()) {
            log.severe("CONFIG ERROR: Abbreviations list is empty, or has no valid entries.");
            this.valid = false;
        }

        // Commands
        for (String line : config.getStringList("commands")) {
            line = line.trim();
            String[] split = line.split(" ");
            if (split.length < 2) {
                log.warning("CONFIG WARNING: Command entry \"" + line + "\" skipped "
                    + "because it was empty or a single word.");
                continue;
            }

            // Get command text (minus arg numbers).
            String command = "";
            int i = 0;
            while (i < split.length) {
                try {
                    Integer.parseInt(split[i]);
                    // Loop will only break if we've reached our first number
                    break;
                } catch (NullPointerException | NumberFormatException ex) {
                    command += split[i] + " ";
                    i++;
                }
            }
            command = command.trim().toLowerCase();
            if (command.equals("")) {
                log.warning("CONFIG WARNING: Command entry \"" + line + "\" skipped "
                    + "because there was no command text, only argument numbers.");
                continue;
            }
            if (commands.containsKey(command)) {
                log.warning("CONFIG WARNING: Command entry \"" + line + "\" skipped "
                    + "because \"" + command + "\" has already been registered.");
                continue;
            }

            // Get arg numbers (continuing from position of last loop)
            List<Integer> argNums = new ArrayList<>();
            while (i < split.length) {
                try {
                    int arg = Integer.parseInt(split[i]);
                    if (!(argNums.contains(arg) || arg <= 0))
                        argNums.add(arg);
                    else
                        log.warning("CONFIG WARNING: Command entry \"" + line + "\": "
                            + "Argument value \"" + split[i] + "\" was skipped because it was a duplicate or less than 1.");
                    i++;
                } catch (NumberFormatException ex) {
                    log.warning("CONFIG WARNING: Command entry \"" + line + "\": "
                        + "Argument value \"" + split[i] + "\" and beyond was ignored due to non-integer value.");
                    break;
                }
            }
            if (argNums.isEmpty()) {
                log.warning("CONFIG WARNING: Command entry \"" + line + "\" skipped "
                    + "because there were no valid argument numbers.");
                continue;
            }

            commands.put(command, argNums);
        }
        if (commands.isEmpty()) {
            log.severe("CONFIG ERROR: Commands list is empty, or has no valid entries.");
            this.valid = false;
        }
    }
}

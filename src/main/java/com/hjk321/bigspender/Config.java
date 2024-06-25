package com.hjk321.bigspender;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;

class Config {
    Map<String, List<Integer>> commands = new HashMap<>();
    // List of abbreviations with their corresponding multipliers. Used for parsing.
    Map<String, BigDecimal> abbreviations = new HashMap<>();
    // List of multipliers with their corresponding abbreviations. Used for formatting. Sorted in descending order.
    SortedMap<BigDecimal, String> multipliers = new TreeMap<>(Collections.reverseOrder());
    boolean caseSensitive, verbose, valid = true;

    public Config(BigSpender plugin) {
        FileConfiguration config = plugin.getConfig();
        Logger log = plugin.getLogger();
        
        // Verbose?
        this.verbose = config.getBoolean("verbose", false);
        // Case Sensitive?
        this.caseSensitive = config.getBoolean("case-sensitive", false);

        // Abbreviations and Values
        for (String line : config.getStringList("abbreviations")) {
            line = line.trim();
            String[] split = line.split(" ");
            if (split.length != 2) {
                log.warning("CONFIG WARNING: Abbreviation line \"" + line + "\" skipped. "
                    + "Must be a suffix and multiplier value seperated by a space.");
                continue;
            }
            String suffix = split[0];
            if (!this.caseSensitive)
                suffix = suffix.toLowerCase();
            if (!suffix.matches("^[a-zA-Z]+$")) {
                log.warning("CONFIG WARNING: Abbreviation \"" + line + "\" skipped. "
                    + "Suffix \"" + suffix + "\" must be all letters.");
                continue;
            }
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
            if (this.multipliers.containsKey(multiplier)) {
                log.warning("CONFIG WARNING: Abbreviation \"" + line + "\" skipped. "
                        + "Multiplier \"" + split[1] + "\" already exists under a different suffix.");
                continue;
            }

            this.abbreviations.put(suffix, multiplier);
            this.multipliers.put(multiplier, split[0]);
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
            StringBuilder command = new StringBuilder();
            int i = 0;
            while (i < split.length) {
                try {
                    Integer.parseInt(split[i]);
                    // Loop will only break if we've reached our first number
                    break;
                } catch (NullPointerException | NumberFormatException ex) {
                    command.append(split[i]).append(" ");
                    i++;
                }
            }
            command = new StringBuilder(command.toString().trim().toLowerCase());
            if (command.toString().isEmpty()) {
                log.warning("CONFIG WARNING: Command entry \"" + line + "\" skipped "
                    + "because there was no command text, only argument numbers.");
                continue;
            }
            if (commands.containsKey(command.toString())) {
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

            commands.put(command.toString(), argNums);
        }
        if (commands.isEmpty()) {
            log.severe("CONFIG ERROR: Commands list is empty, or has no valid entries.");
            this.valid = false;
        }
    }
}

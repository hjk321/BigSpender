package com.hjk321.bigspender;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandPreprocessor implements Listener {

    // Matches a number, then a suffix, exactly once
    private static Pattern pattern = Pattern.compile("^(\\d+\\.?\\d*)([a-zA-Z]+)$");

    private Config config;
    private Logger log;

    public CommandPreprocessor(BigSpender plugin) {
        this.config = plugin.config;
        this.log = plugin.getLogger();
    }

    @EventHandler
    public void preprocess(PlayerCommandPreprocessEvent e) {
        logVerbose("Processing " + e.getPlayer().getName() + "'s command \"" + e.getMessage() + "\":");

        if (e.isCancelled()) {
            logVerbose("Command has been cancelled by another plugin. Won't process.");
            return;
        }

        if (!e.getPlayer().hasPermission("bigspender.use")) {
            logVerbose("Player does not have permission \"bigspender.use\".");
            return;
        }

        // Split command message
        String[] split = e.getMessage().split(" ");
        if (split.length <= 1) {
            logVerbose("Command had no arguments so there's nothing to replace.");
            return;
        }

        // Remove the forward slash from arg0 but preserve it for later.
        boolean leadingSlash = false;
        if (split[0].charAt(0) == '/') {
            leadingSlash = true;
            if (split[0].length() <= 1) {
                logVerbose("The command was only a slash character. Won't process.");
                return;
            }
            split[0] = split[0].substring(1); // remove the slash for now
        }

        // Try to match input to a command entry in the config.
        // We attempt matches in reverse order (most array elements to least) for subcommand support.
        String command = null;
        for (int i = split.length; i > 0; i--) {
            String testCommand = String.join(" ", Arrays.copyOf(split, i)).toLowerCase();
            if (config.commands.containsKey(testCommand)) {
                command = testCommand;
                break;
            }
        }
        if (command == null) {
            logVerbose("Could not match input to any command entry in the config.");
            return;
        }
        logVerbose("Matched input to command entry \"" + command + "\".");

        // Process each argument
        List<Integer> argNums = config.commands.get(command);
        String argNumsString = "";
        for (int n : argNums) {
            argNumsString += String.valueOf(n) + " ";
        }
        logVerbose("The following argument numbers will be processed: " + argNumsString.trim());
        for (int argNum : argNums) {
            if (argNum <= 0 || argNum >= split.length) {
                logVerbose("ArgNum " + String.valueOf(argNum) + " skipped, not in input.");
                continue;
            }

            // Get the number and the suffix if the argument has it
            Matcher matcher = pattern.matcher(split[argNum]);
            if (!matcher.matches()) {
                logVerbose("ArgNum " + String.valueOf(argNum) + " with value \"" + split[argNum] 
                    + "\" skipped, not a number plus a suffix.");
                continue;
            }
            BigDecimal number = new BigDecimal(matcher.group(1)); // should never fail due to the regex earlier
            String suffix = matcher.group(2);
            logVerbose("Suffix case-sensitivity is " + (config.caseSensitive ? "on" : "off"));
            if (!config.caseSensitive)
                suffix = suffix.toLowerCase();
            BigDecimal multiplier = config.abbreviations.get(suffix);
            if (multiplier == null) {
                logVerbose("ArgNum " + String.valueOf(argNum) + " with value \"" + split[argNum] 
                    + "\" skipped, suffix \"" + suffix + "\" not recognized.");
                continue;
            }
            String newNumString = number.multiply(multiplier).toPlainString();
            logVerbose("ArgNum " + String.valueOf(argNum) + " with value \"" + split[argNum] 
                + "\" was expanded to the number " + newNumString);
            split[argNum] = newNumString;
        }

        // Join the split back together and replace the original message if it differs.
        String newMessage = String.join(" ", split);
        if (leadingSlash)
            newMessage = "/" + newMessage;
        if (e.getMessage().hashCode() != newMessage.hashCode()) {
            e.setMessage(newMessage);
            log.info("Command was expanded to \"" + newMessage + "\"");
        }
    }

    private void logVerbose(String msg) {
        if (config.verbose) log.info("VERBOSE: " + msg);
    }
}

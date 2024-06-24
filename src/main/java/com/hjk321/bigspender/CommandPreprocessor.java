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
    private static final Pattern pattern = Pattern.compile("^(\\d+\\.?\\d*)([a-zA-Z]+)$");

    private final Config config;
    private final Logger log;

    public CommandPreprocessor(BigSpender plugin) {
        this.config = plugin.config;
        this.log = plugin.getLogger();
    }

    @EventHandler
    @SuppressWarnings("unused") // Registered by Listener
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
            if (split[0].length() == 1) {
                logVerbose("The command was only a slash character. Won't process.");
                return;
            }
            split[0] = split[0].substring(1); // remove the slash for now
        }

        // Try to match input to a command entry in the config.
        // We attempt matches in reverse order (most array elements to least) for subcommand support.
        String command = null;
        int argNumOffset = 0;
        for (int i = split.length; i > 0; i--) {
            String testCommand = String.join(" ", Arrays.copyOf(split, i)).toLowerCase();
            if (config.commands.containsKey(testCommand)) {
                command = testCommand;
                argNumOffset = i - 1;
                break;
            }
        }
        if (command == null) {
            logVerbose("Could not match input to any command entry in the config.");
            return;
        }
        logVerbose("Matched input to command entry \"" + command + "\".");

        
        List<Integer> argNums = config.commands.get(command);
        if (config.verbose) {
            StringBuilder argNumsBuilder = new StringBuilder("[");
            for (int n : argNums) {
                argNumsBuilder.append(n).append(" ");
            }
            String argNumsString = argNumsBuilder.toString().trim() + "]";
            logVerbose("The following ArgNums will be processed: " + argNumsString);
        }

        // Process each argument
        for (int argNum : argNums) {
            int index = argNum + argNumOffset;
            if (index <= 0 || index >= split.length) {
                logVerbose("ArgNum " + argNum + " skipped, not in input.");
                continue;
            }

            // Get the number and the suffix if the argument has it
            Matcher matcher = pattern.matcher(split[index]);
            if (!matcher.matches()) {
                logVerbose("ArgNum " + argNum + " with value \"" + split[index]
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
                logVerbose("ArgNum " + argNum + " with value \"" + split[index]
                    + "\" skipped, suffix \"" + suffix + "\" not recognized.");
                continue;
            }
            String newNumString = number.multiply(multiplier).stripTrailingZeros().toPlainString();
            logVerbose("ArgNum " + argNum + " with value \"" + split[index]
                + "\" was expanded to the number " + newNumString);
            split[index] = newNumString;
        }

        // Join the split back together and replace the original message if it differs.
        String newMessage = String.join(" ", split);
        if (leadingSlash)
            newMessage = "/" + newMessage;
        if (e.getMessage().hashCode() != newMessage.hashCode()) {
            e.setMessage(newMessage);
            log.info("Command was expanded to \"" + newMessage + "\"");
        } else {
            logVerbose("The final command was unchanged from the original");
        }
    }

    private void logVerbose(String msg) {
        if (config.verbose) log.info("VERBOSE: " + msg);
    }
}

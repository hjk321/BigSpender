package com.hjk321.bigspender;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

class CommandPreprocessor implements Listener {
    private final BigSpender plugin;

    public CommandPreprocessor(BigSpender plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    @SuppressWarnings("unused") // Registered by Listener
    public void preprocess(PlayerCommandPreprocessEvent e) {
        plugin.logVerbose("Processing " + e.getPlayer().getName() + "'s command \"" + e.getMessage() + "\":");

        if (e.isCancelled()) {
            plugin.logVerbose("Command has been cancelled by another plugin. Won't process.");
            return;
        }

        if (!e.getPlayer().hasPermission("bigspender.use")) {
            plugin.logVerbose("Player does not have permission \"bigspender.use\".");
            return;
        }

        // Split command message
        String[] split = e.getMessage().split(" ");
        if (split.length <= 1) {
            plugin.logVerbose("Command had no arguments so there's nothing to replace.");
            return;
        }

        // Remove the forward slash from arg0 but preserve it for later.
        boolean leadingSlash = false;
        if (split[0].charAt(0) == '/') {
            leadingSlash = true;
            if (split[0].length() == 1) {
                plugin.logVerbose("The command was only a slash character. Won't process.");
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
            if (plugin.config.commands.containsKey(testCommand)) {
                command = testCommand;
                argNumOffset = i - 1;
                break;
            }
        }
        if (command == null) {
            plugin.logVerbose("Could not match input to any command entry in the config.");
            return;
        }
        plugin.logVerbose("Matched input to command entry \"" + command + "\".");

        List<Integer> argNums = plugin.config.commands.get(command);
        if (plugin.config.verbose) {
            StringBuilder argNumsBuilder = new StringBuilder("[");
            for (int n : argNums) {
                argNumsBuilder.append(n).append(" ");
            }
            String argNumsString = argNumsBuilder.toString().trim() + "]";
            plugin.logVerbose("The following ArgNums will be processed: " + argNumsString);
        }

        // Process each argument
        for (int argNum : argNums) {
            int index = argNum + argNumOffset;
            if (index <= 0 || index >= split.length) {
                plugin.logVerbose("ArgNum " + argNum + " skipped, not in input.");
                continue;
            }

            plugin.logVerbose("Parsing ArgNum " + argNum + " with value \"" + split[index] + "\"");
            BigDecimal parsed = plugin.parseAbbreviation(split[index]);
            if (parsed == null)
                continue;
            String newNumString = parsed.toPlainString();
            split[index] = newNumString;
        }

        // Join the split back together and replace the original message if it differs.
        String newMessage = String.join(" ", split);
        if (leadingSlash)
            newMessage = "/" + newMessage;
        if (e.getMessage().hashCode() != newMessage.hashCode()) {
            e.setMessage(newMessage);
            plugin.getLogger().info("Command was expanded to \"" + newMessage + "\"");
        } else {
            plugin.logVerbose("The final command was unchanged from the original");
        }
    }
}

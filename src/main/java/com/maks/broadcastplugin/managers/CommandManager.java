package com.maks.broadcastplugin.managers;

import com.maks.broadcastplugin.BroadcastPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Manages command registration and execution for the BroadcastPlugin.
 */
public class CommandManager implements CommandExecutor {
    private final BroadcastPlugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;

    /**
     * Constructs a new CommandManager.
     *
     * @param plugin The plugin instance
     * @param configManager The configuration manager
     * @param messageManager The message manager (can be null if categories are enabled)
     */
    public CommandManager(BroadcastPlugin plugin, ConfigManager configManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        registerCommands();
    }

    /**
     * Registers all commands for the plugin.
     */
    private void registerCommands() {
        plugin.getCommand("broadcast").setExecutor(this);
    }

    /**
     * Handles command execution.
     *
     * @param sender The command sender
     * @param command The command
     * @param label The command label
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("broadcast")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    return handleReloadCommand(sender);
                } else if (args[0].equalsIgnoreCase("debug")) {
                    return handleDebugCommand(sender);
                } else if (args[0].equalsIgnoreCase("help")) {
                    return handleHelpCommand(sender);
                }
            }

            // If no valid subcommand was provided, show help
            return handleHelpCommand(sender);
        }
        return false;
    }

    /**
     * Handles the reload command.
     *
     * @param sender The command sender
     * @return true if the command was handled
     */
    private boolean handleReloadCommand(CommandSender sender) {
        // Check if the sender has permission
        if (!sender.hasPermission("broadcast.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to execute this command!");
            return true;
        }

        try {
            // Reload configuration
            configManager.loadConfig();

            // Check if categories are enabled
            boolean categoriesEnabled = plugin.areCategoriesEnabled();

            if (categoriesEnabled) {
                // If categories are enabled, reload the category managers
                if (plugin.getCategoryManagerFactory() != null) {
                    plugin.getCategoryManagerFactory().reloadManagers();
                }

                // Send success message
                sender.sendMessage(ChatColor.GOLD + "[Broadcast] " + ChatColor.GRAY + "Configuration reloaded successfully!");

                // Log debug information if debug mode is enabled
                if (configManager.isDebugMode()) {
                    sender.sendMessage(ChatColor.GOLD + "[Broadcast] " + ChatColor.GRAY + "Debug info: " + 
                                      "Loaded " + configManager.getCategoryCount() + " categories");
                }
            } else {
                // If categories are disabled, update the legacy message manager
                if (messageManager != null) {
                    messageManager.setMessages(configManager.getMessages());
                }

                // Send success message
                sender.sendMessage(ChatColor.GOLD + "[Broadcast] " + ChatColor.GRAY + "Configuration reloaded successfully!");

                // Log debug information if debug mode is enabled
                if (configManager.isDebugMode()) {
                    sender.sendMessage(ChatColor.GOLD + "[Broadcast] " + ChatColor.GRAY + "Debug info: " + 
                                      "Loaded " + configManager.getMessages().size() + " messages, " +
                                      "Interval: " + configManager.getIntervalMinutes() + " minutes");
                }
            }

            // Restart broadcasting with the new configuration
            plugin.startBroadcastMessages();

        } catch (Exception e) {
            // Handle any errors during reload
            sender.sendMessage(ChatColor.RED + "[Broadcast] Error reloading configuration: " + e.getMessage());
            plugin.getLogger().severe("Error during reload command: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Handles the debug command.
     *
     * @param sender The command sender
     * @return true if the command was handled
     */
    private boolean handleDebugCommand(CommandSender sender) {
        // Check if the sender has permission
        if (!sender.hasPermission("broadcast.debug")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to execute this command!");
            return true;
        }

        // Display debug information
        sender.sendMessage(ChatColor.GOLD + "=== Broadcast Plugin Debug Info ===");
        sender.sendMessage(ChatColor.GRAY + "Debug Mode: " + (configManager.isDebugMode() ? "Enabled" : "Disabled"));

        // Check if categories are enabled
        boolean categoriesEnabled = plugin.areCategoriesEnabled();

        if (categoriesEnabled) {
            // Display category-based debug information
            sender.sendMessage(ChatColor.GRAY + "Broadcasting Mode: " + ChatColor.YELLOW + "Category-based");
            sender.sendMessage(ChatColor.GRAY + "Categories Loaded: " + ChatColor.YELLOW + configManager.getCategoryCount());

            // Show information for each category
            if (configManager.getCategoryCount() > 0) {
                sender.sendMessage(ChatColor.GOLD + "--- Categories ---");

                for (String categoryName : configManager.getCategoryNames()) {
                    CategoryMessageManager manager = plugin.getCategoryManagerFactory().getManager(categoryName);
                    if (manager != null) {
                        sender.sendMessage(ChatColor.YELLOW + categoryName + ChatColor.GRAY + ": " + 
                                          manager.getMessageCount() + " messages, " +
                                          manager.getCategory().getIntervalMinutes() + " minute interval, " +
                                          "randomized: " + manager.getCategory().isRandomizeMessages());
                    }
                }
            }
        } else {
            // Display legacy debug information
            sender.sendMessage(ChatColor.GRAY + "Broadcasting Mode: " + ChatColor.YELLOW + "Legacy");

            if (messageManager != null) {
                sender.sendMessage(ChatColor.GRAY + "Messages Loaded: " + ChatColor.YELLOW + configManager.getMessages().size());
                sender.sendMessage(ChatColor.GRAY + "Current Message Index: " + ChatColor.YELLOW + messageManager.getCurrentIndex());
                sender.sendMessage(ChatColor.GRAY + "Broadcast Interval: " + ChatColor.YELLOW + configManager.getIntervalMinutes() + " minutes");
                sender.sendMessage(ChatColor.GRAY + "Randomize Messages: " + ChatColor.YELLOW + 
                                  (configManager.getConfig().getBoolean("broadcast.order.randomize_messages", false) ? "Yes" : "No"));
            } else {
                sender.sendMessage(ChatColor.RED + "Message manager not initialized!");
            }
        }

        return true;
    }

    /**
     * Handles the help command.
     *
     * @param sender The command sender
     * @return true if the command was handled
     */
    private boolean handleHelpCommand(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Broadcast Plugin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/broadcast reload" + ChatColor.GRAY + " - Reloads the configuration");
        sender.sendMessage(ChatColor.YELLOW + "/broadcast debug" + ChatColor.GRAY + " - Shows debug information");
        sender.sendMessage(ChatColor.YELLOW + "/broadcast help" + ChatColor.GRAY + " - Shows this help message");
        return true;
    }
}

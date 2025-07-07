package com.maks.broadcastplugin;

import com.maks.broadcastplugin.conditions.MessageConditionEvaluator;
import com.maks.broadcastplugin.managers.CategoryManagerFactory;
import com.maks.broadcastplugin.managers.CategoryMessageManager;
import com.maks.broadcastplugin.managers.CommandManager;
import com.maks.broadcastplugin.managers.ConfigManager;
import com.maks.broadcastplugin.managers.ConfigWatcher;
import com.maks.broadcastplugin.managers.MessageManager;
import com.maks.broadcastplugin.managers.PlaceholderManager;
import com.maks.broadcastplugin.models.MessageCategory;
import com.maks.broadcastplugin.scheduling.MessageScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

/**
 * Main class for the BroadcastPlugin.
 * Manages the lifecycle of the plugin and coordinates the different managers.
 */
public class BroadcastPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private CommandManager commandManager;
    private CategoryManagerFactory categoryManagerFactory;
    private ConfigWatcher configWatcher;
    private PlaceholderManager placeholderManager;
    private MessageScheduler messageScheduler;
    private MessageConditionEvaluator conditionEvaluator;

    // Task for legacy broadcast mode
    private BukkitTask broadcastTask;

    // Tasks for category-based broadcast mode
    private Map<String, BukkitTask> categoryTasks = new HashMap<>();

    @Override
    public void onEnable() {
        try {
            // Initialize managers
            configManager = new ConfigManager(this);

            // Initialize category manager factory if categories are enabled
            if (configManager.areCategoriesEnabled()) {
                categoryManagerFactory = new CategoryManagerFactory(this, configManager);
                getLogger().info("Category-based broadcasting enabled with " + 
                                configManager.getCategoryCount() + " categories");
            } else {
                // Initialize legacy message manager if categories are disabled
                messageManager = new MessageManager(this, configManager.getMessages());
                getLogger().info("Legacy broadcasting mode enabled");
            }

            // Initialize placeholder manager
            placeholderManager = new PlaceholderManager(this);

            // Initialize message condition evaluator
            conditionEvaluator = new MessageConditionEvaluator(this);

            // Load message conditions if they exist
            if (configManager.getConfig().contains("conditions")) {
                conditionEvaluator.loadConditions(configManager.getConfig().getConfigurationSection("conditions"));
            }

            // Initialize message scheduler
            int defaultInterval = configManager.areCategoriesEnabled() ? 
                configManager.getConfig().getInt("categories.defaults.interval", 5) : 
                configManager.getIntervalMinutes();
            messageScheduler = new MessageScheduler(this, defaultInterval);

            // Load message schedules if they exist
            if (configManager.getConfig().contains("schedules")) {
                messageScheduler.loadSchedules(configManager.getConfig().getConfigurationSection("schedules"));
            }

            // Initialize command manager
            commandManager = new CommandManager(this, configManager, messageManager);

            // Start broadcasting messages
            startBroadcastMessages();

            // Initialize and start the config watcher
            configWatcher = new ConfigWatcher(this, configManager);
            configWatcher.startWatching();

            getLogger().info("BroadcastPlugin is enabled!");
        } catch (Exception e) {
            getLogger().severe("Error enabling BroadcastPlugin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        // Cancel legacy broadcast task if it exists
        if (broadcastTask != null) {
            broadcastTask.cancel();
        }

        // Cancel all category tasks
        for (BukkitTask task : categoryTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        categoryTasks.clear();

        // Cancel all tasks from this plugin as a safety measure
        Bukkit.getScheduler().cancelTasks(this);

        // Stop the message scheduler
        if (messageScheduler != null) {
            messageScheduler.stopAllSchedules();
        }

        // Stop the config watcher
        if (configWatcher != null) {
            configWatcher.stopWatching();
        }

        getLogger().info("BroadcastPlugin is disabled.");
    }

    /**
     * Starts the scheduled broadcasting of messages.
     * 
     * This method sets up repeating tasks using Bukkit's scheduler to broadcast
     * messages at regular intervals. It supports two modes:
     * 
     * 1. Category-based broadcasting: Each category has its own scheduler with its own interval
     * 2. Legacy broadcasting: A single scheduler for all messages
     * 
     * The implementation follows these steps:
     * 
     * 1. Cancel any existing broadcast tasks to prevent duplicate messages
     * 2. Check if categories are enabled
     * 3. If categories are enabled:
     *    - Create a scheduler for each category with its own interval
     * 4. If categories are disabled:
     *    - Create a single scheduler for all messages
     * 
     * Error handling is implemented at multiple levels:
     * - The entire scheduler setup is wrapped in a try-catch block
     * - Each individual broadcast attempt is also wrapped in a try-catch
     * - Debug logging is conditionally enabled based on configuration
     */
    public void startBroadcastMessages() {
        // First, cancel any existing broadcast tasks to prevent duplicate messages
        // This is important when reloading the plugin or configuration
        if (broadcastTask != null) {
            broadcastTask.cancel();
        }

        // Cancel all category tasks
        for (BukkitTask task : categoryTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        categoryTasks.clear();

        try {
            // Check if categories are enabled
            if (configManager.areCategoriesEnabled()) {
                // Start category-based broadcasting
                startCategoryBroadcasting();
            } else {
                // Start legacy broadcasting
                startLegacyBroadcasting();
            }
        } catch (Exception e) {
            // Handle any errors that occur when setting up the scheduler
            getLogger().severe("Error starting broadcast scheduler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Starts the category-based broadcasting.
     * Creates a separate scheduler for each category with its own interval.
     */
    private void startCategoryBroadcasting() {
        // Get all category managers
        Map<String, CategoryMessageManager> managers = categoryManagerFactory.getManagers();

        if (managers.isEmpty()) {
            getLogger().warning("No valid categories found for broadcasting!");
            return;
        }

        // Create a scheduler for each category
        for (CategoryMessageManager manager : managers.values()) {
            MessageCategory category = manager.getCategory();
            String categoryName = category.getName();

            // Skip categories with no messages
            if (category.getMessages().isEmpty()) {
                getLogger().warning("No messages found in category: " + categoryName);
                continue;
            }

            // Create a scheduler for this category
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                this,  // Plugin instance - owner of the task
                () -> {
                    // This lambda is the code that runs on each scheduled execution
                    try {
                        // Delegate to the CategoryMessageManager to handle the actual broadcasting
                        manager.broadcastNextMessage();
                    } catch (Exception e) {
                        // Catch and log any exceptions that occur during broadcasting
                        getLogger().severe("Error broadcasting message for category " + categoryName + ": " + e.getMessage());

                        // Only print stack traces in debug mode to avoid log spam
                        if (configManager.isDebugMode()) {
                            e.printStackTrace();
                        }
                    }
                },
                0L,  // Initial delay: 0 ticks = start immediately
                category.getInterval()  // Repeat interval in ticks (from category)
            );

            // Store the task
            categoryTasks.put(categoryName, task);

            // Log scheduler startup information if in debug mode
            if (configManager.isDebugMode()) {
                getLogger().info("Broadcast scheduler started for category " + categoryName + 
                                " with interval: " + category.getIntervalMinutes() + " minutes");
            }
        }

        getLogger().info("Started broadcasting for " + categoryTasks.size() + " categories");
    }

    /**
     * Starts the legacy broadcasting.
     * Creates a single scheduler for all messages.
     */
    private void startLegacyBroadcasting() {
        // Verify that we have messages to broadcast
        // If no messages are found, log a warning and exit early
        if (configManager.getMessages().isEmpty()) {
            getLogger().warning("No messages found in configuration!");
            return;
        }

        // Initialize messageManager if it's null (this can happen when switching from category mode to legacy mode)
        if (messageManager == null) {
            getLogger().info("Initializing message manager for legacy mode");
            messageManager = new MessageManager(this, configManager.getMessages());

            // Update the command manager with the new message manager
            if (commandManager != null) {
                commandManager.updateMessageManager(messageManager);
            }
        }

        // Set up a repeating task using Bukkit's scheduler
        // This creates a task that runs at fixed intervals
        broadcastTask = Bukkit.getScheduler().runTaskTimer(
            this,  // Plugin instance - owner of the task
            () -> {
                // This lambda is the code that runs on each scheduled execution
                try {
                    // Delegate to the MessageManager to handle the actual broadcasting
                    // The MessageManager keeps track of which message to send next
                    messageManager.broadcastNextMessage();
                } catch (Exception e) {
                    // Catch and log any exceptions that occur during broadcasting
                    // This prevents the scheduler from stopping if an error occurs
                    getLogger().severe("Error broadcasting message: " + e.getMessage());

                    // Only print stack traces in debug mode to avoid log spam
                    if (configManager.isDebugMode()) {
                        e.printStackTrace();
                    }
                }
            },
            0L,  // Initial delay: 0 ticks = start immediately
            configManager.getInterval()  // Repeat interval in ticks (20 ticks = 1 second)
        );

        // Log scheduler startup information if in debug mode
        if (configManager.isDebugMode()) {
            getLogger().info("Legacy broadcast scheduler started with interval: " + 
                            configManager.getIntervalMinutes() + " minutes");
        }
    }

    /**
     * Gets the configuration manager.
     *
     * @return The configuration manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the message manager.
     * This is only used for legacy broadcast mode.
     *
     * @return The message manager, or null if categories are enabled
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * Gets the category manager factory.
     * This is only used for category-based broadcast mode.
     *
     * @return The category manager factory, or null if categories are disabled
     */
    public CategoryManagerFactory getCategoryManagerFactory() {
        return categoryManagerFactory;
    }

    /**
     * Checks if categories are enabled.
     *
     * @return true if categories are enabled, false otherwise
     */
    public boolean areCategoriesEnabled() {
        return configManager != null && configManager.areCategoriesEnabled();
    }

    /**
     * Gets the placeholder manager.
     *
     * @return The placeholder manager
     */
    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    /**
     * Gets the message condition evaluator.
     *
     * @return The message condition evaluator
     */
    public MessageConditionEvaluator getConditionEvaluator() {
        return conditionEvaluator;
    }

}

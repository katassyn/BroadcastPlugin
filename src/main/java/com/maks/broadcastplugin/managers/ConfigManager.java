package com.maks.broadcastplugin.managers;

import com.maks.broadcastplugin.models.MessageCategory;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the configuration for the BroadcastPlugin.
 * Handles loading, validation, and access to configuration values.
 */
public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    // Legacy configuration fields
    private List<String> messages;
    private int interval;
    private boolean debugMode;

    // Category configuration fields
    private boolean categoriesEnabled;
    private String categoriesDirectory;
    private Map<String, MessageCategory> categories;

    // Default category settings
    private int defaultInterval;
    private String defaultSeparator;
    private boolean defaultEnableClickableLinks;
    private boolean defaultRandomizeMessages;

    /**
     * Constructs a new ConfigManager.
     *
     * @param plugin The plugin instance
     */
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig();
        this.categories = new HashMap<>();
        loadConfig();
    }

    /**
     * Loads and validates the configuration.
     */
    public void loadConfig() {
        try {
            // Reload main config
            plugin.reloadConfig();
            this.config = plugin.getConfig();

            // Load and validate main configuration
            validateAndLoadConfig();

            // Load category configurations if enabled
            if (categoriesEnabled) {
                loadCategoryConfigurations();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading configuration: " + e.getMessage());
            e.printStackTrace();

            // Set default values if configuration loading fails
            this.messages = new ArrayList<>();
            this.interval = 300; // 5 minutes in ticks
            this.debugMode = false;
            this.categoriesEnabled = false;
            this.categories.clear();
        }
    }

    /**
     * Loads all category configuration files from the categories directory.
     */
    private void loadCategoryConfigurations() {
        // Clear existing categories
        categories.clear();

        // Get the categories directory
        File categoriesDir = new File(plugin.getDataFolder(), categoriesDirectory);

        // Create the directory if it doesn't exist
        if (!categoriesDir.exists()) {
            if (categoriesDir.mkdirs()) {
                plugin.getLogger().info("Created categories directory: " + categoriesDir.getPath());

                // Save default category files
                saveDefaultCategoryFiles();
            } else {
                plugin.getLogger().severe("Failed to create categories directory: " + categoriesDir.getPath());
                return;
            }
        }

        // Get all .yml files in the directory
        File[] categoryFiles = categoriesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));

        if (categoryFiles == null || categoryFiles.length == 0) {
            plugin.getLogger().warning("No category files found in: " + categoriesDir.getPath());
            return;
        }

        // Load each category file
        for (File file : categoryFiles) {
            try {
                loadCategoryFile(file);
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading category file " + file.getName() + ": " + e.getMessage());
                if (debugMode) {
                    e.printStackTrace();
                }
            }
        }

        if (debugMode) {
            plugin.getLogger().info("Loaded " + categories.size() + " message categories");
        }
    }

    /**
     * Saves default category files to the categories directory.
     */
    private void saveDefaultCategoryFiles() {
        saveResourceIfNotExists("categories/general.yml");
        saveResourceIfNotExists("categories/gameplay.yml");
    }

    /**
     * Saves a resource from the plugin jar to the plugin's data folder if it doesn't already exist.
     * 
     * @param resourcePath The path to the resource in the plugin jar
     */
    private void saveResourceIfNotExists(String resourcePath) {
        File outFile = new File(plugin.getDataFolder(), resourcePath);
        if (!outFile.exists()) {
            plugin.saveResource(resourcePath, false);
            if (debugMode) {
                plugin.getLogger().info("Saved default resource: " + resourcePath);
            }
        }
    }

    /**
     * Loads a single category file and adds it to the categories map.
     * 
     * @param file The category file to load
     */
    private void loadCategoryFile(File file) {
        // Load the category file
        YamlConfiguration categoryConfig = YamlConfiguration.loadConfiguration(file);

        // Get the category name from the file name (without extension)
        String categoryName = file.getName().substring(0, file.getName().lastIndexOf('.'));

        if (debugMode) {
            plugin.getLogger().info("Loading category: " + categoryName + " from file: " + file.getName());
        }

        // Load category settings with defaults
        int interval = categoryConfig.getInt("interval", defaultInterval);
        String separator = categoryConfig.getString("appearance.separator", defaultSeparator);
        boolean enableClickableLinks = categoryConfig.getBoolean("appearance.enable_clickable_links", defaultEnableClickableLinks);
        boolean randomizeMessages = categoryConfig.getBoolean("order.randomize_messages", defaultRandomizeMessages);

        // Validate interval
        if (interval <= 0) {
            plugin.getLogger().warning("Invalid interval value in category " + categoryName + ": " + interval + ". Using default value: " + defaultInterval);
            interval = defaultInterval;
        }

        // Convert minutes to ticks
        int intervalTicks = interval * 60 * 20;

        // Load messages
        List<String> messages = categoryConfig.getStringList("messages");

        // Filter out null or empty messages
        List<String> validMessages = new ArrayList<>();
        for (String message : messages) {
            if (message != null && !message.trim().isEmpty()) {
                validMessages.add(message);
            }
        }

        if (validMessages.isEmpty()) {
            plugin.getLogger().warning("No valid messages found in category: " + categoryName);
            return;
        }

        // Create the category
        MessageCategory category = new MessageCategory(
            categoryName,
            validMessages,
            intervalTicks,
            separator,
            enableClickableLinks,
            randomizeMessages
        );

        // Add to categories map
        categories.put(categoryName, category);

        if (debugMode) {
            plugin.getLogger().info("Loaded category: " + category);
        }
    }

    /**
     * Validates and loads configuration values.
     * Performs comprehensive validation of all configuration settings and provides
     * helpful error messages for invalid values.
     */
    private void validateAndLoadConfig() {
        // Validation results counter
        int validationErrors = 0;

        // Load and validate debug mode
        this.debugMode = config.getBoolean("debug", false);
        if (debugMode) {
            plugin.getLogger().info("Debug mode enabled");
        }

        // Load and validate categories settings
        validationErrors += validateCategoriesSettings();

        // If categories are not enabled, load legacy broadcast settings
        if (!categoriesEnabled) {
            // Determine configuration format and load messages
            List<String> configMessages;
            boolean isNewFormat = config.contains("broadcast.messages");

            if (isNewFormat) {
                // New config format
                configMessages = validateNewConfigFormat(validationErrors);
            } else {
                // Legacy config format for backward compatibility
                configMessages = validateLegacyConfigFormat(validationErrors);
            }

            // Process and validate messages
            this.messages = new ArrayList<>();
            int skippedMessages = 0;

            // Filter out null or empty messages
            for (String message : configMessages) {
                if (message != null && !message.trim().isEmpty()) {
                    messages.add(message);
                } else {
                    skippedMessages++;
                    plugin.getLogger().warning("Skipping empty message in configuration");
                    validationErrors++;
                }
            }

            if (messages.isEmpty()) {
                plugin.getLogger().severe("No valid messages found in configuration! The plugin will not broadcast any messages.");
                validationErrors++;
            } else if (skippedMessages > 0) {
                plugin.getLogger().warning("Skipped " + skippedMessages + " empty messages in configuration");
            }

            // Validate appearance and order settings if using new format
            if (isNewFormat) {
                validationErrors += validateAppearanceSettings();
                validationErrors += validateOrderSettings();
            }

            if (debugMode) {
                plugin.getLogger().info("Loaded " + messages.size() + " messages");
                plugin.getLogger().info("Broadcast interval: " + getIntervalMinutes() + " minutes (" + interval + " ticks)");
            }
        }

        // Log validation summary
        if (validationErrors > 0) {
            plugin.getLogger().warning("Found " + validationErrors + " configuration issues. Please check your config.yml file.");
        } else if (debugMode) {
            plugin.getLogger().info("Configuration validation passed with no errors");
        }
    }

    /**
     * Validates the categories settings in the configuration.
     * 
     * @return Number of validation errors found
     */
    private int validateCategoriesSettings() {
        int validationErrors = 0;

        // Check if categories section exists
        if (!config.contains("categories")) {
            plugin.getLogger().info("Missing 'categories' section. Using legacy broadcast settings.");
            this.categoriesEnabled = false;
            return validationErrors;
        }

        // Load categories enabled flag
        this.categoriesEnabled = config.getBoolean("categories.enabled", false);

        if (!categoriesEnabled) {
            plugin.getLogger().info("Categories are disabled. Using legacy broadcast settings.");
            return validationErrors;
        }

        // Load categories directory
        this.categoriesDirectory = config.getString("categories.directory", "categories");

        if (categoriesDirectory == null || categoriesDirectory.trim().isEmpty()) {
            plugin.getLogger().warning("Invalid categories directory. Using default: 'categories'");
            this.categoriesDirectory = "categories";
            validationErrors++;
        }

        // Load default settings for categories
        if (!config.contains("categories.defaults")) {
            plugin.getLogger().warning("Missing 'categories.defaults' section. Using hardcoded defaults.");
            validationErrors++;
            setHardcodedDefaults();
        } else {
            // Load default interval
            this.defaultInterval = config.getInt("categories.defaults.interval", 5);
            if (defaultInterval <= 0) {
                plugin.getLogger().warning("Invalid default interval: " + defaultInterval + ". Using default: 5 minutes.");
                this.defaultInterval = 5;
                validationErrors++;
            }

            // Load default separator
            this.defaultSeparator = config.getString("categories.defaults.appearance.separator", "------------------------------");
            if (defaultSeparator == null || defaultSeparator.trim().isEmpty()) {
                plugin.getLogger().warning("Invalid default separator. Using default: '------------------------------'");
                this.defaultSeparator = "------------------------------";
                validationErrors++;
            }

            // Load default enable_clickable_links
            this.defaultEnableClickableLinks = config.getBoolean("categories.defaults.appearance.enable_clickable_links", true);

            // Load default randomize_messages
            this.defaultRandomizeMessages = config.getBoolean("categories.defaults.order.randomize_messages", false);
        }

        if (debugMode) {
            plugin.getLogger().info("Categories enabled: " + categoriesEnabled);
            plugin.getLogger().info("Categories directory: " + categoriesDirectory);
            plugin.getLogger().info("Default interval: " + defaultInterval + " minutes");
            plugin.getLogger().info("Default separator: " + defaultSeparator);
            plugin.getLogger().info("Default enable clickable links: " + defaultEnableClickableLinks);
            plugin.getLogger().info("Default randomize messages: " + defaultRandomizeMessages);
        }

        return validationErrors;
    }

    /**
     * Sets hardcoded default values for category settings.
     */
    private void setHardcodedDefaults() {
        this.defaultInterval = 5;
        this.defaultSeparator = "------------------------------";
        this.defaultEnableClickableLinks = true;
        this.defaultRandomizeMessages = false;
    }

    /**
     * Validates the new configuration format.
     * 
     * @param validationErrors Counter for validation errors
     * @return List of messages from the configuration
     */
    private List<String> validateNewConfigFormat(int validationErrors) {
        // Log format information in debug mode
        if (debugMode) {
            plugin.getLogger().info("Using new configuration format");
        }

        // Validate broadcast section exists
        if (!config.contains("broadcast")) {
            plugin.getLogger().severe("Missing 'broadcast' section in configuration!");
            validationErrors++;
            return new ArrayList<>();
        }

        // Load and validate interval from broadcast section
        if (!config.contains("broadcast.interval")) {
            plugin.getLogger().warning("Missing 'broadcast.interval' in configuration. Using default value of 5 minutes.");
            validationErrors++;
            this.interval = 5 * 60 * 20; // 5 minutes in ticks
        } else {
            int configInterval = config.getInt("broadcast.interval", 5);
            if (configInterval <= 0) {
                plugin.getLogger().warning("Invalid interval value: " + configInterval + ". Must be greater than 0. Using default value of 5 minutes.");
                configInterval = 5;
                validationErrors++;
            } else if (configInterval > 60) {
                plugin.getLogger().warning("Very large interval detected: " + configInterval + " minutes. This may not be intentional.");
            }

            // Convert minutes to ticks (20 ticks = 1 second)
            this.interval = configInterval * 60 * 20;
        }

        // Validate messages section exists
        if (!config.contains("broadcast.messages")) {
            plugin.getLogger().severe("Missing 'broadcast.messages' section in configuration!");
            validationErrors++;
            return new ArrayList<>();
        }

        // Get messages list
        List<String> messages = config.getStringList("broadcast.messages");
        if (messages.isEmpty()) {
            plugin.getLogger().severe("Empty 'broadcast.messages' list in configuration!");
            validationErrors++;
        }

        return messages;
    }

    /**
     * Validates the legacy configuration format.
     * 
     * @param validationErrors Counter for validation errors
     * @return List of messages from the configuration
     */
    private List<String> validateLegacyConfigFormat(int validationErrors) {
        if (debugMode) {
            plugin.getLogger().info("Using legacy configuration format");
            plugin.getLogger().warning("Legacy configuration format is deprecated. Please update to the new format.");
        } else {
            plugin.getLogger().warning("Using deprecated legacy configuration format. Please update to the new format.");
        }

        // Load and validate interval from root
        if (!config.contains("interval")) {
            plugin.getLogger().warning("Missing 'interval' in configuration. Using default value of 5 minutes.");
            validationErrors++;
            this.interval = 5 * 60 * 20; // 5 minutes in ticks
        } else {
            int configInterval = config.getInt("interval", 5);
            if (configInterval <= 0) {
                plugin.getLogger().warning("Invalid interval value: " + configInterval + ". Must be greater than 0. Using default value of 5 minutes.");
                configInterval = 5;
                validationErrors++;
            }

            // Convert minutes to ticks (20 ticks = 1 second)
            this.interval = configInterval * 60 * 20;
        }

        // Validate messages section exists
        if (!config.contains("messages")) {
            plugin.getLogger().severe("Missing 'messages' section in configuration!");
            validationErrors++;
            return new ArrayList<>();
        }

        // Get messages list
        List<String> messages = config.getStringList("messages");
        if (messages.isEmpty()) {
            plugin.getLogger().severe("Empty 'messages' list in configuration!");
            validationErrors++;
        }

        return messages;
    }

    /**
     * Validates the appearance settings in the configuration.
     * 
     * @return Number of validation errors found
     */
    private int validateAppearanceSettings() {
        int validationErrors = 0;

        // Check if appearance section exists
        if (!config.contains("broadcast.appearance")) {
            plugin.getLogger().info("Missing 'broadcast.appearance' section. Using default appearance settings.");
            return validationErrors;
        }

        // Validate separator
        if (config.contains("broadcast.appearance.separator")) {
            String separator = config.getString("broadcast.appearance.separator");
            if (separator == null || separator.isEmpty()) {
                plugin.getLogger().warning("Empty separator in configuration. Using default separator.");
                validationErrors++;
            } else if (separator.length() > 50) {
                plugin.getLogger().warning("Separator is very long (" + separator.length() + " characters). This may cause display issues.");
            }
        }

        // Validate enable_clickable_links
        if (config.contains("broadcast.appearance.enable_clickable_links")) {
            if (!config.isBoolean("broadcast.appearance.enable_clickable_links")) {
                plugin.getLogger().warning("Invalid value for 'enable_clickable_links'. Must be true or false. Using default value (true).");
                validationErrors++;
            }
        }

        return validationErrors;
    }

    /**
     * Validates the order settings in the configuration.
     * 
     * @return Number of validation errors found
     */
    private int validateOrderSettings() {
        int validationErrors = 0;

        // Check if order section exists
        if (!config.contains("broadcast.order")) {
            plugin.getLogger().info("Missing 'broadcast.order' section. Using default order settings.");
            return validationErrors;
        }

        // Validate randomize_messages
        if (config.contains("broadcast.order.randomize_messages")) {
            if (!config.isBoolean("broadcast.order.randomize_messages")) {
                plugin.getLogger().warning("Invalid value for 'randomize_messages'. Must be true or false. Using default value (false).");
                validationErrors++;
            }
        }

        return validationErrors;
    }

    /**
     * Gets the list of messages to broadcast.
     * This is only used for legacy broadcast mode.
     *
     * @return The list of messages
     */
    public List<String> getMessages() {
        return messages;
    }

    /**
     * Gets the broadcast interval in ticks.
     * This is only used for legacy broadcast mode.
     *
     * @return The interval in ticks
     */
    public int getInterval() {
        return interval;
    }

    /**
     * Gets the broadcast interval in minutes.
     * This is only used for legacy broadcast mode.
     *
     * @return The interval in minutes
     */
    public int getIntervalMinutes() {
        return interval / (60 * 20);
    }

    /**
     * Checks if debug mode is enabled.
     *
     * @return true if debug mode is enabled, false otherwise
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Gets the raw FileConfiguration object.
     *
     * @return The FileConfiguration object
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Checks if categories are enabled.
     *
     * @return true if categories are enabled, false otherwise
     */
    public boolean areCategoriesEnabled() {
        return categoriesEnabled;
    }

    /**
     * Gets the map of message categories.
     *
     * @return The map of message categories
     */
    public Map<String, MessageCategory> getCategories() {
        return categories;
    }

    /**
     * Gets a specific message category by name.
     *
     * @param categoryName The name of the category
     * @return The message category, or null if not found
     */
    public MessageCategory getCategory(String categoryName) {
        return categories.get(categoryName);
    }

    /**
     * Gets the list of category names.
     *
     * @return The list of category names
     */
    public List<String> getCategoryNames() {
        return new ArrayList<>(categories.keySet());
    }

    /**
     * Gets the number of categories.
     *
     * @return The number of categories
     */
    public int getCategoryCount() {
        return categories.size();
    }

    /**
     * Gets the categories directory.
     *
     * @return The categories directory
     */
    public String getCategoriesDirectory() {
        return categoriesDirectory;
    }
}

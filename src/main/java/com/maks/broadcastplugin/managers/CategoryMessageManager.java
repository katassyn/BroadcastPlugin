package com.maks.broadcastplugin.managers;

import com.maks.broadcastplugin.BroadcastPlugin;
import com.maks.broadcastplugin.models.MessageCategory;
import com.maks.broadcastplugin.utils.InteractiveMessageParser;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the formatting and broadcasting of messages for a specific category.
 * This class extends the functionality of MessageManager to handle category-specific settings.
 */
public class CategoryMessageManager {
    private final JavaPlugin plugin;
    private final MessageCategory category;
    private int currentIndex = 0;
    private final Pattern urlPattern;
    private final Random random = new Random();
    private final InteractiveMessageParser interactiveMessageParser;

    // Cache for formatted messages
    private final Map<Integer, List<TextComponent>> messageCache = new HashMap<>();
    private boolean cacheValid = false;

    // Message conditions
    private final Map<Integer, String> messageConditions = new HashMap<>();

    /**
     * Constructs a new CategoryMessageManager.
     *
     * @param plugin The plugin instance
     * @param category The message category to manage
     */
    public CategoryMessageManager(JavaPlugin plugin, MessageCategory category) {
        this.plugin = plugin;
        this.category = category;
        // Compile the pattern once at initialization for better performance
        this.urlPattern = Pattern.compile("(https?://\\S+)");
        // Initialize the interactive message parser
        this.interactiveMessageParser = new InteractiveMessageParser(plugin);

        // Load message conditions if they exist
        loadMessageConditions();

        // Initialize the message cache
        processAndCacheMessages();
    }

    /**
     * Loads message conditions from the configuration.
     */
    private void loadMessageConditions() {
        // Clear existing conditions
        messageConditions.clear();

        // Get the category configuration
        ConfigurationSection categoryConfig = null;
        try {
            // Try to get the category configuration from the plugin's data folder
            File categoryFile = new File(plugin.getDataFolder(), "categories/" + category.getName() + ".yml");
            if (categoryFile.exists()) {
                categoryConfig = YamlConfiguration.loadConfiguration(categoryFile);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error loading category configuration for " + category.getName() + ": " + e.getMessage());
        }

        // If no category configuration was found, return
        if (categoryConfig == null) {
            return;
        }

        // Load message conditions
        if (categoryConfig.contains("messages_conditions")) {
            ConfigurationSection conditionsSection = categoryConfig.getConfigurationSection("messages_conditions");
            if (conditionsSection != null) {
                for (String key : conditionsSection.getKeys(false)) {
                    try {
                        int messageIndex = Integer.parseInt(key);
                        String conditionName = conditionsSection.getString(key);
                        if (conditionName != null && !conditionName.isEmpty()) {
                            messageConditions.put(messageIndex, conditionName);
                            if (plugin.getConfig().getBoolean("debug", false)) {
                                plugin.getLogger().info("Loaded condition for message " + messageIndex + " in category " + 
                                                      category.getName() + ": " + conditionName);
                            }
                        }
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid message index in conditions for category " + 
                                                 category.getName() + ": " + key);
                    }
                }
            }
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Loaded " + messageConditions.size() + " message conditions for category " + 
                                   category.getName());
        }
    }

    /**
     * Clears the message cache.
     */
    public void clearCache() {
        messageCache.clear();
        cacheValid = false;
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Message cache cleared for category: " + category.getName());
        }
    }

    /**
     * Processes and caches all messages for this category.
     */
    public void processAndCacheMessages() {
        List<String> messages = category.getMessages();
        if (messages == null || messages.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();

        // Clear existing cache
        messageCache.clear();

        // Process each message and store in cache
        for (int i = 0; i < messages.size(); i++) {
            String message = messages.get(i);
            if (message == null || message.trim().isEmpty()) {
                continue;
            }

            List<TextComponent> components = new ArrayList<>();

            // Process each line of the message
            for (String line : message.split("\n")) {
                line = line.trim();
                TextComponent lineComponent = processLine(line);
                if (lineComponent != null) {
                    components.add(lineComponent);
                }
            }

            // Store in cache
            messageCache.put(i, components);
        }

        cacheValid = true;

        long endTime = System.currentTimeMillis();
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Cached " + messageCache.size() + " messages for category " + 
                                   category.getName() + " in " + (endTime - startTime) + "ms");
        }
    }

    /**
     * Processes a single line of text, handling interactive elements.
     * 
     * This method takes a plain text line and converts it into a TextComponent with
     * interactive elements (hover events, clickable links, commands).
     * The process works as follows:
     * 
     * 1. Use the InteractiveMessageParser to parse the line and extract interactive elements:
     *    - Hover events: [hover:Hover text]Text to hover over[/hover]
     *    - Command events: [command:/command arg1 arg2]Click to run command[/command]
     *    - Suggest events: [suggest:/command arg1 arg2]Click to suggest command[/suggest]
     *    - URLs are automatically detected and made clickable if enabled
     * 
     * This approach allows for rich interactive messages with hover text, commands,
     * and clickable links.
     * 
     * @param line The line to process
     * @return A TextComponent containing the formatted line with interactive elements
     */
    private TextComponent processLine(String line) {
        if (line.isEmpty()) {
            return null;
        }

        // Use the interactive message parser to parse the line
        List<TextComponent> components = interactiveMessageParser.parse(line, category.isEnableClickableLinks());

        // Create a container component to hold all the parsed components
        TextComponent lineComponent = new TextComponent("");

        // Add all parsed components to the container
        for (TextComponent component : components) {
            lineComponent.addExtra(component);
        }

        return lineComponent;
    }

    /**
     * Broadcasts the next message in the rotation for this category.
     * If randomize_messages is enabled, a random message will be selected instead of sequential order.
     */
    public void broadcastNextMessage() {
        List<String> messages = category.getMessages();
        if (messages == null || messages.isEmpty()) {
            plugin.getLogger().warning("No messages available to broadcast for category: " + category.getName());
            return;
        }

        // Ensure cache is valid
        if (!cacheValid) {
            processAndCacheMessages();
        }

        try {
            long startTime = System.currentTimeMillis();

            // Create decorative line as TextComponent using the category's separator
            TextComponent separator = new TextComponent(category.getSeparatorLine());

            // Determine which message to broadcast
            int messageIndex;
            if (category.isRandomizeMessages()) {
                // Select a random message
                List<Integer> validIndices = new ArrayList<>(messageCache.keySet());
                if (validIndices.isEmpty()) {
                    plugin.getLogger().warning("No valid messages available to broadcast for category: " + category.getName());
                    return;
                }

                // Filter out messages with conditions that are not met
                List<Integer> eligibleIndices = new ArrayList<>();
                for (int index : validIndices) {
                    String conditionName = messageConditions.get(index);
                    if (conditionName == null || ((BroadcastPlugin)plugin).getConditionEvaluator().isConditionMet(conditionName)) {
                        eligibleIndices.add(index);
                    }
                }

                // If no eligible messages, return
                if (eligibleIndices.isEmpty()) {
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("No messages with met conditions available to broadcast for category: " + category.getName());
                    }
                    return;
                }

                messageIndex = eligibleIndices.get(random.nextInt(eligibleIndices.size()));

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Randomly selected message index: " + messageIndex + " for category: " + category.getName());
                }
            } else {
                // Use sequential order
                messageIndex = currentIndex;
                int startIndex = messageIndex;
                boolean foundEligibleMessage = false;
                List<String> categoryMessages = category.getMessages();

                // Loop through messages until we find one with met conditions or we've checked all
                do {
                    // Skip empty messages
                    if (!messageCache.containsKey(messageIndex)) {
                        messageIndex = (messageIndex + 1) % categoryMessages.size();
                        continue;
                    }

                    // Check if the message condition is met
                    String conditionName = messageConditions.get(messageIndex);
                    if (conditionName == null || ((BroadcastPlugin)plugin).getConditionEvaluator().isConditionMet(conditionName)) {
                        foundEligibleMessage = true;
                        break;
                    }

                    // Move to the next message
                    messageIndex = (messageIndex + 1) % categoryMessages.size();
                } while (messageIndex != startIndex);

                // If no eligible messages, return
                if (!foundEligibleMessage) {
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("No messages with met conditions available to broadcast for category: " + category.getName());
                    }
                    return;
                }

                // Update the current index for next time
                currentIndex = (messageIndex + 1) % categoryMessages.size();
            }

            // Send the separator line
            Bukkit.spigot().broadcast(separator);

            // Get cached message components
            List<TextComponent> components = messageCache.get(messageIndex);

            // Broadcast each component
            for (TextComponent component : components) {
                Bukkit.spigot().broadcast(component);
            }

            // Send the final separator line
            Bukkit.spigot().broadcast(separator);

            // Log performance metrics if debug mode is enabled
            long endTime = System.currentTimeMillis();
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Message broadcast completed in " + (endTime - startTime) + 
                                       "ms for category: " + category.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error broadcasting message for category " + category.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets the current index of the message rotation.
     *
     * @return The current index
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Gets the total number of messages in this category.
     *
     * @return The number of messages
     */
    public int getMessageCount() {
        return category.getMessages().size();
    }

    /**
     * Gets the category managed by this manager.
     *
     * @return The message category
     */
    public MessageCategory getCategory() {
        return category;
    }
}

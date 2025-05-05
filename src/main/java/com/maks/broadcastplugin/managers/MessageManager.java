package com.maks.broadcastplugin.managers;

import com.maks.broadcastplugin.BroadcastPlugin;
import com.maks.broadcastplugin.utils.InteractiveMessageParser;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the formatting and broadcasting of messages to players.
 */
public class MessageManager {
    private final BroadcastPlugin plugin;
    private List<String> messages;
    private int currentIndex = 0;
    private final Pattern urlPattern;
    private final Random random = new Random();
    private final InteractiveMessageParser interactiveMessageParser;

    // Customization options
    private String separatorLine;
    private boolean enableClickableLinks;
    private boolean randomizeMessages;

    // Cache for formatted messages
    private final Map<Integer, List<TextComponent>> messageCache = new HashMap<>();
    private boolean cacheValid = false;

    // Message conditions
    private final Map<Integer, String> messageConditions = new HashMap<>();

    /**
     * Constructs a new MessageManager.
     *
     * @param plugin The plugin instance
     * @param messages The list of messages to broadcast
     */
    public MessageManager(BroadcastPlugin plugin, List<String> messages) {
        this.plugin = plugin;
        this.messages = messages;
        // Compile the pattern once at initialization for better performance
        this.urlPattern = Pattern.compile("(https?://\\S+)");
        // Initialize the interactive message parser
        this.interactiveMessageParser = new InteractiveMessageParser(plugin);

        // Load customization options from config
        loadCustomizationOptions();

        // Initialize the message cache
        processAndCacheMessages();
    }

    /**
     * Loads customization options from the configuration.
     */
    private void loadCustomizationOptions() {
        FileConfiguration config = plugin.getConfig();

        // Load separator line
        this.separatorLine = config.getString("broadcast.appearance.separator", "------------------------------");

        // Load clickable links option
        this.enableClickableLinks = config.getBoolean("broadcast.appearance.enable_clickable_links", true);

        // Load randomize messages option
        this.randomizeMessages = config.getBoolean("broadcast.order.randomize_messages", false);

        // Load message conditions
        messageConditions.clear();
        if (config.contains("broadcast.messages_conditions")) {
            ConfigurationSection conditionsSection = config.getConfigurationSection("broadcast.messages_conditions");
            if (conditionsSection != null) {
                for (String key : conditionsSection.getKeys(false)) {
                    try {
                        int messageIndex = Integer.parseInt(key);
                        String conditionName = conditionsSection.getString(key);
                        if (conditionName != null && !conditionName.isEmpty()) {
                            messageConditions.put(messageIndex, conditionName);
                            if (plugin.getConfig().getBoolean("debug", false)) {
                                plugin.getLogger().info("Loaded condition for message " + messageIndex + ": " + conditionName);
                            }
                        }
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid message index in conditions: " + key);
                    }
                }
            }
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Loaded customization options:");
            plugin.getLogger().info("- Separator: " + separatorLine);
            plugin.getLogger().info("- Clickable links: " + enableClickableLinks);
            plugin.getLogger().info("- Randomize messages: " + randomizeMessages);
            plugin.getLogger().info("- Message conditions: " + messageConditions.size());
        }
    }

    /**
     * Sets the messages to be broadcast and reloads customization options.
     *
     * @param messages The list of messages
     */
    public void setMessages(List<String> messages) {
        this.messages = messages;
        this.currentIndex = 0;

        // Reload customization options in case they've changed
        loadCustomizationOptions();

        // Invalidate cache when messages are changed
        clearCache();
    }

    /**
     * Clears the message cache.
     */
    public void clearCache() {
        messageCache.clear();
        cacheValid = false;
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Message cache cleared");
        }
    }

    /**
     * Processes and caches all messages.
     */
    public void processAndCacheMessages() {
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
                TextComponent lineComponent = processLine(line, null);
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
            plugin.getLogger().info("Cached " + messageCache.size() + " messages in " + (endTime - startTime) + "ms");
        }
    }

    /**
     * Processes a single line of text, handling interactive elements and placeholder replacement.
     * 
     * This method takes a plain text line and converts it into a TextComponent with
     * interactive elements (hover events, clickable links, commands) and replaced placeholders.
     * The process works as follows:
     * 
     * 1. Replace placeholders in the line using the PlaceholderManager
     * 2. Use the InteractiveMessageParser to parse the line and extract interactive elements:
     *    - Hover events: [hover:Hover text]Text to hover over[/hover]
     *    - Command events: [command:/command arg1 arg2]Click to run command[/command]
     *    - Suggest events: [suggest:/command arg1 arg2]Click to suggest command[/suggest]
     *    - URLs are automatically detected and made clickable if enabled
     * 
     * This approach allows for rich interactive messages with hover text, commands,
     * and clickable links.
     * 
     * @param line The line to process
     * @param player The player to use for player-specific placeholders (can be null)
     * @return A TextComponent containing the formatted line with interactive elements and replaced placeholders
     */
    private TextComponent processLine(String line, Player player) {
        if (line.isEmpty()) {
            return null;
        }

        // Replace placeholders in the line
        if (plugin.getPlaceholderManager() != null) {
            line = plugin.getPlaceholderManager().replacePlaceholders(line, player);
        }

        // Use the interactive message parser to parse the line
        List<TextComponent> components = interactiveMessageParser.parse(line, enableClickableLinks);

        // Create a container component to hold all the parsed components
        TextComponent lineComponent = new TextComponent("");

        // Add all parsed components to the container
        for (TextComponent component : components) {
            lineComponent.addExtra(component);
        }

        return lineComponent;
    }

    /**
     * Processes a single line of text, handling URL formatting.
     * This is a convenience method that calls processLine(line, null).
     *
     * @param line The line to process
     * @return A TextComponent containing the formatted line with clickable URLs (if enabled)
     */
    private TextComponent processLine(String line) {
        return processLine(line, null);
    }

    /**
     * Broadcasts the next message in the rotation to all players.
     * If randomize_messages is enabled, a random message will be selected instead of sequential order.
     */
    public void broadcastNextMessage() {
        // Call the targeted version with null to broadcast to all players
        broadcastNextMessage(null, null);
    }

    /**
     * Broadcasts the next message in the rotation to specific players or groups.
     * If randomize_messages is enabled, a random message will be selected instead of sequential order.
     *
     * @param targetPlayers List of specific players to receive the message (null for all players)
     * @param requiredPermission Permission required to receive the message (null for no permission check)
     */
    public void broadcastNextMessage(List<Player> targetPlayers, String requiredPermission) {
        if (messages == null || messages.isEmpty()) {
            plugin.getLogger().warning("No messages available to broadcast!");
            return;
        }

        // Ensure cache is valid
        if (!cacheValid) {
            processAndCacheMessages();
        }

        try {
            long startTime = System.currentTimeMillis();

            // Create decorative line as TextComponent using the configured separator
            TextComponent separator = new TextComponent(separatorLine);

            // Determine which message to broadcast
            int messageIndex;
            if (randomizeMessages) {
                // Select a random message
                List<Integer> validIndices = new ArrayList<>(messageCache.keySet());
                if (validIndices.isEmpty()) {
                    plugin.getLogger().warning("No valid messages available to broadcast!");
                    return;
                }

                // Filter out messages with conditions that are not met
                List<Integer> eligibleIndices = new ArrayList<>();
                for (int index : validIndices) {
                    String conditionName = messageConditions.get(index);
                    if (conditionName == null || plugin.getConditionEvaluator().isConditionMet(conditionName)) {
                        eligibleIndices.add(index);
                    }
                }

                // If no eligible messages, return
                if (eligibleIndices.isEmpty()) {
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("No messages with met conditions available to broadcast");
                    }
                    return;
                }

                messageIndex = eligibleIndices.get(random.nextInt(eligibleIndices.size()));

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Randomly selected message index: " + messageIndex);
                }
            } else {
                // Use sequential order
                messageIndex = currentIndex;
                int startIndex = messageIndex;
                boolean foundEligibleMessage = false;

                // Loop through messages until we find one with met conditions or we've checked all
                do {
                    // Skip empty messages
                    if (!messageCache.containsKey(messageIndex)) {
                        messageIndex = (messageIndex + 1) % messages.size();
                        continue;
                    }

                    // Check if the message condition is met
                    String conditionName = messageConditions.get(messageIndex);
                    if (conditionName == null || plugin.getConditionEvaluator().isConditionMet(conditionName)) {
                        foundEligibleMessage = true;
                        break;
                    }

                    // Move to the next message
                    messageIndex = (messageIndex + 1) % messages.size();
                } while (messageIndex != startIndex);

                // If no eligible messages, return
                if (!foundEligibleMessage) {
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("No messages with met conditions available to broadcast");
                    }
                    return;
                }

                // Update the current index for next time
                currentIndex = (messageIndex + 1) % messages.size();
            }

            // Get cached message components
            List<TextComponent> components = messageCache.get(messageIndex);

            // Determine the recipients of the message
            if (targetPlayers == null) {
                // Broadcast to all players, possibly with permission check
                if (requiredPermission == null) {
                    // No permission check, broadcast to everyone
                    // Send the separator line
                    Bukkit.spigot().broadcast(separator);

                    // Broadcast each component
                    for (TextComponent component : components) {
                        Bukkit.spigot().broadcast(component);
                    }

                    // Send the final separator line
                    Bukkit.spigot().broadcast(separator);
                } else {
                    // Permission check required, broadcast only to players with permission
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.hasPermission(requiredPermission)) {
                            // Send the separator line
                            player.spigot().sendMessage(separator);

                            // Send each component with placeholders replaced for this player
                            for (String line : messages.get(messageIndex).split("\n")) {
                                line = line.trim();
                                if (!line.isEmpty()) {
                                    TextComponent lineComponent = processLine(line, player);
                                    if (lineComponent != null) {
                                        player.spigot().sendMessage(lineComponent);
                                    }
                                }
                            }

                            // Send the final separator line
                            player.spigot().sendMessage(separator);
                        }
                    }
                }
            } else {
                // Broadcast only to specific players, possibly with permission check
                for (Player player : targetPlayers) {
                    if (player != null && player.isOnline() && 
                        (requiredPermission == null || player.hasPermission(requiredPermission))) {
                        // Send the separator line
                        player.spigot().sendMessage(separator);

                        // Send each component with placeholders replaced for this player
                        for (String line : messages.get(messageIndex).split("\n")) {
                            line = line.trim();
                            if (!line.isEmpty()) {
                                TextComponent lineComponent = processLine(line, player);
                                if (lineComponent != null) {
                                    player.spigot().sendMessage(lineComponent);
                                }
                            }
                        }

                        // Send the final separator line
                        player.spigot().sendMessage(separator);
                    }
                }
            }

            // Log performance metrics if debug mode is enabled
            long endTime = System.currentTimeMillis();
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Message broadcast completed in " + (endTime - startTime) + "ms");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error broadcasting message: " + e.getMessage());
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
     * Sets the current index of the message rotation.
     * This is used by the MessageScheduler to broadcast specific messages.
     *
     * @param index The index to set
     */
    public void setCurrentIndex(int index) {
        if (index >= 0 && index < messages.size()) {
            this.currentIndex = index;
        }
    }

    /**
     * Gets the total number of messages.
     *
     * @return The number of messages
     */
    public int getMessageCount() {
        return messages != null ? messages.size() : 0;
    }
}

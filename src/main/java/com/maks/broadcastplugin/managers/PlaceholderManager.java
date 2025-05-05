package com.maks.broadcastplugin.managers;

import com.maks.broadcastplugin.BroadcastPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages placeholder replacement in messages.
 * Supports both built-in placeholders and integration with PlaceholderAPI if available.
 */
public class PlaceholderManager {
    private final BroadcastPlugin plugin;
    private final boolean placeholderApiEnabled;
    private final Pattern placeholderPattern;
    private final Map<String, Supplier<String>> builtinPlaceholders = new HashMap<>();
    
    /**
     * Constructs a new PlaceholderManager.
     *
     * @param plugin The plugin instance
     */
    public PlaceholderManager(BroadcastPlugin plugin) {
        this.plugin = plugin;
        this.placeholderApiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        this.placeholderPattern = Pattern.compile("\\{([^{}]+)\\}");
        
        // Register built-in placeholders
        registerBuiltinPlaceholders();
        
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("PlaceholderManager initialized. PlaceholderAPI " + 
                                   (placeholderApiEnabled ? "detected" : "not detected"));
        }
    }
    
    /**
     * Registers all built-in placeholders.
     */
    private void registerBuiltinPlaceholders() {
        // Server-related placeholders
        builtinPlaceholders.put("server_name", () -> Bukkit.getServer().getName());
        builtinPlaceholders.put("server_version", () -> Bukkit.getServer().getVersion());
        builtinPlaceholders.put("server_motd", () -> Bukkit.getServer().getMotd());
        builtinPlaceholders.put("server_ip", () -> Bukkit.getServer().getIp());
        builtinPlaceholders.put("server_port", () -> String.valueOf(Bukkit.getServer().getPort()));
        
        // Player-related placeholders
        builtinPlaceholders.put("online_players", () -> String.valueOf(Bukkit.getOnlinePlayers().size()));
        builtinPlaceholders.put("max_players", () -> String.valueOf(Bukkit.getMaxPlayers()));
        
        // Performance-related placeholders
        builtinPlaceholders.put("server_tps", this::getServerTPS);
        
        // Time-related placeholders
        builtinPlaceholders.put("time", () -> new SimpleDateFormat("HH:mm:ss").format(new Date()));
        builtinPlaceholders.put("date", () -> new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        builtinPlaceholders.put("datetime", () -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        
        // Plugin-related placeholders
        builtinPlaceholders.put("plugin_version", () -> plugin.getDescription().getVersion());
        builtinPlaceholders.put("plugin_author", () -> String.join(", ", plugin.getDescription().getAuthors()));
        
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Registered " + builtinPlaceholders.size() + " built-in placeholders");
        }
    }
    
    /**
     * Gets the server TPS (Ticks Per Second).
     * This is an approximation since the actual TPS is not directly accessible.
     *
     * @return The server TPS as a string
     */
    private String getServerTPS() {
        try {
            // Try to access the TPS through reflection (works on some server implementations)
            Object minecraftServer = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            double[] tps = (double[]) minecraftServer.getClass().getField("recentTps").get(minecraftServer);
            return String.format("%.2f", tps[0]);
        } catch (Exception e) {
            // If reflection fails, return a placeholder value
            return "20.00";
        }
    }
    
    /**
     * Replaces all placeholders in a message.
     * This method handles both built-in placeholders and PlaceholderAPI placeholders.
     *
     * @param message The message to process
     * @param player The player to use for player-specific placeholders (can be null)
     * @return The message with all placeholders replaced
     */
    public String replacePlaceholders(String message, Player player) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        // First, try to use PlaceholderAPI if available
        if (placeholderApiEnabled && player != null) {
            try {
                // Use reflection to avoid direct dependency on PlaceholderAPI
                Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                message = (String) papiClass.getMethod("setPlaceholders", Player.class, String.class)
                    .invoke(null, player, message);
            } catch (Exception e) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().warning("Error using PlaceholderAPI: " + e.getMessage());
                }
            }
        }
        
        // Then, replace built-in placeholders
        Matcher matcher = placeholderPattern.matcher(message);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = getPlaceholderReplacement(placeholder, player);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Gets the replacement value for a placeholder.
     *
     * @param placeholder The placeholder name (without braces)
     * @param player The player to use for player-specific placeholders (can be null)
     * @return The replacement value, or the original placeholder if not found
     */
    private String getPlaceholderReplacement(String placeholder, Player player) {
        // Check built-in placeholders
        if (builtinPlaceholders.containsKey(placeholder)) {
            try {
                return builtinPlaceholders.get(placeholder).get();
            } catch (Exception e) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().warning("Error getting value for placeholder {" + placeholder + "}: " + e.getMessage());
                }
                return "{" + placeholder + "}";
            }
        }
        
        // Handle player-specific placeholders
        if (player != null) {
            switch (placeholder) {
                case "player":
                case "player_name":
                    return player.getName();
                case "player_display_name":
                    return player.getDisplayName();
                case "player_world":
                    return player.getWorld().getName();
                case "player_health":
                    return String.format("%.1f", player.getHealth());
                case "player_level":
                    return String.valueOf(player.getLevel());
                case "player_exp":
                    return String.format("%.2f", player.getExp());
                case "player_food_level":
                    return String.valueOf(player.getFoodLevel());
                case "player_gamemode":
                    return player.getGameMode().name();
                case "player_ping":
                    try {
                        // This is a bit hacky but works on most server implementations
                        Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
                        int ping = (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
                        return String.valueOf(ping);
                    } catch (Exception e) {
                        return "0";
                    }
            }
        }
        
        // If no replacement found, return the original placeholder
        return "{" + placeholder + "}";
    }
    
    /**
     * Checks if PlaceholderAPI is available.
     *
     * @return true if PlaceholderAPI is available, false otherwise
     */
    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }
}
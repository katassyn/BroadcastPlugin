package com.maks.broadcastplugin.conditions;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Evaluates message conditions based on the current server state.
 * This class is responsible for loading conditions from configuration and checking if they are met.
 */
public class MessageConditionEvaluator {
    private final JavaPlugin plugin;
    private final Map<String, MessageCondition> conditions = new HashMap<>();
    
    /**
     * Constructs a new MessageConditionEvaluator.
     *
     * @param plugin The plugin instance
     */
    public MessageConditionEvaluator(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Loads conditions from the configuration.
     *
     * @param config The configuration section containing the conditions
     */
    public void loadConditions(ConfigurationSection config) {
        // Clear existing conditions
        conditions.clear();
        
        if (config == null) {
            return;
        }
        
        // Load each condition
        for (String key : config.getKeys(false)) {
            ConfigurationSection conditionConfig = config.getConfigurationSection(key);
            if (conditionConfig == null) {
                continue;
            }
            
            // Create a builder for this condition
            MessageCondition.Builder builder = MessageCondition.builder();
            
            // Load player count conditions
            if (conditionConfig.contains("min_players")) {
                builder.withMinPlayers(conditionConfig.getInt("min_players", 0));
            }
            
            if (conditionConfig.contains("max_players")) {
                builder.withMaxPlayers(conditionConfig.getInt("max_players", Integer.MAX_VALUE));
            }
            
            // Load permission condition
            if (conditionConfig.contains("required_permission")) {
                builder.withRequiredPermission(conditionConfig.getString("required_permission"));
            }
            
            // Load world condition
            if (conditionConfig.contains("required_world")) {
                builder.withRequiredWorld(conditionConfig.getString("required_world"));
            }
            
            // Load weather condition
            if (conditionConfig.contains("required_weather")) {
                builder.withRequiredWeather(conditionConfig.getString("required_weather"));
            }
            
            // Load time conditions
            if (conditionConfig.contains("require_day")) {
                builder.withRequireDay(conditionConfig.getBoolean("require_day", false));
            }
            
            if (conditionConfig.contains("require_night")) {
                builder.withRequireNight(conditionConfig.getBoolean("require_night", false));
            }
            
            // Build and store the condition
            conditions.put(key, builder.build());
            
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Loaded condition: " + key);
            }
        }
    }
    
    /**
     * Checks if a condition is met.
     *
     * @param conditionName The name of the condition to check
     * @return true if the condition is met or doesn't exist, false otherwise
     */
    public boolean isConditionMet(String conditionName) {
        // If no condition name is specified, or the condition doesn't exist, consider it met
        if (conditionName == null || !conditions.containsKey(conditionName)) {
            return true;
        }
        
        // Get the condition and check if it's met
        MessageCondition condition = conditions.get(conditionName);
        boolean met = condition.isMet();
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Condition " + conditionName + " is " + (met ? "met" : "not met"));
        }
        
        return met;
    }
    
    /**
     * Checks if all conditions in a list are met.
     *
     * @param conditionNames The names of the conditions to check
     * @return true if all conditions are met, false otherwise
     */
    public boolean areAllConditionsMet(String... conditionNames) {
        if (conditionNames == null || conditionNames.length == 0) {
            return true;
        }
        
        for (String conditionName : conditionNames) {
            if (!isConditionMet(conditionName)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if any condition in a list is met.
     *
     * @param conditionNames The names of the conditions to check
     * @return true if any condition is met, false otherwise
     */
    public boolean isAnyConditionMet(String... conditionNames) {
        if (conditionNames == null || conditionNames.length == 0) {
            return true;
        }
        
        for (String conditionName : conditionNames) {
            if (isConditionMet(conditionName)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets the number of loaded conditions.
     *
     * @return The number of conditions
     */
    public int getConditionCount() {
        return conditions.size();
    }
    
    /**
     * Gets a condition by name.
     *
     * @param conditionName The name of the condition
     * @return The condition, or null if not found
     */
    public MessageCondition getCondition(String conditionName) {
        return conditions.get(conditionName);
    }
    
    /**
     * Gets all loaded conditions.
     *
     * @return A map of condition names to conditions
     */
    public Map<String, MessageCondition> getConditions() {
        return new HashMap<>(conditions);
    }
}
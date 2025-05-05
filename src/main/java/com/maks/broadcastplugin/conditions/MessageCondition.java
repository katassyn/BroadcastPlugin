package com.maks.broadcastplugin.conditions;

import org.bukkit.Bukkit;

/**
 * Represents a condition that must be met for a message to be broadcast.
 * This class provides a builder pattern for creating message conditions.
 */
public class MessageCondition {
    private int minPlayers = 0;
    private int maxPlayers = Integer.MAX_VALUE;
    private String requiredPermission;
    private String requiredWorld;
    private String requiredWeather;
    private boolean requireDay = false;
    private boolean requireNight = false;
    
    /**
     * Private constructor to enforce the use of the builder.
     */
    private MessageCondition() {
    }
    
    /**
     * Creates a new builder for a message condition.
     *
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Checks if this condition is met.
     *
     * @return true if the condition is met, false otherwise
     */
    public boolean isMet() {
        // Check player count condition
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        if (onlinePlayers < minPlayers || onlinePlayers > maxPlayers) {
            return false;
        }
        
        // Other conditions would be checked here
        // For now, we'll consider the condition met if the player count condition is met
        return true;
    }
    
    /**
     * Gets the minimum number of players required for this condition.
     *
     * @return The minimum number of players
     */
    public int getMinPlayers() {
        return minPlayers;
    }
    
    /**
     * Gets the maximum number of players allowed for this condition.
     *
     * @return The maximum number of players
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    /**
     * Gets the required permission for this condition.
     *
     * @return The required permission, or null if none
     */
    public String getRequiredPermission() {
        return requiredPermission;
    }
    
    /**
     * Gets the required world for this condition.
     *
     * @return The required world, or null if none
     */
    public String getRequiredWorld() {
        return requiredWorld;
    }
    
    /**
     * Gets the required weather for this condition.
     *
     * @return The required weather, or null if none
     */
    public String getRequiredWeather() {
        return requiredWeather;
    }
    
    /**
     * Checks if this condition requires it to be day.
     *
     * @return true if day is required, false otherwise
     */
    public boolean isRequireDay() {
        return requireDay;
    }
    
    /**
     * Checks if this condition requires it to be night.
     *
     * @return true if night is required, false otherwise
     */
    public boolean isRequireNight() {
        return requireNight;
    }
    
    /**
     * Builder class for creating MessageCondition instances.
     */
    public static class Builder {
        private final MessageCondition condition;
        
        /**
         * Constructs a new Builder.
         */
        public Builder() {
            condition = new MessageCondition();
        }
        
        /**
         * Sets the minimum number of players required for this condition.
         *
         * @param minPlayers The minimum number of players
         * @return This builder
         */
        public Builder withMinPlayers(int minPlayers) {
            condition.minPlayers = minPlayers;
            return this;
        }
        
        /**
         * Sets the maximum number of players allowed for this condition.
         *
         * @param maxPlayers The maximum number of players
         * @return This builder
         */
        public Builder withMaxPlayers(int maxPlayers) {
            condition.maxPlayers = maxPlayers;
            return this;
        }
        
        /**
         * Sets the required permission for this condition.
         *
         * @param permission The required permission
         * @return This builder
         */
        public Builder withRequiredPermission(String permission) {
            condition.requiredPermission = permission;
            return this;
        }
        
        /**
         * Sets the required world for this condition.
         *
         * @param world The required world
         * @return This builder
         */
        public Builder withRequiredWorld(String world) {
            condition.requiredWorld = world;
            return this;
        }
        
        /**
         * Sets the required weather for this condition.
         *
         * @param weather The required weather (clear, rain, thunder)
         * @return This builder
         */
        public Builder withRequiredWeather(String weather) {
            condition.requiredWeather = weather;
            return this;
        }
        
        /**
         * Sets whether this condition requires it to be day.
         *
         * @param requireDay Whether day is required
         * @return This builder
         */
        public Builder withRequireDay(boolean requireDay) {
            condition.requireDay = requireDay;
            return this;
        }
        
        /**
         * Sets whether this condition requires it to be night.
         *
         * @param requireNight Whether night is required
         * @return This builder
         */
        public Builder withRequireNight(boolean requireNight) {
            condition.requireNight = requireNight;
            return this;
        }
        
        /**
         * Builds the message condition.
         *
         * @return The built condition
         */
        public MessageCondition build() {
            return condition;
        }
    }
}
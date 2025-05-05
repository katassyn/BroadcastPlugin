package com.maks.broadcastplugin.managers;

import com.maks.broadcastplugin.models.MessageCategory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class for creating and managing CategoryMessageManager instances.
 * This class ensures that only one CategoryMessageManager is created for each category.
 */
public class CategoryManagerFactory {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final Map<String, CategoryMessageManager> managers = new HashMap<>();
    
    /**
     * Constructs a new CategoryManagerFactory.
     *
     * @param plugin The plugin instance
     * @param configManager The configuration manager
     */
    public CategoryManagerFactory(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        initializeManagers();
    }
    
    /**
     * Initializes the category message managers.
     */
    private void initializeManagers() {
        // Clear existing managers
        managers.clear();
        
        // If categories are not enabled, return
        if (!configManager.areCategoriesEnabled()) {
            return;
        }
        
        // Create a manager for each category
        for (MessageCategory category : configManager.getCategories().values()) {
            CategoryMessageManager manager = new CategoryMessageManager(plugin, category);
            managers.put(category.getName(), manager);
            
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Created message manager for category: " + category.getName());
            }
        }
    }
    
    /**
     * Gets a CategoryMessageManager for the specified category.
     *
     * @param categoryName The name of the category
     * @return The CategoryMessageManager, or null if the category doesn't exist
     */
    public CategoryMessageManager getManager(String categoryName) {
        return managers.get(categoryName);
    }
    
    /**
     * Gets all CategoryMessageManager instances.
     *
     * @return A map of category names to CategoryMessageManager instances
     */
    public Map<String, CategoryMessageManager> getManagers() {
        return managers;
    }
    
    /**
     * Reloads all category message managers.
     * This should be called when the configuration is reloaded.
     */
    public void reloadManagers() {
        initializeManagers();
    }
    
    /**
     * Clears the cache for all category message managers.
     */
    public void clearCache() {
        for (CategoryMessageManager manager : managers.values()) {
            manager.clearCache();
        }
    }
}
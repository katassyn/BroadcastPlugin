package com.maks.broadcastplugin.managers;

import com.maks.broadcastplugin.BroadcastPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Watches configuration files for changes and automatically reloads the configuration.
 * This class implements hot-reloading of configuration files without requiring a command.
 */
public class ConfigWatcher {
    private final BroadcastPlugin plugin;
    private final ConfigManager configManager;
    private WatchService watchService;
    private final Map<WatchKey, Path> watchKeys = new HashMap<>();
    private BukkitTask watcherTask;
    private boolean running = false;
    
    // Cooldown to prevent multiple reloads in quick succession
    private long lastReloadTime = 0;
    private static final long RELOAD_COOLDOWN_MS = 2000; // 2 seconds
    
    /**
     * Constructs a new ConfigWatcher.
     *
     * @param plugin The plugin instance
     * @param configManager The configuration manager
     */
    public ConfigWatcher(BroadcastPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    /**
     * Starts watching configuration files for changes.
     */
    public void startWatching() {
        if (running) {
            return;
        }
        
        try {
            // Create a new watch service
            watchService = FileSystems.getDefault().newWatchService();
            
            // Register the plugin's data folder
            registerDirectory(plugin.getDataFolder().toPath());
            
            // If categories are enabled, register the categories directory
            if (configManager.areCategoriesEnabled()) {
                File categoriesDir = new File(plugin.getDataFolder(), configManager.getCategoriesDirectory());
                if (categoriesDir.exists() && categoriesDir.isDirectory()) {
                    registerDirectory(categoriesDir.toPath());
                }
            }
            
            // Start a task to check for events
            watcherTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::checkForChanges,
                20L, // Initial delay: 1 second
                20L  // Check every 1 second
            );
            
            running = true;
            
            if (configManager.isDebugMode()) {
                plugin.getLogger().info("Started watching configuration files for changes");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error starting configuration watcher: " + e.getMessage());
            if (configManager.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Registers a directory with the watch service.
     *
     * @param directory The directory to watch
     */
    private void registerDirectory(Path directory) {
        try {
            // Register for create, modify, and delete events
            WatchKey key = directory.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
            );
            
            // Store the key and directory
            watchKeys.put(key, directory);
            
            if (configManager.isDebugMode()) {
                plugin.getLogger().info("Registered directory for watching: " + directory);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error registering directory for watching: " + directory + " - " + e.getMessage());
        }
    }
    
    /**
     * Checks for changes to configuration files.
     */
    private void checkForChanges() {
        if (watchService == null) {
            return;
        }
        
        try {
            // Poll for events with a timeout
            WatchKey key = watchService.poll(100, TimeUnit.MILLISECONDS);
            if (key == null) {
                return;
            }
            
            // Get the directory for this key
            Path dir = watchKeys.get(key);
            if (dir == null) {
                return;
            }
            
            // Process events
            boolean configChanged = false;
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                
                // Skip overflow events
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                
                // Get the file name from the event
                @SuppressWarnings("unchecked")
                Path fileName = ((WatchEvent<Path>) event).context();
                Path fullPath = dir.resolve(fileName);
                
                // Check if this is a configuration file
                if (isConfigFile(fullPath.toFile())) {
                    if (configManager.isDebugMode()) {
                        plugin.getLogger().info("Detected change to configuration file: " + fullPath);
                    }
                    configChanged = true;
                }
            }
            
            // Reset the key
            boolean valid = key.reset();
            if (!valid) {
                watchKeys.remove(key);
            }
            
            // If a configuration file changed, reload the configuration
            if (configChanged) {
                reloadConfigIfNeeded();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking for configuration changes: " + e.getMessage());
            if (configManager.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Checks if a file is a configuration file.
     *
     * @param file The file to check
     * @return true if the file is a configuration file, false otherwise
     */
    private boolean isConfigFile(File file) {
        if (!file.isFile()) {
            return false;
        }
        
        String name = file.getName().toLowerCase();
        
        // Check if this is the main config file
        if (name.equals("config.yml")) {
            return true;
        }
        
        // Check if this is a category file
        if (configManager.areCategoriesEnabled() && 
            file.getParentFile().getName().equals(configManager.getCategoriesDirectory()) && 
            name.endsWith(".yml")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Reloads the configuration if needed.
     * This method implements a cooldown to prevent multiple reloads in quick succession.
     */
    private void reloadConfigIfNeeded() {
        long currentTime = System.currentTimeMillis();
        
        // Check if we're still in the cooldown period
        if (currentTime - lastReloadTime < RELOAD_COOLDOWN_MS) {
            return;
        }
        
        // Update the last reload time
        lastReloadTime = currentTime;
        
        // Schedule a task to reload the configuration on the main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                plugin.getLogger().info("Configuration file changed, reloading...");
                
                // Reload the configuration
                configManager.loadConfig();
                
                // Restart broadcasting
                plugin.startBroadcastMessages();
                
                plugin.getLogger().info("Configuration reloaded successfully");
            } catch (Exception e) {
                plugin.getLogger().severe("Error reloading configuration: " + e.getMessage());
                if (configManager.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    /**
     * Stops watching configuration files.
     */
    public void stopWatching() {
        if (!running) {
            return;
        }
        
        try {
            // Cancel the watcher task
            if (watcherTask != null) {
                watcherTask.cancel();
                watcherTask = null;
            }
            
            // Close the watch service
            if (watchService != null) {
                watchService.close();
                watchService = null;
            }
            
            // Clear the watch keys
            watchKeys.clear();
            
            running = false;
            
            if (configManager.isDebugMode()) {
                plugin.getLogger().info("Stopped watching configuration files");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error stopping configuration watcher: " + e.getMessage());
        }
    }
    
    /**
     * Checks if the watcher is running.
     *
     * @return true if the watcher is running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }
}
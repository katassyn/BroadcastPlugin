package com.maks.broadcastplugin.scheduling;

import com.maks.broadcastplugin.BroadcastPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the scheduling of messages, including time-based scheduling and different intervals.
 */
public class MessageScheduler {
    private final BroadcastPlugin plugin;
    private final List<MessageSchedule> schedules = new ArrayList<>();
    private final Map<Integer, BukkitTask> scheduleTasks = new HashMap<>();
    private final int defaultInterval; // in ticks
    
    /**
     * Constructs a new MessageScheduler.
     *
     * @param plugin The plugin instance
     * @param defaultInterval The default interval in minutes
     */
    public MessageScheduler(BroadcastPlugin plugin, int defaultInterval) {
        this.plugin = plugin;
        this.defaultInterval = defaultInterval * 60 * 20; // Convert minutes to ticks
    }
    
    /**
     * Loads schedules from the configuration.
     *
     * @param config The configuration section containing the schedules
     */
    public void loadSchedules(ConfigurationSection config) {
        // Clear existing schedules
        stopAllSchedules();
        schedules.clear();
        
        if (config == null) {
            return;
        }
        
        // Load each message schedule
        for (String key : config.getKeys(false)) {
            ConfigurationSection scheduleConfig = config.getConfigurationSection(key);
            if (scheduleConfig == null) {
                continue;
            }
            
            // Get message index
            int messageIndex = scheduleConfig.getInt("message_index", -1);
            if (messageIndex < 0) {
                plugin.getLogger().warning("Invalid message index in schedule: " + key);
                continue;
            }
            
            // Get custom interval
            int customInterval = scheduleConfig.getInt("interval", -1);
            
            // Create the schedule
            MessageSchedule schedule = new MessageSchedule(messageIndex, customInterval);
            
            // Add active time ranges
            ConfigurationSection timeRangesConfig = scheduleConfig.getConfigurationSection("active_times");
            if (timeRangesConfig != null) {
                for (String rangeKey : timeRangesConfig.getKeys(false)) {
                    String startTime = timeRangesConfig.getString(rangeKey + ".start");
                    String endTime = timeRangesConfig.getString(rangeKey + ".end");
                    
                    if (startTime != null && endTime != null) {
                        if (!schedule.addActiveTimeRange(startTime, endTime)) {
                            plugin.getLogger().warning("Invalid time range in schedule " + key + ": " + startTime + " - " + endTime);
                        }
                    }
                }
            }
            
            // Add active days
            List<String> activeDays = scheduleConfig.getStringList("active_days");
            for (String day : activeDays) {
                if (!schedule.addActiveDay(day)) {
                    plugin.getLogger().warning("Invalid day in schedule " + key + ": " + day);
                }
            }
            
            // Add the schedule
            schedules.add(schedule);
            
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Loaded schedule: " + schedule);
            }
        }
        
        // Start all schedules
        startAllSchedules();
    }
    
    /**
     * Starts all schedules.
     */
    public void startAllSchedules() {
        // Stop any existing schedules first
        stopAllSchedules();
        
        // Group schedules by interval
        Map<Integer, List<MessageSchedule>> schedulesByInterval = new HashMap<>();
        
        // Add schedules with custom intervals
        for (MessageSchedule schedule : schedules) {
            if (schedule.hasCustomInterval()) {
                int interval = schedule.getCustomInterval();
                schedulesByInterval.computeIfAbsent(interval, k -> new ArrayList<>()).add(schedule);
            }
        }
        
        // Add schedules without custom intervals to the default interval group
        List<MessageSchedule> defaultSchedules = new ArrayList<>();
        for (MessageSchedule schedule : schedules) {
            if (!schedule.hasCustomInterval()) {
                defaultSchedules.add(schedule);
            }
        }
        
        if (!defaultSchedules.isEmpty()) {
            schedulesByInterval.put(defaultInterval, defaultSchedules);
        }
        
        // Create a task for each interval group
        for (Map.Entry<Integer, List<MessageSchedule>> entry : schedulesByInterval.entrySet()) {
            int interval = entry.getKey();
            List<MessageSchedule> intervalSchedules = entry.getValue();
            
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> processSchedules(intervalSchedules),
                interval, // Initial delay
                interval  // Repeat interval
            );
            
            scheduleTasks.put(interval, task);
            
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Started scheduler for interval " + (interval / (60 * 20)) + 
                                       " minutes with " + intervalSchedules.size() + " schedules");
            }
        }
    }
    
    /**
     * Processes a list of schedules, broadcasting messages for active schedules.
     *
     * @param schedules The list of schedules to process
     */
    private void processSchedules(List<MessageSchedule> schedules) {
        for (MessageSchedule schedule : schedules) {
            if (schedule.isActiveNow()) {
                broadcastScheduledMessage(schedule);
            } else if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Schedule not active: " + schedule);
            }
        }
    }
    
    /**
     * Broadcasts a message according to its schedule.
     *
     * @param schedule The schedule for the message
     */
    private void broadcastScheduledMessage(MessageSchedule schedule) {
        try {
            int messageIndex = schedule.getMessageIndex();
            
            // Get the message manager
            if (plugin.getMessageManager() != null) {
                // Set the current index to the scheduled message
                plugin.getMessageManager().setCurrentIndex(messageIndex);
                
                // Broadcast the message
                plugin.getMessageManager().broadcastNextMessage();
                
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Broadcast scheduled message: " + messageIndex);
                }
            } else {
                plugin.getLogger().warning("Cannot broadcast scheduled message: MessageManager is null");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error broadcasting scheduled message: " + e.getMessage());
            if (plugin.getConfigManager().isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Stops all schedules.
     */
    public void stopAllSchedules() {
        for (BukkitTask task : scheduleTasks.values()) {
            task.cancel();
        }
        scheduleTasks.clear();
    }
    
    /**
     * Gets the list of schedules.
     *
     * @return The list of schedules
     */
    public List<MessageSchedule> getSchedules() {
        return new ArrayList<>(schedules);
    }
    
    /**
     * Gets the default interval.
     *
     * @return The default interval in ticks
     */
    public int getDefaultInterval() {
        return defaultInterval;
    }
}
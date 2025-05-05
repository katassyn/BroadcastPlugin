package com.maks.broadcastplugin.scheduling;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a schedule for a message, including time-based constraints and custom intervals.
 */
public class MessageSchedule {
    private final int messageIndex;
    private final int customInterval; // in ticks, -1 means use default interval
    private final List<TimeRange> activeTimeRanges = new ArrayList<>();
    private final List<String> activeDays = new ArrayList<>();
    
    /**
     * Constructs a new MessageSchedule.
     *
     * @param messageIndex The index of the message in the message list
     * @param customInterval The custom interval for this message in minutes, -1 to use default
     */
    public MessageSchedule(int messageIndex, int customInterval) {
        this.messageIndex = messageIndex;
        this.customInterval = customInterval > 0 ? customInterval * 60 * 20 : -1; // Convert minutes to ticks
    }
    
    /**
     * Adds an active time range to the schedule.
     *
     * @param startTime The start time in HH:mm format
     * @param endTime The end time in HH:mm format
     * @return true if the time range was added successfully, false otherwise
     */
    public boolean addActiveTimeRange(String startTime, String endTime) {
        try {
            LocalTime start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime end = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"));
            activeTimeRanges.add(new TimeRange(start, end));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
    
    /**
     * Adds an active day to the schedule.
     * Valid day names are: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
     *
     * @param day The day name
     * @return true if the day was added successfully, false otherwise
     */
    public boolean addActiveDay(String day) {
        String normalizedDay = day.trim().toUpperCase();
        if (isValidDay(normalizedDay)) {
            activeDays.add(normalizedDay);
            return true;
        }
        return false;
    }
    
    /**
     * Checks if a day name is valid.
     *
     * @param day The day name to check
     * @return true if the day name is valid, false otherwise
     */
    private boolean isValidDay(String day) {
        return day.equals("MONDAY") || day.equals("TUESDAY") || day.equals("WEDNESDAY") ||
               day.equals("THURSDAY") || day.equals("FRIDAY") || day.equals("SATURDAY") || 
               day.equals("SUNDAY") || day.equals("WEEKDAY") || day.equals("WEEKEND");
    }
    
    /**
     * Checks if the schedule is active at the current time.
     *
     * @return true if the schedule is active, false otherwise
     */
    public boolean isActiveNow() {
        // If no time ranges or days are specified, the schedule is always active
        if (activeTimeRanges.isEmpty() && activeDays.isEmpty()) {
            return true;
        }
        
        // Check if current time is within any active time range
        boolean timeRangeActive = activeTimeRanges.isEmpty(); // If no time ranges, consider time constraints met
        if (!activeTimeRanges.isEmpty()) {
            LocalTime now = LocalTime.now();
            for (TimeRange range : activeTimeRanges) {
                if (range.contains(now)) {
                    timeRangeActive = true;
                    break;
                }
            }
        }
        
        // Check if current day is an active day
        boolean dayActive = activeDays.isEmpty(); // If no days specified, consider day constraints met
        if (!activeDays.isEmpty()) {
            String today = java.time.LocalDate.now().getDayOfWeek().name();
            boolean isWeekend = today.equals("SATURDAY") || today.equals("SUNDAY");
            boolean isWeekday = !isWeekend;
            
            for (String day : activeDays) {
                if (day.equals(today) || 
                    (day.equals("WEEKEND") && isWeekend) || 
                    (day.equals("WEEKDAY") && isWeekday)) {
                    dayActive = true;
                    break;
                }
            }
        }
        
        // Both time range and day must be active
        return timeRangeActive && dayActive;
    }
    
    /**
     * Gets the index of the message.
     *
     * @return The message index
     */
    public int getMessageIndex() {
        return messageIndex;
    }
    
    /**
     * Gets the custom interval for this message.
     *
     * @return The custom interval in ticks, or -1 if using default interval
     */
    public int getCustomInterval() {
        return customInterval;
    }
    
    /**
     * Checks if this message has a custom interval.
     *
     * @return true if the message has a custom interval, false otherwise
     */
    public boolean hasCustomInterval() {
        return customInterval > 0;
    }
    
    /**
     * Gets the active time ranges for this schedule.
     *
     * @return The list of active time ranges
     */
    public List<TimeRange> getActiveTimeRanges() {
        return new ArrayList<>(activeTimeRanges);
    }
    
    /**
     * Gets the active days for this schedule.
     *
     * @return The list of active days
     */
    public List<String> getActiveDays() {
        return new ArrayList<>(activeDays);
    }
    
    /**
     * Represents a time range with a start and end time.
     */
    public static class TimeRange {
        private final LocalTime start;
        private final LocalTime end;
        
        /**
         * Constructs a new TimeRange.
         *
         * @param start The start time
         * @param end The end time
         */
        public TimeRange(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }
        
        /**
         * Checks if a time is within this range.
         *
         * @param time The time to check
         * @return true if the time is within the range, false otherwise
         */
        public boolean contains(LocalTime time) {
            if (start.isBefore(end)) {
                // Normal case: start time is before end time
                return !time.isBefore(start) && !time.isAfter(end);
            } else {
                // Overnight case: end time is on the next day
                return !time.isBefore(start) || !time.isAfter(end);
            }
        }
        
        /**
         * Gets the start time.
         *
         * @return The start time
         */
        public LocalTime getStart() {
            return start;
        }
        
        /**
         * Gets the end time.
         *
         * @return The end time
         */
        public LocalTime getEnd() {
            return end;
        }
        
        @Override
        public String toString() {
            return start.format(DateTimeFormatter.ofPattern("HH:mm")) + " - " + 
                   end.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MessageSchedule{messageIndex=").append(messageIndex);
        
        if (hasCustomInterval()) {
            sb.append(", interval=").append(customInterval / (60 * 20)).append(" minutes");
        } else {
            sb.append(", using default interval");
        }
        
        if (!activeTimeRanges.isEmpty()) {
            sb.append(", active times=");
            for (int i = 0; i < activeTimeRanges.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(activeTimeRanges.get(i));
            }
        }
        
        if (!activeDays.isEmpty()) {
            sb.append(", active days=");
            for (int i = 0; i < activeDays.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(activeDays.get(i));
            }
        }
        
        sb.append('}');
        return sb.toString();
    }
}
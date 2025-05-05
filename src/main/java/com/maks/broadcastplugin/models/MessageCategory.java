package com.maks.broadcastplugin.models;

import java.util.List;

/**
 * Represents a category of broadcast messages with its own configuration settings.
 * This allows organizing messages into different categories, each with its own
 * interval, appearance settings, and message list.
 */
public class MessageCategory {
    private final String name;
    private final List<String> messages;
    private final int interval;
    private final String separatorLine;
    private final boolean enableClickableLinks;
    private final boolean randomizeMessages;
    
    /**
     * Constructs a new MessageCategory.
     *
     * @param name The name of the category
     * @param messages The list of messages in this category
     * @param interval The broadcast interval in ticks
     * @param separatorLine The separator line to use for this category
     * @param enableClickableLinks Whether to enable clickable links for this category
     * @param randomizeMessages Whether to randomize message order for this category
     */
    public MessageCategory(String name, List<String> messages, int interval, 
                          String separatorLine, boolean enableClickableLinks, 
                          boolean randomizeMessages) {
        this.name = name;
        this.messages = messages;
        this.interval = interval;
        this.separatorLine = separatorLine;
        this.enableClickableLinks = enableClickableLinks;
        this.randomizeMessages = randomizeMessages;
    }
    
    /**
     * Gets the name of the category.
     *
     * @return The category name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the list of messages in this category.
     *
     * @return The list of messages
     */
    public List<String> getMessages() {
        return messages;
    }
    
    /**
     * Gets the broadcast interval in ticks.
     *
     * @return The interval in ticks
     */
    public int getInterval() {
        return interval;
    }
    
    /**
     * Gets the broadcast interval in minutes.
     *
     * @return The interval in minutes
     */
    public int getIntervalMinutes() {
        return interval / (60 * 20);
    }
    
    /**
     * Gets the separator line for this category.
     *
     * @return The separator line
     */
    public String getSeparatorLine() {
        return separatorLine;
    }
    
    /**
     * Checks if clickable links are enabled for this category.
     *
     * @return true if clickable links are enabled, false otherwise
     */
    public boolean isEnableClickableLinks() {
        return enableClickableLinks;
    }
    
    /**
     * Checks if message order should be randomized for this category.
     *
     * @return true if message order should be randomized, false otherwise
     */
    public boolean isRandomizeMessages() {
        return randomizeMessages;
    }
    
    @Override
    public String toString() {
        return "MessageCategory{" +
                "name='" + name + '\'' +
                ", messages=" + messages.size() +
                ", interval=" + getIntervalMinutes() + " minutes" +
                ", enableClickableLinks=" + enableClickableLinks +
                ", randomizeMessages=" + randomizeMessages +
                '}';
    }
}
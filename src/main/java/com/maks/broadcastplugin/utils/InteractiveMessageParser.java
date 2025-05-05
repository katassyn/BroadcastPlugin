package com.maks.broadcastplugin.utils;

import com.maks.broadcastplugin.models.InteractiveMessage;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing interactive messages from text.
 * This class handles the extraction of hover and click events from specially formatted text.
 */
public class InteractiveMessageParser {
    private final JavaPlugin plugin;
    
    // Patterns for interactive elements
    private static final Pattern HOVER_PATTERN = Pattern.compile("\\[hover:([^\\]]+)\\]([^\\[]+)\\[/hover\\]");
    private static final Pattern COMMAND_PATTERN = Pattern.compile("\\[command:([^\\]]+)\\]([^\\[]+)\\[/command\\]");
    private static final Pattern SUGGEST_PATTERN = Pattern.compile("\\[suggest:([^\\]]+)\\]([^\\[]+)\\[/suggest\\]");
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)");
    
    /**
     * Constructs a new InteractiveMessageParser.
     *
     * @param plugin The plugin instance
     */
    public InteractiveMessageParser(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Parses a text message and extracts interactive elements.
     * 
     * This method supports the following formats:
     * - [hover:Hover text]Text to hover over[/hover]
     * - [command:/command arg1 arg2]Click to run command[/command]
     * - [suggest:/command arg1 arg2]Click to suggest command[/suggest]
     * - URLs are automatically detected and made clickable
     *
     * @param text The text to parse
     * @param enableClickableLinks Whether to make URLs clickable
     * @return A list of TextComponents representing the parsed message
     */
    public List<TextComponent> parse(String text, boolean enableClickableLinks) {
        List<TextComponent> components = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return components;
        }
        
        try {
            // Process hover events
            text = processHoverEvents(text, components);
            
            // Process command events
            text = processCommandEvents(text, components);
            
            // Process suggest events
            text = processSuggestEvents(text, components);
            
            // Process URLs if enabled
            if (enableClickableLinks) {
                text = processUrls(text, components);
            }
            
            // Add any remaining text
            if (!text.isEmpty()) {
                components.add(new TextComponent(text));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing interactive message: " + e.getMessage());
            // Return the original text as a fallback
            components.clear();
            components.add(new TextComponent(text));
        }
        
        return components;
    }
    
    /**
     * Processes hover events in the text.
     *
     * @param text The text to process
     * @param components The list to add components to
     * @return The remaining text after processing
     */
    private String processHoverEvents(String text, List<TextComponent> components) {
        Matcher matcher = HOVER_PATTERN.matcher(text);
        int lastIndex = 0;
        
        while (matcher.find()) {
            // Add text before the hover event
            String beforeText = text.substring(lastIndex, matcher.start());
            if (!beforeText.isEmpty()) {
                components.add(new TextComponent(beforeText));
            }
            
            // Extract hover text and display text
            String hoverText = matcher.group(1);
            String displayText = matcher.group(2);
            
            // Create interactive message with hover
            InteractiveMessage message = InteractiveMessage.builder(displayText)
                .withHoverText(hoverText)
                .build();
            
            // Add to components
            components.add(message.toTextComponent());
            
            // Update last index
            lastIndex = matcher.end();
        }
        
        // Return remaining text
        return lastIndex > 0 ? text.substring(lastIndex) : text;
    }
    
    /**
     * Processes command events in the text.
     *
     * @param text The text to process
     * @param components The list to add components to
     * @return The remaining text after processing
     */
    private String processCommandEvents(String text, List<TextComponent> components) {
        Matcher matcher = COMMAND_PATTERN.matcher(text);
        int lastIndex = 0;
        
        while (matcher.find()) {
            // Add text before the command event
            String beforeText = text.substring(lastIndex, matcher.start());
            if (!beforeText.isEmpty()) {
                components.add(new TextComponent(beforeText));
            }
            
            // Extract command and display text
            String command = matcher.group(1);
            String displayText = matcher.group(2);
            
            // Create interactive message with command
            InteractiveMessage message = InteractiveMessage.builder(displayText)
                .withClickAction(InteractiveMessage.ClickAction.RUN_COMMAND, command)
                .build();
            
            // Add to components
            components.add(message.toTextComponent());
            
            // Update last index
            lastIndex = matcher.end();
        }
        
        // Return remaining text
        return lastIndex > 0 ? text.substring(lastIndex) : text;
    }
    
    /**
     * Processes suggest events in the text.
     *
     * @param text The text to process
     * @param components The list to add components to
     * @return The remaining text after processing
     */
    private String processSuggestEvents(String text, List<TextComponent> components) {
        Matcher matcher = SUGGEST_PATTERN.matcher(text);
        int lastIndex = 0;
        
        while (matcher.find()) {
            // Add text before the suggest event
            String beforeText = text.substring(lastIndex, matcher.start());
            if (!beforeText.isEmpty()) {
                components.add(new TextComponent(beforeText));
            }
            
            // Extract command and display text
            String command = matcher.group(1);
            String displayText = matcher.group(2);
            
            // Create interactive message with suggest command
            InteractiveMessage message = InteractiveMessage.builder(displayText)
                .withClickAction(InteractiveMessage.ClickAction.SUGGEST_COMMAND, command)
                .build();
            
            // Add to components
            components.add(message.toTextComponent());
            
            // Update last index
            lastIndex = matcher.end();
        }
        
        // Return remaining text
        return lastIndex > 0 ? text.substring(lastIndex) : text;
    }
    
    /**
     * Processes URLs in the text.
     *
     * @param text The text to process
     * @param components The list to add components to
     * @return The remaining text after processing
     */
    private String processUrls(String text, List<TextComponent> components) {
        Matcher matcher = URL_PATTERN.matcher(text);
        int lastIndex = 0;
        
        while (matcher.find()) {
            // Add text before the URL
            String beforeText = text.substring(lastIndex, matcher.start());
            if (!beforeText.isEmpty()) {
                components.add(new TextComponent(beforeText));
            }
            
            // Extract URL
            String url = matcher.group(1);
            
            // Create interactive message with URL
            InteractiveMessage message = InteractiveMessage.builder(url)
                .withClickAction(InteractiveMessage.ClickAction.OPEN_URL, url)
                .withColor(ChatColor.AQUA)
                .withUnderlined(true)
                .build();
            
            // Add to components
            components.add(message.toTextComponent());
            
            // Update last index
            lastIndex = matcher.end();
        }
        
        // Return remaining text
        return lastIndex > 0 ? text.substring(lastIndex) : text;
    }
}
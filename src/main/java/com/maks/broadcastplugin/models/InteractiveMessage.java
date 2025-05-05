package com.maks.broadcastplugin.models;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Represents an interactive message with hover and click events.
 * This class provides a builder pattern for creating interactive messages.
 */
public class InteractiveMessage {
    private String text;
    private String hoverText;
    private ClickAction clickAction;
    private String clickValue;
    private ChatColor color;
    private boolean bold = false;
    private boolean italic = false;
    private boolean underlined = false;
    private boolean strikethrough = false;
    private boolean obfuscated = false;

    /**
     * Enum representing the different types of click actions.
     */
    public enum ClickAction {
        OPEN_URL,
        RUN_COMMAND,
        SUGGEST_COMMAND,
        COPY_TO_CLIPBOARD,
        NONE
    }

    /**
     * Private constructor to enforce the use of the builder.
     */
    private InteractiveMessage() {
    }

    /**
     * Creates a new builder for an interactive message.
     *
     * @param text The text of the message
     * @return A new builder
     */
    public static Builder builder(String text) {
        return new Builder(text);
    }

    /**
     * Converts this interactive message to a TextComponent.
     *
     * @return The TextComponent representation of this message
     */
    public TextComponent toTextComponent() {
        TextComponent component = new TextComponent(text);

        // Apply styling
        if (color != null) {
            component.setColor(color);
        }
        component.setBold(bold);
        component.setItalic(italic);
        component.setUnderlined(underlined);
        component.setStrikethrough(strikethrough);
        component.setObfuscated(obfuscated);

        // Apply hover event
        if (hoverText != null && !hoverText.isEmpty()) {
            component.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(hoverText).create()
            ));
        }

        // Apply click event
        if (clickAction != ClickAction.NONE && clickValue != null && !clickValue.isEmpty()) {
            ClickEvent.Action action;
            switch (clickAction) {
                case OPEN_URL:
                    action = ClickEvent.Action.OPEN_URL;
                    break;
                case RUN_COMMAND:
                    action = ClickEvent.Action.RUN_COMMAND;
                    break;
                case SUGGEST_COMMAND:
                    action = ClickEvent.Action.SUGGEST_COMMAND;
                    break;
                case COPY_TO_CLIPBOARD:
                    action = ClickEvent.Action.COPY_TO_CLIPBOARD;
                    break;
                default:
                    return component; // No click action
            }
            component.setClickEvent(new ClickEvent(action, clickValue));
        }

        return component;
    }

    /**
     * Builder class for creating InteractiveMessage instances.
     */
    public static class Builder {
        private final InteractiveMessage message;

        /**
         * Constructs a new Builder with the given text.
         *
         * @param text The text of the message
         */
        public Builder(String text) {
            message = new InteractiveMessage();
            message.text = text;
            message.clickAction = ClickAction.NONE;
        }

        /**
         * Sets the hover text for the message.
         *
         * @param hoverText The text to show when hovering over the message
         * @return This builder
         */
        public Builder withHoverText(String hoverText) {
            message.hoverText = hoverText;
            return this;
        }

        /**
         * Sets the click action and value for the message.
         *
         * @param action The click action
         * @param value The value for the click action
         * @return This builder
         */
        public Builder withClickAction(ClickAction action, String value) {
            message.clickAction = action;
            message.clickValue = value;
            return this;
        }

        /**
         * Sets the color of the message.
         *
         * @param color The color
         * @return This builder
         */
        public Builder withColor(ChatColor color) {
            message.color = color;
            return this;
        }

        /**
         * Sets whether the message should be bold.
         *
         * @param bold Whether the message should be bold
         * @return This builder
         */
        public Builder withBold(boolean bold) {
            message.bold = bold;
            return this;
        }

        /**
         * Sets whether the message should be italic.
         *
         * @param italic Whether the message should be italic
         * @return This builder
         */
        public Builder withItalic(boolean italic) {
            message.italic = italic;
            return this;
        }

        /**
         * Sets whether the message should be underlined.
         *
         * @param underlined Whether the message should be underlined
         * @return This builder
         */
        public Builder withUnderlined(boolean underlined) {
            message.underlined = underlined;
            return this;
        }

        /**
         * Sets whether the message should have strikethrough.
         *
         * @param strikethrough Whether the message should have strikethrough
         * @return This builder
         */
        public Builder withStrikethrough(boolean strikethrough) {
            message.strikethrough = strikethrough;
            return this;
        }

        /**
         * Sets whether the message should be obfuscated.
         *
         * @param obfuscated Whether the message should be obfuscated
         * @return This builder
         */
        public Builder withObfuscated(boolean obfuscated) {
            message.obfuscated = obfuscated;
            return this;
        }

        /**
         * Builds the interactive message.
         *
         * @return The built message
         */
        public InteractiveMessage build() {
            return message;
        }
    }
}
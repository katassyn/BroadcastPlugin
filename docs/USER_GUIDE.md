# BroadcastPlugin User Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Installation](#installation)
3. [Configuration](#configuration)
4. [Commands](#commands)
5. [Permissions](#permissions)
6. [Common Use Cases](#common-use-cases)
7. [Troubleshooting](#troubleshooting)

## Introduction

BroadcastPlugin is a simple yet powerful plugin for Minecraft servers that automatically broadcasts configurable messages to all players at regular intervals. Messages can include clickable links, formatted text, and multi-line content.

Key features:
- Scheduled message broadcasting at configurable intervals
- Support for clickable links in messages
- Color code formatting
- Multi-line messages
- Simple configuration
- Debug mode for troubleshooting

## Installation

1. Download the latest version of BroadcastPlugin from the releases page.
2. Place the JAR file in your server's `plugins` directory.
3. Restart your server or use a plugin manager to load the plugin.
4. The plugin will generate a default configuration file at `plugins/BroadcastPlugin/config.yml`.
5. Edit the configuration file to customize messages and settings (see [Configuration](#configuration)).
6. Use the `/broadcast reload` command to apply your changes.

## Configuration

The plugin's configuration file is located at `plugins/BroadcastPlugin/config.yml`. Below is an explanation of all available options:

```yaml
# BroadcastPlugin Configuration

# Debug mode - set to true to enable additional logging
debug: false

# Broadcast settings
broadcast:
  # Interval between messages in minutes
  interval: 5

  # Messages to broadcast
  # You can use color codes with § symbol
  # Use \n for new lines
  messages:
    - "§bJoin our Discord community!\n§fStay updated with server development plans and announcements.\n§eLink: §ahttps://discord.gg/example"
    - "§6Use §e/help §6to see available commands!\n§fThis will show you all commands you can use on the server."
```

### Configuration Options

#### Debug Mode
```yaml
debug: false
```
Set to `true` to enable debug logging. This is useful for troubleshooting issues with the plugin.

#### Broadcast Interval
```yaml
broadcast:
  interval: 5
```
The time in minutes between each broadcast message. Minimum value is 1.

#### Messages
```yaml
broadcast:
  messages:
    - "Your first message here"
    - "Your second message here"
```
A list of messages to broadcast. Messages are shown in sequence, cycling back to the first message after the last one is displayed.

### Formatting Messages

#### Color Codes
You can use Minecraft's color codes in your messages by prefixing them with the § symbol:

- `§0`: Black
- `§1`: Dark Blue
- `§2`: Dark Green
- `§3`: Dark Aqua
- `§4`: Dark Red
- `§5`: Dark Purple
- `§6`: Gold
- `§7`: Gray
- `§8`: Dark Gray
- `§9`: Blue
- `§a`: Green
- `§b`: Aqua
- `§c`: Red
- `§d`: Light Purple
- `§e`: Yellow
- `§f`: White

Formatting codes:
- `§k`: Obfuscated
- `§l`: Bold
- `§m`: Strikethrough
- `§n`: Underline
- `§o`: Italic
- `§r`: Reset

#### Multi-line Messages
Use `\n` to create line breaks in your messages:

```yaml
messages:
  - "§bFirst line\n§eSecond line\n§cThird line"
```

#### Links
URLs starting with `http://` or `https://` are automatically detected and converted to clickable links.

## Commands

BroadcastPlugin provides the following commands:

### `/broadcast reload`
Reloads the configuration file.
- Permission: `broadcast.reload`

### `/broadcast debug`
Shows debug information about the plugin.
- Permission: `broadcast.debug`

### `/broadcast help`
Shows help information about the plugin commands.
- Permission: `broadcast.use`

## Permissions

- `broadcast.use`: Allows using the basic broadcast command (default: true)
- `broadcast.reload`: Allows reloading the configuration (default: op)
- `broadcast.debug`: Allows viewing debug information (default: op)

## Common Use Cases

### Server Rules Reminder
```yaml
messages:
  - "§c§lSERVER RULES:\n§f1. No griefing or stealing\n§f2. Be respectful to other players\n§f3. No spamming or advertising\n§f4. Have fun!"
```

### Website and Social Media Links
```yaml
messages:
  - "§bVisit our website for the latest news and updates!\n§eWebsite: §ahttps://example.com"
  - "§dJoin our Discord community!\n§eDiscord: §ahttps://discord.gg/example"
  - "§cFollow us on Twitter for announcements!\n§eTwitter: §ahttps://twitter.com/example"
```

### Server Features Showcase
```yaml
messages:
  - "§6§lNEW FEATURE: §ePet System\n§fCollect and level up pets that give you special abilities!\n§7Use /pet help to get started."
  - "§6§lWEEKLY EVENTS: §eEvery Saturday at 8 PM EST\n§fJoin our weekly events for special rewards and fun challenges!"
```

### Vote Reminder
```yaml
messages:
  - "§a§lSUPPORT THE SERVER!\n§fVote for us on these sites to earn rewards:\n§e- §ahttps://minecraftservers.org/example\n§e- §ahttps://planetminecraft.com/example"
```

## Troubleshooting

### Messages Not Appearing
1. Check that the plugin is enabled (`/plugins` command)
2. Verify that you have messages configured in the config.yml file
3. Make sure the interval is set to a reasonable value
4. Try reloading the plugin with `/broadcast reload`
5. Enable debug mode in the config and check the console for errors

### Configuration Not Saving
1. Make sure you have write permissions to the config.yml file
2. Check that the file is not open in another program
3. Verify that your YAML syntax is correct (no tab characters, proper indentation)

### Permission Issues
1. Check that permissions are set up correctly in your permissions plugin
2. Verify that the user has the correct permission node
3. Try giving the user OP temporarily to test if it's a permissions issue

### Links Not Clickable
1. Make sure the URL starts with http:// or https://
2. Check that the URL is properly formatted
3. Verify that the player's client supports clickable links

If you continue to experience issues, enable debug mode in the configuration and check the server logs for more detailed error messages.
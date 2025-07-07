# BroadcastPlugin

BroadcastPlugin is a powerful and flexible Minecraft server plugin that automatically broadcasts configurable messages to players at regular intervals. It supports rich formatting, interactive elements, scheduling, and conditional messaging.

## Features

- **Scheduled Broadcasting**: Automatically broadcast messages at configurable intervals
- **Category-Based Messages**: Organize messages into different categories with separate configurations
- **Rich Formatting**: Support for color codes, multi-line messages, and clickable links
- **Interactive Elements**:
  - Hover text: `[hover:Hover text]Text to hover over[/hover]`
  - Command execution: `[command:/command arg1 arg2]Click to run command[/command]`
  - Command suggestions: `[suggest:/command arg1 arg2]Click to suggest command[/suggest]`
- **Placeholders**: Support for built-in placeholders and integration with PlaceholderAPI
- **Message Conditions**: Show messages only when specific conditions are met
- **Time-Based Scheduling**: Schedule messages to appear only during certain hours or days
- **Hot-Reloading**: Configuration files are automatically reloaded when changed
- **Debug Mode**: Detailed logging for troubleshooting

## Installation

1. Download the latest version of BroadcastPlugin
2. Place the JAR file in your server's `plugins` directory
3. Restart your server or use a plugin manager to load the plugin
4. The plugin will generate default configuration files in the `plugins/BroadcastPlugin` directory
5. Edit the configuration files to customize messages and settings

## Configuration

The main configuration file is located at `plugins/BroadcastPlugin/config.yml`. Category-based messages are stored in the `plugins/BroadcastPlugin/categories` directory.

### Main Configuration

```yaml
# Debug mode - set to true to enable additional logging
debug: false

# Categories settings
categories:
  # Enable message categories
  enabled: true

  # Directory where category configuration files are stored
  directory: "categories"

  # Default settings for categories
  defaults:
    # Interval between messages in minutes
    interval: 5

    # Appearance settings
    appearance:
      # Separator line displayed before and after messages
      separator: "------------------------------"
      # Enable clickable links in messages
      enable_clickable_links: true

    # Message order settings
    order:
      # Randomize message order instead of sequential
      randomize_messages: false
```

### Category Configuration

Each category file in the `categories` directory defines a separate set of messages with its own settings:

```yaml
# Interval between messages in minutes
interval: 10

# Appearance settings
appearance:
  # Separator line displayed before and after messages
  separator: "=== GAMEPLAY TIP ==="
  # Enable clickable links in messages
  enable_clickable_links: true

# Message order settings
order:
  # Randomize message order instead of sequential
  randomize_messages: true

# Messages to broadcast
messages:
  - "§6Use §e/help §6to see available commands!\n§fThis will show you all commands you can use on the server."
```

## Commands

- `/broadcast reload` - Reloads the configuration files
- `/broadcast debug` - Shows debug information about the plugin
- `/broadcast help` - Shows help information about the plugin commands

## Permissions

- `broadcast.use` - Allows using the basic broadcast command (default: true)
- `broadcast.reload` - Allows reloading the configuration (default: op)
- `broadcast.debug` - Allows viewing debug information (default: op)

## Documentation

For more detailed information, please refer to:

- [User Guide](docs/USER_GUIDE.md) - Comprehensive guide for server administrators
- [Developer Guide](docs/DEVELOPER_GUIDE.md) - Information for developers who want to extend the plugin
- [Changelog](docs/CHANGELOG.md) - History of changes and updates

## Support

If you encounter any issues or have questions, please:

1. Check the [User Guide](docs/USER_GUIDE.md) for help with common problems
2. Submit an issue on the GitHub repository if available

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

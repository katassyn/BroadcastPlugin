# BroadcastPlugin

A lightweight Minecraft plugin for broadcasting rotating messages to the server chat. Designed to enhance player experience with announcements, tips, and reminders. Messages are fully customizable and rotate automatically at specified intervals.

## Features

- **Customizable Messages**: Define your own messages in `config.yml`.
- **Rotating Broadcasts**: Automatically cycle through a list of messages.
- **Formatting Support**: Use Minecraft color codes and formatting.
- **Config Reload**: Use `/broadcast reload` to reload the configuration without restarting the server.
- **Permission-based Access**: Restrict reload commands to specific users.

## Installation

1. Download the latest release from the [Releases](https://github.com/katassyn/BroadcastPlugin/releases) section.
2. Place the `BroadcastPlugin.jar` file into your server's `plugins` folder.
3. Start the server to generate the default configuration.
4. Edit the `config.yml` file in the `plugins/BroadcastPlugin` directory to customize messages and settings.
5. Use `/broadcast reload` to apply your changes without restarting the server.

## Configuration

The plugin generates a `config.yml` file with the following structure:

```yaml
messages:
  - "§6Welcome to the server! Use §e/help §6for commands."
  - "§6Join our Discord for updates: §ehttps://discord.gg/your-link"
  - "§6Vote for rewards using §e/vote"
  - "§6Check out the top players with §e/top"
interval: 5 # Time in minutes between messages

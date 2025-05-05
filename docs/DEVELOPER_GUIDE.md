# BroadcastPlugin Developer Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Plugin Architecture](#plugin-architecture)
3. [Extending the Plugin](#extending-the-plugin)
4. [Build and Deployment](#build-and-deployment)
5. [Code Style Guidelines](#code-style-guidelines)
6. [Contributing](#contributing)

## Introduction

This developer guide is intended for developers who want to understand, modify, or extend the BroadcastPlugin. It provides an overview of the plugin's architecture, instructions for extending its functionality, and details about the build and deployment process.

## Plugin Architecture

BroadcastPlugin follows a modular architecture with clear separation of concerns. The plugin is organized into the following main components:

### Main Plugin Class
`BroadcastPlugin` (in `com.maks.broadcastplugin`) is the entry point of the plugin. It:
- Initializes the managers
- Handles plugin lifecycle events (enable/disable)
- Coordinates the broadcasting scheduler

### Manager Classes
Located in the `com.maks.broadcastplugin.managers` package:

1. **ConfigManager**
   - Handles loading and validating configuration
   - Provides access to configuration values
   - Manages debug mode settings

2. **MessageManager**
   - Formats and processes messages
   - Handles URL detection and clickable links
   - Manages the message cache
   - Broadcasts messages to players
   - Tracks performance metrics

3. **CommandManager**
   - Registers and handles commands
   - Processes command arguments
   - Manages command permissions

### Class Relationships
- `BroadcastPlugin` creates and owns instances of all managers
- `CommandManager` depends on both `ConfigManager` and `MessageManager`
- `MessageManager` uses configuration values from `ConfigManager`

### Data Flow
1. Configuration is loaded by `ConfigManager` during plugin initialization
2. Messages are processed and cached by `MessageManager`
3. `BroadcastPlugin` sets up a scheduler to trigger message broadcasts
4. When triggered, `MessageManager` broadcasts the next message
5. Commands from players are handled by `CommandManager`

## Extending the Plugin

### Adding New Commands

To add a new command:

1. Modify the `CommandManager` class:
   ```java
   // In the onCommand method
   if (args[0].equalsIgnoreCase("yourNewCommand")) {
       return handleYourNewCommand(sender, args);
   }
   
   // Add a new method to handle your command
   private boolean handleYourNewCommand(CommandSender sender, String[] args) {
       // Your command logic here
       return true;
   }
   ```

2. Register the command in `plugin.yml`:
   ```yaml
   commands:
     broadcast:
       description: Main command for the broadcast plugin
       usage: |
         /broadcast reload - Reloads the configuration
         /broadcast yourNewCommand - Description of your new command
   ```

3. Add a permission if needed:
   ```yaml
   permissions:
     broadcast.yourNewCommand:
       description: Allows using your new command
       default: op
   ```

### Adding Configuration Options

To add new configuration options:

1. Add the option to the default `config.yml`:
   ```yaml
   broadcast:
     yourNewOption: defaultValue
   ```

2. Add a getter method in `ConfigManager`:
   ```java
   public YourType getYourNewOption() {
       return config.getYourType("broadcast.yourNewOption", defaultValue);
   }
   ```

3. Use the new option in your code:
   ```java
   if (configManager.getYourNewOption()) {
       // Do something
   }
   ```

### Adding Message Features

To add new message formatting features:

1. Modify the `processLine` method in `MessageManager`:
   ```java
   private TextComponent processLine(String line) {
       // Existing code...
       
       // Your new formatting logic
       if (line.contains("yourSpecialFormat")) {
           // Process special format
       }
       
       // Rest of existing code...
   }
   ```

2. Update the cache if needed:
   ```java
   public void processAndCacheMessages() {
       // Existing code...
       
       // Your new caching logic
       
       // Rest of existing code...
   }
   ```

## Build and Deployment

### Prerequisites
- Java Development Kit (JDK) 8 or higher
- Maven

### Building the Plugin

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/BroadcastPlugin.git
   cd BroadcastPlugin
   ```

2. Build with Maven:
   ```bash
   mvn clean package
   ```

3. The compiled JAR file will be in the `target` directory:
   ```
   target/BroadcastPlugin-1.0-SNAPSHOT.jar
   ```

### Development Environment Setup

1. Import the project into your IDE:
   - For IntelliJ IDEA: File > Open > Select the pom.xml file > Open as Project
   - For Eclipse: File > Import > Maven > Existing Maven Projects

2. Configure the project SDK to use JDK 8 or higher

3. Set up a local test server:
   - Download a Spigot/Paper server jar
   - Create a test server directory
   - Add the server jar and start scripts
   - Configure the server to use offline mode for testing

4. Create a run configuration that:
   - Builds the plugin
   - Copies it to the test server's plugins directory
   - Starts the server

### Deployment

To deploy the plugin to a production server:

1. Build the plugin as described above
2. Copy the JAR file to the server's `plugins` directory
3. Restart the server or use a plugin manager to load the plugin
4. Verify that the plugin is working correctly

### Version Management

The plugin version is defined in the `pom.xml` file:

```xml
<version>1.0-SNAPSHOT</version>
```

When releasing a new version:

1. Update the version in `pom.xml`
2. Update the changelog in `docs/CHANGELOG.md`
3. Build the plugin
4. Create a release tag in Git
5. Upload the JAR file to your distribution platform

## Code Style Guidelines

To maintain code quality and consistency:

1. Follow Java naming conventions:
   - CamelCase for class names (e.g., `MessageManager`)
   - camelCase for method and variable names (e.g., `broadcastNextMessage`)
   - ALL_CAPS for constants (e.g., `DEFAULT_INTERVAL`)

2. Add JavaDoc comments to all public classes and methods

3. Use meaningful variable and method names

4. Keep methods focused on a single responsibility

5. Add inline comments for complex logic

6. Use proper exception handling with specific catch blocks

7. Follow the existing code structure and patterns

## Contributing

Contributions to the plugin are welcome! To contribute:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Make your changes
4. Add tests if applicable
5. Update documentation
6. Commit your changes (`git commit -m 'Add your feature'`)
7. Push to the branch (`git push origin feature/your-feature`)
8. Create a Pull Request

Please ensure your code follows the style guidelines and includes appropriate documentation.
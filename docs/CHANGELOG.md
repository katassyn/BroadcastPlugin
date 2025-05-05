# BroadcastPlugin Changelog

## Version 1.1.0 (Current Development)

### Code Structure and Organization
- Refactored the main plugin class to separate concerns:
  - Created a dedicated MessageManager class to handle message formatting and broadcasting
  - Created a ConfigManager class to handle configuration loading and validation
  - Created a CommandManager class to handle command registration and execution
- Implemented a proper package structure:
  - Created separate packages for managers
  - Moved classes to their appropriate packages
- Converted Polish comments to English for better international collaboration
- Removed placeholder comments

### Error Handling and Robustness
- Added null checks and validation for configuration values:
  - Validated interval is greater than zero
  - Added checks for null or empty messages
  - Implemented handling for potential exceptions when parsing configuration
- Implemented proper error handling for the scheduler:
  - Added try-catch blocks around scheduler code
  - Added proper exception logging
  - Ensured the plugin doesn't crash on scheduler errors
- Added graceful handling for missing permissions:
  - Improved permission error messages
  - Added logging for permission issues
- Implemented a debug mode for troubleshooting:
  - Added debug logging throughout the code
  - Created a configuration option to enable/disable debug mode

### Performance Optimizations
- Optimized the URL detection regex pattern:
  - Compiled the pattern once at initialization instead of for each message
- Optimized scheduler usage:
  - Used BukkitTask for better task management
  - Implemented proper task cancellation on plugin disable
- Implemented message caching:
  - Pre-processed and cached formatted messages
  - Only reprocess messages when configuration is reloaded
  - Added cache invalidation when messages are changed
- Added performance metrics:
  - Track time spent processing messages
  - Log performance statistics in debug mode

### Documentation and Comments
- Added comprehensive JavaDoc comments:
  - Documented all classes, methods, and fields
  - Included parameter and return value descriptions
  - Documented exceptions that may be thrown
- Added detailed inline comments explaining complex logic:
  - Documented the URL detection and formatting logic
  - Explained the scheduler implementation with step-by-step comments
- Created a comprehensive user guide:
  - Documented installation instructions
  - Explained configuration options with examples
  - Provided examples of common use cases
  - Added troubleshooting section
- Created a developer guide:
  - Documented the plugin architecture
  - Provided instructions for extending the plugin
  - Included build and deployment instructions
  - Added code style guidelines

### Configuration Improvements
- Restructured the configuration file for better organization
- Added debug mode option
- Implemented backward compatibility with old configuration format
- Added customization options:
  - Added option to customize the separator line
  - Added option to enable/disable clickable links
  - Added option to randomize message order
- Implemented comprehensive configuration validation:
  - Added validation for all configuration values
  - Added detailed error messages for invalid configurations
  - Added warnings for potentially problematic values
  - Added validation summary in logs

### Multiple Configuration Files
- Added support for category-based broadcasting:
  - Implemented a system for organizing messages into categories
  - Each category can have its own configuration settings
  - Categories are loaded from separate YAML files
  - Added support for different intervals for different message categories
  - Created a category manager factory to handle multiple categories
  - Added default category files (general.yml and gameplay.yml)
- Updated commands to support category-based broadcasting:
  - Enhanced reload command to handle categories
  - Updated debug command to show category information
  - Added category-specific debug output

### Hot-Reloading Configuration
- Implemented automatic configuration reloading:
  - Added a ConfigWatcher class that monitors configuration files for changes
  - Automatically reloads configuration when files are modified
  - No need to use the reload command manually
  - Added cooldown to prevent multiple reloads in quick succession
  - Works with both main config and category files
  - Provides detailed logging of file changes and reload events

## Remaining Tasks
The following tasks are planned for future updates:

### Performance Optimizations
<!-- All performance optimization tasks have been completed -->

### Documentation and Comments
<!-- All documentation tasks have been completed -->

### Configuration Options
<!-- All configuration tasks have been completed -->

### Placeholder Support
- Added comprehensive placeholder system:
  - Implemented a PlaceholderManager to handle placeholder replacement
  - Added support for built-in placeholders like {online_players}, {server_tps}, {time}, etc.
  - Added player-specific placeholders like {player}, {player_health}, {player_world}, etc.
  - Integrated with PlaceholderAPI for extended placeholder support
  - Implemented automatic detection of PlaceholderAPI
  - Added placeholder processing to message formatting

### Message Targeting
- Implemented targeted message broadcasting:
  - Added support for sending messages to specific players or groups
  - Implemented permission-based message filtering
  - Enhanced MessageManager to handle targeted broadcasts
  - Added player-specific placeholder replacement for targeted messages
  - Maintained backward compatibility with global broadcasts

### Message Scheduling
- Implemented advanced message scheduling options:
  - Added support for time-based scheduling (messages can be shown only during certain hours)
  - Implemented day-based scheduling (messages can be shown only on specific days)
  - Added support for different intervals for different messages
  - Created a MessageSchedule class to represent schedule constraints
  - Implemented a MessageScheduler to manage scheduled messages
  - Added support for overnight time ranges (e.g., 22:00 - 06:00)
  - Added special day groups (WEEKDAY, WEEKEND) for easier scheduling

### Interactive Messages
- Implemented comprehensive interactive message system:
  - Added support for hover events with custom hover text
  - Implemented clickable text that can run commands
  - Added support for command suggestions on click
  - Created a flexible syntax for defining interactive elements in messages:
    - `[hover:Hover text]Text to hover over[/hover]` for hover events
    - `[command:/command arg1 arg2]Click to run command[/command]` for running commands
    - `[suggest:/command arg1 arg2]Click to suggest command[/suggest]` for suggesting commands
  - Developed an InteractiveMessage model with builder pattern for programmatic creation
  - Created an InteractiveMessageParser to extract interactive elements from text
  - Integrated with existing URL detection for backward compatibility
  - Updated both MessageManager and CategoryMessageManager to support interactive messages

### Message Conditions
- Implemented comprehensive message condition system:
  - Added support for player count conditions (min/max players)
  - Implemented permission-based conditions
  - Added world-specific conditions
  - Created weather and time-of-day conditions
  - Developed a flexible MessageCondition model with builder pattern
  - Created a MessageConditionEvaluator to check if conditions are met
  - Added configuration options for defining conditions
  - Updated both MessageManager and CategoryMessageManager to check conditions before broadcasting
  - Implemented condition filtering for both sequential and random message selection

### Feature Enhancements

### Testing and Quality Assurance
- Implement unit tests
- Set up continuous integration
- Implement integration tests
- Add code quality checks

### Deployment and Distribution
- Prepared for distribution:
  - Created a comprehensive README.md file with features, installation instructions, and configuration examples
  - Added MIT License information
  - Created a release notes template for future releases
- Set up automated releases
- Implement update checking

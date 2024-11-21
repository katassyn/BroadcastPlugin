package com.maks.broadcastplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class BroadcastPlugin extends JavaPlugin {

    private List<String> messages; // Lista wiadomości
    private int interval; // Interwał w tickach
    private int currentIndex = 0; // Obecny indeks wiadomości

    @Override
    public void onEnable() {
        saveDefaultConfig(); // Tworzy config.yml, jeśli nie istnieje
        loadConfigValues(); // Wczytanie wartości z konfiguracji
        startBroadcastMessages(); // Uruchomienie harmonogramu
        getLogger().info("BroadcastPlugin is enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BroadcastPlugin is disabled.");
    }

    private void loadConfigValues() {
        reloadConfig(); // Ponowne wczytanie konfiguracji
        messages = getConfig().getStringList("messages");
        interval = getConfig().getInt("interval") * 60 * 20; // Minuty -> Ticki
    }

    private void startBroadcastMessages() {
        if (messages.isEmpty()) {
            getLogger().warning("No messages found in config.yml!");
            return;
        }

        // Anuluj istniejące zadania, aby uniknąć ich duplikacji
        Bukkit.getScheduler().cancelTasks(this);

        // Harmonogram wiadomości
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            String message = messages.get(currentIndex);

            // Wyświetl linię dekoracyjną, wiadomość i kolejną linię
            Bukkit.broadcastMessage("------------------------------");
            for (String line : message.split("\n")) {
                Bukkit.broadcastMessage(line.trim());
            }
            Bukkit.broadcastMessage("------------------------------");

            // Zwiększ indeks lub zresetuj, jeśli lista się skończy
            currentIndex = (currentIndex + 1) % messages.size();
        }, 0L, interval);
    }

    // Obsługa komendy /broadcast reload
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("broadcast") && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            // Sprawdź, czy gracz ma odpowiednie uprawnienia
            if (!sender.hasPermission("broadcast.reload")) {
                sender.sendMessage("§cYou don't have permission to execute this command!");
                return true;
            }

            // Wczytaj nową konfigurację i uruchom ponownie harmonogram
            loadConfigValues();
            startBroadcastMessages();
            sender.sendMessage("§6[Broadcast] §7Config reloaded!");
            return true;
        }
        return false;
    }

}

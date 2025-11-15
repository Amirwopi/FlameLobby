package ir.mrwopi.flameLobby.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MusicDataManager {

    private final Plugin plugin;
    private final Path dataFile;
    private final Path backupFile;
    private final Gson gson;
    private final Map<UUID, PlayerMusicData> playerData;
    private final ScheduledExecutorService autoSaveExecutor;
    private volatile boolean modified;

    public MusicDataManager(Plugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.playerData = new ConcurrentHashMap<>();
        this.modified = false;

        var coreFolder = Path.of("FlameCore");
        var dataFolder = coreFolder.resolve("data");

        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create data folder: " + e.getMessage());
        }

        this.dataFile = dataFolder.resolve("music_data.json");
        this.backupFile = dataFolder.resolve("music_data.json.backup");

        loadData();

        this.autoSaveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            var thread = new Thread(r, "MusicDataManager-AutoSave");
            thread.setDaemon(true);
            return thread;
        });

        autoSaveExecutor.scheduleAtFixedRate(() -> {
            if (modified) {
                saveData();
                modified = false;
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    private void loadData() {
        if (!Files.exists(dataFile)) {
            plugin.getLogger().info("No existing music data found, starting fresh");
            return;
        }

        try {
            var json = Files.readString(dataFile);
            var type = new TypeToken<HashMap<String, PlayerMusicData>>(){}.getType();
            Map<String, PlayerMusicData> loaded = gson.fromJson(json, type);

            if (loaded != null) {
                loaded.forEach((uuidStr, data) -> {
                    try {
                        var uuid = UUID.fromString(uuidStr);
                        playerData.put(uuid, data);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in data file: " + uuidStr);
                    }
                });
            }

            plugin.getLogger().info("Loaded music data for " + playerData.size() + " players");

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to read music data: " + e.getMessage());
            tryLoadBackup();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to parse music data: " + e.getMessage());
            tryLoadBackup();
        }
    }

    private void tryLoadBackup() {
        if (!Files.exists(backupFile)) {
            return;
        }

        try {
            plugin.getLogger().info("Attempting to restore from backup...");
            Files.copy(backupFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            loadData();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to restore backup: " + e.getMessage());
        }
    }

    public synchronized void saveData() {
        try {
            if (Files.exists(dataFile)) {
                Files.copy(dataFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
            }

            var toSave = new HashMap<String, PlayerMusicData>();
            playerData.forEach((uuid, data) -> toSave.put(uuid.toString(), data));

            var json = gson.toJson(toSave);
            Files.writeString(dataFile, json);

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save music data: " + e.getMessage());
        }
    }

    public boolean isMusicDisabled(UUID uuid) {
        var data = playerData.get(uuid);
        return data != null && data.disabled;
    }

    public void setMusicDisabled(UUID uuid, boolean disabled) {
        var data = playerData.computeIfAbsent(uuid, k -> new PlayerMusicData());
        data.disabled = disabled;
        modified = true;
        saveData();
    }

    public void cleanup() {
        if (modified) {
            saveData();
        }

        autoSaveExecutor.shutdown();
        try {
            if (!autoSaveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                autoSaveExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            autoSaveExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        playerData.clear();
    }

    public static class PlayerMusicData {
        public boolean disabled = false;
    }
}
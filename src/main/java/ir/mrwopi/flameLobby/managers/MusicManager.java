package ir.mrwopi.flameLobby.managers;

import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.event.SongEndEvent;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class MusicManager implements Listener {

    private static final Material[] DISC_MATERIALS = {
            Material.MUSIC_DISC_13, Material.MUSIC_DISC_CAT, Material.MUSIC_DISC_BLOCKS,
            Material.MUSIC_DISC_CHIRP, Material.MUSIC_DISC_FAR, Material.MUSIC_DISC_MALL,
            Material.MUSIC_DISC_MELLOHI, Material.MUSIC_DISC_STAL, Material.MUSIC_DISC_STRAD,
            Material.MUSIC_DISC_WARD, Material.MUSIC_DISC_11, Material.MUSIC_DISC_WAIT
    };

    private final Plugin plugin;
    private final MusicDataManager dataManager;
    private final Map<UUID, RadioSongPlayer> activePlayers;
    private final Map<UUID, String> currentTracks;
    private final Map<UUID, Float> volumes;
    private final Map<UUID, Boolean> pausedPlayers;
    private final List<MusicTrack> tracks;
    private final AtomicInteger discIndex;

    public MusicManager(Plugin plugin, MusicDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.activePlayers = new ConcurrentHashMap<>();
        this.currentTracks = new ConcurrentHashMap<>();
        this.volumes = new ConcurrentHashMap<>();
        this.pausedPlayers = new ConcurrentHashMap<>();
        this.tracks = new CopyOnWriteArrayList<>();
        this.discIndex = new AtomicInteger(0);

        loadNBSFiles();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void loadNBSFiles() {
        var musicFolder = new File(plugin.getDataFolder(), "music");

        if (!musicFolder.exists()) {
            if (musicFolder.mkdirs()) {
                plugin.getLogger().info("Created music folder: " + musicFolder.getAbsolutePath());
            }
            return;
        }

        File[] files = musicFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".nbs"));

        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No .nbs files found in music folder");
            return;
        }

        Arrays.stream(files).forEach(this::loadNBSFile);

        plugin.getLogger().info("Loaded " + tracks.size() + " music tracks");
    }

    private void loadNBSFile(File file) {
        try {
            if (!Files.isReadable(file.toPath())) {
                plugin.getLogger().warning("Cannot read file: " + file.getName());
                return;
            }

            var song = NBSDecoder.parse(file);
            if (song == null) {
                plugin.getLogger().warning("Failed to parse: " + file.getName());
                return;
            }

            var fileName = file.getName().replace(".nbs", "");
            var title = song.getTitle();
            var displayName = (title != null && !title.trim().isEmpty()) ? title : fileName;
            var discMaterial = DISC_MATERIALS[discIndex.getAndIncrement() % DISC_MATERIALS.length];

            tracks.add(new MusicTrack(displayName, discMaterial, song));
            plugin.getLogger().info("Loaded: " + displayName);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load " + file.getName() + ": " + e.getMessage());
        }
    }

    public List<MusicTrack> getAllTracks() {
        return List.copyOf(tracks);
    }

    public void playTrackByName(Player player, String trackName) {
        if (dataManager.isMusicDisabled(player.getUniqueId())) {
            return;
        }

        tracks.stream()
                .filter(t -> t.name().equals(trackName))
                .findFirst()
                .ifPresent(track -> {
                    stopMusic(player);
                    playTrack(player, track);
                });
    }

    public void playRandomSong(Player player) {
        if (tracks.isEmpty() || dataManager.isMusicDisabled(player.getUniqueId())) {
            return;
        }

        var randomIndex = ThreadLocalRandom.current().nextInt(tracks.size());
        stopMusic(player);
        playTrack(player, tracks.get(randomIndex));
    }

    public void playNextSong(Player player) {
        if (tracks.isEmpty() || dataManager.isMusicDisabled(player.getUniqueId())) {
            return;
        }

        var currentName = currentTracks.get(player.getUniqueId());
        var currentIndex = -1;

        if (currentName != null) {
            for (int i = 0; i < tracks.size(); i++) {
                if (tracks.get(i).name().equals(currentName)) {
                    currentIndex = i;
                    break;
                }
            }
        }

        var nextIndex = (currentIndex + 1) % tracks.size();
        stopMusic(player);
        playTrack(player, tracks.get(nextIndex));
    }

    private void playTrack(Player player, MusicTrack track) {
        if (track == null || dataManager.isMusicDisabled(player.getUniqueId())) {
            return;
        }

        var uuid = player.getUniqueId();
        currentTracks.put(uuid, track.name());
        pausedPlayers.put(uuid, false);

        try {
            var songPlayer = new RadioSongPlayer(track.song());
            var volume = volumes.computeIfAbsent(uuid, k -> 0.5f);
            songPlayer.setVolume((byte) Math.min(100, Math.max(0, volume * 100)));
            songPlayer.addPlayer(player);
            songPlayer.setPlaying(true);
            activePlayers.put(uuid, songPlayer);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to play track for " + player.getName() + ": " + e.getMessage());
            currentTracks.remove(uuid);
            pausedPlayers.remove(uuid);
        }
    }

    @EventHandler
    public void onSongEnd(SongEndEvent event) {
        var songPlayer = event.getSongPlayer();
        if (songPlayer == null) return;

        var playerUUIDs = new HashSet<>(songPlayer.getPlayerUUIDs());

        playerUUIDs.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .filter(Player::isOnline)
                .filter(player -> !dataManager.isMusicDisabled(player.getUniqueId()))
                .forEach(player -> Bukkit.getScheduler().runTaskLater(
                        plugin,
                        () -> {
                            if (player.isOnline() && !dataManager.isMusicDisabled(player.getUniqueId())) {
                                playNextSong(player);
                            }
                        },
                        20L
                ));
    }

    public void pauseMusic(Player player) {
        var uuid = player.getUniqueId();
        var songPlayer = activePlayers.get(uuid);

        if (songPlayer != null && songPlayer.isPlaying()) {
            songPlayer.setPlaying(false);
            pausedPlayers.put(uuid, true);
        }
    }

    public void resumeMusic(Player player) {
        var uuid = player.getUniqueId();
        var songPlayer = activePlayers.get(uuid);

        if (songPlayer != null && !songPlayer.isPlaying()) {
            songPlayer.setPlaying(true);
            pausedPlayers.put(uuid, false);
        }
    }

    public boolean isPaused(Player player) {
        return pausedPlayers.getOrDefault(player.getUniqueId(), false);
    }

    public void permanentlyStopMusic(Player player) {
        var uuid = player.getUniqueId();
        dataManager.setMusicDisabled(uuid, true);
        stopMusic(player);
    }

    public void enableMusic(Player player) {
        dataManager.setMusicDisabled(player.getUniqueId(), false);
    }

    public boolean isMusicDisabled(Player player) {
        return dataManager.isMusicDisabled(player.getUniqueId());
    }

    public String getCurrentSongName(Player player) {
        return currentTracks.getOrDefault(player.getUniqueId(), "No Song Playing");
    }

    public boolean isPlaying(Player player) {
        var uuid = player.getUniqueId();
        var songPlayer = activePlayers.get(uuid);
        return songPlayer != null && songPlayer.isPlaying();
    }

    public void stopMusic(Player player) {
        var uuid = player.getUniqueId();
        var songPlayer = activePlayers.remove(uuid);

        if (songPlayer != null) {
            try {
                songPlayer.setPlaying(false);
                songPlayer.removePlayer(player);
                songPlayer.destroy();
            } catch (Exception e) {
                plugin.getLogger().warning("Error stopping music for " + player.getName() + ": " + e.getMessage());
            }
        }

        currentTracks.remove(uuid);
        pausedPlayers.remove(uuid);
    }

    public void stopAllMusic() {
        var players = new HashSet<>(activePlayers.keySet());

        players.forEach(uuid -> {
            var songPlayer = activePlayers.remove(uuid);
            if (songPlayer != null) {
                try {
                    songPlayer.setPlaying(false);
                    var playerUUIDs = new HashSet<>(songPlayer.getPlayerUUIDs());
                    playerUUIDs.stream()
                            .map(Bukkit::getPlayer)
                            .filter(Objects::nonNull)
                            .forEach(songPlayer::removePlayer);
                    songPlayer.destroy();
                } catch (Exception e) {
                    plugin.getLogger().warning("Error destroying song player: " + e.getMessage());
                }
            }
        });

        activePlayers.clear();
        currentTracks.clear();
        pausedPlayers.clear();
    }

    public void cleanup() {
        stopAllMusic();
        volumes.clear();
        tracks.clear();
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public record MusicTrack(String name, Material discMaterial, Song song) {}
}
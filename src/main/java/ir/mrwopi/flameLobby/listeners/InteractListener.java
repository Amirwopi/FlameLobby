package ir.mrwopi.flameLobby.listeners;

import ir.mrwopi.flameLobby.FlameLobby;
import ir.mrwopi.flameLobby.LobbyItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

public class InteractListener implements Listener {

    private static final Set<Action> RIGHT_CLICK_ACTIONS = EnumSet.of(
            Action.RIGHT_CLICK_AIR,
            Action.RIGHT_CLICK_BLOCK
    );

    private final FlameLobby plugin;
    private final MusicGUIListener musicGUIListener;

    public InteractListener(FlameLobby plugin) {
        this.plugin = plugin;
        this.musicGUIListener = new MusicGUIListener(plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRightClick(PlayerInteractEvent event) {
        try {
            Player player = event.getPlayer();

            if (!player.isOnline()) {
                return;
            }

            if (!isInSpawnWorld(player)) {
                return;
            }

            if (!isRightClick(event.getAction())) {
                return;
            }

            ItemStack item = event.getItem();
            if (item == null || item.getType() == Material.AIR) {
                return;
            }

            Material itemType = item.getType();

            if (itemType == Material.COMPASS) {
                event.setCancelled(true);
                handleCompassClick(player);
            } else if (itemType == Material.NOTE_BLOCK) {
                event.setCancelled(true);
                handleNoteBlockClick(player);
            } else if (LobbyItems.hasTag(plugin, item, "parkour_start")) {
                event.setCancelled(true);
                handleParkourStart(player);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in PlayerInteractEvent: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!player.isOnline()) {
            return;
        }
        String msg = event.getMessage();
        if (msg == null) {
            return;
        }

        String cmd = msg.startsWith("/") ? msg.substring(1) : msg;
        cmd = cmd.trim();
        if (cmd.isEmpty()) {
            return;
        }

        String start = plugin.getConfig().getString("ajparkour.start-command", "/ajp start");
        String stop = plugin.getConfig().getString("ajparkour.stop-command", "/ajp leave");

        String startNorm = normalizeCmd(start);
        String stopNorm = normalizeCmd(stop);

        if (!startNorm.isEmpty() && cmd.toLowerCase().startsWith(startNorm)) {
            removeLobbyElytra(player);
        } else if (!stopNorm.isEmpty() && cmd.toLowerCase().startsWith(stopNorm)) {
            equipLobbyElytra(player);
        }
    }

    private boolean isInSpawnWorld(Player player) {
        try {
            String spawnWorldName = plugin.getConfiguredSpawnWorldName();
            return spawnWorldName.equals(player.getWorld().getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking world: " + e.getMessage());
            return false;
        }
    }

    private boolean isRightClick(Action action) {
        return action != null && RIGHT_CLICK_ACTIONS.contains(action);
    }

    private void handleCompassClick(Player player) {
        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (plugin.getServerSelectorGUI() != null) {
                    plugin.getServerSelectorGUI().open(player);
                } else {
                    sendConfigured(player, "messages.errors.server-selector-unavailable", "§cServer selector is not available!");
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling compass click: " + e.getMessage());
        }
    }

    private void handleNoteBlockClick(Player player) {
        try {
            if (plugin.isNoteBlockAPIEnabled()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        musicGUIListener.openMusicGUI(player)
                );
            } else {
                sendConfigured(player, "messages.errors.music-unavailable", "§cMusic system is not available!");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling noteblock click: " + e.getMessage());
        }
    }

    private void handleParkourStart(Player player) {
        if (!plugin.getConfig().getBoolean("ajparkour.enabled", true)) {
            sendConfigured(player, "messages.parkour.disabled", "§cParkour is disabled.");
            return;
        }
        if (plugin.getServer().getPluginManager().getPlugin("ajParkour") == null) {
            sendConfigured(player, "messages.parkour.ajparkour-missing", "§cajParkour is not installed.");
            return;
        }

        String cmd = plugin.getConfig().getString("ajparkour.start-command", "/ajp start");
        if (cmd == null || cmd.isBlank()) {
            return;
        }
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        String finalCmd = cmd.replace("{player}", player.getName());
        removeLobbyElytra(player);
        Bukkit.dispatchCommand(player, finalCmd);
    }

    private String normalizeCmd(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.startsWith("/")) {
            s = s.substring(1);
        }
        s = s.trim().toLowerCase();
        return s;
    }

    private void removeLobbyElytra(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        if (LobbyItems.hasTag(plugin, chest, "lobby_elytra")) {
            player.getInventory().setChestplate(null);
        }
    }

    private void equipLobbyElytra(Player player) {
        if (!isInSpawnWorld(player)) {
            return;
        }
        if (!plugin.getConfig().getBoolean("lobby-items.enabled", true)) {
            return;
        }
        player.getInventory().setChestplate(LobbyItems.elytraItem(plugin));
    }

    private void sendConfigured(Player player, String path, String fallback) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String raw = plugin.getConfig().getString(path, fallback);
        if (raw == null) raw = fallback;
        player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(prefix + raw));
    }

    public MusicGUIListener getMusicGUIListener() {
        return musicGUIListener;
    }
}
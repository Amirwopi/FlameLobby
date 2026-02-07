package ir.mrwopi.flameLobby.tasks;

import ir.mrwopi.flameLobby.FlameLobby;
import ir.mrwopi.flameLobby.LobbyItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class LobbyTask extends BukkitRunnable {

    private static final String SPAWN_WORLD = "world";
    private static final double MIN_Y = 0.0;

    private final FlameLobby plugin;

    public LobbyTask(FlameLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isInSpawn(player)) {
                continue;
            }
            handleFallenPlayer(player);

            ensureUnlimitedFireworks(player);

            resetPlayerStats(player);
        }
    }

    private boolean isInSpawn(Player player) {
        String spawnWorld = plugin.getSpawnWorldName();
        if (spawnWorld == null || spawnWorld.isBlank()) spawnWorld = SPAWN_WORLD;
        return spawnWorld.equals(player.getWorld().getName());
    }

    private void handleFallenPlayer(Player player) {
        Location loc = player.getLocation();

        if (loc.getY() < MIN_Y) {
            teleportToSpawn(player);
            sendTeleportMessage(player);
            stopAjParkour(player);
        }
    }

    private void teleportToSpawn(Player player) {
        Location loc = plugin.getConfiguredSpawnLocation();
        if (loc != null) {
            player.teleport(loc);
        }
    }

    private void ensureUnlimitedFireworks(Player player) {
        if (!plugin.getConfig().getBoolean("lobby-items.enabled", true)) {
            return;
        }
        int slot = plugin.getConfig().getInt("lobby-items.slots.fireworks", 8);
        if (slot < 0 || slot > 8) slot = 8;

        ItemStack it = player.getInventory().getItem(slot);
        if (it == null || it.getType() == Material.AIR) {
            player.getInventory().setItem(slot, LobbyItems.fireworksItem(plugin));
            return;
        }

        if (!LobbyItems.hasTag(plugin, it, "lobby_fireworks")) {
            return;
        }

        if (it.getAmount() != 1) {
            it.setAmount(1);
        }
    }

    private void stopAjParkour(Player player) {
        if (plugin == null) {
            return;
        }
        if (!plugin.getConfig().getBoolean("ajparkour.enabled", true)) {
            return;
        }
        if (plugin.getServer().getPluginManager().getPlugin("ajParkour") == null) {
            return;
        }
        String cmd = plugin.getConfig().getString("ajparkour.stop-command", "");
        if (cmd == null || cmd.isBlank()) {
            return;
        }
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        String finalCmd = cmd.replace("{player}", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
    }

    private void sendTeleportMessage(Player player) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String raw = plugin.getConfig().getString("messages.lobby.fallen-teleport", "§7Teleported you to Lobby, stay safe :)");
        if (raw == null) raw = "§7Teleported you to Lobby, stay safe :)";
        player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(prefix + raw));
    }

    private void resetPlayerStats(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20.0f);

        if (player.getFireTicks() > 0) {
            player.setFireTicks(0);
        }
    }
}
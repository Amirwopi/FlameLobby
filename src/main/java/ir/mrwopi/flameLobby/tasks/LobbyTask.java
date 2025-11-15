package ir.mrwopi.flameLobby.tasks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class LobbyTask extends BukkitRunnable {

    private static final String SPAWN_WORLD = "spawn";
    private static final double MIN_Y = 0.0;
    private static final double SPAWN_X = -56.5;
    private static final double SPAWN_Y = 96.0;
    private static final double SPAWN_Z = 0.5;
    private static final float SPAWN_YAW = -90.90032196044922f;
    private static final float SPAWN_PITCH = 0.9000003933906555f;

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isInSpawn(player)) {
                continue;
            }
            handleFallenPlayer(player);

            resetPlayerStats(player);
        }
    }

    private boolean isInSpawn(Player player) {
        return SPAWN_WORLD.equals(player.getWorld().getName());
    }

    private void handleFallenPlayer(Player player) {
        Location loc = player.getLocation();

        if (loc.getY() < MIN_Y) {
            teleportToSpawn(player);
            sendTeleportMessage(player);
        }
    }

    private void teleportToSpawn(Player player) {
        World spawnWorld = Bukkit.getWorld(SPAWN_WORLD);

        if (spawnWorld != null) {
            Location spawnLocation = new Location(
                    spawnWorld,
                    SPAWN_X, SPAWN_Y, SPAWN_Z,
                    SPAWN_YAW, SPAWN_PITCH
            );

            player.teleport(spawnLocation);
        }
    }

    private void sendTeleportMessage(Player player) {
        Component message = Component.text("Teleported you to Lobby, stay safe :)", NamedTextColor.GRAY);
        player.sendMessage(message);
    }

    private void resetPlayerStats(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20.0f);

        if (player.getFireTicks() > 0) {
            player.setFireTicks(0);
        }
    }
}
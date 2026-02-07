package ir.mrwopi.flameLobby.listeners;

import ir.mrwopi.flameLobby.FlameLobby;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class InventoryListener implements Listener {

    private static final String SPAWN_WORLD = "world";
    private static final String ADMIN_PERMISSION = "flamelobby.admin";

    private final FlameLobby plugin;

    public InventoryListener(FlameLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!plugin.getConfig().getBoolean("protections.spawn.prevent-inventory-move", true)) {
            return;
        }

        if (isMusicGUI(event.getView().title()) || isServerSelectorGUI(event.getView().title())) {
            return;
        }

        if (isInSpawn(player) && !player.hasPermission(ADMIN_PERMISSION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!plugin.getConfig().getBoolean("protections.spawn.prevent-inventory-move", true)) {
            return;
        }

        if (isInSpawn(player) && !player.hasPermission(ADMIN_PERMISSION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        var player = event.getPlayer();

        if (!plugin.getConfig().getBoolean("protections.spawn.prevent-item-drop", true)) {
            return;
        }

        if (isInSpawn(player) && !player.hasPermission(ADMIN_PERMISSION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        var player = event.getPlayer();

        if (!plugin.getConfig().getBoolean("protections.spawn.prevent-swap-hand", true)) {
            return;
        }

        if (isInSpawn(player) && !player.hasPermission(ADMIN_PERMISSION)) {
            event.setCancelled(true);
        }
    }

    private boolean isInSpawn(Player player) {
        String spawnWorldName = plugin.getConfiguredSpawnWorldName();
        if (spawnWorldName == null || spawnWorldName.isBlank()) spawnWorldName = SPAWN_WORLD;
        return spawnWorldName.equals(player.getWorld().getName());
    }

    private boolean isMusicGUI(Component title) {
        if (title == null) {
            return false;
        }

        var plainTitle = PlainTextComponentSerializer.plainText().serialize(title);
        return plainTitle.contains("Music Player");
    }

    private boolean isServerSelectorGUI(Component title) {
        if (title == null) {
            return false;
        }
        var plainTitle = PlainTextComponentSerializer.plainText().serialize(title);
        return plainTitle.contains("Game Selector");
    }
}
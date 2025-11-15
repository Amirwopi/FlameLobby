package ir.mrwopi.flameLobby.listeners;


import ir.mrwopi.flameLobby.FlameLobby;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in PlayerInteractEvent: " + e.getMessage());
        }
    }

    private boolean isInSpawnWorld(Player player) {
        try {
            return "spawn".equals(player.getWorld().getName());
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
                    player.sendMessage(Component.text("Server selector is not available!", NamedTextColor.RED));
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
                player.sendMessage(
                        Component.text("Music system is not available!", NamedTextColor.RED)
                );
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling noteblock click: " + e.getMessage());
        }
    }

    public MusicGUIListener getMusicGUIListener() {
        return musicGUIListener;
    }
}
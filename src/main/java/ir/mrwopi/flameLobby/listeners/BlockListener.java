package ir.mrwopi.flameLobby.listeners;

import ir.mrwopi.flameLobby.FlameLobby;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class BlockListener implements Listener {

    private static final String ADMIN_PERMISSION = "flamelobby.admin";

    private final FlameLobby plugin;

    public BlockListener(FlameLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        var player = event.getPlayer();

        if (!plugin.getConfig().getBoolean("protections.spawn.prevent-block-break", true)) {
            return;
        }

        if (isInSpawn(player) && !player.hasPermission(ADMIN_PERMISSION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        var player = event.getPlayer();

        if (!plugin.getConfig().getBoolean("protections.spawn.prevent-block-place", true)) {
            return;
        }

        if (isInSpawn(player) && !player.hasPermission(ADMIN_PERMISSION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        var player = event.getPlayer();

        if (!isInSpawn(player) || player.hasPermission(ADMIN_PERMISSION)) {
            return;
        }

        var clickedBlock = event.getClickedBlock();

        if (clickedBlock == null) {
            return;
        }

        var blockType = clickedBlock.getType();

        if (isInteractableBlock(blockType)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        var player = event.getPlayer();

        if (!isInSpawn(player) || player.hasPermission(ADMIN_PERMISSION)) {
            return;
        }

        var entity = event.getRightClicked();

        if (entity.getType() != EntityType.PLAYER) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!isInSpawn(player) || player.hasPermission(ADMIN_PERMISSION)) {
            return;
        }

        var entity = event.getEntity();

        if (entity.getType() != EntityType.PLAYER) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        var player = event.getPlayer();

        if (!plugin.getConfig().getBoolean("protections.spawn.prevent-bucket-empty", true)) {
            return;
        }

        if (isInSpawn(player) && !player.hasPermission(ADMIN_PERMISSION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        var player = event.getPlayer();

        if (!plugin.getConfig().getBoolean("protections.spawn.prevent-bucket-fill", true)) {
            return;
        }

        if (isInSpawn(player) && !player.hasPermission(ADMIN_PERMISSION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        var blockType = event.getBlock().getType();

        if (blockType == Material.FARMLAND) {
            event.setCancelled(true);
            return;
        }

        if (isInSpawn(event.getBlock().getWorld().getName())) {
            if (blockType == Material.ICE ||
                    blockType == Material.SNOW ||
                    blockType == Material.SNOW_BLOCK ||
                    blockType == Material.POWDER_SNOW) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isInSpawn(Player player) {
        String spawnWorldName = plugin.getConfiguredSpawnWorldName();
        if (spawnWorldName == null || spawnWorldName.isBlank()) spawnWorldName = "world";
        return spawnWorldName.equals(player.getWorld().getName());
    }

    private boolean isInSpawn(String worldName) {
        String spawnWorldName = plugin.getConfiguredSpawnWorldName();
        if (spawnWorldName == null || spawnWorldName.isBlank()) spawnWorldName = "world";
        return spawnWorldName.equals(worldName);
    }

    private boolean isInteractableBlock(Material material) {
        return switch (material) {
            case CHEST, TRAPPED_CHEST, ENDER_CHEST, BARREL,
                 FURNACE, BLAST_FURNACE, SMOKER,
                 CRAFTING_TABLE, ENCHANTING_TABLE, ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL,
                 BREWING_STAND, HOPPER, DROPPER, DISPENSER,
                 LEVER, STONE_BUTTON, OAK_BUTTON, SPRUCE_BUTTON, BIRCH_BUTTON,
                 JUNGLE_BUTTON, ACACIA_BUTTON, DARK_OAK_BUTTON, CRIMSON_BUTTON, WARPED_BUTTON,
                 OAK_DOOR, SPRUCE_DOOR, BIRCH_DOOR, JUNGLE_DOOR, ACACIA_DOOR, DARK_OAK_DOOR,
                 CRIMSON_DOOR, WARPED_DOOR, IRON_DOOR,
                 OAK_TRAPDOOR, SPRUCE_TRAPDOOR, BIRCH_TRAPDOOR, JUNGLE_TRAPDOOR,
                 ACACIA_TRAPDOOR, DARK_OAK_TRAPDOOR, CRIMSON_TRAPDOOR, WARPED_TRAPDOOR, IRON_TRAPDOOR,
                 OAK_FENCE_GATE, SPRUCE_FENCE_GATE, BIRCH_FENCE_GATE, JUNGLE_FENCE_GATE,
                 ACACIA_FENCE_GATE, DARK_OAK_FENCE_GATE, CRIMSON_FENCE_GATE, WARPED_FENCE_GATE,
                 REPEATER, COMPARATOR, REDSTONE_ORE,
                 CAKE, DRAGON_EGG, RESPAWN_ANCHOR -> true;
            default -> false;
        };
    }
}
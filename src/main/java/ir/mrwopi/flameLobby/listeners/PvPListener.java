package ir.mrwopi.flameLobby.listeners;

import ir.mrwopi.flameLobby.FlameLobby;
import ir.mrwopi.flameLobby.managers.PvPManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PvPListener implements Listener {
    private static final String SPAWN_WORLD = "spawn";
    private static final int SWORD_SLOT = 4; // hotbar slot index

    private final FlameLobby plugin;
    private final PvPManager pvpManager;
    private final Map<UUID, BukkitTask> countdowns = new ConcurrentHashMap<>();

    public PvPListener(FlameLobby plugin, PvPManager pvpManager) {
        this.plugin = plugin;
        this.pvpManager = pvpManager;
    }

    private boolean isInSpawn(Player p) {
        return SPAWN_WORLD.equals(p.getWorld().getName());
    }

    private boolean isPvPSword(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_SWORD) return false;
        if (item.getItemMeta() == null || item.getItemMeta().displayName() == null) return false;
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        return plain.toLowerCase().contains("pvp");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!isInSpawn(player)) return;

        int newSlot = event.getNewSlot();
        ItemStack inHand = player.getInventory().getItem(newSlot);

        // Cancel previous countdown
        cancelCountdown(player.getUniqueId());
        pvpManager.setEnabled(player, false);

        if (newSlot == SWORD_SLOT && isPvPSword(inHand)) {
            // Start 5-second countdown while holding
            pvpManager.setHoldingSlot(player, newSlot);
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                int sec = 5;
                @Override
                public void run() {
                    if (!player.isOnline() || !isInSpawn(player)) {
                        cancelCountdown(player.getUniqueId());
                        pvpManager.setEnabled(player, false);
                        return;
                    }
                    if (!pvpManager.isStillHolding(player, SWORD_SLOT) || !isPvPSword(player.getInventory().getItem(SWORD_SLOT))) {
                        player.sendMessage(Component.text("PvP disabled (you switched items)", NamedTextColor.GRAY));
                        cancelCountdown(player.getUniqueId());
                        pvpManager.setEnabled(player, false);
                        return;
                    }
                    if (sec <= 0) {
                        pvpManager.setEnabled(player, true);
                        player.sendMessage(Component.text("PvP enabled!", NamedTextColor.RED));
                        cancelCountdown(player.getUniqueId());
                        return;
                    }
                    player.sendActionBar(Component.text("Hold to enable PvP: " + sec + "s", NamedTextColor.RED));
                    sec--;
                }
            }, 0L, 20L);
            countdowns.put(player.getUniqueId(), task);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (!isInSpawn(victim) || !isInSpawn(attacker)) return;

        boolean allow = pvpManager.isEnabled(attacker) && pvpManager.isEnabled(victim);
        if (!allow) {
            // Block PvP if either side not enabled
            event.setCancelled(true);
            return;
        }

        // Allowed: play effects
        World w = victim.getWorld();
        BlockData redstoneBlock = Material.REDSTONE_BLOCK.createBlockData();
        w.spawnParticle(Particle.BLOCK_CRACK, victim.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.02, redstoneBlock);
        w.playSound(victim.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isInSpawn(player)) return;
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        // Optional: disable PvP after death
        pvpManager.setEnabled(player, false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        cancelCountdown(id);
        pvpManager.clear(event.getPlayer());
    }

    private void cancelCountdown(UUID id) {
        BukkitTask t = countdowns.remove(id);
        if (t != null) t.cancel();
    }
}

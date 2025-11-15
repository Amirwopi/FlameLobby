package ir.mrwopi.flameLobby.listeners;

import ir.mrwopi.flameLobby.FlameLobby;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import static org.bukkit.potion.PotionEffectType.*;
import org.bukkit.potion.PotionEffectType;
import java.util.List;

public class PlayerDamageListener implements Listener {
    private final FlameLobby plugin;
    private static final List<PotionEffectType> HARMFUL_EFFECTS = List.of(POISON, WITHER, WEAKNESS, HARM, BLINDNESS, HUNGER, SLOW, LEVITATION, UNLUCK, DARKNESS);

    public PlayerDamageListener(FlameLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Protect players in spawn from environmental damage
        if (isInSpawn(player)) {
            event.setCancelled(true);

            if (isFireDamage(event.getCause())) {
                player.setFireTicks(0);
            }

            if (isPotionDamage(event.getCause())) {
                removeHarmfulEffects(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        // In spawn, let PvPListener decide (handles enabled PvP, effects, etc.)
        if (isInSpawn(victim)) {
            return;
        }
        // Outside spawn: do nothing special here (don't interfere)
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (isInSpawn(player)) {
            event.setCancelled(true);
            player.setFireTicks(0);
        }
    }

    private boolean isPotionDamage(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.POISON ||
                cause == EntityDamageEvent.DamageCause.WITHER ||
                cause == EntityDamageEvent.DamageCause.MAGIC;
    }

    private boolean isFireDamage(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.FIRE ||
                cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                cause == EntityDamageEvent.DamageCause.LAVA ||
                cause == EntityDamageEvent.DamageCause.HOT_FLOOR ||
                cause == EntityDamageEvent.DamageCause.LIGHTNING;
    }

    private boolean isInSpawn(Player player) {
        String spawnWorld = plugin.getSpawnWorldName();
        if (spawnWorld == null || spawnWorld.isBlank()) {
            spawnWorld = "spawn";
        }
        return spawnWorld.equals(player.getWorld().getName());
    }

    private void removeHarmfulEffects(Player player) {
        for (PotionEffectType type : HARMFUL_EFFECTS) {
            if (player.hasPotionEffect(type)) {
                player.removePotionEffect(type);
            }
        }
    }
}
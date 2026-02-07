package ir.mrwopi.flameLobby.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class FireProtectionListener implements Listener {

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!player.getWorld().getName().equals("world")) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                event.getCause() == EntityDamageEvent.DamageCause.LAVA) {

            event.setCancelled(true);
            player.setFireTicks(0);
        }
    }

    @EventHandler
    public void onPlayerCombust(org.bukkit.event.entity.EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.getWorld().getName().equals("world")) {
            event.setCancelled(true);
            player.setFireTicks(0);
        }
    }
}
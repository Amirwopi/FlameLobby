package ir.mrwopi.flameLobby.listeners;

import ir.mrwopi.flameLobby.FlameLobby;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

public class JumpPadListener implements Listener {

    private final FlameLobby plugin;

    public JumpPadListener(FlameLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onStep(PlayerInteractEvent e) {
        if (!plugin.getConfig().getBoolean("jump-pad.enabled", true)) {
            return;
        }

        if (e.getHand() != null && e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (e.getAction() != Action.PHYSICAL) {
            return;
        }
        Block clicked = e.getClickedBlock();
        if (clicked == null) {
            return;
        }

        Material trigger = Material.LIGHT_WEIGHTED_PRESSURE_PLATE;
        String triggerRaw = plugin.getConfig().getString("jump-pad.trigger", "LIGHT_WEIGHTED_PRESSURE_PLATE");
        if (triggerRaw != null) {
            try {
                trigger = Material.valueOf(triggerRaw);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (clicked.getType() != trigger) {
            return;
        }

        Player p = e.getPlayer();
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        double horiz = plugin.getConfig().getDouble("jump-pad.horizontal", 8.5);
        double vert = plugin.getConfig().getDouble("jump-pad.vertical", 1.6);
        Vector vel = dir.multiply(horiz);
        vel.setY(vert);
        p.setVelocity(vel);

        p.spawnParticle(Particle.CLOUD, p.getLocation().add(0.0, 0.2, 0.0), 30, 0.4, 0.4, 0.4, 0.05);
        p.spawnParticle(Particle.FLAME, p.getLocation().add(0.0, 0.2, 0.0), 16, 0.3, 0.6, 0.3, 0.02);
        for (double angle = 0.0; angle < 360.0; angle += 20.0) {
            double rad = Math.toRadians(angle);
            double x = Math.cos(rad) * 1.0;
            double z = Math.sin(rad) * 1.0;
            p.spawnParticle(Particle.FLAME, p.getLocation().add(x, 0.1, z), 1);
        }

        try {
            p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        } catch (IllegalArgumentException | NoSuchFieldError ex) {
            p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        }
    }
}

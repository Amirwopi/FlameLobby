package ir.mrwopi.flameLobby.managers;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PvPManager {
    private final Set<UUID> enabled = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> holdingSlot = new ConcurrentHashMap<>();
    private final Map<UUID, Long> combatUntil = new ConcurrentHashMap<>();

    public void setEnabled(Player player, boolean value) {
        if (value) enabled.add(player.getUniqueId()); else enabled.remove(player.getUniqueId());
    }

    public boolean isEnabled(Player player) {
        return enabled.contains(player.getUniqueId());
    }

    public void setHoldingSlot(Player player, int slot) {
        holdingSlot.put(player.getUniqueId(), slot);
    }

    public boolean isStillHolding(Player player, int expectedSlot) {
        return player.getInventory().getHeldItemSlot() == expectedSlot;
    }

    public void clear(Player player) {
        enabled.remove(player.getUniqueId());
        holdingSlot.remove(player.getUniqueId());
        combatUntil.remove(player.getUniqueId());
    }

    // Combat tagging: keep PvP active for N seconds after a valid hit
    public void tagCombat(Player player, int seconds) {
        long until = System.currentTimeMillis() + (seconds * 1000L);
        combatUntil.put(player.getUniqueId(), until);
    }

    public boolean isInCombat(Player player) {
        Long until = combatUntil.get(player.getUniqueId());
        return until != null && until > System.currentTimeMillis();
    }

    public boolean isPvpActive(Player player) {
        return isEnabled(player) || isInCombat(player);
    }

    public int getCombatSecondsLeft(Player player) {
        Long until = combatUntil.get(player.getUniqueId());
        if (until == null) return 0;
        long diff = until - System.currentTimeMillis();
        return (int) Math.max(0, Math.ceil(diff / 1000.0));
    }
}

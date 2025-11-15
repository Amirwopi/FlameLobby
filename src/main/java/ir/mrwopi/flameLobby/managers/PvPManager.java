package ir.mrwopi.flameLobby.managers;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PvPManager {
    private final Set<UUID> enabled = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> holdingSlot = new ConcurrentHashMap<>();

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
    }
}

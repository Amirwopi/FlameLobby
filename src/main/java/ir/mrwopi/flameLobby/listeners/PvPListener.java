package ir.mrwopi.flameLobby.listeners;

import ir.mrwopi.flameLobby.FlameLobby;
import ir.mrwopi.flameLobby.managers.PvPManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PvPListener implements Listener {
    private static final String SPAWN_WORLD = "spawn";
    private static final int SWORD_SLOT = 4;
    private static final int COMBAT_SECONDS = 10;

    private final FlameLobby plugin;
    private final PvPManager pvpManager;
    private final Map<UUID, BukkitTask> countdowns = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> combatBars = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastOpponent = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> killStreaks = new ConcurrentHashMap<>();
    private final NamespacedKey pvpArmorKey;

    public PvPListener(FlameLobby plugin, PvPManager pvpManager) {
        this.plugin = plugin;
        this.pvpManager = pvpManager;
        this.pvpArmorKey = new NamespacedKey(plugin, "pvp_armor");
    }

    private void ensureBelowNameHealthObjective() {
        try {
            ScoreboardManager mgr = Bukkit.getScoreboardManager();
            if (mgr == null) return;
            Scoreboard board = mgr.getMainScoreboard();

            Objective existingBelow = board.getObjective(DisplaySlot.BELOW_NAME);
            if (existingBelow != null && !"pvp_hp".equals(existingBelow.getName())) {
                return;
            }

            Objective obj = board.getObjective("pvp_hp");
            if (obj == null) {
                try {
                    obj = board.registerNewObjective("pvp_hp", Criteria.HEALTH, Component.text("\u2665", NamedTextColor.RED));
                } catch (NoSuchMethodError e) {

                    obj = board.registerNewObjective("pvp_hp", "health");
                    obj.setDisplayName("\u2665");
                }
            }
            if (obj.getDisplaySlot() != DisplaySlot.BELOW_NAME) {
                obj.setDisplaySlot(DisplaySlot.BELOW_NAME);
            }
        } catch (Exception ignored) { }
    }

    private boolean isInSpawn(Player p) {
        String spawnWorldName = plugin.getSpawnWorldName();
        if (spawnWorldName == null || spawnWorldName.isBlank()) spawnWorldName = SPAWN_WORLD;
        return spawnWorldName.equals(p.getWorld().getName());
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


        cancelCountdown(player.getUniqueId());

        if (pvpManager.isInCombat(player)) {
            int left = pvpManager.getCombatSecondsLeft(player);
            if (player.getSaturation() > 0f) player.setSaturation(0f);
            ItemStack item = player.getInventory().getItem(SWORD_SLOT);
            boolean holding = isPvPSword(item) && player.getInventory().getHeldItemSlot() == SWORD_SLOT;
            if (holding) {
                player.sendActionBar(Component.text("PvP active", NamedTextColor.RED));
            } else {
                player.sendActionBar(Component.text("PvP disabling in " + left + "s", NamedTextColor.YELLOW));
            }
        }

        if (newSlot == SWORD_SLOT && isPvPSword(inHand)) {

            if (pvpManager.isEnabled(player)) {
                pvpManager.setHoldingSlot(player, newSlot);
                return;
            }

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
                        cancelCountdown(player.getUniqueId());
                        if (!pvpManager.isInCombat(player)) {
                            pvpManager.setEnabled(player, false);
                            player.sendMessage(Component.text("PvP disabled (you switched items)", NamedTextColor.GRAY));

                            playDisableFx(player);
                            restoreArmor(player);
                        } else {
                            int left = pvpManager.getCombatSecondsLeft(player);

            if (player.getSaturation() > 0f) player.setSaturation(0f);
            ItemStack item = player.getInventory().getItem(SWORD_SLOT);
            boolean holding = isPvPSword(item) && player.getInventory().getHeldItemSlot() == SWORD_SLOT;
            if (holding) {
                player.sendActionBar(Component.text("PvP active", NamedTextColor.RED));
            } else {
                player.sendActionBar(Component.text("PvP disabling in " + left + "s", NamedTextColor.YELLOW));
            }
                        }
                        return;
                    }
                    if (sec <= 0) {
                        pvpManager.setEnabled(player, true);
                        player.sendMessage(Component.text("PvP enabled!", NamedTextColor.RED));

                        playEnableFx(player);
                        equipPvpArmor(player);

                        pvpManager.tagCombat(player, COMBAT_SECONDS);
                        startOrRefreshCombatBar(player);
                        cancelCountdown(player.getUniqueId());
                        return;
                    }
                    player.sendActionBar(Component.text("Hold to enable PvP: " + sec + "s", NamedTextColor.RED));

                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.3f);
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

        boolean allow = pvpManager.isPvpActive(attacker) && pvpManager.isPvpActive(victim);
        if (!allow) {

            event.setCancelled(true);
            return;
        }


        World w = victim.getWorld();
        BlockData redstoneBlock = Material.REDSTONE_BLOCK.createBlockData();
        w.spawnParticle(Particle.BLOCK_CRACK, victim.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.02, redstoneBlock);
        w.playSound(victim.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);


        pvpManager.tagCombat(attacker, COMBAT_SECONDS);
        pvpManager.tagCombat(victim, COMBAT_SECONDS);

        lastOpponent.put(attacker.getUniqueId(), victim.getUniqueId());
        lastOpponent.put(victim.getUniqueId(), attacker.getUniqueId());
        attacker.setSaturation(0f);
        victim.setSaturation(0f);
        ensureBelowNameHealthObjective();
        startOrRefreshCombatBar(attacker);
        startOrRefreshCombatBar(victim);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isInSpawn(player)) return;
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);

        pvpManager.clear(player);
        stopCombatBar(player.getUniqueId());

        restoreArmor(player);


        Player killer = player.getKiller();
        if (killer != null && killer.isOnline()) {
            if (isInSpawn(killer)) {
                int streak = killStreaks.getOrDefault(killer.getUniqueId(), 0) + 1;
                killStreaks.put(killer.getUniqueId(), streak);
                playKillStreakFx(killer, streak);
                if (streak >= 10) {
                    killer.showTitle(Title.title(
                            Component.text("Serial Killer", NamedTextColor.DARK_RED),
                            Component.text("10 Kill Streak", NamedTextColor.RED)));
                    Bukkit.broadcast(Component.text(killer.getName() + " is a serial killer ruuuun !", NamedTextColor.RED));
                }
            }
            double hp = killer.getHealth();
            double max = 20.0;
            var attr = killer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
            if (attr != null) max = attr.getValue();
            double hearts = Math.round((hp / 2.0) * 10.0) / 10.0;

            Component msg = Component.text("⚔ ", NamedTextColor.DARK_RED)
                    .append(Component.text(killer.getName(), NamedTextColor.RED))
                    .append(Component.text(" obliterated ", NamedTextColor.GRAY))
                    .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" ", NamedTextColor.GRAY))
                    .append(Component.text("[", NamedTextColor.DARK_GRAY))
                    .append(Component.text(hearts + "\u2665", NamedTextColor.GREEN))
                    .append(Component.text("]", NamedTextColor.DARK_GRAY));
            event.deathMessage(msg);
        }

        killStreaks.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (isInSpawn(player)) {
            String spawnWorldName = plugin.getSpawnWorldName();
            if (spawnWorldName == null || spawnWorldName.isBlank()) spawnWorldName = SPAWN_WORLD;
            World spawnWorld = Bukkit.getWorld(spawnWorldName);
            if (spawnWorld != null) {
                event.setRespawnLocation(spawnWorld.getSpawnLocation());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        cancelCountdown(id);
        stopCombatBar(id);
        Player p = event.getPlayer();
        pvpManager.clear(p);
        restoreArmor(p);
        lastOpponent.remove(id);
        killStreaks.remove(id);
    }

    private void cancelCountdown(UUID id) {
        BukkitTask t = countdowns.remove(id);
        if (t != null) t.cancel();
    }

    private void startOrRefreshCombatBar(Player player) {

        BukkitTask prev = combatBars.remove(player.getUniqueId());
        if (prev != null) prev.cancel();

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !isInSpawn(player)) {
                stopCombatBar(player.getUniqueId());
                return;
            }
            int left = pvpManager.getCombatSecondsLeft(player);
            if (left <= 0) {

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                stopCombatBar(player.getUniqueId());

                ItemStack item = player.getInventory().getItem(SWORD_SLOT);
                boolean holdingPvpSword = isPvPSword(item) && player.getInventory().getHeldItemSlot() == SWORD_SLOT;
                if (!holdingPvpSword) {
                    if (pvpManager.isEnabled(player)) {
                        pvpManager.setEnabled(player, false);
                        player.sendMessage(Component.text("PvP disabled (combat ended)", NamedTextColor.GRAY));

                        playDisableFx(player);
                        restoreArmor(player);
                    }
                }
                return;
            }

            if (player.getSaturation() > 0f) player.setSaturation(0f);
            Component bar = Component.text("PvP: " + left + "s", NamedTextColor.RED);
            UUID oppId = lastOpponent.get(player.getUniqueId());
            if (oppId != null) {
                Player opp = Bukkit.getPlayer(oppId);
                if (opp != null && opp.isOnline()) {
                    double hp = opp.getHealth();
                    double max = 20.0;
                    var attr = opp.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (attr != null) max = attr.getValue();
                    double hearts = Math.round((hp / 2.0) * 10.0) / 10.0;
                    bar = bar.append(Component.text(" | " + opp.getName() + " " + hearts + "\u2665", NamedTextColor.YELLOW));
                }
            }
            player.sendActionBar(bar);

            if (left <= 5) {
                float pitch = 1.0f + (5 - left) * 0.1f;
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
            }
        }, 0L, 20L);

        combatBars.put(player.getUniqueId(), task);
    }

    private void stopCombatBar(UUID id) {
        BukkitTask t = combatBars.remove(id);
        if (t != null) t.cancel();
    }

    private void equipPvpArmor(Player player) {
        if (!isInSpawn(player)) return;
        ItemStack[] current = player.getInventory().getArmorContents();
        boolean already = false;
        for (ItemStack it : current) {
            if (isTaggedArmor(it)) {
                already = true;
                break;
            }
        }
        if (!already) {
            savedArmor.putIfAbsent(player.getUniqueId(), current.clone());
        }


        ItemStack chest = tag(make(Material.DIAMOND_CHESTPLATE));
        ItemStack legs = tag(make(Material.DIAMOND_LEGGINGS));

        player.getInventory().setChestplate(chest);
        player.getInventory().setLeggings(legs);
    }

    private void restoreArmor(Player player) {
        ItemStack[] prev = savedArmor.remove(player.getUniqueId());
        if (prev != null) {
            player.getInventory().setArmorContents(prev);
        } else {
            if (isTaggedArmor(player.getInventory().getHelmet())) player.getInventory().setHelmet(null);
            if (isTaggedArmor(player.getInventory().getChestplate())) player.getInventory().setChestplate(null);
            if (isTaggedArmor(player.getInventory().getLeggings())) player.getInventory().setLeggings(null);
            if (isTaggedArmor(player.getInventory().getBoots())) player.getInventory().setBoots(null);
        }
    }

    private ItemStack make(Material mat) {
        ItemStack it = new ItemStack(mat);
        it.editMeta(meta -> {
            meta.setUnbreakable(true);
            meta.displayName(Component.text("PvP Armor", NamedTextColor.RED));
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE, org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

            if (mat == Material.DIAMOND_CHESTPLATE || mat == Material.DIAMOND_LEGGINGS
                    || mat == Material.DIAMOND_HELMET || mat == Material.DIAMOND_BOOTS) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
            }
            if (meta instanceof org.bukkit.inventory.meta.Damageable dmg) {
                dmg.setDamage(0);
            }
        });
        return it;
    }

    private ItemStack tag(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(pvpArmorKey, PersistentDataType.BYTE, (byte) 1);
            it.setItemMeta(meta);
        }
        return it;
    }

    private boolean isTaggedArmor(ItemStack it) {
        if (it == null) return false;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return false;
        Byte val = meta.getPersistentDataContainer().get(pvpArmorKey, PersistentDataType.BYTE);
        return val != null && val == (byte) 1;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isInSpawn(player)) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        boolean involvesTagged = isTaggedArmor(current) || isTaggedArmor(cursor);
        boolean armorSlot = event.getSlotType() == InventoryType.SlotType.ARMOR;

        if (involvesTagged || armorSlot) {
            if (isTaggedArmor(current) || isTaggedArmor(cursor) || armorSlot) {
                event.setCancelled(true);
                player.updateInventory();
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!isInSpawn(player)) return;
        ItemStack drop = event.getItemDrop().getItemStack();
        if (isTaggedArmor(drop)) {
            event.setCancelled(true);
        }
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onRegain(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isInSpawn(player)) return;
        if (!pvpManager.isPvpActive(player)) return;
        EntityRegainHealthEvent.RegainReason reason = event.getRegainReason();
        if (reason == EntityRegainHealthEvent.RegainReason.REGEN || reason == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (isTaggedArmor(item) || isPvPSword(item)) {
            event.setCancelled(true);
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.Damageable dmg) {
                dmg.setDamage(0);
                item.setItemMeta(meta);
            }
        }
    }

    private void playEnableFx(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.2f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, 1.0f), 0L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, 1.25f), 2L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, 1.5f), 5L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f), 10L);
    }

    private void playDisableFx(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.0f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.9f), 0L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f), 3L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 0.6f, 0.6f), 6L);
    }

    private void playKillStreakFx(Player killer, int streak) {

        float pitch = Math.min(2.0f, 0.9f + 0.05f * streak);
        killer.playSound(killer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
        if (streak % 3 == 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f), 2L);
        }
        if (streak == 5) {
            killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
        }
        if (streak == 10) {
            killer.playSound(killer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }
}

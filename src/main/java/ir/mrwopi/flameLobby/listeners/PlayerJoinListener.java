package ir.mrwopi.flameLobby.listeners;

import ir.mrwopi.flameLobby.FlameLobby;
import ir.mrwopi.flameLobby.LobbyItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public record PlayerJoinListener(FlameLobby plugin) implements Listener {
    private static final Map<UUID, Queue<BukkitTask>> taskRegistry = new ConcurrentHashMap<>();
    private static final ThreadLocal<Random> rng = ThreadLocal.withInitial(Random::new);

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Optional.ofNullable(taskRegistry.remove(uuid))
                .ifPresent(q -> q.forEach(BukkitTask::cancel));

        Bukkit.getScheduler().runTask(plugin, () -> {
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.setGameMode(GameMode.ADVENTURE);

            Location spawnLoc = plugin.getConfiguredSpawnLocation();
            final String spawnWorldName;
            if (spawnLoc != null && spawnLoc.getWorld() != null) {
                spawnWorldName = spawnLoc.getWorld().getName();
                player.teleport(spawnLoc);
            } else {
                String tmp = plugin.getSpawnWorldName();
                if (tmp == null || tmp.isBlank()) tmp = "world";
                spawnWorldName = tmp;
                plugin.getLogger().warning("World '" + spawnWorldName + "' not found for player: " + player.getName());
            }

            stopAjParkour(player);

            giveCompass(player);
            givePvPSword(player);
            giveLobbyExtras(player);
            equipLobbyElytra(player);

            if (plugin.isNoteBlockAPIEnabled()) {
                giveNoteBlock(player);
                enqueueTask(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && spawnWorldName.equals(player.getWorld().getName())) {
                        plugin.getMusicManager().playRandomSong(player);
                    }
                }, 20L));
            }

            int selectorSlot = plugin.getConfig().getInt("lobby-items.slots.server-selector", 0);
            if (selectorSlot < 0 || selectorSlot > 8) selectorSlot = 0;
            player.getInventory().setHeldItemSlot(selectorSlot);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    playSpawnEffects(player);
                }
            }, 5L);
        });
    }

    private void stopAjParkour(Player player) {
        if (!plugin.getConfig().getBoolean("ajparkour.enabled", true)) {
            return;
        }
        if (plugin.getServer().getPluginManager().getPlugin("ajParkour") == null) {
            return;
        }
        String cmd = plugin.getConfig().getString("ajparkour.stop-command", "");
        if (cmd == null || cmd.isBlank()) {
            return;
        }
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        String finalCmd = cmd.replace("{player}", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
    }

    private void giveLobbyExtras(Player player) {
        if (!plugin.getConfig().getBoolean("lobby-items.enabled", true)) {
            return;
        }

        int parkourSlot = plugin.getConfig().getInt("lobby-items.slots.parkour-start", 1);
        int fireworksSlot = plugin.getConfig().getInt("lobby-items.slots.fireworks", 8);

        if (parkourSlot >= 0 && parkourSlot <= 8) {
            player.getInventory().setItem(parkourSlot, LobbyItems.parkourStartItem(plugin));
        }
        if (fireworksSlot >= 0 && fireworksSlot <= 8) {
            player.getInventory().setItem(fireworksSlot, LobbyItems.fireworksItem(plugin));
        }
    }

    private void equipLobbyElytra(Player player) {
        if (!plugin.getConfig().getBoolean("lobby-items.enabled", true)) {
            return;
        }
        player.getInventory().setChestplate(LobbyItems.elytraItem(plugin));
    }

    private void playSpawnEffects(Player player) {
        Location loc = player.getLocation();
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        player.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.2f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.8f);
            }
        }, 10L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.5f);
            }
        }, 20L);

        enqueueTask(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) spawnFireworkEffect(player, player.getLocation(), world);
        }, 5L));

        enqueueTask(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) spawnSpiralEffect(player, player.getLocation(), world);
        }, 10L));

        enqueueTask(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) spawnExplosionEffect(player, player.getLocation(), world);
        }, 15L));

        enqueueTask(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) animateParticles(player, player.getLocation(), world);
        }, 20L));
    }

    private void spawnFireworkEffect(Player player, Location loc, World world) {
        if (!player.isOnline()) return;

        IntStream.range(0, 50).forEach(i -> {
            double angle = rng.get().nextDouble() * Math.PI * 2;
            double radius = rng.get().nextDouble() * 2;
            double x = Math.cos(angle) * radius;
            double y = rng.get().nextDouble() * 3;
            double z = Math.sin(angle) * radius;

            Location particleLoc = loc.clone().add(x, y, z);
            world.spawnParticle(Particle.FLAME, particleLoc, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticle(Particle.LAVA, particleLoc, 1, 0.0, 0.0, 0.0, 0.0);
        });

        player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.0f);
    }

    private void spawnSpiralEffect(Player player, Location loc, World world) {
        if (!player.isOnline()) return;

        AtomicInteger counter = new AtomicInteger(0);
        double[] angleRef = {0};

        BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                int c = counter.getAndIncrement();
                if (c >= 40 || !player.isOnline()) {
                    cancel();
                    return;
                }

                IntStream.range(0, 3).forEach(i -> {
                    double currentAngle = angleRef[0] + (i * Math.PI * 2 / 3);
                    double x = Math.cos(currentAngle) * 1.5;
                    double y = c * 0.1;
                    double z = Math.sin(currentAngle) * 1.5;

                    Location particleLoc = loc.clone().add(x, y, z);
                    world.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 1, 0.0, 0.0, 0.0, 0.0);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0.0, 0.0, 0.0, 0.0);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 2, 0.1, 0.1, 0.1, 0.0);
                });
                angleRef[0] += Math.PI / 8;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        enqueueTask(player.getUniqueId(), task);
    }

    private void spawnExplosionEffect(Player player, Location loc, World world) {
        if (!player.isOnline()) return;

        IntStream.range(0, 100).forEach(i -> {
            Vector direction = new Vector(
                    rng.get().nextDouble() - 0.5,
                    rng.get().nextDouble() - 0.5,
                    rng.get().nextDouble() - 0.5
            ).normalize().multiply(2);

            Location particleLoc = loc.clone().add(0, 1, 0);
            world.spawnParticle(Particle.FLAME, particleLoc, 0, direction.getX(), direction.getY(), direction.getZ(), 0.3);
            world.spawnParticle(Particle.FIREWORKS_SPARK, particleLoc, 0, direction.getX(), direction.getY(), direction.getZ(), 0.2);
            world.spawnParticle(Particle.ENCHANTMENT_TABLE, particleLoc, 0, direction.getX(), direction.getY(), direction.getZ(), 0.5);
        });

        player.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
    }

    private void animateParticles(Player player, Location loc, World world) {
        if (!player.isOnline()) return;

        AtomicInteger tickCounter = new AtomicInteger(0);

        BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                int ticks = tickCounter.getAndIncrement();
                if (ticks >= 60 || !player.isOnline()) {
                    cancel();
                    return;
                }

                double radius = Math.max(2 - (ticks * 0.03), 0.3);

                IntStream.range(0, 360)
                        .filter(i -> i % 15 == 0)
                        .mapToDouble(Math::toRadians)
                        .forEach(angle -> {
                            double x = Math.cos(angle) * radius;
                            double y = Math.sin(ticks * 0.2) * 0.5 + 1;
                            double z = Math.sin(angle) * radius;

                            Location particleLoc = loc.clone().add(x, y, z);

                            Particle.DustOptions dustOrange = new Particle.DustOptions(Color.fromRGB(255, 100, 0), 1.5f);
                            Particle.DustOptions dustYellow = new Particle.DustOptions(Color.fromRGB(255, 200, 0), 1.0f);

                            world.spawnParticle(Particle.REDSTONE, particleLoc, 1, 0.0, 0.0, 0.0, 0.0, dustOrange);
                            world.spawnParticle(Particle.REDSTONE, particleLoc, 1, 0.0, 0.0, 0.0, 0.0, dustYellow);
                        });

                if (ticks % 10 == 0) {
                    world.spawnParticle(Particle.TOTEM, loc.clone().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                }

                if (ticks % 5 == 0) {
                    world.spawnParticle(Particle.GLOW, loc.clone().add(0, 0.5, 0), 5, 0.3, 0.3, 0.3, 0.0);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        enqueueTask(player.getUniqueId(), task);
    }

    private void giveCompass(Player player) {
        int slot = plugin.getConfig().getInt("lobby-items.slots.server-selector", 0);
        if (slot < 0 || slot > 8) slot = 0;

        if (player.getInventory().getItem(slot) != null && Objects.requireNonNull(player.getInventory().getItem(slot)).getType() != Material.AIR) {
            return;
        }

        ItemStack compass = new ItemStack(Material.COMPASS);
        compass.editMeta(meta -> {
            meta.displayName(Component.text("Game Selector ", NamedTextColor.YELLOW)
                    .append(Component.text("(Right-Click)", NamedTextColor.GRAY))
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Right-click to open menu", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        });
        player.getInventory().setItem(slot, compass);
    }

    private void givePvPSword(Player player) {
        int slot = plugin.getConfig().getInt("lobby-items.slots.pvp-sword", 4);
        if (slot < 0 || slot > 8) slot = 4;

        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.editMeta(meta -> {
            meta.displayName(Component.text("PvP Sword ", NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Hold for 5s to enable PvP", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Spawn-only duels", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);

            meta.addEnchant(Enchantment.DAMAGE_ALL, 2, true);
            if (meta instanceof org.bukkit.inventory.meta.Damageable dmg) {
                dmg.setDamage(0);
            }
        });
        player.getInventory().setItem(slot, sword);
    }

    private void giveNoteBlock(Player player) {
        int slot = plugin.getConfig().getInt("lobby-items.slots.music-player", 8);
        if (slot < 0 || slot > 8) slot = 8;

        ItemStack noteBlock = new ItemStack(Material.NOTE_BLOCK);
        noteBlock.editMeta(meta -> {
            meta.displayName(Component.text("Music Player ", NamedTextColor.GOLD)
                    .append(Component.text("(Right-Click)", NamedTextColor.GRAY))
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Right-click to control music", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        });
        player.getInventory().setItem(slot, noteBlock);
    }

    private void enqueueTask(UUID uuid, BukkitTask task) {
        taskRegistry.computeIfAbsent(uuid, k -> new ArrayDeque<>()).offer(task);
    }
}
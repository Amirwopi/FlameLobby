package ir.mrwopi.flameLobby.gui;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import ir.mrwopi.flameLobby.FlameLobby;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServerSelectorGUI implements Listener {

    private static final int SIZE = 45;
    private final FlameLobby plugin;

    public ServerSelectorGUI(FlameLobby plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        var holder = new SelectorHolder();
        String titleRaw = plugin.getConfig().getString("server-selector.title", "Game Selector");
        var title = Component.text(titleRaw, NamedTextColor.RED).decorate(TextDecoration.BOLD);
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);


        var bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        var bgMeta = bg.getItemMeta();
        if (bgMeta != null) {
            bgMeta.displayName(Component.empty());
            bg.setItemMeta(bgMeta);
        }
        int[] bgSlots = new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,28,29,30,31,32,33,34,35,36,37,43,44};
        for (int s : bgSlots) inv.setItem(s, bg);


        var red = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        var redMeta = red.getItemMeta();
        if (redMeta != null) {
            redMeta.displayName(Component.text(" "));
            red.setItemMeta(redMeta);
        }

        int[] accents = new int[]{
                9, 17, 27, 35
        };
        for (int s : accents) inv.setItem(s, red);


        inv.setItem(10, skull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjI1MDJhMzhlMTg5Nzg4MGM4Y2U1OWNjNjEzZDIyYWRhOGYwYjg0NzU0ODM5MmY1OTMxYzdhYWJhODZjYTA4MyJ9fX0=",
                Component.text("☁ ", NamedTextColor.AQUA).append(Component.text("SkyBlock", NamedTextColor.AQUA).decorate(TextDecoration.BOLD)),
                List.of(
                        Component.text("", NamedTextColor.GRAY),
                        Component.text("Build your island in the sky!", NamedTextColor.GRAY),
                        Component.text("", NamedTextColor.GRAY),
                        Component.text("► Click to Join!", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
                ),
                Material.PLAYER_HEAD));


        inv.setItem(11, skull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjEyNTNjY2NmNzMyNmFmYjUyMjNhMWFkNjI1MzJhZDU2YzQyMWQ4OTM2NzMwOTE1YzQxNDU5NzhjOWU3ZWY3MiJ9fX0=",
                Component.text("⛏ ", NamedTextColor.GOLD).append(Component.text("Survival", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)),
                List.of(
                        Component.text("", NamedTextColor.GRAY),
                        Component.text("Survive and thrive in a block world!", NamedTextColor.GRAY),
                        Component.text("", NamedTextColor.GRAY),
                        Component.text("► Click to Join!", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
                ),
                Material.PLAYER_HEAD));


        inv.setItem(38, skull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDNkMjVkNTVjYWVkZmQ3MGVlN2YzYTgwNmFmMDAyNWYxNTNkNGJmMzRmNDFlNmVmZDQ0ZWM4ZmU3NDE5OGYzNSJ9fX0=",
                Component.text(plugin.getConfig().getString("server-selector.social.telegram.name", "✈ Telegram"), NamedTextColor.BLUE).decorate(TextDecoration.BOLD),
                List.of(Component.text(plugin.getConfig().getString("server-selector.social.telegram.url", "https://github.com/amirwopi"), NamedTextColor.BLUE)),
                Material.PLAYER_HEAD));

        inv.setItem(41, skull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWQ1MmYwYTZmNDNlZjdhMTU2NGViZmEyMGFkOGM4ZTdmNjk0YTFmMjZjMmJhZTMwMzc2ZGJiM2NhMzE2MzZlYiJ9fX0=",
                Component.text(plugin.getConfig().getString("server-selector.social.teamspeak.name", "🎧 TeamSpeak"), NamedTextColor.AQUA).decorate(TextDecoration.BOLD),
                List.of(Component.text(plugin.getConfig().getString("server-selector.social.teamspeak.url", "github.com/amirwopi"), NamedTextColor.AQUA)),
                Material.PLAYER_HEAD));

        inv.setItem(40, skull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTJjYTI3Y2FiODc3MjI4OTZkYzY2OGM3YjliNzZlNmZjM2UyZGNlNzcwNTgzYWRmYmUxNjJhZGM5NGU5ZDgyZCJ9fX0=",
                Component.text(plugin.getConfig().getString("server-selector.social.discord.name", "💬 Discord"), NamedTextColor.BLUE).decorate(TextDecoration.BOLD),
                List.of(Component.text(plugin.getConfig().getString("server-selector.social.discord.url", "https://github.com/amirwopi"), NamedTextColor.BLUE)),
                Material.PLAYER_HEAD));

        inv.setItem(39, skull(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDM4Y2YzZjhlNTRhZmMzYjNmOTFkMjBhNDlmMzI0ZGNhMTQ4NjAwN2ZlNTQ1Mzk5MDU1NTI0YzE3OTQxZjRkYyJ9fX0=",
                Component.text(plugin.getConfig().getString("server-selector.social.website.name", "🌐 Website"), NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                List.of(Component.text(plugin.getConfig().getString("server-selector.social.website.url", "https://github.com/amirwopi"), NamedTextColor.YELLOW)),
                Material.PLAYER_HEAD));


        int[] soonSlots = new int[]{12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int s : soonSlots) {
            inv.setItem(s, skull(
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjM0MGQ1MGQ3ZDEyOTNiYTE2ZDIzYzZkMDdhYjA2NmNkYzE1NzVjNjhiY2E2OWU5NmYwYmI2ZDFjZTFiZjFiYSJ9fX0=",
                    Component.text("⏳ Soon...", NamedTextColor.GRAY).decorate(TextDecoration.BOLD),
                    List.of(Component.text("Coming soon", NamedTextColor.DARK_GRAY)),
                    Material.PLAYER_HEAD));
        }


        player.openInventory(inv);
        playGuiOpenFx(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof SelectorHolder)) return;
        event.setCancelled(true);

        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getInventory())) return;
        if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) return;

        int slot = event.getSlot();
        switch (slot) {
            case 10 -> {
                sound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                connectToServer(player, "skyblock");
            }
            case 11 -> {
                sound(player, Sound.BLOCK_STONE_BREAK, 1f, 1f);
                connectToServer(player, "survival");
            }
            case 38 -> {
                sound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                String url = plugin.getConfig().getString("server-selector.social.telegram.url", "https://github.com/amirwopi");
                String label = plugin.getConfig().getString("server-selector.social.telegram.chat-label", "Telegram » ");
                player.sendMessage(clickableLink(label, url, NamedTextColor.BLUE));
                player.closeInventory();
            }
            case 41 -> {
                sound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                String url = plugin.getConfig().getString("server-selector.social.teamspeak.url", "github.com/amirwopi");
                String label = plugin.getConfig().getString("server-selector.social.teamspeak.chat-label", "TeamSpeak » ");
                player.sendMessage(clickableLink(label, url, NamedTextColor.AQUA));
                player.closeInventory();
            }
            case 40 -> {
                sound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                String url = plugin.getConfig().getString("server-selector.social.discord.url", "https://github.com/amirwopi");
                String label = plugin.getConfig().getString("server-selector.social.discord.chat-label", "Discord » ");
                player.sendMessage(clickableLink(label, url, NamedTextColor.BLUE));
                player.closeInventory();
            }
            case 39 -> {
                sound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                String url = plugin.getConfig().getString("server-selector.social.website.url", "https://github.com/amirwopi");
                String label = plugin.getConfig().getString("server-selector.social.website.chat-label", "Website » ");
                player.sendMessage(clickableLink(label, url, NamedTextColor.YELLOW));
                player.closeInventory();
            }
            default -> {

                sound(player, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SelectorHolder) {
            event.setCancelled(true);
        }
    }

    private void connectToServer(Player player, String serverName) {
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Connect");
                out.writeUTF(serverName);
                byte[] payload = out.toByteArray();

                player.sendPluginMessage(plugin, "BungeeCord", payload);
                player.sendPluginMessage(plugin, "bungeecord:main", payload);
            } catch (Exception e) {
                String raw = plugin.getConfig().getString("messages.server-connect-failed", "Failed to connect to server: {server}");
                player.sendMessage(Component.text(raw.replace("{server}", serverName), NamedTextColor.RED));
            }
        });
    }

    private void sound(Player player, Sound sound, float vol, float pitch) {
        player.playSound(player.getLocation(), sound, vol, pitch);
    }

    private void playGuiOpenFx(Player player) {

        sound(player, Sound.UI_TOAST_IN, 0.8f, 1.0f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> sound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.2f), 2L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> sound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f), 6L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> sound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 1.8f), 10L);
    }

    private ItemStack skull(String texture, Component name, List<Component> lore, Material fallback) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            var profile = Bukkit.getServer().createProfile(UUID.randomUUID());
            profile.getProperties().add(new ProfileProperty("textures", texture));
            meta.setPlayerProfile(profile);
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            var loreList = new ArrayList<Component>();
            for (Component c : lore) loreList.add(c.decoration(TextDecoration.ITALIC, false));
            meta.lore(loreList);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private static class SelectorHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(this, 9, Component.text("Temp"));
        }
    }

    private Component clickableLink(String label, String url, NamedTextColor labelColor) {
        Component link = Component.text(url, NamedTextColor.GRAY)
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(Component.text("Click to open", NamedTextColor.GREEN)));
        return Component.text(label + " ", labelColor).append(link);
    }
}

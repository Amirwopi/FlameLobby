package ir.mrwopi.flameLobby.listeners;

import com.destroystokyo.paper.profile.ProfileProperty;
import ir.mrwopi.flameLobby.FlameLobby;
import ir.mrwopi.flameLobby.managers.MusicManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
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

import java.util.*;
import java.util.stream.IntStream;

public class MusicGUIListener implements Listener {

    private static final int GUI_SIZE = 54;
    private static final int TRACKS_PER_PAGE = 35;
    private static final int[] TRACK_SLOTS = IntStream.range(0, 35)
            .map(i -> 10 + (i % 7) + (i / 7) * 9)
            .toArray();

    private static final String ARROW_LEFT_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTE4NWM5N2RiYjgzNTNkZTY1MjY5OGQyNGI2NDMyN2I3OTNhM2YzMmE5OGJlNjdiNzE5ZmJlZGFiMzVlIn19fQ==";
    private static final String ARROW_RIGHT_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzFjMGVkZWRkNzExNWZjMWIyM2Q1MWNlOTY2MzU4YjI3MTk1ZGFmMjZlYmI2ZTQ1YTY2YzM0YzY5YzM0MDkxIn19fQ==";
    private static final String CLOSE_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGVmNWM3YjY5OGJmZjEyZmRiZTY2Mjk4ZDEwYWQyYjQzYzFlMWMwYmZmZjkwZDlmNWViNmVjNjMxMzhjNjE4In19fQ==";

    private final MusicManager musicManager;
    private final PlainTextComponentSerializer plainSerializer;

    public MusicGUIListener(FlameLobby plugin) {
        this.musicManager = plugin.getMusicManager();
        this.plainSerializer = PlainTextComponentSerializer.plainText();
    }

    public void openMusicGUI(Player player) {
        openMusicGUI(player, 0);
    }

    public void openMusicGUI(Player player, int page) {
        var tracks = musicManager.getAllTracks();

        if (tracks.isEmpty()) {
            player.sendMessage(Component.text("No music tracks available!", NamedTextColor.RED));
            return;
        }

        var totalPages = (int) Math.ceil((double) tracks.size() / TRACKS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));

        var holder = new MusicInventoryHolder(page);
        var title = Component.text("Music Player ", NamedTextColor.GOLD)
                .append(Component.text("- Page " + (page + 1) + "/" + totalPages, NamedTextColor.GRAY));

        var gui = Bukkit.createInventory(holder, GUI_SIZE, title);

        fillBorders(gui);
        fillTracks(gui, tracks, page, player);
        setNavigationButtons(gui, page, totalPages);
        setControlButtons(gui, player);

        player.openInventory(gui);
    }

    private void fillBorders(Inventory gui) {
        var border = createBorderItem();
        IntStream.range(0, GUI_SIZE)
                .filter(i -> i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8)
                .forEach(i -> gui.setItem(i, border));
    }

    private void fillTracks(Inventory gui, List<MusicManager.MusicTrack> tracks, int page, Player player) {
        if (tracks.isEmpty()) return;

        var startIndex = page * TRACKS_PER_PAGE;
        var endIndex = Math.min(startIndex + TRACKS_PER_PAGE, tracks.size());

        if (startIndex >= tracks.size()) return;

        var currentTrack = musicManager.getCurrentSongName(player);
        var isPlaying = musicManager.isPlaying(player);

        for (int i = startIndex; i < endIndex; i++) {
            var slotIndex = i - startIndex;
            if (slotIndex >= TRACK_SLOTS.length) break;

            var track = tracks.get(i);
            var isCurrentlyPlaying = isPlaying && currentTrack.equals(track.name());
            var item = createTrackItem(track, isCurrentlyPlaying);
            gui.setItem(TRACK_SLOTS[slotIndex], item);
        }
    }

    private void setNavigationButtons(Inventory gui, int page, int totalPages) {
        if (page > 0) {
            gui.setItem(47, createSkullWithTexture(
                    ARROW_LEFT_TEXTURE,
                    Component.text("◄ Previous Page", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                    List.of(Component.text("Go to page " + page, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
            ));
        }

        if (page < totalPages - 1) {
            gui.setItem(51, createSkullWithTexture(
                    ARROW_RIGHT_TEXTURE,
                    Component.text("Next Page ►", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                    List.of(Component.text("Go to page " + (page + 2), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
            ));
        }
    }

    private void setControlButtons(Inventory gui, Player player) {
        var isPlaying = musicManager.isPlaying(player);
        var isPaused = musicManager.isPaused(player);
        var isDisabled = musicManager.isMusicDisabled(player);

        if (isDisabled) {
            gui.setItem(48, createEnableMusicItem());
        } else {
            if (isPlaying || isPaused) {
                gui.setItem(48, createPauseResumeItem(isPaused));
            }
            gui.setItem(49, createStopItem());
        }

        gui.setItem(50, createSkullWithTexture(
                CLOSE_TEXTURE,
                Component.text("Close", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                List.of(Component.text("Close this menu", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
        ));
    }

    private ItemStack createSkullWithTexture(String texture, Component name, List<Component> lore) {
        var skull = new ItemStack(Material.PLAYER_HEAD);
        var meta = (SkullMeta) skull.getItemMeta();

        if (meta != null) {
            var profile = Bukkit.getServer().createProfile(UUID.randomUUID());
            profile.getProperties().add(new ProfileProperty("textures", texture));
            meta.setPlayerProfile(profile);
            meta.displayName(name);
            meta.lore(lore);
            skull.setItemMeta(meta);
        }

        return skull;
    }

    private ItemStack createTrackItem(MusicManager.MusicTrack track, boolean isPlaying) {
        var item = new ItemStack(track.discMaterial());
        var meta = item.getItemMeta();

        if (meta == null) return item;

        meta.displayName(Component.text("♪ " + track.name(), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        var lore = new ArrayList<Component>();

        if (isPlaying) {
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            lore.add(Component.text("▶ Currently Playing", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
        }

        lore.add(Component.text("Click to play this track", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createPauseResumeItem(boolean isPaused) {
        var item = new ItemStack(isPaused ? Material.LIME_DYE : Material.ORANGE_DYE);
        var meta = item.getItemMeta();

        if (meta != null) {
            var text = isPaused ? "▶ Resume" : "⏸ Pause";
            var color = isPaused ? NamedTextColor.GREEN : NamedTextColor.GOLD;

            meta.displayName(Component.text(text, color)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text(isPaused ? "Resume the music" : "Pause the music", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createStopItem() {
        var item = new ItemStack(Material.REDSTONE_BLOCK);
        var meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("⏹ Disable Music Forever", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Permanently disable music", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Music won't play when you join", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createEnableMusicItem() {
        var item = new ItemStack(Material.EMERALD_BLOCK);
        var meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("✓ Enable Music", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Click to enable music again", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Music will play when you join", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createBorderItem() {
        var item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MusicInventoryHolder holder)) return;

        event.setCancelled(true);

        var clickType = event.getClick();
        if (clickType != ClickType.LEFT && clickType != ClickType.RIGHT) {
            return;
        }

        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getInventory())) return;

        var clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        handleClick(player, clicked, holder.page());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof MusicInventoryHolder)) return;
        event.setCancelled(true);
    }

    private void handleClick(Player player, ItemStack clicked, int currentPage) {
        var type = clicked.getType();
        var meta = clicked.getItemMeta();

        if (meta == null) return;

        switch (type) {
            case PLAYER_HEAD -> handleHeadClick(player, clicked, currentPage);
            case ORANGE_DYE, LIME_DYE -> handlePauseResume(player, currentPage);
            case REDSTONE_BLOCK -> handlePermanentStop(player);
            case EMERALD_BLOCK -> handleEnableMusic(player, currentPage);
            default -> {
                if (type.name().startsWith("MUSIC_DISC_")) {
                    handleTrackSelection(player, clicked, currentPage);
                }
            }
        }
    }

    private void handleHeadClick(Player player, ItemStack clicked, int currentPage) {
        var meta = clicked.getItemMeta();
        if (meta == null) return;

        var displayName = meta.displayName();
        if (displayName == null) return;

        var name = plainSerializer.serialize(displayName);

        if (name.contains("Previous") || name.contains("◄")) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            openMusicGUI(player, currentPage - 1);
        } else if (name.contains("Next") || name.contains("►")) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            openMusicGUI(player, currentPage + 1);
        } else if (name.contains("Close")) {
            handleClose(player);
        }
    }

    private void handlePauseResume(Player player, int currentPage) {
        if (musicManager.isPaused(player)) {
            musicManager.resumeMusic(player);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 2.0f);
        } else {
            musicManager.pauseMusic(player);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
        }

        scheduleGUIRefresh(player, currentPage, 2L);
    }

    private void handlePermanentStop(Player player) {
        musicManager.permanentlyStopMusic(player);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
        player.closeInventory();
        player.sendMessage(Component.text("Music has been permanently disabled!", NamedTextColor.RED));
    }

    private void handleEnableMusic(Player player, int currentPage) {
        musicManager.enableMusic(player);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.sendMessage(Component.text("Music has been enabled!", NamedTextColor.GREEN));

        scheduleGUIRefresh(player, currentPage, 2L);
    }

    private void handleClose(Player player) {
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    private void handleTrackSelection(Player player, ItemStack clicked, int currentPage) {
        var meta = clicked.getItemMeta();
        if (meta == null) return;

        var displayName = meta.displayName();
        if (displayName == null) return;

        var name = plainSerializer.serialize(displayName);
        var trackName = name.replace("♪ ", "").trim();

        musicManager.playTrackByName(player, trackName);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.5f);

        scheduleGUIRefresh(player, currentPage, 5L);
    }

    private void scheduleGUIRefresh(Player player, int page, long delay) {
        Bukkit.getScheduler().runTaskLater(
                musicManager.getPlugin(),
                () -> {
                    if (player.isOnline() &&
                            player.getOpenInventory().getTopInventory().getHolder() instanceof MusicInventoryHolder) {
                        openMusicGUI(player, page);
                    }
                },
                delay
        );
    }

    private record MusicInventoryHolder(int page) implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(this, 9, Component.text("Temporary Inventory"));
        }
    }
}
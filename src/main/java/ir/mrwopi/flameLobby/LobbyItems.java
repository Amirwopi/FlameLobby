package ir.mrwopi.flameLobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class LobbyItems {

    private LobbyItems() {
    }

    public static NamespacedKey key(FlameLobby plugin, String id) {
        return new NamespacedKey(plugin, id);
    }

    public static boolean hasTag(FlameLobby plugin, ItemStack item, String id) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte val = pdc.get(key(plugin, id), PersistentDataType.BYTE);
        return val != null && val == (byte) 1;
    }

    public static ItemStack tag(FlameLobby plugin, ItemStack item, String id) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key(plugin, id), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack parkourStartItem(FlameLobby plugin) {
        String matRaw = plugin.getConfig().getString("lobby-items.items.parkour-start.material", "LIME_DYE");
        Material mat = Material.LIME_DYE;
        if (matRaw != null) {
            try {
                mat = Material.valueOf(matRaw);
            } catch (IllegalArgumentException ignored) {
            }
        }

        ItemStack it = new ItemStack(mat);
        String name = plugin.getConfig().getString("lobby-items.items.parkour-start.name", "§aParkour §7(Click)");
        List<String> loreRaw = plugin.getConfig().getStringList("lobby-items.items.parkour-start.lore");
        applyDisplay(it, name, loreRaw);
        it.addUnsafeEnchantment(Enchantment.MENDING, 1);
        it.editMeta(m -> m.addItemFlags(ItemFlag.HIDE_ENCHANTS));
        return tag(plugin, it, "parkour_start");
    }

    public static ItemStack elytraItem(FlameLobby plugin) {
        ItemStack it = new ItemStack(Material.ELYTRA);
        String name = plugin.getConfig().getString("lobby-items.items.elytra.name", "§bElytra");
        List<String> loreRaw = plugin.getConfig().getStringList("lobby-items.items.elytra.lore");
        applyDisplay(it, name, loreRaw);
        it.editMeta(m -> {
            m.setUnbreakable(true);
            m.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        });
        return tag(plugin, it, "lobby_elytra");
    }

    public static ItemStack fireworksItem(FlameLobby plugin) {
        int amount = Math.max(1, Math.min(64, plugin.getConfig().getInt("lobby-items.items.fireworks.amount", 64)));
        ItemStack it = new ItemStack(Material.FIREWORK_ROCKET, amount);
        String name = plugin.getConfig().getString("lobby-items.items.fireworks.name", "§eFireworks");
        List<String> loreRaw = plugin.getConfig().getStringList("lobby-items.items.fireworks.lore");
        applyDisplay(it, name, loreRaw);
        return tag(plugin, it, "lobby_fireworks");
    }

    public static void applyDisplay(ItemStack item, String nameLegacy, List<String> loreLegacy) {
        item.editMeta(meta -> {
            if (nameLegacy != null && !nameLegacy.isBlank()) {
                Component name = LegacyComponentSerializer.legacySection().deserialize(nameLegacy);
                meta.displayName(name);
            }
            if (loreLegacy != null && !loreLegacy.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreLegacy) {
                    if (line == null) continue;
                    lore.add(LegacyComponentSerializer.legacySection().deserialize(line));
                }
                meta.lore(lore);
            }
        });
    }
}

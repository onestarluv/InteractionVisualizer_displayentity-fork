package com.loohp.interactionvisualizer;

import com.loohp.interactionvisualizer.api.InteractionVisualizerAPI;
import com.loohp.interactionvisualizer.objectholders.EntryKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public final class ToggleGui implements Listener {
    static final int INVENTORY_SIZE = 45;
    static final int PAGE_SIZE = INVENTORY_SIZE - 9;

    static int pageCount(int count) { return Math.max(1, (count + PAGE_SIZE - 1) / PAGE_SIZE); }

    public static void open(Player player) {
        Session session = new Session(player.getUniqueId(), List.copyOf(InteractionVisualizerAPI.getRegisteredEntries()));
        render(player, session);
        player.openInventory(session.inventory);
    }

    private static void render(Player player, Session session) {
        session.inventory.clear();
        int start = session.page * PAGE_SIZE;
        for (int slot = 0; slot < PAGE_SIZE && start + slot < session.entries.size(); slot++) {
            EntryKey entry = session.entries.get(start + slot);
            boolean enabled = Toggle.hasAnyEnabled(player, entry);
            session.inventory.setItem(slot, button(icon(entry), InteractionVisualizerAPI.getUserFriendlyName(entry), enabled,
                    enabled ? "클릭하면 모든 효과를 끕니다." : "클릭하면 모든 효과를 켭니다."));
        }
        ItemStack filler = button(Material.GRAY_STAINED_GLASS_PANE, " ", false, "");
        for (int slot = PAGE_SIZE; slot < INVENTORY_SIZE; slot++) session.inventory.setItem(slot, filler);
        if (session.page > 0) session.inventory.setItem(PAGE_SIZE, button(Material.ARROW, "이전 페이지", false, "클릭하여 이전 페이지로 이동합니다."));
        session.inventory.setItem(PAGE_SIZE + 4, button(Material.LEVER, "전체 효과", Toggle.hasAnyEnabled(player, session.entries.toArray(new EntryKey[0])),
                "클릭하면 모든 항목을 함께 전환합니다.", "현재 페이지: " + (session.page + 1) + "/" + pageCount(session.entries.size())));
        if (session.page + 1 < pageCount(session.entries.size())) session.inventory.setItem(INVENTORY_SIZE - 1, button(Material.ARROW, "다음 페이지", false, "클릭하여 다음 페이지로 이동합니다."));
    }

    private static ItemStack button(Material material, String name, boolean enabled, String... lines) {
        if (!material.isItem()) material = Material.PAPER;
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        if (!name.equals(" ") && material != Material.ARROW) lore.add(Component.text(enabled ? "상태: 켜짐" : "상태: 꺼짐", enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        for (String line : lines) lore.add(Component.text(line, NamedTextColor.GRAY).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        meta.lore(lore);
        if (enabled) meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    static Material icon(EntryKey entry) {
        String key = entry.toString().substring(entry.toString().indexOf(':') + 1).toUpperCase(Locale.ROOT);
        return switch (key) {
            case "ENCHANTMENT_TABLE" -> Material.ENCHANTING_TABLE;
            case "DOUBLE_CHEST" -> Material.CHEST;
            case "BANNER" -> Material.WHITE_BANNER;
            case "ITEM" -> Material.DIAMOND;
            case "VILLAGER" -> Material.VILLAGER_SPAWN_EGG;
            default -> {
                Material material = Material.matchMaterial(key);
                yield material == null || material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR ? Material.PAPER : material;
            }
        };
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Session session)) return;
        event.setCancelled(true); // Includes shift clicks, number keys, double clicks and bottom-inventory moves.
        if (!(event.getWhoClicked() instanceof Player player) || !session.owner.equals(player.getUniqueId())) return;
        if (!player.hasPermission("interactionvisualizer.toggle")) { player.closeInventory(); return; }
        if (!event.getClick().isLeftClick() && !event.getClick().isRightClick()) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= INVENTORY_SIZE) return;
        if (slot == PAGE_SIZE && session.page > 0) session.page--;
        else if (slot == INVENTORY_SIZE - 1 && session.page + 1 < pageCount(session.entries.size())) session.page++;
        else if (slot == PAGE_SIZE + 4 && !session.entries.isEmpty()) {
            Toggle.toggleAll(player, player, null, "전체", false, session.entries.toArray(new EntryKey[0]));
        } else if (slot < PAGE_SIZE && session.page * PAGE_SIZE + slot < session.entries.size()) {
            EntryKey entry = session.entries.get(session.page * PAGE_SIZE + slot);
            Toggle.toggleAll(player, player, null, InteractionVisualizerAPI.getUserFriendlyName(entry), false, entry);
        } else return;
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        render(player, session);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Session
                && event.getRawSlots().stream().anyMatch(slot -> slot < INVENTORY_SIZE)) event.setCancelled(true);
    }

    private static final class Session implements InventoryHolder {
        final UUID owner;
        final List<EntryKey> entries;
        final Inventory inventory;
        int page;
        Session(UUID owner, List<EntryKey> entries) {
            this.owner = owner;
            this.entries = entries;
            this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE, Component.text("시각 효과 설정"));
        }
        @Override public Inventory getInventory() { return inventory; }
    }
}

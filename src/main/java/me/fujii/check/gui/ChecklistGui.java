package me.fujii.check.gui;

import me.fujii.check.data.ChecklistStore;
import me.fujii.check.util.ItemChecklistTagger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class ChecklistGui implements Listener {

    private static final int SIZE = 54;              // 6 rows
    private static final int ITEMS_PER_PAGE = 45;    // 0-44
    private static final int SLOT_PREV = 45;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_NEXT = 53;

    private static final String META_PAGE = "itemchecklist_page";
    private static final String TITLE_PREFIX = "Item Checklist ";

    private final JavaPlugin plugin;
    private final ChecklistStore store;

    private final List<Material> allItems;

    public static final class ChecklistHolder implements InventoryHolder {
        private final int page;
        public ChecklistHolder(int page) { this.page = page; }
        public int getPage() { return page; }
        @Override public org.bukkit.inventory.Inventory getInventory() { return null; } // 使わない
    }

    public ChecklistGui(JavaPlugin plugin, ChecklistStore store) {
        this.plugin = plugin;
        this.store = store;

        this.allItems = new ArrayList<>();
        for (Material m : Material.values()) {
            if (m.isItem() && m != Material.AIR) allItems.add(m);
        }
        allItems.sort(Comparator.comparing(Enum::name));
    }

    public void open(Player player, int page) {
        int maxPage = Math.max(0, (allItems.size() - 1) / ITEMS_PER_PAGE);
        int p = Math.max(0, Math.min(page, maxPage));

        Inventory inv = Bukkit.createInventory(new ChecklistHolder(p), SIZE,
                TITLE_PREFIX + "(" + (p + 1) + "/" + (maxPage + 1) + ")");

        int start = p * ITEMS_PER_PAGE;
        int end = Math.min(allItems.size(), start + ITEMS_PER_PAGE);

        for (int i = start; i < end; i++) {
            Material m = allItems.get(i);
            boolean checked = store.isChecked(m);

            ItemStack icon = new ItemStack(m);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                Component name =
                        Component.text(checked ? "[@:✓] " : "[@: ] ", checked ? NamedTextColor.GREEN : NamedTextColor.RED)
                                .append(Component.translatable(m.translationKey()));

                meta.displayName(name);
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(checked ? "チェック済み" : "未チェック", checked ? NamedTextColor.GREEN : NamedTextColor.RED));
                lore.add(Component.text("クリックで切り替え", NamedTextColor.GRAY));
                meta.lore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(i - start, icon);
        }

        inv.setItem(SLOT_PREV, button(Material.ARROW, ChatColor.YELLOW + "Prev"));
        inv.setItem(SLOT_NEXT, button(Material.ARROW, ChatColor.YELLOW + "Next"));

        inv.setItem(SLOT_INFO, button(Material.BOOK,
                ChatColor.AQUA + "Checked: " + store.getCheckedCount() + " / " + allItems.size()));

        player.openInventory(inv);
        player.setMetadata(META_PAGE, new FixedMetadataValue(plugin, p));
    }

    private ItemStack button(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        // ★ holderでこのGUIか判定（TopInventoryだけを見る）
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof ChecklistHolder holder)) return;

        // ★ このGUIが開いている間は “一切の操作” を禁止（確実に掴めない）
        e.setCancelled(true);

        // Top側以外（自分のインベントリ）を触るのも禁止にしたくないなら、ここで return せず下の条件にする
        // 今回は「掴めない」の切り分け優先で全面禁止にしています

        int raw = e.getRawSlot();
        if (raw < 0 || raw >= top.getSize()) return; // Top以外はここで終了（全面禁止ならこの行すら不要）

        int page = holder.getPage();

        if (raw == SLOT_PREV) {
            open(player, page - 1);
            return;
        }
        if (raw == SLOT_NEXT) {
            open(player, page + 1);
            return;
        }

        if (raw >= 0 && raw < ITEMS_PER_PAGE) {
            int index = page * ITEMS_PER_PAGE + raw;
            if (index < 0 || index >= allItems.size()) return;

            Material m = allItems.get(index);
            store.toggle(m);
            store.save();

            open(player, page);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof ChecklistHolder)) return;

        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCreative(InventoryCreativeEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof ChecklistHolder)) return;

        // クリエイティブの編集は強いので確実に止める
        e.setCancelled(true);
        e.setResult(Event.Result.DENY);

        // クライアント側が掴んでしまった表示を戻す
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.setItemOnCursor(null);
            player.updateInventory();
        });
    }

}

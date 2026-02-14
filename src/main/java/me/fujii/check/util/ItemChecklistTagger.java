package me.fujii.check.util;

import me.fujii.check.data.ChecklistStore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class ItemChecklistTagger {

    // Loreの識別子（これがある行だけを安全に追加/削除する）
    private static final String LORE_PREFIX_PLAIN = "[Checklist]";
    private static final String LORE_LINE_CHECKED = ChatColor.GREEN + LORE_PREFIX_PLAIN + " ✓";
    private static final String LORE_LINE_UNCHECKED = ChatColor.RED + LORE_PREFIX_PLAIN + " ✗"; // showUnchecked=trueの時だけ使われる

    // PDCキー（このアイテムにチェックリスト行を付けたかの目印）
    private final NamespacedKey keyTagged;

    private final JavaPlugin plugin;
    private final ChecklistStore store;

    // 未チェック表示も付けたいなら true（表示がうるさくなるのでデフォはfalse推奨）
    private final boolean showUnchecked;

    public ItemChecklistTagger(JavaPlugin plugin, ChecklistStore store, boolean showUnchecked) {
        this.plugin = plugin;
        this.store = store;
        this.showUnchecked = showUnchecked;
        this.keyTagged = new NamespacedKey(plugin, "checklist_tagged");
    }

    public void applyToItem(ItemStack item) {
        if (item == null) return;
        Material type = item.getType();
        if (type == Material.AIR) return;
        if (!type.isItem()) return;

        boolean checked = store.isChecked(type);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        // 既存のチェックリスト行を削除（prefix一致で安全に消す）
        lore.removeIf(line -> {
            if (line == null) return false;
            String stripped = ChatColor.stripColor(line);
            return stripped != null && stripped.startsWith(LORE_PREFIX_PLAIN);
        });

        if (checked) {
            lore.add(0, LORE_LINE_CHECKED);
            meta.getPersistentDataContainer().set(keyTagged, PersistentDataType.BYTE, (byte) 1);
        } else {
            if (showUnchecked) {
                lore.add(0, LORE_LINE_UNCHECKED);
                meta.getPersistentDataContainer().set(keyTagged, PersistentDataType.BYTE, (byte) 1);
            } else {
                // 未チェックは何も付けない（付いてたら消えるだけ）
                meta.getPersistentDataContainer().remove(keyTagged);
            }
        }

        if (lore.isEmpty()) {
            meta.setLore(null);
        } else {
            meta.setLore(lore);
        }

        // 見た目用：余計な表示を隠す（任意）
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
    }

    public void applyToInventory(org.bukkit.inventory.Inventory inv) {
        if (inv == null) return;
        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;
            applyToItem(item);
        }
    }
}

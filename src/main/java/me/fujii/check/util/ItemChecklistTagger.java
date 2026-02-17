package me.fujii.check.util;

import me.fujii.check.data.ChecklistStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.ShulkerBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 付与(チェック表示)と、cleanup(痕跡除去/スタック復旧)を扱うユーティリティ
 */
public final class ItemChecklistTagger {

    // Loreの識別子（これがある行だけを安全に追加/削除する）
    private static final String LORE_PREFIX_PLAIN = "[Checklist] ";
    private static final String LORE_LINE_CHECKED = ChatColor.GREEN + LORE_PREFIX_PLAIN + "✓ SORTABLE";
    private static final String LORE_LINE_UNCHECKED = ChatColor.RED + LORE_PREFIX_PLAIN + "✖ NOT CHECKED";

    // テストで付けたチェックマーク（testmark）
    private static final String TESTMARK_PREFIX = "✓ ";

    // 以前あなたが付けてた「名前プレフィックス」(例)
    private static final String[] NAME_PREFIXES = {
            "[@:✓] ", "[@:✓]",
            "[@: ] ", "[@: ]",
            "[Checklist] ✓ ", "[Checklist] ✓"
    };

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    // PDCキー（このアイテムにチェックリスト行を付けたかの目印）
    private final NamespacedKey keyTagged;
    private final String pluginNamespace;

    private final JavaPlugin plugin;
    private final ChecklistStore store;

    // 未チェック表示も付けたいなら true（表示がうるさくなるのでデフォはfalse推奨）
    private final boolean showUnchecked;

    public ItemChecklistTagger(JavaPlugin plugin, ChecklistStore store, boolean showUnchecked) {
        this.plugin = plugin;
        this.store = store;
        this.showUnchecked = showUnchecked;
        this.keyTagged = new NamespacedKey(plugin, "checklist_tagged");
        this.pluginNamespace = this.keyTagged.getNamespace();
    }

    /**
     * 付与：このメソッドは「基本 displayName を変更しない」。
     * 過去に付けたプレフィックスが残っていたら剥がすだけ行う。
     */
    public void applyToItem(ItemStack item) {
        if (item == null) return;
        Material type = item.getType();
        if (type == Material.AIR) return;
        if (!type.isItem()) return;

        boolean checked = store.isChecked(type);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // ✅ 過去の「名前プレフィックス」が残っていたら剥がす
        stripOldNamePrefixIfNeeded(meta);

        // lore(legacy String) を安全に編集
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
                meta.getPersistentDataContainer().remove(keyTagged);
            }
        }

        meta.setLore(lore.isEmpty() ? null : lore);

        // 見た目用（昔の版で付けてた可能性がある）
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
    }

    public void applyToInventory(org.bukkit.inventory.Inventory inv) {
        if (inv == null) return;
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null) continue;
            applyToItem(it);
        }
        inv.setContents(contents);
    }

    /**
     * ★スタック復旧向け：このアイテムを「同一視できる状態」へ正規化し、必要なら “素のItemStack” を返す。
     * 返り値を必ずスロット/エンティティにセットしてください。
     */
    public ItemStack normalizeForStacking(ItemStack item) {
        return normalizeForStacking(item, 0);
    }

    private ItemStack normalizeForStacking(ItemStack item, int depth) {
        if (item == null) return null;
        Material type = item.getType();
        if (type == Material.AIR) return item;
        if (!type.isItem()) return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (depth < 3) {
            normalizeContainedInventoryIfShulkerBoxItem(meta, depth);
        }

        // 1) 表示名プレフィックス（過去分 + testmark）を剥がす
        stripOldNamePrefixIfNeeded(meta);
        stripTestMarkPrefixIfNeeded(meta);

        // 2) Lore（legacy String）からチェック行だけ削除
        if (meta.hasLore()) {
            List<String> lore = new ArrayList<>(meta.getLore());
            lore.removeIf(line -> {
                if (line == null) return false;
                String stripped = ChatColor.stripColor(line);
                return stripped != null && stripped.startsWith(LORE_PREFIX_PLAIN);
            });
            meta.setLore(lore.isEmpty() ? null : lore);
        }

        // 3) Lore（Component）が入ってる環境でも削れるように（Paper）
        try {
            List<Component> loreC = meta.lore();
            if (loreC != null) {
                List<Component> filtered = new ArrayList<>();
                for (Component c : loreC) {
                    String plain = PLAIN.serialize(c);
                    if (plain == null) {
                        filtered.add(c);
                        continue;
                    }
                    // checklist行だけ落とす
                    if (plain.startsWith(LORE_PREFIX_PLAIN) || plain.startsWith("[Checklist]")) continue;
                    filtered.add(c);
                }
                meta.lore(filtered.isEmpty() ? null : filtered);
            }
        } catch (NoSuchMethodError ignored) {
            // Spigot互換用（Paper以外）
        }

        // 4) ItemFlag（昔付けた HIDE_ATTRIBUTES が残ってるとスタック阻害）
        meta.removeItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // 5) PDC：namespace違いでも checklist/testmark 系を掃除
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Set<NamespacedKey> keys = new HashSet<>(pdc.getKeys());
        for (NamespacedKey k : keys) {
            String kk = k.getKey();
            if (pluginNamespace.equals(k.getNamespace())
                    || "checklist_tagged".equals(kk)
                    || kk.startsWith("checklist")
                    || kk.startsWith("testmark")
                    || "testmark_active".equals(kk)) {
                pdc.remove(k);
            }
        }
        // 念のため現行の keyTagged も
        pdc.remove(keyTagged);

        // 一旦このmetaを反映（この時点で “余計な差分” は落ちてる）
        item.setItemMeta(meta);

        // 6) ここが重要：最大スタック数>1 のアイテムで、メタが実質空なら “素のItemStack” に作り直す
        //    (これで「見えない差分」が完全に消えてスタックが復旧する)
        if (type.getMaxStackSize() > 1) {
            ItemMeta m2 = item.getItemMeta();
            if (m2 != null && isEffectivelyEmptyMeta(m2)) {
                return new ItemStack(type, item.getAmount());
            }
        }

        return item;
    }

    /**
     * チェックリスト関連の内部データを「現在の形式」で付け直す（古い/壊れたPDCやLoreを掃除してから再付与）。
     * アイテムの付加価値（エンチャント等）は維持し、チェックリスト用のLore/PDCだけを正規化する。
     */
    public void rewriteInventoryContents(org.bukkit.inventory.Inventory inv) {
        if (inv == null) return;
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null) continue;
            rewriteChecklistData(it);
            contents[i] = it;
        }
        inv.setContents(contents);
    }

    public void rewriteChecklistData(ItemStack item) {
        rewriteChecklistData(item, 0);
    }

    private void rewriteChecklistData(ItemStack item, int depth) {
        if (item == null) return;
        Material type = item.getType();
        if (type == Material.AIR) return;
        if (!type.isItem()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (depth < 3) {
            rewriteContainedInventoryIfShulkerBoxItem(meta, depth);
        }

        stripOldNamePrefixIfNeeded(meta);
        stripTestMarkPrefixIfNeeded(meta);

        // checklist Loreは一旦除去（applyToItemで付け直す）
        if (meta.hasLore()) {
            List<String> lore = new ArrayList<>(meta.getLore());
            lore.removeIf(line -> {
                if (line == null) return false;
                String stripped = ChatColor.stripColor(line);
                return stripped != null && stripped.startsWith(LORE_PREFIX_PLAIN);
            });
            meta.setLore(lore.isEmpty() ? null : lore);
        }

        // PDC: このプラグイン由来のキーは一旦全部消す（古いキー名でも除去できる）
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Set<NamespacedKey> keys = new HashSet<>(pdc.getKeys());
        for (NamespacedKey k : keys) {
            if (pluginNamespace.equals(k.getNamespace())) {
                pdc.remove(k);
                continue;
            }
            String kk = k.getKey();
            if (kk.startsWith("checklist") || kk.startsWith("testmark") || "testmark_active".equals(kk)) {
                pdc.remove(k);
            }
        }

        item.setItemMeta(meta);

        // 現在の仕様で付与し直す
        applyToItem(item);
    }

    public void normalizeInventoryForStacking(org.bukkit.inventory.Inventory inv) {
        if (inv == null) return;
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null) continue;
            ItemStack normalized = normalizeForStacking(it);
            contents[i] = normalized;
        }
        inv.setContents(contents);
    }

    /**
     * “実質空メタ”判定：これが true なら作り直してOK（スタック復旧用）
     * ※ここでは「スタックできる普通のアイテム」向けに、危ない要素が少しでもあれば空扱いしない
     */
    private void normalizeContainedInventoryIfShulkerBoxItem(ItemMeta meta, int depth) {
        if (!(meta instanceof BlockStateMeta bsm)) return;
        org.bukkit.block.BlockState bs = bsm.getBlockState();
        if (!(bs instanceof ShulkerBox box)) return;

        org.bukkit.inventory.Inventory inv = box.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null) continue;
            contents[i] = normalizeForStacking(it, depth + 1);
        }
        inv.setContents(contents);

        bsm.setBlockState(box);
    }

    private void rewriteContainedInventoryIfShulkerBoxItem(ItemMeta meta, int depth) {
        if (!(meta instanceof BlockStateMeta bsm)) return;
        org.bukkit.block.BlockState bs = bsm.getBlockState();
        if (!(bs instanceof ShulkerBox box)) return;

        org.bukkit.inventory.Inventory inv = box.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null) continue;
            rewriteChecklistData(it, depth + 1);
            contents[i] = it;
        }
        inv.setContents(contents);

        bsm.setBlockState(box);
    }

    private boolean isEffectivelyEmptyMeta(ItemMeta meta) {
        if (meta.hasDisplayName()) return false;
        if (meta.hasLore()) return false;

        try {
            Component dn = meta.displayName();
            if (dn != null) return false;
        } catch (NoSuchMethodError ignored) {}

        try {
            List<Component> loreC = meta.lore();
            if (loreC != null && !loreC.isEmpty()) return false;
        } catch (NoSuchMethodError ignored) {}

        if (!meta.getEnchants().isEmpty()) return false;
        if (meta.hasCustomModelData()) return false;
        if (meta.isUnbreakable()) return false;

        if (meta.getAttributeModifiers() != null && !meta.getAttributeModifiers().isEmpty()) return false;
        if (!meta.getItemFlags().isEmpty()) return false;
        if (!meta.getPersistentDataContainer().getKeys().isEmpty()) return false;

        return true;
    }

    /**
     * すでに付いた「名前プレフィックス」だけを剥がす。
     * displayName自体は、プレフィックスが付いていた場合だけ null に戻す（=デフォルト名に戻る）。
     */
    private void stripOldNamePrefixIfNeeded(ItemMeta meta) {
        // legacy
        if (meta.hasDisplayName()) {
            String plain = ChatColor.stripColor(meta.getDisplayName());
            if (plain != null && hasAnyPrefix(plain)) {
                meta.setDisplayName(null);
            }
        }

        // component (Paper)
        try {
            Component dn = meta.displayName();
            if (dn != null) {
                String plain = PLAIN.serialize(dn);
                if (plain != null && hasAnyPrefix(plain)) {
                    meta.displayName(null);
                }
            }
        } catch (NoSuchMethodError ignored) {
        }
    }

    private void stripTestMarkPrefixIfNeeded(ItemMeta meta) {
        // legacy
        if (meta.hasDisplayName()) {
            String plain = ChatColor.stripColor(meta.getDisplayName());
            if (plain != null && plain.startsWith(TESTMARK_PREFIX)) {
                meta.setDisplayName(null);
            }
        }
        // component (Paper)
        try {
            Component dn = meta.displayName();
            if (dn != null) {
                String plain = PLAIN.serialize(dn);
                if (plain != null && plain.startsWith(TESTMARK_PREFIX)) {
                    meta.displayName(null);
                }
            }
        } catch (NoSuchMethodError ignored) {
        }
    }

    private boolean hasAnyPrefix(String plain) {
        if (plain == null) return false;
        if (plain.startsWith("[@:")) return true;
        if (plain.startsWith("[Checklist]")) return true;
        for (String prefix : NAME_PREFIXES) {
            if (plain.startsWith(prefix)) return true;
        }
        return false;
    }
}

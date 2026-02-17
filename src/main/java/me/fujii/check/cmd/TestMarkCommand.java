package me.fujii.check.cmd;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class TestMarkCommand implements CommandExecutor {

    private static final String PREFIX = "✓ ";

    private final JavaPlugin plugin;
    private final NamespacedKey kActive;
    private final NamespacedKey kMode;
    private final NamespacedKey kOrigComponentJson;
    private final NamespacedKey kOrigLegacy;

    public TestMarkCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.kActive = new NamespacedKey(plugin, "testmark_active");
        this.kMode = new NamespacedKey(plugin, "testmark_mode"); // component / legacy / none
        this.kOrigComponentJson = new NamespacedKey(plugin, "testmark_orig_component_json");
        this.kOrigLegacy = new NamespacedKey(plugin, "testmark_orig_legacy");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player p)) {
            sender.sendMessage("プレイヤーのみ実行できます。");
            return true;
        }

        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            p.sendMessage("手にアイテムを持ってください。");
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            p.sendMessage("このアイテムは変更できません。");
            return true;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // すでに付与済みなら「元に戻す」
        if (pdc.has(kActive, PersistentDataType.BYTE)) {
            meta.displayName(null);
            meta.setDisplayName(null);
            meta.setLore(null);
            meta.removeItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

// testmarkのPDCを消す
            pdc.remove(kActive);
            pdc.remove(kMode);
            pdc.remove(kOrigComponentJson);
            pdc.remove(kOrigLegacy);

// 念のため：checklist系が混ざってたら消す（テスト中の事故対策）
            for (var k : new java.util.HashSet<>(pdc.getKeys())) {
                String kk = k.getKey();
                if (kk.startsWith("checklist") || kk.equals("checklist_tagged")) {
                    pdc.remove(k);
                }
            }

            item.setItemMeta(meta);
            p.updateInventory();
            p.sendMessage("✓ を外して完全初期化しました（スタック復活）。");
            return true;
        }

        // ---- ここから「付ける」処理 ----

        // 元の名前を保存（component優先）
        Component origComponent = meta.displayName();
        if (origComponent != null) {
            pdc.set(kMode, PersistentDataType.STRING, "component");
            pdc.set(kOrigComponentJson, PersistentDataType.STRING,
                    GsonComponentSerializer.gson().serialize(origComponent));
        } else if (meta.hasDisplayName()) {
            pdc.set(kMode, PersistentDataType.STRING, "legacy");
            pdc.set(kOrigLegacy, PersistentDataType.STRING, meta.getDisplayName());
        } else {
            pdc.set(kMode, PersistentDataType.STRING, "none");
        }

        // 付与用のベース名
        Component baseName;
        if (origComponent != null) {
            baseName = origComponent;
        } else if (meta.hasDisplayName()) {
            baseName = Component.text(meta.getDisplayName());
        } else {
            baseName = Component.translatable(item.getType().translationKey());
        }

        // Componentで付与（legacyは混在防止のためクリア）
        meta.setDisplayName(null);
        meta.displayName(Component.text(PREFIX).append(baseName));

        pdc.set(kActive, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        p.updateInventory();
        p.sendMessage("✓ を付けました。");

        return true;
    }
}

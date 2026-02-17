package me.fujii.check.listener;

import me.fujii.check.data.ChecklistStore;
import me.fujii.check.util.ItemChecklistTagger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class CraftResultTagListener implements Listener {

    private final JavaPlugin plugin;
    private final ChecklistStore store;

    public CraftResultTagListener(JavaPlugin plugin, ChecklistStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    // ★クラフト結果(プレビュー)を作るタイミングで、結果をタグ付きに差し替える
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        ItemStack result = e.getInventory().getResult();
        if (result == null) return;

    }

    // ★取り出し時の保険（クリック/シフトクリックの取りこぼし対策）
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        ItemStack current = e.getCurrentItem(); // 結果スロットのアイテム
        if (current == null) return;
    }
}

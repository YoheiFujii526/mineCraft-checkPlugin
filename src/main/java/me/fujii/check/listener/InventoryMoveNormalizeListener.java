package me.fujii.check.listener;

import me.fujii.check.data.ChecklistStore;
import me.fujii.check.gui.ChecklistGui;
import me.fujii.check.util.ItemChecklistTagger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class InventoryMoveNormalizeListener implements Listener {

    private final JavaPlugin plugin;
    private final ChecklistStore store;

    public InventoryMoveNormalizeListener(JavaPlugin plugin, ChecklistStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    // プレイヤー操作（チェスト⇄インベントリ）
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        Inventory top = e.getView().getTopInventory();
        // チェックリストGUIは対象外
        if (top.getHolder() instanceof ChecklistGui.ChecklistHolder) return;

        int raw = e.getRawSlot();
        boolean clickTop = raw >= 0 && raw < top.getSize();

        InventoryAction act = e.getAction();

        // SHIFTクリック（MOVE_TO_OTHER_INVENTORY）が一番重要
        if (act == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            ItemStack cur = e.getCurrentItem();
            if (cur == null) return;
            return;
        }
    }

    // ドラッグでコンテナに入れる時も剥がす
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (top.getHolder() instanceof ChecklistGui.ChecklistHolder) return;

        // ドラッグで top に触れるなら、カーソル（oldCursor）を剥がす
        boolean touchesTop = e.getRawSlots().stream().anyMatch(slot -> slot >= 0 && slot < top.getSize());
        if (!touchesTop) return;

        ItemStack old = e.getOldCursor();
        if (old == null) return;

        e.setCursor(old);
    }

    // ホッパー移動：常に剥がす（ここが仕分け機対策の要）
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent e) {
        ItemStack item = e.getItem();
        if (item == null) return;

        e.setItem(item);
    }
}

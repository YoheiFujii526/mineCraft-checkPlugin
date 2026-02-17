package me.fujii.check.listener;

import me.fujii.check.gui.ChecklistGui;
import me.fujii.check.util.ItemChecklistTagger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class ContainerInventoryTagListener implements Listener {

    private final JavaPlugin plugin;

    public ContainerInventoryTagListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // チェストなどを開いたタイミングで反映（次回開くと必ず反映される）
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;

        // 自前GUI(チェックリスト)には触らない
        if (e.getInventory().getHolder() instanceof ChecklistGui.ChecklistHolder) return;
    }

    // チェスト⇄インベントリの移動で「タグ付き/未タグ」が混在しないよう保険
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        Inventory top = e.getView().getTopInventory();
        if (top.getHolder() instanceof ChecklistGui.ChecklistHolder) return;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        Inventory top = e.getView().getTopInventory();
        if (top.getHolder() instanceof ChecklistGui.ChecklistHolder) return;
    }
}

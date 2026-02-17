package me.fujii.check.listener;

import me.fujii.check.ui.ChecklistSidebar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public final class ChecklistSidebarListener implements Listener {

    private final ChecklistSidebar sidebar;

    public ChecklistSidebarListener(ChecklistSidebar sidebar) {
        this.sidebar = sidebar;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        sidebar.attach(p);
        sidebar.update(p, p.getInventory().getItemInMainHand());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        sidebar.detach(e.getPlayer());
    }

    // 手に持つアイテムが変わったら更新
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        ItemStack it = p.getInventory().getItem(e.getNewSlot());
        sidebar.update(p, it);
    }

    // インベントリでクリックしたアイテムを表示（ホバーは取れないけど、クリックなら取れる）
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        sidebar.update(p, e.getCurrentItem());
    }
}

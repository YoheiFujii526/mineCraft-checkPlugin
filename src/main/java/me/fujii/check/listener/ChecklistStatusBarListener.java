package me.fujii.check.listener;

import me.fujii.check.data.ChecklistStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChecklistStatusBarListener implements Listener {

    private final JavaPlugin plugin;
    private final ChecklistStore store;

    public ChecklistStatusBarListener(JavaPlugin plugin, ChecklistStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    private void show(Player p, ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.getType().isItem()) {
            p.sendActionBar(Component.empty());
            return;
        }
        boolean checked = store.isChecked(item.getType());

        Component msg = checked
                ? Component.text("✓ SORTABLE", NamedTextColor.GREEN)
                : Component.text("✖ NOT CHECKED", NamedTextColor.RED);

        p.sendActionBar(msg);
    }

    // 参加時：現在の手持ちで表示
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            show(p, p.getInventory().getItemInMainHand());
        });
    }

    // ホットバー持ち替え時：そのアイテムで表示
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItem(e.getNewSlot());
        show(p, item);
    }

    // インベントリクリック時：クリックしたアイテム or カーソル上のアイテムで表示
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        // クリック処理後の確定状態で見たいので次tick
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack cursor = p.getItemOnCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                show(p, cursor); // 掴んでるならそれを優先
            } else {
                show(p, e.getCurrentItem()); // そうでなければクリック対象
            }
        });
    }

    // ドラッグ時：カーソル上のアイテムで表示
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInvDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            show(p, p.getItemOnCursor());
        });
    }
}

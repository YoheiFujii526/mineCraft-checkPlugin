package me.fujii.check.listener;

import me.fujii.check.data.ChecklistStore;
import me.fujii.check.util.ItemChecklistTagger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerInventoryTagListener implements Listener {

    private final JavaPlugin plugin;
    private final ChecklistStore store;

    public PlayerInventoryTagListener(JavaPlugin plugin, ChecklistStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    // サーバー参加時に一度適用（保険）
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) return;

        ItemStack stack = e.getItem().getItemStack();
        if (stack == null) return;

        Material type = stack.getType();
        if (type == Material.AIR) return;
        if (!type.isItem()) return;
    }
}

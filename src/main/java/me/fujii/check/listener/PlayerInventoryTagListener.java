package me.fujii.check.listener;

import me.fujii.check.util.ItemChecklistTagger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerInventoryTagListener implements Listener {

    private final JavaPlugin plugin;
    private final ItemChecklistTagger tagger;

    public PlayerInventoryTagListener(JavaPlugin plugin, ItemChecklistTagger tagger) {
        this.plugin = plugin;
        this.tagger = tagger;
    }

    // “操作直後”だとまだ反映されてないことがあるので 1tick 遅らせて更新する
    private void applyLater(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                tagger.applyToInventory(player.getInventory());
            }
        }.runTask(plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            tagger.applyToInventory(e.getPlayer().getInventory());
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player player) {
            applyLater(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player player) {
            applyLater(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player player) {
            applyLater(player);
        }
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent e) {
        applyLater(e.getPlayer());
    }
}

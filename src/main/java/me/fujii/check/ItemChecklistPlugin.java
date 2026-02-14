package me.fujii.check;

import me.fujii.check.cmd.ChecklistCommand;
import me.fujii.check.data.ChecklistStore;
import me.fujii.check.gui.ChecklistGui;
import org.bukkit.plugin.java.JavaPlugin;
import me.fujii.check.listener.PlayerInventoryTagListener;
import me.fujii.check.util.ItemChecklistTagger;

public final class ItemChecklistPlugin extends JavaPlugin {

    private ChecklistStore store;
    private ChecklistGui gui;
    private ItemChecklistTagger tagger;

    public ItemChecklistTagger getTagger() {
        return tagger;
    }

    @Override
    public void onEnable() {
        this.store = new ChecklistStore(this);
        this.store.load();

        this.tagger = new ItemChecklistTagger(this, store, false); // false=未チェックは表示しない
        getServer().getPluginManager().registerEvents(new PlayerInventoryTagListener(this, tagger), this);
        this.gui = new ChecklistGui(this, store, tagger);

        if (getCommand("checklist") != null) {
            ChecklistCommand cmd = new ChecklistCommand(store, gui);
            getCommand("checklist").setExecutor(cmd);
            getCommand("checklist").setTabCompleter(cmd);
        }

        getServer().getPluginManager().registerEvents(gui, this);

        // 参加中プレイヤーに一回適用（起動直後）
        getServer().getScheduler().runTask(this, () -> {
            for (var p : getServer().getOnlinePlayers()) {
                tagger.applyToInventory(p.getInventory());
            }
        });

        // 2秒に1回だけ適用（取りこぼし対策）
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (var p : getServer().getOnlinePlayers()) {
                tagger.applyToInventory(p.getInventory());
            }
        }, 40L, 40L);

        getLogger().info("ItemChecklist enabled!");
    }

    @Override
    public void onDisable() {
        store.save();
        getLogger().info("ItemChecklist disabled!");
    }
}

package me.fujii.check;

import me.fujii.check.cmd.ChecklistCommand;
import me.fujii.check.cmd.CleanupCommand;
import me.fujii.check.cmd.DataResetCommand;
import me.fujii.check.cmd.DataRewriteCommand;
import me.fujii.check.data.ChecklistStore;
import me.fujii.check.gui.ChecklistGui;
import me.fujii.check.listener.*;
import me.fujii.check.ui.ChecklistSidebar;
import me.fujii.check.util.ItemChecklistTagger;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemChecklistPlugin extends JavaPlugin {

    private ChecklistStore store;
    private ChecklistGui gui;
    private ChecklistSidebar sidebar;
    private ItemChecklistTagger tagger; // ★追加

    public ItemChecklistTagger getTagger() { // ★必要ならリスナー側から参照できる
        return tagger;
    }

    @Override
    public void onEnable() {
        this.store = new ChecklistStore(this);
        this.store.load();

        // ★taggerを生成（cleanupにも必要）
        this.tagger = new ItemChecklistTagger(this, store, false);

        this.sidebar = new ChecklistSidebar(store);

        // ※以下のリスナーが tagger を必要とするなら、コンストラクタ引数に tagger を渡す形にしてください
        // 例：new PlayerInventoryTagListener(this, store, tagger)
//        getServer().getPluginManager().registerEvents(new PlayerInventoryTagListener(this, store), this);
//        getServer().getPluginManager().registerEvents(new CraftResultTagListener(this, store), this);
//        getServer().getPluginManager().registerEvents(new ContainerInventoryTagListener(this), this);
//        getServer().getPluginManager().registerEvents(new InventoryMoveNormalizeListener(this, store), this);
        getServer().getPluginManager().registerEvents(new ChecklistStatusBarListener(this, store), this);
        getServer().getPluginManager().registerEvents(new ChecklistSidebarListener(sidebar), this);

        this.gui = new ChecklistGui(this, store);

        if (getCommand("checklist") != null) {
            ChecklistCommand cmd = new ChecklistCommand(store, gui);
            getCommand("checklist").setExecutor(cmd);
            getCommand("checklist").setTabCompleter(cmd);
        }

        if (getCommand("checklistcleanup") != null) {
            getCommand("checklistcleanup").setExecutor(new CleanupCommand(tagger));
        }

        if (getCommand("checklistdatarewrite") != null) {
            getCommand("checklistdatarewrite").setExecutor(new DataRewriteCommand(tagger));
        }

        if (getCommand("checklistdatareset") != null) {
            getCommand("checklistdatareset").setExecutor(new DataResetCommand(tagger));
        }

        if (getCommand("testmark") != null) {
            getCommand("testmark").setExecutor(new me.fujii.check.cmd.TestMarkCommand(this));
        }

        getServer().getPluginManager().registerEvents(gui, this);

        getLogger().info("ItemChecklist enabled!");
    }

    @Override
    public void onDisable() {
        store.save();
        getLogger().info("ItemChecklist disabled!");
    }
}

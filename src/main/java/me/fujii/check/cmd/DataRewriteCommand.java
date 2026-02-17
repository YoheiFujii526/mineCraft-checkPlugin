package me.fujii.check.cmd;

import me.fujii.check.util.ItemChecklistTagger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class DataRewriteCommand implements CommandExecutor {

    private final ItemChecklistTagger tagger;

    public DataRewriteCommand(ItemChecklistTagger tagger) {
        this.tagger = tagger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        String mode = (args.length >= 1) ? args[0].toLowerCase() : "hand";
        boolean doHand = mode.equals("hand");
        boolean doOpen = mode.equals("open");
        boolean doLoaded = mode.equals("loaded");

        if (!(doHand || doOpen || doLoaded)) {
            sender.sendMessage("§eUsage: /checklistdatarewrite [hand|open|loaded]");
            return true;
        }

        if (doHand) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cPlayer only. /checklistdatarewrite hand");
                return true;
            }
            ItemStack item = p.getInventory().getItemInMainHand();
            tagger.rewriteChecklistData(item);
            p.updateInventory();
            sender.sendMessage("§a[Checklist] rewritten checklist internal data for item in hand.");
            return true;
        }

        if (doOpen) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cPlayer only. /checklistdatarewrite open");
                return true;
            }
            Inventory top = p.getOpenInventory().getTopInventory();
            ItemStack[] contents = top.getContents();
            int rewritten = 0;
            for (int i = 0; i < contents.length; i++) {
                ItemStack it = contents[i];
                if (it == null) continue;
                tagger.rewriteChecklistData(it);
                contents[i] = it;
                rewritten++;
            }
            top.setContents(contents);
            p.updateInventory();
            sender.sendMessage("§a[Checklist] rewritten opened inventory (" + rewritten + " slots processed).");
            return true;
        }

        int rewrittenContainers = 0;
        int rewrittenGroundItems = 0;
        int rewrittenFrames = 0;
        int rewrittenInvEntities = 0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            tagger.rewriteInventoryContents(p.getInventory());
            tagger.rewriteInventoryContents(p.getEnderChest());
            p.updateInventory();
        }

        for (World w : Bukkit.getWorlds()) {
            for (Chunk c : w.getLoadedChunks()) {
                BlockState[] tileEntities;
                try {
                    tileEntities = c.getTileEntities(false);
                } catch (NoSuchMethodError ignored) {
                    tileEntities = c.getTileEntities();
                }

                for (BlockState st : tileEntities) {
                    if (st instanceof Container cont) {
                        Inventory inv = cont.getInventory();
                        ItemStack[] contents = inv.getContents();
                        for (int i = 0; i < contents.length; i++) {
                            ItemStack it = contents[i];
                            if (it == null) continue;
                            tagger.rewriteChecklistData(it);
                            contents[i] = it;
                        }
                        inv.setContents(contents);
                        st.update(true, false);
                        rewrittenContainers++;
                    }
                }

                for (org.bukkit.entity.Entity ent : c.getEntities()) {
                    if (ent instanceof Item dropped) {
                        ItemStack stack = dropped.getItemStack();
                        tagger.rewriteChecklistData(stack);
                        dropped.setItemStack(stack);
                        rewrittenGroundItems++;
                        continue;
                    }
                    if (ent instanceof ItemFrame frame) {
                        ItemStack stack = frame.getItem();
                        tagger.rewriteChecklistData(stack);
                        frame.setItem(stack);
                        rewrittenFrames++;
                        continue;
                    }
                    if (ent instanceof InventoryHolder holder) {
                        Inventory inv = holder.getInventory();
                        ItemStack[] contents = inv.getContents();
                        for (int i = 0; i < contents.length; i++) {
                            ItemStack it = contents[i];
                            if (it == null) continue;
                            tagger.rewriteChecklistData(it);
                            contents[i] = it;
                        }
                        inv.setContents(contents);
                        rewrittenInvEntities++;
                    }
                }
            }
        }

        sender.sendMessage("§a[Checklist] rewrite done.");
        sender.sendMessage("§7- loaded containers: " + rewrittenContainers);
        sender.sendMessage("§7- loaded ground items: " + rewrittenGroundItems);
        sender.sendMessage("§7- loaded item frames: " + rewrittenFrames);
        sender.sendMessage("§7- loaded inventory entities: " + rewrittenInvEntities);
        sender.sendMessage("§eTip: this command is heavy; prefer '/checklistdatarewrite open' when possible.");
        return true;
    }
}

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

public final class DataResetCommand implements CommandExecutor {

    private final ItemChecklistTagger tagger;

    public DataResetCommand(ItemChecklistTagger tagger) {
        this.tagger = tagger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        String mode = (args.length >= 1) ? args[0].toLowerCase() : "hand";
        boolean doHand = mode.equals("hand");
        boolean doOpen = mode.equals("open");
        boolean doLoaded = mode.equals("loaded");

        if (!(doHand || doOpen || doLoaded)) {
            sender.sendMessage("§eUsage: /checklistdatareset [hand|open|loaded]");
            return true;
        }

        if (doHand) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cPlayer only. /checklistdatareset hand");
                return true;
            }
            ItemStack item = p.getInventory().getItemInMainHand();
            ItemStack normalized = tagger.normalizeForStacking(item);
            p.getInventory().setItemInMainHand(normalized);
            p.updateInventory();
            sender.sendMessage("§a[Checklist] reset checklist internal data for item in hand.");
            return true;
        }

        if (doOpen) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cPlayer only. /checklistdatareset open");
                return true;
            }
            Inventory top = p.getOpenInventory().getTopInventory();
            ItemStack[] contents = top.getContents();
            int reset = 0;
            for (int i = 0; i < contents.length; i++) {
                ItemStack it = contents[i];
                if (it == null) continue;
                contents[i] = tagger.normalizeForStacking(it);
                reset++;
            }
            top.setContents(contents);
            p.updateInventory();
            sender.sendMessage("§a[Checklist] reset opened inventory (" + reset + " slots processed).");
            return true;
        }

        int cleanedContainers = 0;
        int cleanedGroundItems = 0;
        int cleanedFrames = 0;
        int cleanedInvEntities = 0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            tagger.normalizeInventoryForStacking(p.getInventory());
            tagger.normalizeInventoryForStacking(p.getEnderChest());
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
                        tagger.normalizeInventoryForStacking(cont.getInventory());
                        st.update(true, false);
                        cleanedContainers++;
                    }
                }

                for (org.bukkit.entity.Entity ent : c.getEntities()) {
                    if (ent instanceof Item dropped) {
                        ItemStack stack = dropped.getItemStack();
                        dropped.setItemStack(tagger.normalizeForStacking(stack));
                        cleanedGroundItems++;
                        continue;
                    }
                    if (ent instanceof ItemFrame frame) {
                        ItemStack stack = frame.getItem();
                        frame.setItem(tagger.normalizeForStacking(stack));
                        cleanedFrames++;
                        continue;
                    }
                    if (ent instanceof InventoryHolder holder) {
                        tagger.normalizeInventoryForStacking(holder.getInventory());
                        cleanedInvEntities++;
                    }
                }
            }
        }

        sender.sendMessage("§a[Checklist] reset done (stack-normalized).");
        sender.sendMessage("§7- loaded containers: " + cleanedContainers);
        sender.sendMessage("§7- loaded ground items: " + cleanedGroundItems);
        sender.sendMessage("§7- loaded item frames: " + cleanedFrames);
        sender.sendMessage("§7- loaded inventory entities: " + cleanedInvEntities);
        sender.sendMessage("§eTip: this command is heavy; prefer '/checklistdatareset open' when possible.");
        return true;
    }
}

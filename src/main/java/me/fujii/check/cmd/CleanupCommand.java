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
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class CleanupCommand implements CommandExecutor {

    private final ItemChecklistTagger tagger;

    public CleanupCommand(ItemChecklistTagger tagger) {
        this.tagger = tagger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        String mode = (args.length >= 1) ? args[0].toLowerCase() : "";
        boolean doOpen = mode.equals("open");
        boolean doLoaded = mode.equals("loaded");

        // 1) オンラインプレイヤー：所持品・エンダーチェストをスタック復旧用に正規化
        for (Player p : Bukkit.getOnlinePlayers()) {
            tagger.normalizeInventoryForStacking(p.getInventory());
            tagger.normalizeInventoryForStacking(p.getEnderChest());
            p.updateInventory();
        }

        // open：いま開いてるコンテナだけ確実に正規化
        if (doOpen) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cプレイヤーのみ実行できます: /checklistcleanup open");
                return true;
            }
            Inventory top = p.getOpenInventory().getTopInventory();
            tagger.normalizeInventoryForStacking(top);
            p.updateInventory();
            sender.sendMessage("§a[Checklist] cleaned opened inventory (stack-normalized).");
            return true;
        }

        int cleanedContainers = 0;
        int cleanedGroundItems = 0;
        int cleanedFrames = 0;
        int cleanedInvEntities = 0;

        // loaded：ロード済みチャンク内のコンテナ・地面アイテムも正規化（仕分け機対策）
        if (doLoaded) {
            for (World w : Bukkit.getWorlds()) {
                for (Chunk c : w.getLoadedChunks()) {

                    // コンテナ（チェスト/ホッパー/ドロッパー等）
                    // Use useSnapshot=false (when available) so inventory edits apply to the actual container.
                    BlockState[] tileEntities;
                    try {
                        tileEntities = c.getTileEntities(false);
                    } catch (NoSuchMethodError ignored) {
                        tileEntities = c.getTileEntities();
                    }

                    for (BlockState st : tileEntities) {
                        if (st instanceof Container cont) {
                            tagger.normalizeInventoryForStacking(cont.getInventory());
                            // Force update without physics, to persist changes for snapshot states.
                            st.update(true, false);
                            cleanedContainers++;
                        }
                    }

                    // チャンク内エンティティ
                    for (org.bukkit.entity.Entity ent : c.getEntities()) {

                        // 地面アイテム
                        if (ent instanceof Item dropped) {
                            ItemStack stack = dropped.getItemStack();
                            ItemStack normalized = tagger.normalizeForStacking(stack);
                            dropped.setItemStack(normalized);
                            cleanedGroundItems++;
                            continue;
                        }

                        // 額縁（あれば）
                        if (ent instanceof ItemFrame frame) {
                            ItemStack stack = frame.getItem();
                            ItemStack normalized = tagger.normalizeForStacking(stack);
                            frame.setItem(normalized);
                            cleanedFrames++;
                            continue;
                        }

                        // チェスト付きトロッコ等
                        if (ent instanceof InventoryHolder holder) {
                            tagger.normalizeInventoryForStacking(holder.getInventory());
                            cleanedInvEntities++;
                        }
                    }
                }
            }
        }

        sender.sendMessage("§a[Checklist] cleanup done (stack-normalized).");
        if (doLoaded) {
            sender.sendMessage("§7- loaded containers: " + cleanedContainers);
            sender.sendMessage("§7- loaded ground items: " + cleanedGroundItems);
            sender.sendMessage("§7- loaded item frames: " + cleanedFrames);
            sender.sendMessage("§7- loaded inventory entities: " + cleanedInvEntities);
            sender.sendMessage("§e※未ロードのチェスト等は、その場所がロードされるまで対象外です。近づいてからもう一度 /checklistcleanup loaded");
        } else {
            sender.sendMessage("§eチェスト/ホッパー(ロード済み)も直す: /checklistcleanup loaded");
            sender.sendMessage("§e開いてるチェストだけ直す: /checklistcleanup open");
        }

        return true;
    }
}

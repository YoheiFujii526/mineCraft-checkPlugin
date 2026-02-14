package me.fujii.check.cmd;

import me.fujii.check.data.ChecklistStore;
import me.fujii.check.gui.ChecklistGui;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class ChecklistCommand implements CommandExecutor, TabCompleter {

    private final ChecklistStore store;
    private final ChecklistGui gui;

    public ChecklistCommand(ChecklistStore store, ChecklistGui gui) {
        this.store = store;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Player only.");
            return true;
        }
        if (!sender.hasPermission("itemchecklist.use")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0) {
            gui.open(player, 0);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reset" -> {
                if (!sender.hasPermission("itemchecklist.admin")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                store.reset();
                store.save();
                sender.sendMessage(ChatColor.GREEN + "Checklist reset.");
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("itemchecklist.admin")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                store.load();
                sender.sendMessage(ChatColor.GREEN + "Reloaded sorted.yml");
                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /checklist [reset|reload]");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("itemchecklist.admin")) {
            List<String> c = new ArrayList<>();
            String p = args[0].toLowerCase();
            if ("reset".startsWith(p)) c.add("reset");
            if ("reload".startsWith(p)) c.add("reload");
            return c;
        }
        return List.of();
    }
}


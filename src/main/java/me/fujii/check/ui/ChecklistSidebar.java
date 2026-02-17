package me.fujii.check.ui;

import me.fujii.check.data.ChecklistStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ChecklistSidebar {

    private static final String OBJ_NAME = "checklist";
    private static final String TITLE = ChatColor.GOLD + "Checklist";

    // 右側表示は1人1つなので、プレイヤーごとに「元のスコアボード」を退避して戻せるようにする
    private final Map<UUID, Scoreboard> prevBoards = new HashMap<>();
    private final Map<UUID, Scoreboard> myBoards = new HashMap<>();

    private final ChecklistStore store;

    // 行の“入れ物”(ユニークなエントリ文字列)
    private static final String[] ENTRIES = new String[] {
            ChatColor.BLACK.toString(),
            ChatColor.DARK_BLUE.toString(),
            ChatColor.DARK_GREEN.toString(),
            ChatColor.DARK_AQUA.toString(),
            ChatColor.DARK_RED.toString(),
            ChatColor.DARK_PURPLE.toString(),
            ChatColor.GOLD.toString(),
            ChatColor.GRAY.toString(),
            ChatColor.DARK_GRAY.toString(),
            ChatColor.BLUE.toString(),
            ChatColor.GREEN.toString(),
            ChatColor.AQUA.toString(),
            ChatColor.RED.toString(),
            ChatColor.LIGHT_PURPLE.toString(),
            ChatColor.YELLOW.toString()
    };

    public ChecklistSidebar(ChecklistStore store) {
        this.store = store;
    }

    public void attach(Player p) {
        UUID id = p.getUniqueId();
        if (myBoards.containsKey(id)) return;

        prevBoards.put(id, p.getScoreboard());

        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective(OBJ_NAME, Criteria.DUMMY, TITLE);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // 3行分用意（必要なら増やせる）
        setupLine(sb, obj, 0, "");
        setupLine(sb, obj, 1, "");
        setupLine(sb, obj, 2, "");

        p.setScoreboard(sb);
        myBoards.put(id, sb);
    }

    public void detach(Player p) {
        UUID id = p.getUniqueId();
        Scoreboard prev = prevBoards.remove(id);
        myBoards.remove(id);
        if (prev != null) {
            p.setScoreboard(prev);
        }
    }

    public void update(Player p, ItemStack item) {
        UUID id = p.getUniqueId();
        Scoreboard sb = myBoards.get(id);
        if (sb == null) return;

        Objective obj = sb.getObjective(OBJ_NAME);
        if (obj == null) return;

        if (item == null || item.getType() == Material.AIR || !item.getType().isItem()) {
            setLine(sb, 0, ChatColor.WHITE + "Item: " + ChatColor.GRAY + "-");
            setLine(sb, 1, ChatColor.WHITE + "Status: " + ChatColor.GRAY + "-");
            setLine(sb, 2, ChatColor.WHITE + "Checked: " + ChatColor.GRAY + store.getCheckedCount());
            return;
        }

        Material type = item.getType();
        boolean checked = store.isChecked(type);

        setLine(sb, 0, ChatColor.WHITE + "Item: " + ChatColor.YELLOW + type.name());
        setLine(sb, 1, ChatColor.WHITE + "Status: " + (checked
                ? (ChatColor.GREEN + "✓ SORTABLE")
                : (ChatColor.RED + "✖ NOT CHECKED")));
        setLine(sb, 2, ChatColor.WHITE + "Checked: " + ChatColor.AQUA
                + store.getCheckedCount() + " / " + Material.values().length);
    }

    private void setupLine(Scoreboard sb, Objective obj, int idx, String text) {
        String entry = ENTRIES[idx];
        Team t = sb.registerNewTeam("cl_" + idx);
        t.addEntry(entry);
        obj.getScore(entry).setScore(15 - idx); // 上から順に表示
        setLine(sb, idx, text);
    }

    private void setLine(Scoreboard sb, int idx, String text) {
        Team t = sb.getTeam("cl_" + idx);
        if (t == null) return;
        // prefixは長すぎると切れるので短め推奨
        t.setPrefix(text);
    }
}

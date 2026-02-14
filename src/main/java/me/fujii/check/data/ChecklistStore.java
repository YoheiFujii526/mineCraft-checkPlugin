package me.fujii.check.data;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ChecklistStore {

    private final JavaPlugin plugin;
    private final File file;

    // 仕分け可能（チェック済み）Material
    private final EnumSet<Material> checked = EnumSet.noneOf(Material.class);

    public ChecklistStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "sorted.yml");
    }

    public boolean isChecked(Material m) {
        return checked.contains(m);
    }

    public boolean toggle(Material m) {
        if (m == null || !m.isItem() || m == Material.AIR) return false;
        if (checked.contains(m)) {
            checked.remove(m);
            return false; // now unchecked
        } else {
            checked.add(m);
            return true; // now checked
        }
    }

    public int getCheckedCount() {
        return checked.size();
    }

    public Set<Material> snapshot() {
        return EnumSet.copyOf(checked);
    }

    public void reset() {
        checked.clear();
    }

    public void load() {
        checked.clear();
        if (!file.exists()) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        List<String> list = yml.getStringList("checked");

        for (String s : list) {
            try {
                Material m = Material.valueOf(s);
                if (m.isItem() && m != Material.AIR) checked.add(m);
            } catch (IllegalArgumentException ignore) {}
        }
    }

    public void save() {
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("checked", checked.stream().map(Material::name).collect(Collectors.toList()));
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save sorted.yml: " + e.getMessage());
        }
    }
}

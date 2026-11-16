package dev.killcorrupt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class KillCorrupt extends JavaPlugin {

    private final Set<UUID> corrupted = Collections.synchronizedSet(new HashSet<>());
    private File dataFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dataFile = new File(getDataFolder(), "data.yml");
        loadData();

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);

        PluginCommand cmd = getCommand("killcorrupt");
        if (cmd != null) {
            KillCorruptCommand handler = new KillCorruptCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }
    }

    @Override
    public void onDisable() {
        saveData();
    }

    public boolean isCorrupted(UUID uuid) {
        return corrupted.contains(uuid);
    }

    public boolean addCorrupted(UUID uuid) {
        boolean changed = corrupted.add(uuid);
        if (changed) saveData();
        return changed;
    }

    public boolean removeCorrupted(UUID uuid) {
        boolean changed = corrupted.remove(uuid);
        if (changed) saveData();
        return changed;
    }

    public Set<UUID> getCorrupted() {
        synchronized (corrupted) {
            return new HashSet<>(corrupted);
        }
    }

    public void reloadAll() {
        reloadConfig();
        loadData();
    }

    /** Builds a message from config (messages.<key>) with the prefix and %player% / %count% replaced. */
    public Component msg(String key, String player, int count) {
        String prefix = getConfig().getString("messages.prefix", "");
        String raw = getConfig().getString("messages." + key, key);
        raw = raw.replace("%player%", player == null ? "" : player).replace("%count%", String.valueOf(count));
        return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + raw);
    }

    private void loadData() {
        corrupted.clear();
        if (!dataFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        for (String s : yaml.getStringList("players")) {
            try {
                corrupted.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                getLogger().warning("Invalid UUID in data.yml: " + s);
            }
        }
    }

    private void saveData() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<String> list = new ArrayList<>();
        for (UUID uuid : getCorrupted()) list.add(uuid.toString());
        yaml.set("players", list);
        try {
            if (!getDataFolder().exists()) getDataFolder().mkdirs();
            yaml.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }
}

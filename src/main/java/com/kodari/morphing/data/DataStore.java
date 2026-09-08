package com.kodari.morphing.data;

import com.kodari.morphing.MorphingPlugin;
import com.kodari.morphing.model.PlayerProgress;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class DataStore {
    private final MorphingPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerProgress> progress = new HashMap<>();

    public DataStore(MorphingPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not create players.yml: " + exception.getMessage());
            }
        }
    }

    public PlayerProgress get(Player player) {
        return get(player.getUniqueId());
    }

    public PlayerProgress get(UUID uuid) {
        return progress.computeIfAbsent(uuid, this::load);
    }

    private PlayerProgress load(UUID uuid) {
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        String path = "players." + uuid;
        PlayerProgress playerProgress = new PlayerProgress();
        playerProgress.setMorphCount(data.getInt(path + ".morph-count", 0));
        playerProgress.setDistanceMorphed(data.getDouble(path + ".distance-morphed", 0.0));
        playerProgress.setUnlockedMilestones(new HashSet<>(data.getStringList(path + ".milestones")));
        return playerProgress;
    }

    public void save(UUID uuid) {
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        PlayerProgress playerProgress = get(uuid);
        String path = "players." + uuid;
        data.set(path + ".morph-count", playerProgress.getMorphCount());
        data.set(path + ".distance-morphed", playerProgress.getDistanceMorphed());
        data.set(path + ".milestones", playerProgress.getUnlockedMilestones().stream().toList());
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save player data: " + exception.getMessage());
        }
    }

    public void saveAll() {
        for (UUID uuid : progress.keySet()) {
            save(uuid);
        }
    }
}

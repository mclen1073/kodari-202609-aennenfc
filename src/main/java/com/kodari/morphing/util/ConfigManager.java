package com.kodari.morphing.util;

import com.kodari.morphing.MorphingPlugin;
import com.kodari.morphing.model.MorphDefinition;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConfigManager {
    private final MorphingPlugin plugin;
    private final Map<String, MorphDefinition> morphs = new HashMap<>();

    public ConfigManager(MorphingPlugin plugin) {
        this.plugin = plugin;
        loadMorphs();
    }

    private void loadMorphs() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("morphs");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection morph = section.getConfigurationSection(id);
            if (morph == null) {
                continue;
            }
            String entityName = morph.getString("entity", "ZOMBIE").toUpperCase(Locale.ROOT);
            try {
                EntityType entityType = EntityType.valueOf(entityName);
                String displayName = color(morph.getString("display-name", id));
                String permission = morph.getString("permission", "morphing.morph." + id);
                List<String> abilities = new ArrayList<>(morph.getStringList("abilities"));
                morphs.put(id.toLowerCase(Locale.ROOT), new MorphDefinition(id, displayName, entityType, permission, abilities));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Invalid entity type for morph '" + id + "': " + entityName);
            }
        }
    }

    public MorphDefinition getMorph(String id) {
        return morphs.get(id.toLowerCase(Locale.ROOT));
    }

    public Map<String, MorphDefinition> getMorphs() {
        return Collections.unmodifiableMap(morphs);
    }

    public boolean isAbductionEnabled() {
        return plugin.getConfig().getBoolean("abduction.enabled", true);
    }

    public int getAbductionFrequency() {
        return Math.max(1, plugin.getConfig().getInt("abduction.every-morphs", 10));
    }

    public int getChallengeSeconds() {
        return Math.max(5, plugin.getConfig().getInt("abduction.challenge-seconds", 45));
    }

    public double getChallengeDistance() {
        return Math.max(4.0, plugin.getConfig().getDouble("abduction.challenge-distance", 18.0));
    }

    public double getAbductionHeight() {
        return Math.max(5.0, plugin.getConfig().getDouble("abduction.height", 12.0));
    }

    public double getRollingStoneDistance() {
        return Math.max(1.0, plugin.getConfig().getDouble("milestones.rolling-stone.distance", 1000.0));
    }

    public int getFirstMorphCount() {
        return Math.max(1, plugin.getConfig().getInt("milestones.first-morph.morphs", 1));
    }

    public int getCollectorMorphCount() {
        return Math.max(1, plugin.getConfig().getInt("milestones.morph-collector.morphs", 5));
    }

    public String getMilestoneMessage(String id, String fallback) {
        return color(plugin.getConfig().getString("milestones." + id + ".message", fallback));
    }

    public String message(String key) {
        return color(plugin.getConfig().getString("messages." + key, "&cMissing message: " + key));
    }

    public String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}

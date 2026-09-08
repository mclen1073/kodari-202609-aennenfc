package com.kodari.morphing;

import com.kodari.morphing.command.MorphCommand;
import com.kodari.morphing.data.DataStore;
import com.kodari.morphing.manager.AbductionManager;
import com.kodari.morphing.manager.MorphManager;
import com.kodari.morphing.util.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MorphingPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private DataStore dataStore;
    private MorphManager morphManager;
    private AbductionManager abductionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.dataStore = new DataStore(this);
        this.morphManager = new MorphManager(this, configManager, dataStore);
        this.abductionManager = new AbductionManager(this, configManager, morphManager);

        MorphCommand morphCommand = new MorphCommand(morphManager, configManager);
        if (getCommand("morph") != null) {
            getCommand("morph").setExecutor(morphCommand);
            getCommand("morph").setTabCompleter(morphCommand);
        }

        getServer().getPluginManager().registerEvents(morphManager, this);
        getServer().getPluginManager().registerEvents(abductionManager, this);
        getServer().getPluginManager().registerEvents(new com.kodari.morphing.listener.MorphListener(morphManager), this);
        getServer().getScheduler().runTaskTimer(this, () -> {
            morphManager.tick();
            abductionManager.tick();
        }, 1L, 1L);
    }

    @Override
    public void onDisable() {
        if (abductionManager != null) {
            abductionManager.shutdown();
        }
        if (morphManager != null) {
            morphManager.shutdown();
        }
        if (dataStore != null) {
            dataStore.saveAll();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public MorphManager getMorphManager() {
        return morphManager;
    }

    public AbductionManager getAbductionManager() {
        return abductionManager;
    }
}

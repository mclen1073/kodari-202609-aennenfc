package com.kodari.morphing.command;

import com.kodari.morphing.manager.MorphManager;
import com.kodari.morphing.model.MorphState;
import com.kodari.morphing.util.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MorphCommand implements CommandExecutor, TabCompleter {
    private final MorphManager morphManager;
    private final ConfigManager config;

    public MorphCommand(MorphManager morphManager, ConfigManager config) {
        this.morphManager = morphManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use morphs.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            List<String> available = config.getMorphs().values().stream()
                    .filter(morph -> player.hasPermission("morphing.use") && player.hasPermission(morph.getPermission()))
                    .map(morph -> morph.getId())
                    .sorted()
                    .toList();
            player.sendMessage(config.message("prefix") + config.message("morph-list").replace("{morphs}", String.join(", ", available)));
            return true;
        }
        if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("stop")) {
            morphManager.endMorph(player);
            return true;
        }
        morphManager.morph(player, args[0].toLowerCase(Locale.ROOT));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        List<String> suggestions = new ArrayList<>();
        suggestions.add("list");
        suggestions.add("off");
        suggestions.addAll(config.getMorphs().keySet());
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return suggestions.stream().filter(value -> value.startsWith(prefix)).sorted().toList();
    }
}

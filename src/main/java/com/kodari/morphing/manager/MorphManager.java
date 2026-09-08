package com.kodari.morphing.manager;

import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.XSound;
import com.kodari.morphing.MorphingPlugin;
import com.kodari.morphing.data.DataStore;
import com.kodari.morphing.model.MorphDefinition;
import com.kodari.morphing.model.MorphState;
import com.kodari.morphing.model.PlayerProgress;
import com.kodari.morphing.util.ConfigManager;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MorphManager implements Listener {
    private final MorphingPlugin plugin;
    private final ConfigManager config;
    private final DataStore dataStore;
    private final Map<UUID, MorphState> active = new HashMap<>();
    private final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private int tickCounter;

    public MorphManager(MorphingPlugin plugin, ConfigManager config, DataStore dataStore) {
        this.plugin = plugin;
        this.config = config;
        this.dataStore = dataStore;
    }

    public boolean morph(Player player, String id) {
        MorphDefinition definition = config.getMorph(id);
        if (definition == null) {
            player.sendMessage(config.message("prefix") + config.message("unknown-morph"));
            return false;
        }
        if (!player.hasPermission("morphing.use") || !player.hasPermission(definition.getPermission())) {
            player.sendMessage(config.message("prefix") + config.message("no-permission"));
            return false;
        }
        MorphState current = active.get(player.getUniqueId());
        if (current != null) {
            if (current.getDefinition().getId().equalsIgnoreCase(definition.getId())) {
                player.sendMessage(config.message("prefix") + config.message("already-morphed").replace("{morph}", definition.getDisplayName()));
                return false;
            }
            endMorph(player);
        }

        Entity entity = player.getWorld().spawnEntity(player.getLocation(), definition.getEntityType());
        if (!(entity instanceof LivingEntity visual)) {
            entity.remove();
            return false;
        }
        visual.setAI(false);
        visual.setGravity(false);
        visual.setInvulnerable(true);
        visual.setSilent(true);
        visual.setCollidable(false);
        visual.setCustomName(definition.getDisplayName());
        visual.setCustomNameVisible(true);
        visual.setPersistent(false);
        MorphState state = new MorphState(definition, visual, player.getLocation().clone());
        active.put(player.getUniqueId(), state);
        player.setInvisible(true);
        applyAbilityEffects(player, state);

        PlayerProgress progress = dataStore.get(player);
        progress.setMorphCount(progress.getMorphCount() + 1);
        checkMilestones(player, progress);
        player.sendMessage(config.message("prefix") + config.message("transformed").replace("{morph}", definition.getDisplayName()));

        if (config.isAbductionEnabled() && progress.getMorphCount() % config.getAbductionFrequency() == 0) {
            plugin.getAbductionManager().abduct(player);
        }
        return true;
    }

    public void endMorph(Player player) {
        MorphState state = active.remove(player.getUniqueId());
        abilityCooldowns.remove(player.getUniqueId());
        if (state == null) {
            return;
        }
        if (state.getVisual() != null && !state.getVisual().isDead()) {
            state.getVisual().remove();
        }
        player.setInvisible(false);
        clearAbilityEffects(player);
        player.sendMessage(config.message("prefix") + config.message("ended"));
    }

    public void tick() {
        tickCounter++;
        for (Map.Entry<UUID, MorphState> entry : new HashMap<>(active).entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            MorphState state = entry.getValue();
            if (player == null || !player.isOnline()) {
                if (state.getVisual() != null && !state.getVisual().isDead()) {
                    state.getVisual().remove();
                }
                active.remove(entry.getKey());
                continue;
            }
            Location location = player.getLocation();
            if (state.getVisual() != null && !state.getVisual().isDead()) {
                state.getVisual().teleport(location);
            }
            Location last = state.getLastLocation();
            if (last != null && last.getWorld() == location.getWorld()) {
                double distance = last.distance(location);
                if (distance > 0.01) {
                    PlayerProgress progress = dataStore.get(player);
                    progress.setDistanceMorphed(progress.getDistanceMorphed() + distance);
                    checkDistanceMilestone(player, progress);
                }
            }
            state.setLastLocation(location.clone());
            if (tickCounter % 20 == 0) {
                applyAbilityEffects(player, state);
            }
        }
    }

    private void checkMilestones(Player player, PlayerProgress progress) {
        if (progress.getMorphCount() >= config.getFirstMorphCount() && progress.getUnlockedMilestones().add("first-morph")) {
            announceMilestone(player, config.getMilestoneMessage("first-morph", "First Transformation"));
        }
        if (progress.getMorphCount() >= config.getCollectorMorphCount() && progress.getUnlockedMilestones().add("morph-collector")) {
            announceMilestone(player, config.getMilestoneMessage("morph-collector", "Morph Collector"));
        }
    }

    private void checkDistanceMilestone(Player player, PlayerProgress progress) {
        if (progress.getDistanceMorphed() >= config.getRollingStoneDistance()
                && progress.getUnlockedMilestones().add("rolling-stone")) {
            announceMilestone(player, config.getMilestoneMessage("rolling-stone", "Rolling Stone"));
        }
    }

    private void announceMilestone(Player player, String milestone) {
        player.sendMessage(config.message("prefix") + config.message("milestone").replace("{milestone}", milestone));
        XSound.matchXSound("ENTITY_PLAYER_LEVELUP").ifPresent(sound -> sound.play(player));
    }

    private void applyAbilityEffects(Player player, MorphState state) {
        Set<String> abilities = new HashSet<>(state.getDefinition().getAbilities());
        if (abilities.contains("speed")) {
            XPotion.matchXPotion("SPEED").map(potion -> potion.buildPotionEffect(60, 1)).ifPresent(player::addPotionEffect);
        }
        if (abilities.contains("strength")) {
            XPotion.matchXPotion("INCREASE_DAMAGE").map(potion -> potion.buildPotionEffect(60, 0)).ifPresent(player::addPotionEffect);
        }
        if (abilities.contains("jump")) {
            XPotion.matchXPotion("JUMP").map(potion -> potion.buildPotionEffect(60, 1)).ifPresent(player::addPotionEffect);
        }
    }

    private void clearAbilityEffects(Player player) {
        XPotion.matchXPotion("SPEED").ifPresent(potion -> player.removePotionEffect(potion.getPotionEffectType()));
        XPotion.matchXPotion("INCREASE_DAMAGE").ifPresent(potion -> player.removePotionEffect(potion.getPotionEffectType()));
        XPotion.matchXPotion("JUMP").ifPresent(potion -> player.removePotionEffect(potion.getPotionEffectType()));
    }

    public boolean hasAbility(Player player, String ability) {
        MorphState state = active.get(player.getUniqueId());
        return state != null && state.getDefinition().getAbilities().stream().anyMatch(value -> value.equalsIgnoreCase(ability));
    }

    public boolean useAbility(Player player, String ability, long cooldownMillis) {
        if (!hasAbility(player, ability)) {
            return false;
        }
        long now = System.currentTimeMillis();
        long nextUse = abilityCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (nextUse > now) {
            return false;
        }
        abilityCooldowns.put(player.getUniqueId(), now + cooldownMillis);
        return true;
    }

    public void launchFireball(Player player) {
        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setYield(1.0F);
        fireball.setIsIncendiary(false);
    }

    public void leap(Player player) {
        Vector direction = player.getLocation().getDirection().normalize();
        player.setVelocity(direction.multiply(1.15).setY(0.8));
    }

    public boolean isVisual(Entity entity) {
        for (MorphState state : active.values()) {
            if (state.getVisual().getUniqueId().equals(entity.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public MorphState getState(Player player) {
        return active.get(player.getUniqueId());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (isVisual(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        dataStore.get(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        endMorph(event.getPlayer());
        dataStore.save(event.getPlayer().getUniqueId());
    }

    public void shutdown() {
        for (UUID uuid : new ArrayList<>(active.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                endMorph(player);
            }
        }
    }
}

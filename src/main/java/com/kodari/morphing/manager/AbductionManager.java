package com.kodari.morphing.manager;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.particles.XParticle;
import com.kodari.morphing.MorphingPlugin;
import com.kodari.morphing.util.ConfigManager;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AbductionManager implements Listener {
    private final MorphingPlugin plugin;
    private final ConfigManager config;
    private final MorphManager morphManager;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public AbductionManager(MorphingPlugin plugin, ConfigManager config, MorphManager morphManager) {
        this.plugin = plugin;
        this.config = config;
        this.morphManager = morphManager;
    }

    public void abduct(Player player) {
        if (sessions.containsKey(player.getUniqueId())) {
            return;
        }
        Location origin = player.getLocation().clone();
        morphManager.endMorph(player);
        Location arena = origin.clone().add(0, config.getAbductionHeight(), 0);
        Location target = arena.clone().add(config.getChallengeDistance(), 0, 0);
        Session session = new Session(origin, arena, target,
                System.currentTimeMillis() + config.getChallengeSeconds() * 1000L,
                player.getAllowFlight(), player.isFlying());
        sessions.put(player.getUniqueId(), session);
        player.teleport(arena);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFallDistance(0);
        session.ufo.addAll(createUfo(arena));
        player.sendMessage(config.message("prefix") + config.message("abducted"));
    }

    private List<ArmorStand> createUfo(Location center) {
        List<ArmorStand> stands = new ArrayList<>();
        ItemStack helmet = XMaterial.matchXMaterial("SEA_LANTERN").map(XMaterial::parseItem).orElse(null);
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2 * i / 6.0;
            Location location = center.clone().add(Math.cos(angle) * 2.5, 2.5, Math.sin(angle) * 2.5);
            ArmorStand stand = (ArmorStand) center.getWorld().spawnEntity(location, org.bukkit.entity.EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            if (helmet != null) {
                stand.getEquipment().setHelmet(helmet);
            }
            stands.add(stand);
        }
        return stands;
    }

    public void tick() {
        for (Map.Entry<UUID, Session> entry : new HashMap<>(sessions).entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            Session session = entry.getValue();
            if (player == null || !player.isOnline()) {
                cleanup(entry.getKey(), false);
                continue;
            }
            moveUfo(session, player.getLocation());
            XParticle.of("END_ROD").ifPresent(particle -> player.getWorld().spawnParticle(particle.get(), session.target, 12, 0.4, 0.4, 0.4, 0.01));
            if (player.getLocation().distanceSquared(session.target) <= 4.0) {
                finish(player, true);
            } else if (System.currentTimeMillis() >= session.expiresAt) {
                finish(player, false);
            }
        }
    }

    private void moveUfo(Session session, Location playerLocation) {
        Location center = playerLocation.clone().add(0, 2.5, 0);
        for (int i = 0; i < session.ufo.size(); i++) {
            ArmorStand stand = session.ufo.get(i);
            if (stand.isDead()) {
                continue;
            }
            double angle = Math.PI * 2 * i / session.ufo.size();
            stand.teleport(center.clone().add(Math.cos(angle) * 2.5, 0, Math.sin(angle) * 2.5));
        }
    }

    private void finish(Player player, boolean success) {
        player.teleport(sessions.get(player.getUniqueId()).origin);
        player.setFlying(sessions.get(player.getUniqueId()).wasFlying);
        player.setAllowFlight(sessions.get(player.getUniqueId()).hadFlightPermission);
        player.setFallDistance(0);
        cleanup(player.getUniqueId(), true);
        player.sendMessage(config.message("prefix") + config.message(success ? "escape-success" : "escape-failed"));
    }

    private void cleanup(UUID uuid, boolean restorePlayer) {
        Session session = sessions.remove(uuid);
        if (session == null) {
            return;
        }
        for (ArmorStand stand : session.ufo) {
            if (!stand.isDead()) {
                stand.remove();
            }
        }
        Player player = plugin.getServer().getPlayer(uuid);
        if (restorePlayer && player != null) {
            player.setFlying(session.wasFlying);
            player.setAllowFlight(session.hadFlightPermission);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cleanup(event.getPlayer().getUniqueId(), false);
    }

    public void shutdown() {
        for (UUID uuid : new ArrayList<>(sessions.keySet())) {
            cleanup(uuid, false);
        }
    }

    private static class Session {
        private final Location origin;
        private final Location arena;
        private final Location target;
        private final long expiresAt;
        private final boolean hadFlightPermission;
        private final boolean wasFlying;
        private final List<ArmorStand> ufo = new ArrayList<>();

        private Session(Location origin, Location arena, Location target, long expiresAt, boolean hadFlightPermission, boolean wasFlying) {
            this.origin = origin;
            this.arena = arena;
            this.target = target;
            this.expiresAt = expiresAt;
            this.hadFlightPermission = hadFlightPermission;
            this.wasFlying = wasFlying;
        }
    }
}

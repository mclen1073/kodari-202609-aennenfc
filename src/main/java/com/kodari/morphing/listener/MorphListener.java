package com.kodari.morphing.listener;

import com.cryptomorin.xseries.XSound;
import com.kodari.morphing.manager.MorphManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class MorphListener implements Listener {
    private final MorphManager morphManager;

    public MorphListener(MorphManager morphManager) {
        this.morphManager = morphManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (morphManager.useAbility(player, "fireball", 2500L)) {
                morphManager.launchFireball(player);
                event.setCancelled(true);
                return;
            }
        }
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (morphManager.useAbility(player, "roar", 4000L)) {
                XSound.matchXSound("ENTITY_RAVAGER_ROAR").ifPresent(sound -> sound.play(player));
                player.getNearbyEntities(5, 3, 5).stream()
                        .filter(entity -> entity instanceof org.bukkit.entity.LivingEntity && entity != player)
                        .forEach(entity -> ((org.bukkit.entity.LivingEntity) entity).damage(4.0, player));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        if (morphManager.useAbility(player, "leap", 1800L)) {
            morphManager.leap(player);
        }
    }
}

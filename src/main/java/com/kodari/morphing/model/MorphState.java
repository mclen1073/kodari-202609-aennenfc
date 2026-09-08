package com.kodari.morphing.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

@Getter
@Setter
public class MorphState {
    private final MorphDefinition definition;
    private final LivingEntity visual;
    private Location lastLocation;

    public MorphState(MorphDefinition definition, LivingEntity visual, Location lastLocation) {
        this.definition = definition;
        this.visual = visual;
        this.lastLocation = lastLocation;
    }
}

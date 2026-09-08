package com.kodari.morphing.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.EntityType;

import java.util.List;

@Getter
@AllArgsConstructor
public class MorphDefinition {
    private final String id;
    private final String displayName;
    private final EntityType entityType;
    private final String permission;
    private final List<String> abilities;
}

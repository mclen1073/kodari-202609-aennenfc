package com.kodari.morphing.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
public class PlayerProgress {
    private int morphCount;
    private double distanceMorphed;
    private Set<String> unlockedMilestones = new HashSet<>();
}

package ru.siynes.api.model;

import lombok.Builder;
import lombok.Value;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

@Value
@Builder
public class TeleportParams {
    String name;
    TeleportMode mode;
    World world;
    int minRadius;
    int maxRadius;
    Duration timeout;
    Duration cooldown;
    Set<Biome> biomes;

    public TeleportParams(String name, TeleportMode mode, World world, int minRadius,
                          int maxRadius, Duration timeout, Duration cooldown, Set<Biome> biomes) {
        if (minRadius < 0 || maxRadius <= minRadius)
            throw new IllegalArgumentException("Неверные радиусы");
        this.name = Objects.requireNonNull(name);
        this.mode = Objects.requireNonNull(mode);
        this.world = Objects.requireNonNull(world);
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.timeout = timeout;
        this.cooldown = cooldown;
        if (biomes == null) {
            this.biomes = Set.of();
        } else {
            this.biomes = Set.copyOf(biomes);
        }

        if (mode == TeleportMode.BIOME && this.biomes.isEmpty())
            throw new IllegalArgumentException("для режима BIOME нужено указать хотя бы один биом");

    }
}
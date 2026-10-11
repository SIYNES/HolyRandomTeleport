package ru.siynes.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import ru.siynes.HolyRandomTeleport;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.Supplier;

@UtilityClass
public class SafeSpot {
    private final Set<Material> UNSAFE = EnumSet.of(
            Material.WATER, Material.LAVA, Material.CACTUS, Material.FIRE,
            Material.MAGMA_BLOCK, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.SWEET_BERRY_BUSH);

    private final Executor MAIN = r -> {
        if (Bukkit.isPrimaryThread()) r.run();
        else Bukkit.getScheduler().runTask(HolyRandomTeleport.getInstance(), r);
    };

    private final Executor NEXT_TICK = r ->
            Bukkit.getScheduler().runTask(HolyRandomTeleport.getInstance(), r);

    public int[] ring(int cx, int cz, int min, int max) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double dist = Math.sqrt(r.nextDouble((double) min * min, (double) max * max));
        double a = r.nextDouble(Math.PI * 2);
        return new int[]{cx + (int) (dist * Math.cos(a)), cz + (int) (dist * Math.sin(a))};
    }

    public CompletableFuture<Location> search(World w, Supplier<int[]> picker, long deadlineNanos) {
        return search(w, picker, b -> true, 1, deadlineNanos);
    }

    public CompletableFuture<Location> search(World w, Supplier<int[]> picker,
                                              Predicate<Biome> biomeFilter, int parallel,
                                              long deadlineNanos) {
        if (System.nanoTime() > deadlineNanos) return CompletableFuture.completedFuture(null);

        List<CompletableFuture<Location>> batch = new ArrayList<>(parallel);
        for (int i = 0; i < parallel; i++) batch.add(check(w, picker.get(), biomeFilter));

        return CompletableFuture.allOf(batch.toArray(new CompletableFuture[0])).thenCompose(v -> {
            for (CompletableFuture<Location> f : batch) {
                Location found = f.join();
                if (found != null) return CompletableFuture.completedFuture(found);
            }
            return CompletableFuture.runAsync(() -> { }, NEXT_TICK)
                    .thenCompose(x -> search(w, picker, biomeFilter, parallel, deadlineNanos));
        });
    }

    private CompletableFuture<Location> check(World w, int[] xz, Predicate<Biome> biomeFilter) {
        int x = xz[0], z = xz[1];

        return w.getChunkAtAsync(x >> 4, z >> 4)
                .thenApplyAsync(chunk -> {
                    int y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
                    Block ground = w.getBlockAt(x, y, z);
                    Biome biome = ground.getBiome();

                    boolean safe = y > 0 && y < 253
                            && biomeFilter.test(biome)
                            && !biome.name().contains("OCEAN")
                            && !UNSAFE.contains(ground.getType())
                            && w.getBlockAt(x, y + 1, z).isPassable()
                            && w.getBlockAt(x, y + 2, z).isPassable();

                    if (!safe) return null;
                    return new Location(w, x + 0.5, y + 1, z + 0.5);
                }, MAIN)
                .exceptionally(e -> null);
    }
}
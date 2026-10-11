package ru.siynes.locationfinder.impl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import ru.siynes.api.model.TeleportParams;
import ru.siynes.locationfinder.LocationFinder;
import ru.siynes.utils.SafeSpot;

import java.util.concurrent.CompletableFuture;

public class RandomFinder implements LocationFinder {
    @Override
    public CompletableFuture<Location> find(Player player, TeleportParams p) {
        World w = p.getWorld();
        WorldBorder wb = w.getWorldBorder();
        Location c = wb.getCenter();
        int max = (int) Math.min(p.getMaxRadius(), wb.getSize() / 2 - 16);
        long deadline = System.nanoTime() + p.getTimeout().toNanos();

        return SafeSpot.search(w,
                () -> SafeSpot.ring(c.getBlockX(), c.getBlockZ(), p.getMinRadius(), max),
                deadline);
    }
}
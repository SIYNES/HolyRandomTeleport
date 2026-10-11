package ru.siynes.locationfinder.impl;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.siynes.api.model.TeleportParams;
import ru.siynes.locationfinder.LocationFinder;
import ru.siynes.utils.SafeSpot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class NearPlayerFinder implements LocationFinder {
    @Override
    public CompletableFuture<Location> find(Player player, TeleportParams p) {
        World w = p.getWorld();
        List<Player> candidates = new ArrayList<>();
        for (Player t : w.getPlayers()) {
            if (t != player && t.getGameMode() != GameMode.SPECTATOR) candidates.add(t);
        }
        if (candidates.isEmpty()) return CompletableFuture.completedFuture(null);

        Location tl = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size())).getLocation();
        long deadline = System.nanoTime() + p.getTimeout().toNanos();

        return SafeSpot.search(w,
                () -> SafeSpot.ring(tl.getBlockX(), tl.getBlockZ(), p.getMinRadius(), p.getMaxRadius()),
                deadline);
    }
}
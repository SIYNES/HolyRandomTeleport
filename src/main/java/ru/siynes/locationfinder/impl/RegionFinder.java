package ru.siynes.locationfinder.impl;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.espi.protectionstones.PSRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.siynes.api.model.TeleportParams;
import ru.siynes.locationfinder.LocationFinder;
import ru.siynes.utils.SafeSpot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RegionFinder implements LocationFinder {
    @Override
    public CompletableFuture<Location> find(Player player, TeleportParams p) {
        World world = p.getWorld();
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));
        if (manager == null) return CompletableFuture.completedFuture(null);

        UUID id = player.getUniqueId();
        List<ProtectedRegion> all = new ArrayList<>(manager.getRegions().values());
        Collections.shuffle(all);

        for (ProtectedRegion region : all) {
            PSRegion ps = PSRegion.fromWGRegion(world, region);
            if (ps == null) continue;
            if (ps.isOwner(id) || ps.isMember(id)) continue;

            return searchAround(world, region, p);
        }
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Location> searchAround(World world, ProtectedRegion region, TeleportParams p) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        int cx = (min.getBlockX() + max.getBlockX()) / 2;
        int cz = (min.getBlockZ() + max.getBlockZ()) / 2;

        double halfX = (max.getBlockX() - min.getBlockX() + 1) / 2.0;
        double halfZ = (max.getBlockZ() - min.getBlockZ() + 1) / 2.0;
        int base = (int) Math.ceil(Math.sqrt(halfX * halfX + halfZ * halfZ));

        int minR = base + p.getMinRadius();
        int maxR = base + p.getMaxRadius();
        long deadline = System.nanoTime() + p.getTimeout().toNanos();

        return SafeSpot.search(world, () -> SafeSpot.ring(cx, cz, minR, maxR), deadline);
    }
}
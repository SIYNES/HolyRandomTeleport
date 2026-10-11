package ru.siynes.locationfinder;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.siynes.api.model.TeleportParams;

import java.util.concurrent.CompletableFuture;

public interface LocationFinder {
    CompletableFuture<Location> find(Player player, TeleportParams params);
}
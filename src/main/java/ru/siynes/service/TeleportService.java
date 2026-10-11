package ru.siynes.service;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.siynes.HolyRandomTeleport;
import ru.siynes.api.model.TeleportMode;
import ru.siynes.api.model.TeleportParams;
import ru.siynes.configs.MessagesConfig;
import ru.siynes.configs.SettingsConfig;
import ru.siynes.locationfinder.LocationFinder;
import ru.siynes.locationfinder.impl.BiomeFinder;
import ru.siynes.locationfinder.impl.NearPlayerFinder;
import ru.siynes.locationfinder.impl.RandomFinder;
import ru.siynes.locationfinder.impl.RegionFinder;
import ru.siynes.utils.ColorUtil;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class TeleportService {
    private final JavaPlugin plugin;
    private final MessagesConfig messagesConfig;
    private final SettingsConfig settingsConfig;
    private final Map<TeleportMode, LocationFinder> finders = new EnumMap<>(TeleportMode.class);
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> searching = ConcurrentHashMap.newKeySet();

    public TeleportService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messagesConfig = HolyRandomTeleport.getInstance().getConfig(MessagesConfig.class);
        this.settingsConfig = HolyRandomTeleport.getInstance().getConfig(SettingsConfig.class);
        finders.put(TeleportMode.RANDOM, new RandomFinder());
        finders.put(TeleportMode.NEAR_PLAYER, new NearPlayerFinder());
        finders.put(TeleportMode.BIOME, new BiomeFinder());
        if (Bukkit.getPluginManager().isPluginEnabled("ProtectionStones")) {
            finders.put(TeleportMode.REGION, new RegionFinder());
        }
    }

    public void teleport(Player player, TeleportParams params) {
        UUID id = player.getUniqueId();
        boolean bypass = player.hasPermission("holyrandomteleport.bypass");

        LocationFinder finder = finders.get(params.getMode());
        if (finder == null) {
            send(player, messagesConfig.getFailedTeleport());
            return;
        }

        long left = getCooldownLeft(id, params.getName());
        if (left > 0 && !bypass) {
            send(player, messagesConfig.getCooldown()
                    .replace("{time}", String.valueOf((left + 999) / 1000)));
            return;
        }

        if (!searching.add(id)) return;

        boolean useWarmup = settingsConfig.isWarmupEnabled()
                && settingsConfig.getWarmupSeconds() > 0
                && !bypass;

        CompletableFuture<Boolean> warmup;
        if (useWarmup) {
            warmup = startWarmup(player);
        } else {
            warmup = CompletableFuture.completedFuture(true);
        }

        warmup.thenAccept(ok -> {
            if (!ok || !player.isOnline()) {
                searching.remove(id);
                return;
            }
            runSearch(player, params, finder);
        });
    }

    private void runSearch(Player player, TeleportParams params, LocationFinder finder) {
        UUID id = player.getUniqueId();

        CompletableFuture<Location> future;
        try {
            future = finder.find(player, params);
        } catch (Exception e) {
            searching.remove(id);
            send(player, messagesConfig.getFailedTeleport());
            return;
        }

        future.orTimeout(params.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((loc, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
                    searching.remove(id);
                    if (!player.isOnline()) return;
                    if (ex != null || loc == null) {
                        send(player, messagesConfig.getFailedTeleport());
                        return;
                    }

                    player.teleportAsync(loc).thenAccept(ok -> {
                        if (!ok) return;
                        setCooldown(id, params.getName(), params.getCooldown().toMillis());
                        send(player, messagesConfig.getSuccessfulTeleport()
                                .replace("{x}", String.valueOf(loc.getBlockX()))
                                .replace("{y}", String.valueOf(loc.getBlockY()))
                                .replace("{z}", String.valueOf(loc.getBlockZ())));
                    });
                }));
    }

    private CompletableFuture<Boolean> startWarmup(Player player) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        int seconds = settingsConfig.getWarmupSeconds();
        boolean cancelOnMove = settingsConfig.isCancelOnMove();
        boolean cancelOnDamage = settingsConfig.isCancelOnDamage();
        Location start = player.getLocation();

        new BukkitRunnable() {
            int ticks = 0;
            double lastHealth = player.getHealth();

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    result.complete(false);
                    return;
                }

                Location now = player.getLocation();
                if (cancelOnMove && (now.getBlockX() != start.getBlockX() || now.getBlockZ() != start.getBlockZ())) {
                    cancel();
                    send(player, messagesConfig.getWarmupCancelledMove());
                    result.complete(false);
                    return;
                }

                double health = player.getHealth();
                if (cancelOnDamage && health < lastHealth) {
                    cancel();
                    send(player, messagesConfig.getWarmupCancelledDamage());
                    result.complete(false);
                    return;
                }
                lastHealth = health;

                if (ticks % 20 == 0) {
                    int remaining = seconds - ticks / 20;
                    actionBar(player, messagesConfig.getWarmupStarted()
                            .replace("{time}", String.valueOf(remaining)));
                }

                ticks += 5;
                if (ticks >= seconds * 20) {
                    cancel();
                    result.complete(true);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);

        return result;
    }

    private long getCooldownLeft(UUID id, String teleportName) {
        Map<String, Long> playerCooldowns = cooldowns.get(id);
        if (playerCooldowns == null) return 0;

        Long until = playerCooldowns.get(teleportName);
        if (until == null) return 0;

        return until - System.currentTimeMillis();
    }

    private void setCooldown(UUID id, String teleportName, long millis) {
        cooldowns.computeIfAbsent(id, k -> new ConcurrentHashMap<>())
                .put(teleportName, System.currentTimeMillis() + millis);
    }

    private void send(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        player.sendMessage(ColorUtil.translate(message));
    }

    private void actionBar(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(ColorUtil.translate(message)));
    }
}

package ru.siynes.configs;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.siynes.api.model.TeleportMode;
import ru.siynes.api.model.TeleportParams;

import java.io.File;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TeleportRegistry {
    private final JavaPlugin plugin;
    private final Map<String, TeleportParams> teleports = new HashMap<>();

    public TeleportRegistry(JavaPlugin plugin) { this.plugin = plugin; }

    public void load() {
        teleports.clear();
        File dir = new File(plugin.getDataFolder(), "teleports");
        if (!dir.exists()) {
            plugin.saveResource("teleports/default.yml", false);
            plugin.saveResource("teleports/long.yml", false);
            plugin.saveResource("teleports/desert.yml", false);
            plugin.saveResource("teleports/near.yml", false);
            plugin.saveResource("teleports/base.yml", false);
        }
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;

        for (File f : files) {
            String name = f.getName().substring(0, f.getName().length() - 4).toLowerCase();
            ConfigurationSection s = YamlConfiguration.loadConfiguration(f).getConfigurationSection("teleport");
            if (s == null) { plugin.getLogger().warning(f.getName() + " нет секции teleport"); continue; }

            World world = Bukkit.getWorld(s.getString("world", "world"));
            if (world == null) { plugin.getLogger().warning(f.getName() + " мир не найден"); continue; }

            try {
                TeleportMode mode = TeleportMode.valueOf(s.getString("mode", "RANDOM").toUpperCase());

                if (mode == TeleportMode.BIOME && world.getEnvironment() != World.Environment.NORMAL) {
                    plugin.getLogger().warning(f.getName() + " режим BIOME поддерживает только обычный мир");
                    continue;
                }

                Set<Biome> biomes = EnumSet.noneOf(Biome.class);
                for (String raw : s.getStringList("biomes")) {
                    try {
                        biomes.add(Biome.valueOf(raw.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning(f.getName() + " неизвестный биом " + raw);
                    }
                }

                teleports.put(name, TeleportParams.builder()
                        .name(name)
                        .mode(mode)
                        .world(world)
                        .minRadius(s.getInt("min-radius"))
                        .maxRadius(s.getInt("max-radius"))
                        .timeout(Duration.ofSeconds(s.getInt("timeout")))
                        .cooldown(Duration.ofSeconds(s.getInt("cooldown")))
                        .biomes(biomes)
                        .build());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(f.getName() + " " + e.getMessage());
            }
        }
    }

    public TeleportParams get(String name) { return teleports.get(name); }
    public Set<String> names() { return teleports.keySet(); }
}
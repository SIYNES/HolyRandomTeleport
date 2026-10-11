package ru.siynes.api.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.siynes.HolyRandomTeleport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Getter
public abstract class AbstractConfig {
    private final String name;
    private File file;
    private FileConfiguration config;

    // ВАЖНО: в наследниках не задавайте значения полям при объявлении (private int x = 3;),
    // инициализаторы выполняются ПОСЛЕ этого конструктора и затрут результат parse()
    public AbstractConfig(String name) {
        this.name = name;
        reload();
    }

    public abstract void parse();

    public final void reload() {
        HolyRandomTeleport plugin = HolyRandomTeleport.getInstance();
        file = new File(plugin.getDataFolder(), name + ".yml");
        if (!file.exists()) plugin.saveResource(name + ".yml", false);

        config = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource(name + ".yml")) {
            if (in != null) {
                config.setDefaults(YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException ignored) { }

        parse();
    }
}

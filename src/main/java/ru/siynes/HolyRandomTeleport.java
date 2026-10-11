package ru.siynes;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.siynes.api.config.AbstractConfig;
import ru.siynes.commands.TeleportCommand;
import ru.siynes.configs.MessagesConfig;
import ru.siynes.configs.SettingsConfig;
import ru.siynes.configs.TeleportRegistry;
import ru.siynes.service.TeleportService;

import java.util.HashMap;
import java.util.Map;

@Getter
public final class HolyRandomTeleport extends JavaPlugin {
    private final Map<Class<? extends AbstractConfig>, AbstractConfig> configs = new HashMap<>();
    @Getter private static HolyRandomTeleport instance;
    private TeleportRegistry teleportRegistry;
    private TeleportService teleportService;

    @Override
    public void onEnable() {
        instance = this;
        loadConfigs(new MessagesConfig(), new SettingsConfig());

        teleportRegistry = new TeleportRegistry(this);
        teleportService = new TeleportService(this);

        getCommand("rtp").setExecutor(new TeleportCommand());

        Bukkit.getScheduler().runTask(this, teleportRegistry::load);
    }

    @Override
    public void onDisable() {

    }

    private void loadConfigs(AbstractConfig... configs) {
        for (AbstractConfig config : configs) {
            this.configs.put(config.getClass(), config);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractConfig> T getConfig(Class<T> clazz) {
        return (T) configs.get(clazz);
    }
}

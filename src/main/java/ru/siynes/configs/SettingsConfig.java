package ru.siynes.configs;

import lombok.Getter;
import ru.siynes.api.config.AbstractConfig;

@Getter
public class SettingsConfig extends AbstractConfig {
    public SettingsConfig() {
        super("settings");
    }

    private boolean warmupEnabled;
    private int warmupSeconds;
    private boolean cancelOnMove;
    private boolean cancelOnDamage;

    @Override
    public void parse() {
        this.warmupEnabled = getConfig().getBoolean("settings.warmup.enabled");
        this.warmupSeconds = getConfig().getInt("settings.warmup.seconds");
        this.cancelOnMove = getConfig().getBoolean("settings.warmup.cancel-on-move");
        this.cancelOnDamage = getConfig().getBoolean("settings.warmup.cancel-on-damage");
    }
}

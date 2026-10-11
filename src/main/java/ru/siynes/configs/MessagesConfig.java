package ru.siynes.configs;

import lombok.Getter;
import ru.siynes.api.config.AbstractConfig;

@Getter
public class MessagesConfig extends AbstractConfig {
    public MessagesConfig() {
        super("messages");
    }

    private String successfulTeleport;
    private String warmupStarted;
    private String warmupCancelledMove;
    private String warmupCancelledDamage;
    private String failedTeleport;
    private String cooldown;
    private String configReload;
    private String permissionDeny;

    @Override
    public void parse() {
        this.successfulTeleport = getConfig().getString("messages.successful-teleport");
        this.warmupStarted = getConfig().getString("messages.warmup-started");
        this.warmupCancelledMove = getConfig().getString("messages.warmup-cancelled-move");
        this.warmupCancelledDamage = getConfig().getString("messages.warmup-cancelled-damage");
        this.failedTeleport = getConfig().getString("messages.failed-teleport");
        this.cooldown = getConfig().getString("messages.cooldown");
        this.configReload = getConfig().getString("messages.config-reload");
        this.permissionDeny = getConfig().getString("messages.permission-deny");
    }
}

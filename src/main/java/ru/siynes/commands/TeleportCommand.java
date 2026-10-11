package ru.siynes.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import ru.siynes.HolyRandomTeleport;
import ru.siynes.api.command.CommandManager;
import ru.siynes.api.command.SubCommand;
import ru.siynes.api.model.TeleportParams;
import ru.siynes.configs.MessagesConfig;
import ru.siynes.configs.SettingsConfig;
import ru.siynes.utils.ColorUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TeleportCommand extends CommandManager {
    private MessagesConfig messagesConfig;

    public TeleportCommand() {
        messagesConfig = HolyRandomTeleport.getInstance().getConfig(MessagesConfig.class);
        addSub(new ReloadSubCommand(), new String[]{"reload"}, new Permission("holyrandomteleport.reload"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            SubCommandWrapper w = getWrapperFromLabel(args[0]);
            if (w != null) {
                if (w.getPermission() != null && !sender.hasPermission(w.getPermission())) {
                    sender.sendMessage(
                            ColorUtil.translate(messagesConfig.getPermissionDeny()));
                    return true;
                }
                w.getCommand().onExecute(sender, Arrays.copyOfRange(args, 1, args.length));
                return true;
            }
        }

        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        String name = "default";
        if (args.length > 0) name = args[0].toLowerCase();
        TeleportParams params = HolyRandomTeleport.getInstance().getTeleportRegistry().get(name);
        if (params == null || !player.hasPermission("holyrandomteleport.teleport." + name)) {
            sender.sendMessage(
                    ColorUtil.translate(messagesConfig.getPermissionDeny()));
            return true;
        }
        HolyRandomTeleport.getInstance().getTeleportService().teleport(player, params);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (SubCommandWrapper w : getSubCommands()) {
                if (w.getPermission() == null || sender.hasPermission(w.getPermission()))
                    out.add(w.getAliases()[0]);
            }
            for (String name : HolyRandomTeleport.getInstance().getTeleportRegistry().names()) {
                if (sender.hasPermission("holyrandomteleport.teleport." + name)) out.add(name);
            }
            String prefix = args[0].toLowerCase();
            out.removeIf(s -> !s.startsWith(prefix));
            return out;
        }
        SubCommandWrapper w = getWrapperFromLabel(args[0]);
        if (w == null) return Collections.emptyList();
        return w.getCommand().onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    private class ReloadSubCommand implements SubCommand {
        @Override
        public void onExecute(CommandSender sender, String[] args) {
            messagesConfig.reload();
            HolyRandomTeleport.getInstance().getConfig(SettingsConfig.class).reload();
            HolyRandomTeleport.getInstance().getTeleportRegistry().load();
            sender.sendMessage(
                    ColorUtil.translate(messagesConfig.getConfigReload()
                            .replace("{teleports_count}",
                                    String.valueOf(
                                            HolyRandomTeleport.getInstance().getTeleportRegistry().names().size()
                                    )
                            )
                    )
            );
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, String[] args) {
            return Collections.emptyList();
        }
    }
}
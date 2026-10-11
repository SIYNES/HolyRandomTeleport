package ru.siynes.api.command;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.command.TabExecutor;
import org.bukkit.permissions.Permission;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class CommandManager implements TabExecutor {
    @Getter(AccessLevel.PROTECTED)
    private final List<SubCommandWrapper> subCommands = new ArrayList<>();

    protected void addSub(SubCommand command, String[] aliases, Permission permission) {
        subCommands.add(new SubCommandWrapper(command, aliases, permission));
    }

    @Nullable
    protected SubCommandWrapper getWrapperFromLabel(String label) {
        for (SubCommandWrapper wrapper : subCommands) {
            for (String aliases : wrapper.getAliases()) {
                if (aliases.equalsIgnoreCase(label)) {
                    return wrapper;
                }
            }
        }
        return null;
    }

    protected List<String> getAliases() {
        final List<String> result = new ArrayList<>();
        for (final SubCommandWrapper wrapper : subCommands) {
            String alias = wrapper.aliases[0];
            result.add(alias);
        }
        return result;
    }

    @AllArgsConstructor
    @Getter
    public static class SubCommandWrapper {
        private final SubCommand command;
        private final String[] aliases;
        private final Permission permission;
    }
}
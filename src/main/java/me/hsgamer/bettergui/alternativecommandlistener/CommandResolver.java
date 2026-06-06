package me.hsgamer.bettergui.alternativecommandlistener;

import me.hsgamer.bettergui.BetterGUI;
import me.hsgamer.bettergui.manager.MenuCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

public final class CommandResolver {

    public Optional<Command> findAlternativeCommand(String label) {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            return Optional.empty();
        }

        Command command = commandMap.getCommand(label);
        if (command == null) {
            return Optional.empty();
        }

        Map<String, Command> menuCommands = BetterGUI.getInstance().get(MenuCommandManager.class).getRegisteredMenuCommand();
        if (!menuCommands.values().contains(command)) {
            return Optional.of(command);
        }

        Map<String, Command> knownCommands = getKnownCommands(commandMap);
        if (knownCommands == null) {
            return Optional.empty();
        }

        String suffix = ":" + label.toLowerCase(java.util.Locale.ENGLISH);
        for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
            String key = entry.getKey().toLowerCase(java.util.Locale.ENGLISH);
            if (key.endsWith(suffix)) {
                Command fallbackCommand = entry.getValue();
                if (!menuCommands.values().contains(fallbackCommand)) {
                    return Optional.of(fallbackCommand);
                }
            }
        }

        return Optional.empty();
    }

    private CommandMap getCommandMap() {
        try {
            return (CommandMap) Bukkit.getServer().getClass().getMethod("getCommandMap").invoke(Bukkit.getServer());
        } catch (Exception e) {
            try {
                Field field = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
                field.setAccessible(true);
                return (CommandMap) field.get(Bukkit.getPluginManager());
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> getKnownCommands(CommandMap commandMap) {
        try {
            Field field = SimpleCommandMap.class.getDeclaredField("knownCommands");
            field.setAccessible(true);
            return (Map<String, Command>) field.get(commandMap);
        } catch (Exception e) {
            return null;
        }
    }
}

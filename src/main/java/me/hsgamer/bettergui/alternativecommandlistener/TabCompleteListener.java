package me.hsgamer.bettergui.alternativecommandlistener;

import me.hsgamer.bettergui.BetterGUI;
import me.hsgamer.bettergui.manager.MenuCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TabCompleteListener implements Listener {
    private final AlternativeCommandListener addon;

    public TabCompleteListener(AlternativeCommandListener addon) {
        this.addon = addon;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(TabCompleteEvent event) {
        String buffer = event.getBuffer();
        if (!buffer.startsWith("/")) {
            return;
        }

        String rawCommand = buffer.substring(1);
        if (!addon.isIgnored(rawCommand)) {
            return;
        }

        String[] split = rawCommand.split(" ", -1);
        if (split.length == 0) {
            return;
        }

        String commandLabel = split[0];
        Command originalCommand = getOriginalCommand(commandLabel);
        if (originalCommand == null) {
            return;
        }

        String[] args = new String[0];
        if (split.length > 1) {
            args = Arrays.copyOfRange(split, 1, split.length);
        }

        try {
            List<String> completions = originalCommand.tabComplete(event.getSender(), commandLabel, args);
            if (completions != null) {
                event.setCompletions(completions);
            }
        } catch (Exception e) {
            // Defensive coding to avoid breaking other functionality
        }
    }

    private Command getOriginalCommand(String label) {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            return null;
        }
        Command command = commandMap.getCommand(label);
        if (command == null) {
            return null;
        }

        Map<String, Command> menuCommands = BetterGUI.getInstance().get(MenuCommandManager.class).getRegisteredMenuCommand();
        if (!menuCommands.values().contains(command)) {
            return command;
        }

        Map<String, Command> knownCommands = getKnownCommands(commandMap);
        if (knownCommands == null) {
            return null;
        }

        String suffix = ":" + label.toLowerCase(java.util.Locale.ENGLISH);
        for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
            String key = entry.getKey().toLowerCase(java.util.Locale.ENGLISH);
            if (key.endsWith(suffix)) {
                Command fallbackCommand = entry.getValue();
                if (!menuCommands.values().contains(fallbackCommand)) {
                    return fallbackCommand;
                }
            }
        }

        return null;
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

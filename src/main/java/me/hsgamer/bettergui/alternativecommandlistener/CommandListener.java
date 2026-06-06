package me.hsgamer.bettergui.alternativecommandlistener;

import me.hsgamer.bettergui.BetterGUI;
import me.hsgamer.bettergui.manager.MenuCommandManager;
import me.hsgamer.hscore.collections.map.CaseInsensitiveStringMap;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class CommandListener implements Listener {
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s");
    private final AlternativeCommandListener addon;

    public CommandListener(AlternativeCommandListener addon) {
        this.addon = addon;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) {
            return;
        }

        String rawCommand = event.getMessage().substring(1);
        String[] split = SPACE_PATTERN.split(rawCommand);
        if (split.length == 0) {
            return;
        }

        String command = split[0];
        String[] args = new String[0];
        if (split.length > 1) {
            args = Arrays.copyOfRange(split, 1, split.length);
        }

        if (isIgnored(rawCommand)) {
            Optional<Command> alternativeCommand = findAlternativeCommand(command);
            if (alternativeCommand.isPresent()) {
                event.setCancelled(true);
                try {
                    alternativeCommand.get().execute(event.getPlayer(), command, args);
                } catch (Exception e) {
                    BetterGUI.getInstance().getLogger().log(Level.WARNING,
                            "Error executing alternative command: " + command, e);
                }
            }
            return;
        }

        Map<String, Command> menuCommand = BetterGUI.getInstance().get(MenuCommandManager.class)
                .getRegisteredMenuCommand();
        if (addon.getConfig().isCaseInsensitive()) {
            menuCommand = new CaseInsensitiveStringMap<>(menuCommand);
        }

        if (menuCommand.containsKey(command)) {
            event.setCancelled(true);
            menuCommand.get(command).execute(event.getPlayer(), command, args);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(TabCompleteEvent event) {
        String buffer = event.getBuffer();
        if (!buffer.startsWith("/")) {
            return;
        }

        String rawCommand = buffer.substring(1);
        if (!isIgnored(rawCommand)) {
            return;
        }

        String[] split = rawCommand.split(" ", -1);
        if (split.length == 0) {
            return;
        }

        String commandLabel = split[0];
        Optional<Command> alternativeCommand = findAlternativeCommand(commandLabel);
        if (!alternativeCommand.isPresent()) {
            return;
        }

        String[] args = new String[0];
        if (split.length > 1) {
            args = Arrays.copyOfRange(split, 1, split.length);
        }

        try {
            List<String> completions = alternativeCommand.get().tabComplete(event.getSender(), commandLabel, args);
            if (completions != null) {
                event.setCompletions(completions);
            }
        } catch (Exception e) {
            // Safe fallback
        }
    }

    private Optional<Command> findAlternativeCommand(String label) {
        Map<String, Command> knownCommands = getKnownCommands();
        if (knownCommands == null) {
            return Optional.empty();
        }

        Command command = knownCommands.get(label.toLowerCase());
        if (command == null) {
            return Optional.empty();
        }

        Map<String, Command> menuCommands = BetterGUI.getInstance().get(MenuCommandManager.class)
                .getRegisteredMenuCommand();
        if (!menuCommands.values().contains(command)) {
            return Optional.empty();
        }

        String suffix = ":" + label.toLowerCase();
        for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
            if (entry.getKey().toLowerCase().endsWith(suffix)) {
                Command fallbackCommand = entry.getValue();
                if (!menuCommands.values().contains(fallbackCommand)) {
                    return Optional.of(fallbackCommand);
                }
            }
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> getKnownCommands() {
        try {
            Field field = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            Object commandMap = field.get(Bukkit.getPluginManager());
            Field knownField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownField.setAccessible(true);
            return (Map<String, Command>) knownField.get(commandMap);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isIgnored(String rawCommand) {
        return addon.getConfig().getIgnoredCommands().stream().anyMatch(s -> matchCommand(rawCommand, s)) == addon
                .getConfig().isShouldIgnore();
    }

    private boolean matchCommand(String rawCommand, String pattern) {
        boolean caseInsensitive = addon.getConfig().isCaseInsensitive();
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return caseInsensitive
                    ? rawCommand.regionMatches(true, 0, prefix, 0, prefix.length())
                    : rawCommand.startsWith(prefix);
        }
        return caseInsensitive ? pattern.equalsIgnoreCase(rawCommand) : pattern.equals(rawCommand);
    }
}

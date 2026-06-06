package me.hsgamer.bettergui.alternativecommandlistener;

import org.bukkit.command.Command;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
        Optional<Command> originalCommand = addon.getCommandResolver().findAlternativeCommand(commandLabel);
        if (!originalCommand.isPresent()) {
            return;
        }

        String[] args = new String[0];
        if (split.length > 1) {
            args = Arrays.copyOfRange(split, 1, split.length);
        }

        try {
            List<String> completions = originalCommand.get().tabComplete(event.getSender(), commandLabel, args);
            if (completions != null) {
                event.setCompletions(completions);
            }
        } catch (Exception e) {
            // Defensive coding to avoid breaking other functionality
        }
    }
}

package me.hsgamer.bettergui.alternativecommandlistener;

import me.hsgamer.bettergui.api.addon.GetPlugin;
import me.hsgamer.bettergui.api.addon.Reloadable;
import me.hsgamer.hscore.bukkit.config.BukkitConfig;
import me.hsgamer.hscore.config.proxy.ConfigGenerator;
import me.hsgamer.hscore.expansion.common.Expansion;
import me.hsgamer.hscore.expansion.extra.expansion.DataFolder;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

import java.io.File;

public final class AlternativeCommandListener implements Expansion, GetPlugin, Reloadable, DataFolder {
    private final CommandConfig config = ConfigGenerator.newInstance(CommandConfig.class, new BukkitConfig(new File(getDataFolder(), "config.yml")));
    private final CommandListener commandListener = new CommandListener(this);
    private final TabCompleteListener tabCompleteListener = new TabCompleteListener(this);
    private final CommandResolver commandResolver = new CommandResolver();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(commandListener, getPlugin());
        Bukkit.getPluginManager().registerEvents(tabCompleteListener, getPlugin());
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(commandListener);
        HandlerList.unregisterAll(tabCompleteListener);
    }

    @Override
    public void onReload() {
        config.reloadConfig();
    }

    public CommandConfig getConfig() {
        return config;
    }

    public CommandResolver getCommandResolver() {
        return commandResolver;
    }

    public boolean isIgnored(String rawCommand) {
        return config.getIgnoredCommands().stream().anyMatch(s -> matchCommand(rawCommand, s)) == config.isShouldIgnore();
    }

    private boolean matchCommand(String rawCommand, String pattern) {
        boolean caseInsensitive = config.isCaseInsensitive();
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return caseInsensitive
                    ? rawCommand.regionMatches(true, 0, prefix, 0, prefix.length())
                    : rawCommand.startsWith(prefix);
        }
        return caseInsensitive ? pattern.equalsIgnoreCase(rawCommand) : pattern.equals(rawCommand);
    }
}

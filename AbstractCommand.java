package dev.tezvn.elitechest.commands;

import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AbstractCommand extends BukkitCommand {

    private final JavaPlugin plugin;

    private List<SubCommand> subCommands;

    private String noPermissionsMessage;

    private String noSubCommandFoundMessage;

    private String noConsoleAllowMessage;

    public AbstractCommand(JavaPlugin plugin, @NotNull String name, @NotNull String description, @NotNull String usageMessage, @NotNull List<String> aliases) {
        super(name.toLowerCase(), description, usageMessage,
                aliases.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toList()));
        this.plugin = plugin;
        this.subCommands = Lists.newArrayList();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            // Consoleo
            if (args.length > 0) {
                String name = args[0];
                Optional<SubCommand> optCommand = this.subCommands.stream().filter(new Predicate<SubCommand>() {
                    @Override
                    public boolean test(SubCommand command) {
                        boolean matchAliases = command.getAliases().contains(name);
                        return command.getName().equalsIgnoreCase(name) || matchAliases;
                    }
                }).findAny();

                if (!optCommand.isPresent()) {
                    sender.sendMessage(this.getNoSubCommandFoundMessage());
                    return true;
                }

                SubCommand command = optCommand.get();

                if (!command.allowConsole()) {
                    sender.sendMessage(this.getNoConsoleAllowMessage());
                    return true;
                }

                optCommand.get().consoleExecute(sender, args);
            }

            return true;
        }

        Player player = (Player) sender;
        if (args.length > 0) {
            String name = args[0];
            Optional<SubCommand> optCommand = this.subCommands.stream().filter(new Predicate<SubCommand>() {
                @Override
                public boolean test(SubCommand command) {
                    boolean matchAliases = command.getAliases().contains(name);
                    return command.getName().equalsIgnoreCase(name) || matchAliases;
                }
            }).findAny();

            if (!optCommand.isPresent()) {
                sender.sendMessage(this.getNoSubCommandFoundMessage());
                return true;
            }

            SubCommand command = optCommand.get();
            if (!player.hasPermission(command.getPermissions())) {
                sender.sendMessage(this.getNoPermissionsMessage());
                return true;
            }

            optCommand.get().playerExecute(sender, args);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args)
            throws IllegalArgumentException {
        return new CommandCompleter(this).onTabComplete(sender, alias, args);
    }

    public AbstractCommand addSubCommand(SubCommand... commands) {
        this.subCommands.addAll(Arrays.asList(commands));
        return this;
    }

    public boolean isRegistered(SubCommand command) {
        return this.subCommands.stream().anyMatch(c -> c.getName().equals(command.getName()));
    }

    public AbstractCommand register() {
        try {
            if (!getKnownCommands().containsKey(getName())) {
                getKnownCommands().put(getName(), this);
                getKnownCommands().put(plugin.getDescription().getName().toLowerCase() + ":" + getName(), this);
            }
            for (String alias : getAliases()) {
                if (getKnownCommands().containsKey(alias))
                    continue;
                getKnownCommands().put(alias, this);
                getKnownCommands().put(plugin.getDescription().getName().toLowerCase() + ":" + alias, this);
            }
            register(getCommandMap());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public AbstractCommand unregister() {
        try {
            unregister(getCommandMap());
            getKnownCommands().entrySet().removeIf(entry -> entry.getValue() instanceof AbstractCommand);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public CommandMap getCommandMap() throws Exception {
        Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        field.setAccessible(true);
        return (CommandMap) field.get(Bukkit.getServer());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Command> getKnownCommands() throws Exception {
        Field cmField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        cmField.setAccessible(true);
        CommandMap cm = (CommandMap) cmField.get(Bukkit.getServer());
        cmField.setAccessible(false);
        Method method = cm.getClass().getDeclaredMethod("getKnownCommands");
        return (Map<String, Command>) method.invoke(cm, new Object[]{});
    }


    public String getNoPermissionsMessage() {
        return noPermissionsMessage;
    }

    public AbstractCommand setNoPermissionsMessage(String noPermissionsMessage) {
        this.noPermissionsMessage = noPermissionsMessage.replace("&", "§");
        return this;
    }

    public String getNoSubCommandFoundMessage() {
        return noSubCommandFoundMessage;
    }

    public AbstractCommand setNoSubCommandFoundMessage(String noSubCommandFoundMessage) {
        this.noSubCommandFoundMessage = noSubCommandFoundMessage.replace("&", "§");
        return this;
    }

    public String getNoConsoleAllowMessage() {
        return noConsoleAllowMessage;
    }

    public AbstractCommand setNoConsoleAllowMessage(String noConsoleAllowMessage) {
        this.noConsoleAllowMessage = noConsoleAllowMessage.replace("&", "§");
        return this;
    }

    public List<SubCommand> getSubCommands() {
        return subCommands;
    }

    public static abstract class SubCommand {

        public SubCommand() {
        }

        public abstract String getName();

        public abstract String getPermissions();

        public abstract String getDescription();

        public abstract String getUsage();

        public abstract List<String> getAliases();

        public abstract boolean allowConsole();

        public abstract void playerExecute(CommandSender sender, String[] args);

        public abstract void consoleExecute(CommandSender sender, String[] args);

        public abstract List<String> tabComplete(CommandSender sender, String[] args);
    }

    protected static class CommandCompleter {

        private List<SubCommand> commands;

        protected CommandCompleter(AbstractCommand handle) {
            this.commands = handle.getSubCommands();
        }

        private List<String> getMainSuggestions(CommandSender sender, String start) {
            return this.commands.stream().filter(new Predicate<SubCommand>() {
                        @Override
                        public boolean test(SubCommand command) {
                            if (start.length() < 1) {
                                return sender.hasPermission(command.getPermissions());
                            } else {
                                return command.getName().startsWith(start) && sender.hasPermission(command.getPermissions());
                            }
                        }
                    })
                    .map(SubCommand::getName)
                    .collect(Collectors.toList());
        }

        private List<String> getSubSuggestions(CommandSender sender, String[] args) {
            Optional<SubCommand> optCommand = this.commands.stream()
                    .filter(command -> command.getName().equalsIgnoreCase(args[0])
                            && sender.hasPermission(command.getPermissions()))
                    .findAny();

            return optCommand.map(subCommand -> subCommand.tabComplete(sender, args)).orElse(null);
        }

        public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
            if (args.length == 1) {
                return this.getMainSuggestions(sender, args[0]);
            } else if (args.length > 1) {
                return this.getSubSuggestions(sender, args);
            }
            return null;
        }
    }
}

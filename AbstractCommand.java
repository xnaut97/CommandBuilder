import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import me.daaz.vapor.utils.ClickableText;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author TezVN
 */
public abstract class AbstractCommand extends BukkitCommand {

    private final Plugin plugin;

    private final UUID uniqueId = UUID.randomUUID();

    private final Map<String, SubCommand> subCommands = Maps.newHashMap();

    private String noPermissionsMessage = "&cYou don't have permission to access.";

    private String noSubCommandFoundMessage = "&cCommand not found, please use /" + getName() + " help for more.";

    private String noConsoleAllowMessage = "&cThis command is for console only.";

    private String helpHeader;

    private String helpFooter;

    private String helpCommandColor = "&a";

    private String helpDescriptionColor = "&7";

    private int helpSuggestions = 5;

    public AbstractCommand(Plugin plugin, String name, String description, String usageMessage, List<String> aliases) {
        super(name.toLowerCase(), description, usageMessage,
                aliases.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toList()));
        this.plugin = plugin;
        this.helpHeader = "- - - - - - - - - -=[ " + plugin.getName() + " ]=- - - - - - - - - -";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < plugin.getName().length(); i++) {
            sb.append("❘");
        }
        this.helpFooter = "- - - - - - - - - -=[ " + sb + " ]=- - - - - - - - - -";
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void onSingleExecute(CommandSender sender, String[] args) {

    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            if (args.length == 0)
                onSingleExecute(sender, args);
            else {
                String name = args[0];
                SubCommand command = this.subCommands.entrySet().stream()
                        .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                        .map(Map.Entry::getValue).findAny().orElse(null);
                if (command == null) {
                    sender.sendMessage(this.noSubCommandFoundMessage);
                    return true;
                }
                if (!command.allowConsole()) {
                    sender.sendMessage(this.noConsoleAllowMessage);
                    return true;
                }
                command.consoleExecute(sender, args);
            }

            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0)
            onSingleExecute(sender, args);
        else {
            String name = args[0];
            SubCommand command = this.subCommands.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .map(Map.Entry::getValue).findAny().orElse(null);
            if (command == null) {
                sender.sendMessage(this.noSubCommandFoundMessage);
                return true;
            }
            String permission = command.getPermission();
            if (permission == null) {
                command.playerExecute(sender, args);
                return true;
            }
            if (!player.hasPermission(command.getPermission())) {
                sender.sendMessage(this.noPermissionsMessage);
                return true;
            }
            command.playerExecute(sender, args);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args)
            throws IllegalArgumentException {
        return new CommandCompleter(this).onTabComplete(sender, args);
    }

    /**
     * Add sub command to your main command
     *
     * @param commands Sub command to add
     */
    public AbstractCommand addSubCommand(SubCommand... commands) {
        for (SubCommand command : commands) {
            if(command.getName() == null || command.getName().isEmpty())
                continue;
            this.subCommands.putIfAbsent(command.getName(), command);
            if(command.getAliases() != null && !command.getAliases().isEmpty()) {
                for (String alias : command.getAliases()) {
                    this.subCommands.putIfAbsent(alias, command);
                }
            }
            if(command.getPermission() == null)
                continue;
            Bukkit.getPluginManager().addPermission(new Permission(
                    command.getPermission(), command.getPermissionDescription(),
                    command.getPermissionDefault(), command.getChildPermissions()));
        }
        return this;
    }

    /**
     * Register command to server in {@code onEnable()} method
     */
    public AbstractCommand register() {
        try {
            addSubCommand(new AbstractHelpCommand(this));
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

    /**
     * Unregister command from server in {@code onDisable()} method
     */
    public AbstractCommand unregister() {
        try {
            unregister(getCommandMap());
            getKnownCommands().entrySet().removeIf(entry -> {
                if(!(entry.getValue() instanceof AbstractCommand))
                    return false;
                AbstractCommand command = (AbstractCommand) entry.getValue();
                command.getSubCommands().entrySet().stream().filter(e -> e.getValue().getPermission() != null)
                        .forEachOrdered(c -> Bukkit.getPluginManager().removePermission(entry.getValue().getPermission()));
                return command.getUniqueId().equals(this.getUniqueId());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    private CommandMap getCommandMap() throws Exception {
        Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        field.setAccessible(true);
        return (CommandMap) field.get(Bukkit.getServer());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> getKnownCommands() throws Exception {
        Field cmField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        cmField.setAccessible(true);
        CommandMap cm = (CommandMap) cmField.get(Bukkit.getServer());
        cmField.setAccessible(false);
        Map<String, Command> knownCommands;
        try {
            knownCommands = (Map<String, Command>) cm.getClass().getDeclaredMethod("getKnownCommands").invoke(cm);
        } catch (Exception e) {
            Field field = SimpleCommandMap.class.getDeclaredField("knownCommands");
            field.setAccessible(true);
            knownCommands = (Map<String, Command>) field.get(cm);
        }
        return knownCommands;
    }

    /**
     * Get unique id of this command
     *
     * @return Comamnd unique id
     */
    public UUID getUniqueId() {
        return uniqueId;
    }

    /**
     * Set message when player don't have permission to access to sub command
     *
     * @param noPermissionsMessage Message to set
     */
    public AbstractCommand setNoPermissionsMessage(String noPermissionsMessage) {
        this.noPermissionsMessage = noPermissionsMessage;
        return this;
    }

    /**
     * Set message when player's input match no sub command
     *
     * @param noSubCommandFoundMessage Message to set
     */
    public AbstractCommand setNoSubCommandFoundMessage(String noSubCommandFoundMessage) {
        this.noSubCommandFoundMessage = noSubCommandFoundMessage;
        return this;
    }

    /**
     * Set message when command is not allowed for console to use
     *
     * @param noConsoleAllowMessage Message to set
     */
    public AbstractCommand setNoConsoleAllowMessage(String noConsoleAllowMessage) {
        this.noConsoleAllowMessage = noConsoleAllowMessage;
        return this;
    }

    private int getHelpSuggestions() {
        return helpSuggestions;
    }

    public AbstractCommand setHelpSuggestions(int helpSuggestions) {
        this.helpSuggestions = Math.max(5, helpSuggestions);
        return this;
    }

    private String getHelpHeader() {
        return this.helpHeader;
    }

    public AbstractCommand setHelpHeader(String helpHeader) {
        this.helpHeader = helpHeader;
        return this;
    }

    private String getHelpFooter() {
        return this.helpFooter;
    }

    public AbstractCommand setHelpFooter(String helpFooter) {
        this.helpFooter = helpFooter;
        return this;
    }

    private String getHelpCommandColor() {
        return helpCommandColor == null ? "&a" : this.helpCommandColor;
    }

    public AbstractCommand setHelpCommandColor(ChatColor color) {
        return setHelpCommandColor(String.valueOf(color.getChar()));
    }

    public AbstractCommand setHelpCommandColor(String color) {
        this.helpCommandColor = color;
        return this;
    }

    private String getHelpDescriptionColor() {
        return helpDescriptionColor == null ? "&7" : this.helpDescriptionColor;
    }

    public AbstractCommand setHelpDescriptionColor(ChatColor color) {
        return setHelpCommandColor(String.valueOf(color.getChar()));
    }

    public AbstractCommand setHelpDescriptionColor(String color) {
        this.helpDescriptionColor = color;
        return this;
    }

    /**
     * Get list of registered sub commands
     *
     * @return List of sub commands
     */
    public Map<String, SubCommand> getSubCommands() {
        return Collections.unmodifiableMap(this.subCommands);
    }

    public static abstract class SubCommand {

        private final Map<String, Boolean> childrens = Maps.newHashMap();

        public SubCommand() {
        }

        public Map<String, Boolean> getChildPermissions() {
            return Collections.unmodifiableMap(this.childrens);
        }

        public void addChildPermission(String permission, boolean child) {
            this.childrens.put(permission, child);
        }

        public void removeChildPermission(String permission) {
            this.childrens.remove(permission);
        }

        /**
         * Get name of sub command
         *
         * @return Sub command name
         */
        public abstract String getName();

        /**
         * Get permission of sub command
         *
         * @return Sub command permission
         */
        public abstract String getPermission();

        /**
         * Get description of permission.
         *
         * @return Permission description.
         */
        public abstract String getPermissionDescription();

        /**
         * Get permission default of command.
         *
         * @return Permission default mode.
         */
        public abstract PermissionDefault getPermissionDefault();

        /**
         * Get description of sub command
         *
         * @return Sub command description
         */
        public abstract String getDescription();

        /**
         * Get usage of sub command
         *
         * @return Sub command usage
         */
        public abstract String getUsage();

        /**
         * Get list of aliases of sub command
         *
         * @return Sub command aliases
         */
        public abstract List<String> getAliases();

        /**
         * Allow console to use this command
         *
         * @return True if allow, otherwise false
         */
        public abstract boolean allowConsole();

        /**
         * Player execution
         */
        public abstract void playerExecute(CommandSender sender, String[] args);

        /**
         * Console execution
         */
        public abstract void consoleExecute(CommandSender sender, String[] args);

        /**
         * Tab complete for sub command
         */
        public abstract List<String> tabComplete(CommandSender sender, String[] args);
    }

    protected static class CommandCompleter {

        private final Map<String, SubCommand> commands;

        protected CommandCompleter(AbstractCommand handle) {
            this.commands = handle.getSubCommands();
        }

        private List<SubCommand> getCommands(CommandSender sender, String start) {
            return this.commands.values().stream().filter(command -> {
                if(!command.getName().startsWith(start))
                    return false;
                boolean hasPermission = command.getPermission() == null || !command.getPermission().isEmpty();
                return !hasPermission || sender.hasPermission(command.getPermission());
            }).collect(Collectors.toList());
        }

        public List<String> onTabComplete(CommandSender sender, String[] args) {
            if(args.length == 0)
                return null;
            List<SubCommand> commands = getCommands(sender, args[0]);
            if(!commands.isEmpty()) {
                if (args.length == 1)
                    return commands.stream().map(SubCommand::getName).collect(Collectors.toList());
                else {
                    SubCommand found = commands.stream().filter(c -> {
                        boolean matchAlias = false;
                        if (!c.getAliases().isEmpty())
                            matchAlias = c.getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(args[0]));
                        return c.getName().equalsIgnoreCase(args[0]) || matchAlias;
                    }).findAny().orElse(null);
                    if (found != null)
                        return found.tabComplete(sender, args);
                }
            }
            return null;
        }
    }

    protected static class AbstractHelpCommand extends SubCommand {

        private final Map<String, SubCommand> subCommands;

        private final AbstractCommand handle;

        public AbstractHelpCommand(AbstractCommand handle) {
            this.handle = handle;
            this.subCommands = handle.getSubCommands();
        }

        @Override
        public String getName() {
            return "help";
        }

        @Override
        public String getPermission() {
            return handle.getName() + ".command.help";
        }

        @Override
        public String getPermissionDescription() {
            return "Access help command.";
        }

        @Override
        public PermissionDefault getPermissionDefault() {
            return PermissionDefault.TRUE;
        }

        @Override
        public String getDescription() {
            return "Shows available commands.";
        }

        @Override
        public String getUsage() {
            return handle.getName() + " help [page]";
        }

        @Override
        public List<String> getAliases() {
            return Collections.singletonList("?");
        }

        @Override
        public boolean allowConsole() {
            return true;
        }

        @Override
        public void playerExecute(CommandSender sender, String[] args) {
            if (args.length == 1) {
                handleCommands(sender, 0);
                return;
            }
            int page = getPage(args[1]);
            handleCommands(sender, page);
        }

        @Override
        public void consoleExecute(CommandSender sender, String[] args) {
            if (args.length == 1) {
                handleCommands(sender, 0);
                return;
            }
            int page = getPage(args[1]);
            handleCommands(sender, page);
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 2) {
                int max = handle.getSubCommands().size() / handle.getHelpSuggestions();
                List<Integer> index = Lists.newArrayList();
                for (int i = 0; i < max; i++) {
                    index.add(i);
                }
                return index.stream().map(String::valueOf).filter(i -> i.startsWith(args[1])).collect(Collectors.toList());
            }
            return null;
        }

        private int getPage(String str) {
            try {
                int page = Integer.parseInt(str);
                return Math.max(page, 0);
            } catch (Exception e) {
                return 0;
            }
        }

        private void handleCommands(CommandSender sender, int page) {
            List<SubCommand> filter = subCommands.values().stream().filter(command -> {
                        boolean hasPermission = command.getPermission() != null || !command.getPermission().isEmpty();
                        return !hasPermission || sender.hasPermission(command.getPermission());
                    }).collect(Collectors.toList());
            int max = Math.min(handle.getHelpSuggestions() * (page + 1), filter.size());
            if (handle.getHelpHeader() != null)
                sender.sendMessage(handle.getHelpHeader());
            for (int i = page * handle.getHelpSuggestions(); i < max; i++) {
                SubCommand command = filter.get(i);
                TextComponent clickableCommand = createClickableCommand(command);
                if (sender instanceof Player)
                    ((Player) sender).spigot().sendMessage(clickableCommand);
                else
                    sender.sendMessage(handle.getHelpCommandColor() + "/" + handle.getUsage() + ": "
                            + handle.getHelpDescriptionColor() + command.getDescription());
            }
            if (sender instanceof Player) {
                Player player = (Player) sender;
                TextComponent previousPage = createClickableButton("&e&l«",
                        "/" + handle.getName() + " help " + (page - 1),
                        "&7Previous page");
                TextComponent nextPage = createClickableButton("&e&l»",
                        "/" + handle.getName() + " help " + (page + 1),
                        "&7Next page");
                TextComponent pageInfo = createClickableButton(" &e&l" + (page + 1) + " ",
                        null, "&7You're in page " + (page + 1));
                ClickableText spacing = new ClickableText("                       ");
                boolean canNextPage = handle.getHelpSuggestions() * (page + 1) < filter.size();
                if (page < 1) {
                    if (canNextPage)
                        player.spigot().sendMessage(spacing.build(), pageInfo, nextPage);
                    else
                        player.spigot().sendMessage(spacing.build(), pageInfo);

                } else {
                    if (canNextPage)
                        player.spigot().sendMessage(spacing.build(), previousPage, pageInfo, nextPage);
                    else
                        player.spigot().sendMessage(spacing.build(), previousPage, pageInfo);
                }
            }
            if (handle.getHelpFooter() != null)
                sender.sendMessage(handle.getHelpFooter());
        }

        private TextComponent createClickableCommand(SubCommand command) {
            return new ClickableText(handle.getHelpCommandColor() + "/" + command.getUsage() + ": "
                    + handle.getHelpDescriptionColor() + command.getDescription())
                    .setHoverAction(HoverEvent.Action.SHOW_TEXT, "&7Click to get this command.")
                    .setClickAction(ClickEvent.Action.SUGGEST_COMMAND, "/" + command.getUsage())
                    .build();
        }

        private TextComponent createClickableButton(String name, String clickAction, String... hoverAction) {
            ClickableText clickableText = new ClickableText(name);
            if (hoverAction.length > 0)
                clickableText.setHoverAction(HoverEvent.Action.SHOW_TEXT, hoverAction);
            if (clickAction != null)
                clickableText.setClickAction(ClickEvent.Action.RUN_COMMAND, clickAction);
            return clickableText.build();
        }
    }
}

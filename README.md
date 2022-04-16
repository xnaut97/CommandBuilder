# ❏ General

Register/unregister command at runtime without writing in ``plugin.yml``
<br>Spigot post: [https://www.spigotmc.org/threads/commandbuilder-register-unregister-command-at-runtime.551946/](https://www.spigotmc.org/threads/commandbuilder-register-unregister-command-at-runtime.551946/)
***

# ❏ Installation

Copy and paste `AbstractCommand.java` to your project
***

# ❏ Instruction

You can register directly in **onEnable()** method
<br>or 
<br>create a manager and then register all together (recommend)
<br>

**Main Command**

```java
public class MainClass extends JavaPlugin {

    private AbstractCommand exampleCommand;

    @Override
    public void onEnable() {
        //You can use Arrays.asList(...) or new ArrayList<>(...)
        this.exampleCommand = new AbstractCommand(
                plugin, //Your main class
                "example", //Command name
                "example command", //Command description
                "/<command>", //Command usage
                Arrays.asList("alias1", "alias2")) //Command aliases
                //Add sub command
                .addSubCommand(new YourSubCommand1(), new YourSubCommand2())
                //You don't need to use color code '§' , i already parse it inside
                .setNoConsoleAllowMessage("&cOnly player can execute this command")
                //When player don't have permission to use sub command
                .setNoPermissionsMessage("&cYou don't have permission to use this command")
                //When player's input not match any sub command
                .setNoSubCommandFoundMessage("&cWrong command, use &6/yourCommand help &cto view more!")
                //Set header and footer for help board.
                .setHelpHeader("&7-----------------=[ &6&l Your Plugin Name &7]=-----------------")
                .setHelpFooter("&7-----------------=[ &6&l❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙ &7]=-----------------")
                .setHelpSuggestions(5)
                .setHelpCommandColor("&a")    ┐__ //Also support ChatColor enum
                .setHelpDescriptionColor("7") ┘
                //The result will like this
                // -----------------=[ &6&l Your Plugin Name &7]=-----------------
                // /yourMainCommandName subCommandName: Sub command description.
                // /yourMainCommandName subCommandName2: Sub command 2 description.
                // /yourMainCommandName subCommandName3: Sub command 3 description.
                // /yourMainCommandName subCommandName4: Sub command 4 description.
                // /yourMainCommandName subCommandName5: Sub command 5 description.
                //                           Current page
                //                 Previous page ┐ ↑ ┌ Next page button
                //                               « 1 »
                // -----------------=[ &6&l❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙❙ &7]=-----------------
                // All buttons are clickable and will lead you to previous/next help page
                // When click on command will suggest it to player
                //Register command onEnable() then we're done :)
                .register();
    }

    @Override
    public void onDisable() {
        //Check null before unregister
        if (this.exampleCommand != null) {
            this.exampleCommand.unregister();
        }
    }
}
```


**Sub Command**

```java
public class YourSubCommand extends SubCommand {

    public YourSubCommand() {
        //Your custom declared field code or sth...
    }

    @Override
    public String getName() {
        return "sub_command_name";
    }

    @Override
    public String getPermissions() {
        return "your.subcommand.permission";
    }

    @Override
    public String getDescription() {
        return "Your description";
    }

    @Override
    public String getUsage() {
        return "&7Syntax: &6/example sub_command_name";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("alias1", "alias2");
    }


    @Override
    public boolean allowConsole() {
        return true;
    }

    @Override
    public void playerExecute(CommandSender sender, String[] args) {
        //Check length
        if (args.length == 1) {
            // [/example sub_command_name] will trigger this
            sender.sendMessage("Hello, player!");
            return;
        }
        //Your execute code
    }

    @Override
    public void consoleExecute(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage("Helle, console!");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        //Leave null if you don't want this sub command have tab complete
        //You don't need to check [length == 1] because it have been already executed
        //All you need is check from next arguments
        // /example sub_command_name [player_name] [args_2] [other_args]...
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return null;
    }
}
```

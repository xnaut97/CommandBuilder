# Installation

Copy and paste `AbstractCommand.java` to your project

# Instruction

You can register directly in **onEnable()** method or create a manager and then register all together
<br>
***
**Main Command**

```java
public class MainClass extends JavaPlugin {

    private AbstractCommand exampleCommand;

    @Override
    public void onEnable() {
        //You can use Arrays.asList(...) or new ArrayList<>(...)
        this.exampleCommand = new AbstractCommand(
                yourMainClass, //Your main class
                "example", //Command name
                "example command", //Command description
                "/<command>", //Command usage
                Arrays.asList("alias1", "alias2")) //Command aliases
                //Add sub command
                .addSubCommand(new YourSubCommand1(), new YourSubCommand2())
                //You don't need to use color code '§' , i already parse it inside
                .setNoConsoleAllowMeessage("&cOnly player can execute this command")
                //When player don't have permission to use sub command
                .setNoPermissionsMessage("&cYou don't have permission to use this command")
                //When player's input not match any sub command
                .setNoSubCommandFoundMessage("&cWrong command, use &6/yourCommand help &cto view more!")
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

***
**Sub Command**

```java
import org.bukkit.Bukkit;
import your.path.AbstractCommand.SubCommand;

import java.util.stream.Collectors;

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
            sender.sendMessage("Hello, player {name}!");
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

package xyz.whatsyouss.frosty.commands;

import xyz.whatsyouss.frosty.commands.impl.*;
import xyz.whatsyouss.frosty.modules.Module;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {
    public static List<Command> commands = new ArrayList<>();

    public static BindCommand bindCommand;
    public static HelpCommand helpCommand;
    public static ItemCommand itemCommand;
    public static PosCommand posCommand;
    public static ToggleCommand toggleCommand;

    public void register() {
        this.addCommand(bindCommand = new BindCommand());
        this.addCommand(helpCommand = new HelpCommand());
        this.addCommand(itemCommand = new ItemCommand());
        this.addCommand(posCommand = new PosCommand());
        this.addCommand(toggleCommand = new ToggleCommand());
    }

    public void addCommand(Command m) {
        commands.add(m);
    }

    public static List<Command> getCommands() {
        return commands;
    }

    public static Command getCommandByName(String input) {
        for (Command c : commands) {
            if (c.getName().equalsIgnoreCase(input) || c.getAliases().equalsIgnoreCase(input)) {
                return c;
            }
        }
        return null;
    }

}

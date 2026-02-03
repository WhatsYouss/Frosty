package xyz.whatsyouss.frosty.commands;

import net.minecraft.text.Text;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.ArrayList;
import java.util.List;

public abstract class Command {
    private final String name;
    private final String alias;
    private final String description;

    public Command(String name, String description, String alias) {
        this.name = name;
        this.description = description;
        this.alias = alias;
    }

    public abstract void execute(String[] args);

    public String getName() {
        return name;
    }

    public String getAliases() {
        return alias;
    }

    public String getDescription() {
        return description;
    }

    protected void sendError(String error) {
        Utils.addChatMessage("§c" + error);
    }
}
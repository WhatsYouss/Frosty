package xyz.whatsyouss.frosty.commands.impl;

import net.minecraft.text.Text;
import xyz.whatsyouss.frosty.commands.Command;
import xyz.whatsyouss.frosty.commands.CommandManager;
import xyz.whatsyouss.frosty.utility.Utils;

import static xyz.whatsyouss.frosty.Frosty.mc;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Help info", "h");
    }

    @Override
    public void execute(String[] args) {
        if (!Utils.nullCheck()) {
            return;
        }
        Utils.addChatMessage("==============================");
        for (Command c : CommandManager.getCommands()) {
            mc.player.sendMessage(Text.literal("§b." + c.getName() + "  §7---  " + c.getDescription() + " - (§b." + c.getAliases() + "§7)"), false);
        }
        Utils.addChatMessage("==============================");
    }
}

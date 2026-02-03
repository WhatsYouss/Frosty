package xyz.whatsyouss.frosty.commands.impl;

import xyz.whatsyouss.frosty.commands.Command;
import xyz.whatsyouss.frosty.utility.Utils;


import static xyz.whatsyouss.frosty.Frosty.mc;

public class ItemCommand extends Command {
    public ItemCommand() {
        super("item", "Get item info", "i");
    }

    @Override
    public void execute(String[] args) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (mc.player.getMainHandStack() != null && mc.player.getMainHandStack().getCustomName() != null) {
            String itemName = Utils.getLiteral(mc.player.getMainHandStack().getCustomName().toString());
            Utils.addToClipboard(itemName);
        }
    }
}
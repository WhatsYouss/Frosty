package xyz.whatsyouss.frosty.commands;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import xyz.whatsyouss.frosty.gui.ClickGui;
import xyz.whatsyouss.frosty.modules.ModuleManager;

public final class FrostyCommand {
    private static boolean openClickGuiPending;

    private FrostyCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("frosty").executes(context -> {
                    openClickGuiPending = true;
                    return 1;
                })));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!openClickGuiPending) return;
            openClickGuiPending = false;
            if (ModuleManager.ui == null || client.player == null) return;
            if (ModuleManager.ui.isEnabled()) {
                client.gui.setScreen(new ClickGui());
            } else {
                ModuleManager.ui.enable();
            }
        });
    }
}

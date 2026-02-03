package xyz.whatsyouss.frosty.commands.impl;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import xyz.whatsyouss.frosty.commands.Command;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.utility.Utils;

public class BindCommand extends Command {
    public BindCommand() {
        super("bind", "Binds a module to a key", "b");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            sendError("Usage: .bind <module> <key>");
            return;
        }

        Module module = ModuleManager.getModuleByName(args[0]);
        if (module == null) {
            sendError("Module not found: " + args[0]);
            return;
        }

        try {
            int keyCode = parseKey(args[1]);
            module.setBind(keyCode);
            Utils.addChatMessage("Bound " + module.getName() + " to " + getKeyName(keyCode));
        } catch (IllegalArgumentException e) {
            sendError("Invalid key: " + args[1]);
        }
    }

    private int parseKey(String keyStr) {
        if (keyStr.equalsIgnoreCase("none")) {
            return 0;
        }

        if (keyStr.toLowerCase().startsWith("mouse")) {
            try {
                int button = Integer.parseInt(keyStr.substring(5));
                return 1000 + button;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid mouse button");
            }
        }

        // Scroll up/down
        if (keyStr.equalsIgnoreCase("scrollup")) return 1069;
        if (keyStr.equalsIgnoreCase("scrolldown")) return 1070;

        // Keyboard keys
        InputUtil.Key key = InputUtil.fromTranslationKey("key.keyboard." + keyStr.toLowerCase());
        if (key.getCode() == 0) {
            throw new IllegalArgumentException("Unknown key");
        }
        return key.getCode();
    }

    private String getKeyName(int keycode) {
        if (keycode == 0) return "None";
        if (keycode == 1069) return "Scroll Up";
        if (keycode == 1070) return "Scroll Down";
        if (keycode >= 1000) return "Mouse " + (keycode - 1000);

        InputUtil.Key key = InputUtil.Type.KEYSYM.createFromCode(keycode);
        return key.getLocalizedText().getString();
    }
}
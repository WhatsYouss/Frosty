package xyz.whatsyouss.frosty.commands.impl;

import com.mojang.blaze3d.platform.InputConstants;
import xyz.whatsyouss.frosty.commands.Command;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.Locale;

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
        InputConstants.Key key = InputConstants.getKey(keyboardTranslationKey(keyStr));
        if (key.getValue() <= 0) {
            throw new IllegalArgumentException("Unknown key");
        }
        return key.getValue();
    }

    private String keyboardTranslationKey(String keyStr) {
        String normalized = keyStr.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return switch (normalized) {
            case "rshift", "rightshift" -> "key.keyboard.right.shift";
            case "lshift", "leftshift" -> "key.keyboard.left.shift";
            case "rctrl", "rightctrl", "rcontrol", "rightcontrol" -> "key.keyboard.right.control";
            case "lctrl", "leftctrl", "lcontrol", "leftcontrol" -> "key.keyboard.left.control";
            case "ralt", "rightalt" -> "key.keyboard.right.alt";
            case "lalt", "leftalt" -> "key.keyboard.left.alt";
            case "space", "spacebar" -> "key.keyboard.space";
            case "enter", "return" -> "key.keyboard.enter";
            case "esc", "escape" -> "key.keyboard.escape";
            case "backspace" -> "key.keyboard.backspace";
            case "capslock" -> "key.keyboard.caps.lock";
            case "pageup" -> "key.keyboard.page.up";
            case "pagedown" -> "key.keyboard.page.down";
            default -> "key.keyboard." + normalized;
        };
    }

    private String getKeyName(int keycode) {
        if (keycode == 0) return "None";
        if (keycode == 1069) return "Scroll Up";
        if (keycode == 1070) return "Scroll Down";
        if (keycode >= 1000) return "Mouse " + (keycode - 1000);

        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keycode);
        return key.getDisplayName().getString();
    }
}

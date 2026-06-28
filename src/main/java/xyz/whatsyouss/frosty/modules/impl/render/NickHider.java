package xyz.whatsyouss.frosty.modules.impl.render;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.InputSetting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NickHider extends Module {

    public InputSetting name, serverNick;

    private static Pattern cachedPattern;
    private static String lastUsername;
    private static String lastNick;
    
    public NickHider() {
        super("NickHider", "匿名保护", category.Render);

        this.registerSetting(name = new InputSetting("Nick Name", 16, "You"));
        this.registerSetting(serverNick = new InputSetting("Server Nick", 16, ""));
    }

    @Override
    public String getDesc() {
        return "Server Nick: Your nick name by Server (/nick)";
    }

    public static Pattern getUsernamePattern() {
        String username = mc.getUser().getName();
        String serverNick = ModuleManager.nickHider.serverNick.getValue();

        if (cachedPattern == null || !username.equals(lastUsername) || !serverNick.equals(lastNick)) {
            lastUsername = username;
            lastNick = serverNick;
            cachedPattern = serverNick != null && !serverNick.isEmpty()
                    ? Pattern.compile("(?i)" + Pattern.quote(username) + "|" + Pattern.quote(serverNick))
                    : Pattern.compile("(?i)" + Pattern.quote(username));
        }
        return cachedPattern;
    }

    public static MutableComponent processText(MutableComponent text) {
        if (text == null) {
            return Component.empty();
        }

        String ownContent = (text.getContents() instanceof PlainTextContents ptc) ? ptc.text() : "";

        MutableComponent result;
        if (!ownContent.isEmpty() && getUsernamePattern().matcher(ownContent).matches()) {
            result = Component.literal(ModuleManager.nickHider.name.getValue())
                    .setStyle(text.getStyle());
        } else {
            result = Component.literal(ownContent).setStyle(text.getStyle());
        }

        for (Component sibling : text.getSiblings()) {
            result.append(processText((MutableComponent) sibling));
        }

        return result;
    }
}

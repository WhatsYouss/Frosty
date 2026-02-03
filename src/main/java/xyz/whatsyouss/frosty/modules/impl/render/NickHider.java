package xyz.whatsyouss.frosty.modules.impl.render;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
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
        super("NickHider", category.Render);

        this.registerSetting(name = new InputSetting("Nick Name", 16, "You"));
        this.registerSetting(serverNick = new InputSetting("Server Nick", 16, ""));
    }

    @Override
    public String getDesc() {
        return "Server Nick: Your nick name by Server (/nick)";
    }

    public static Pattern getUsernamePattern() {
        String username = mc.getSession().getUsername();
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

    public static Text processText(Text text) {
        if (text == null) {
            return Text.empty();
        }
        MutableText result = Text.empty().setStyle(text.getStyle());

        String content = text.getString();
        Matcher matcher = NickHider.getUsernamePattern().matcher(content);

        if (matcher.matches()) {
            result.append(Text.literal(ModuleManager.nickHider.name.getValue())
                    .setStyle(text.getStyle()));
        } else {
            result.append(text.copyContentOnly().setStyle(text.getStyle()));
        }

        for (Text sibling : text.getSiblings()) {
            result.append(processText(sibling));
        }

        return result;
    }
}

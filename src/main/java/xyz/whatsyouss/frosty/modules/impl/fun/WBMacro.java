package xyz.whatsyouss.frosty.modules.impl.fun;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.ReceiveMessageEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WBMacro extends Module {

    private SliderSetting delay;

    private static final Pattern WB_PATTERN =
            Pattern.compile("^\\* HELLO! Welcome Back \\[(.+?)\\] (.+?) <<I$");

    private static final List<String> IGNORED_TIERS = Arrays.asList("T1", "T2", "T3", "T4");

    private static final List<String> MESSAGE_POOL = Arrays.asList(
            "wb", "WB", "welcome back", "Welcome Back", "wbwb",
            "hello", "wb!!", "hey wb", "hi wb", "WB!!", "HI"
    );

    private List<String> messageQueue = new ArrayList<>();

    public WBMacro() {
        super("WBMacro", category.Fun);
        this.registerSetting(delay = new SliderSetting("Delay", "ms", 500, 500, 2000, 500));
    }

    @EventHandler
    public void onReceiveMessage(ReceiveMessageEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        String msg = event.getMessage().getString();

        Matcher matcher = WB_PATTERN.matcher(msg);
        if (!matcher.matches()) return;

        String tier = matcher.group(1);
        String playerId = matcher.group(2);

        if (IGNORED_TIERS.contains(tier)) return;

        if (messageQueue.isEmpty()) {
            messageQueue = new ArrayList<>(MESSAGE_POOL);
            Collections.shuffle(messageQueue);
        }

        String greet = messageQueue.remove(0);
        long delayMs = (long) delay.getInput();

        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            sendCommand("ac " + greet + " " + playerId);
        }).start();
    }

    private void sendCommand(String command) {
        mc.getNetworkHandler().sendChatCommand(command);
    }
}
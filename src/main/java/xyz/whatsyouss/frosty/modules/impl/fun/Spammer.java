package xyz.whatsyouss.frosty.modules.impl.fun;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.InputSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.Random;

public class Spammer extends Module {

    private final SliderSetting delay;
    private final ButtonSetting messageTwo, antiDupe;
    private final InputSetting message1, message2;

    private long lastSendTime = 0L;
    private boolean sendSecondNext = false;
    private final Random random = new Random();

    private static final String CHAR_POOL = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public Spammer() {
        super("Spammer", "刷屏", category.Fun);

        this.registerSetting(message1 = new InputSetting("Message 1", 25, "Frosty - Free 1.21.10 Skyblock Client", "/p xxx"));
        this.registerSetting(messageTwo = new ButtonSetting("Message two", false));
        this.registerSetting(message2 = new InputSetting("Message 2", 25, "", "/p disband"));
        this.registerSetting(delay = new SliderSetting("Delay", "ms", 3000, 100, 10000, 100));
        this.registerSetting(antiDupe = new ButtonSetting("Anti Duplicate", true));
    }

    @Override
    public void guiUpdate() {
        this.message2.setVisibilityCondition(messageTwo::isToggled);
    }

    @Override
    public void onEnable() {
        lastSendTime = 0L;
        sendSecondNext = false;
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) return;

        long currentTime = System.currentTimeMillis();
        long requiredDelay = (long) delay.getInput();

        if (currentTime - lastSendTime >= requiredDelay) {
            String baseMessage;

            if (messageTwo.isToggled()) {
                if (!sendSecondNext) {
                    baseMessage = message1.getValue();
                    sendSecondNext = true;
                } else {
                    baseMessage = message2.getValue();
                    sendSecondNext = false;
                }
            } else {
                baseMessage = message1.getValue();
                sendSecondNext = false;
            }

            if (baseMessage == null || baseMessage.isEmpty()) {
                lastSendTime = currentTime;
                return;
            }

            if (antiDupe.isToggled()) {
                baseMessage = appendAntiDupe(baseMessage);
            }

            mc.player.connection.sendChat(baseMessage);

            lastSendTime = currentTime;
        }
    }

    private String appendAntiDupe(String original) {
        StringBuilder sb = new StringBuilder(original);

        sb.append(" [");
        for (int i = 0; i < 5; i++) {
            int index = random.nextInt(CHAR_POOL.length());
            sb.append(CHAR_POOL.charAt(index));
        }
        sb.append("]");

        return sb.toString();
    }
}
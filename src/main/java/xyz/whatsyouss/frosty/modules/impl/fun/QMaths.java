package xyz.whatsyouss.frosty.modules.impl.fun;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.slot.SlotActionType;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.ReceiveMessageEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QMaths extends Module {

    private SliderSetting delay;

    private boolean active = false;
    private final List<Integer> digits = new ArrayList<>();
    private int digitIndex = 0;
    private boolean readyToSubmit = false;
    private long lastActionTime = 0L;
    private boolean waitingForServerAck = false;

    private static final Pattern MATH_PATTERN =
            Pattern.compile("QUICK MATHS:\\s*(\\d+)\\s*([+\\-*/×÷])\\s*(\\d+)");

    public QMaths() {
        super("QMaths", category.Fun);

        this.registerSetting(delay = new SliderSetting("Delay", "ms", 700, 500, 2500, 50));
    }

    @EventHandler
    public void onReceiveMessage(ReceiveMessageEvent event) {
        String msg = event.getMessage().getString();

        if (msg.contains("has won!")) {
            reset();
            return;
        }

        Matcher m = MATH_PATTERN.matcher(msg);
        if (!m.find()) return;

        int a  = Integer.parseInt(m.group(1));
        String op = m.group(2).trim();
        int b  = Integer.parseInt(m.group(3));

        int result;
        switch (op) {
            case "+":           result = a + b; break;
            case "-":           result = a - b; break;
            case "*": case "×": result = a * b; break;
            case "/": case "÷": result = b != 0 ? a / b : 0; break;
            default: return;
        }

        digits.clear();
        for (char c : String.valueOf(Math.abs(result)).toCharArray()) {
            digits.add(Character.getNumericValue(c));
        }

        digitIndex    = 0;
        readyToSubmit = false;
        lastActionTime = 0L;
        active        = true;

        mc.getNetworkHandler().sendChatCommand("qmath");
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!active
                || mc.player == null
                || mc.getNetworkHandler() == null
                || !(mc.currentScreen instanceof GenericContainerScreen screen)) return;

        if (!screen.getTitle().getString().contains("Enter the answer!")) return;

        var handler = screen.getScreenHandler();

        long now = System.currentTimeMillis();
        if (now - lastActionTime < (long) delay.getInput()) return;
        lastActionTime = now;

        if (!readyToSubmit && digitIndex < digits.size()) {
            String targetName = "Enter " + digits.get(digitIndex);

            for (var slot : handler.slots) {
                var stack = slot.getStack();
                if (stack.isEmpty()) continue;
                if (!stack.getName().getString().equals(targetName)) continue;

                clickSlot(slot.id);
                digitIndex++;

                if (digitIndex >= digits.size()) readyToSubmit = true;
                return;
            }
            return;
        }

        if (readyToSubmit) {
            for (var slot : handler.slots) {
                var stack = slot.getStack();
                if (stack.isEmpty()) continue;
                if (!stack.getName().getString().contains("SUBMIT")) continue;

                clickSlot(slot.id);
                reset();
                return;
            }
        }
    }

    private void reset() {
        active             = false;
        readyToSubmit      = false;
        digitIndex         = 0;
        waitingForServerAck = false;
        digits.clear();
    }

    private void clickSlot(int slotId) {
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;

        var handler = screen.getScreenHandler();
        mc.interactionManager.clickSlot(
                handler.syncId,
                slotId,
                0,
                SlotActionType.PICKUP,
                mc.player
        );
    }
}
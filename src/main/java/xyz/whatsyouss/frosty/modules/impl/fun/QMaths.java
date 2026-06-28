package xyz.whatsyouss.frosty.modules.impl.fun;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.ReceiveMessageEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;

import javax.swing.event.ListDataEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QMaths extends Module {

    private SliderSetting delay;
    private ButtonSetting say, antiDupe;

    private final Random random = new Random();
    private static final String CHAR_POOL = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private boolean active = false;
    private boolean waitingForSolve = false;
    private final List<Integer> digits = new ArrayList<>();
    private int digitIndex = 0;
    private boolean readyToSubmit = false;
    private long lastActionTime = 0L;

    private static final Pattern TYPE_PATTERN =
            Pattern.compile("QUICK MATHS\\s*\\|");

    private static final Pattern END_PATTERN =
            Pattern.compile("QUICK MATHS.*Solved");

    private static final Pattern SOLVE_PATTERN =
            Pattern.compile("Solve:\\s*(.+)");

    private static final Pattern PAREN_PATTERN =
            Pattern.compile("\\(\\s*(-?\\d+)\\s*([+\\-*/%x×])\\s*(-?\\d+)\\s*\\)\\s*([+\\-*/%x×])\\s*(-?\\d+)");

    private static final Pattern BINARY_PATTERN =
            Pattern.compile("(-?\\d+)\\s*([+\\-*/%x×]|mod)\\s*(-?\\d+)");

    private boolean waitingForBinary = false;

    private static final Pattern BINARY_CONVERT_PATTERN =
            Pattern.compile("Convert to decimal:\\s*([01]+)");

    public QMaths() {
        super("QMaths", "速算", category.Fun);

        this.registerSetting(delay = new SliderSetting("Delay", "ms", 700, 200, 2500, 50));
        this.registerSetting(say = new ButtonSetting("Say", true));
        this.registerSetting(antiDupe = new ButtonSetting("Anti Duplicate", true));
    }

    @EventHandler
    public void onReceiveMessage(ReceiveMessageEvent event) {
        String msg = event.getMessage().getString();
        if (END_PATTERN.matcher(msg).find()) {
            reset();
            return;
        }

        if (msg.contains("has won!")) {
            reset();
            return;
        }

        if (TYPE_PATTERN.matcher(msg).find()) {
            waitingForSolve = false;
            waitingForBinary = false;

            if (msg.contains("Binary Conversion")) {
                waitingForBinary = true;
            } else {
                waitingForSolve = true;
            }
            return;
        }

        if (waitingForBinary) {
            if (msg.contains("(") && msg.contains(")")) return;

            Matcher bm = BINARY_CONVERT_PATTERN.matcher(msg);
            if (!bm.find()) return;

            waitingForBinary = false;
            String binaryStr = bm.group(1).trim();
            int result;

            try {
                result = Integer.parseInt(binaryStr, 2);
            } catch (NumberFormatException e) {
                return;
            }

            if (say.isToggled()) {
                long delayMs = (long) delay.getInput();
                if (delayMs == 0) {
                    mc.getConnection().sendCommand("ac I think is " + result);
                    return;
                }
                final int capturedResult = result;
                new Thread(() -> {
                    try {
                        Thread.sleep(delayMs);
                        mc.execute(() ->
                                mc.getConnection().sendCommand("ac I think is " + capturedResult)
                        );
                    } catch (InterruptedException ignored) {}
                }).start();
            }

            digits.clear();
            for (char c : String.valueOf(result).toCharArray()) {
                digits.add(Character.getNumericValue(c));
            }

            digitIndex     = 0;
            readyToSubmit  = false;
            lastActionTime = 0L;
            active         = true;

            mc.getConnection().sendCommand("qmath");
            return;
        }

        if (waitingForSolve) {
            Matcher sm = SOLVE_PATTERN.matcher(msg);
            if (!sm.find()) return;

            waitingForSolve = false;
            String expr = sm.group(1).trim();
            int result = evaluate(expr);

            if (say.isToggled()) {
                long delayMs = (long) delay.getInput();

                if (delayMs == 0) {
                    mc.getConnection().sendCommand("ac I think is " + result);
                    return;
                }

                final int capturedResult = result;
                new Thread(() -> {
                    try {
                        Thread.sleep(delayMs);
                        if (antiDupe.isToggled()) {
                            mc.execute(() ->
                                    mc.getConnection().sendCommand(appendAntiDupe("ac I think is " + capturedResult))
                            );
                        } else {
                            mc.execute(() ->
                                    mc.getConnection().sendCommand("ac I think is " + capturedResult)
                            );
                        }
                    } catch (InterruptedException ignored) {}
                }).start();
            }

            digits.clear();
            for (char c : String.valueOf(Math.abs(result)).toCharArray()) {
                digits.add(Character.getNumericValue(c));
            }

            digitIndex     = 0;
            readyToSubmit  = false;
            lastActionTime = 0L;
            active         = true;

            mc.getConnection().sendCommand("qmath");
        }
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!active
                || mc.player == null
                || mc.getConnection() == null
                || !(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) return; // 💡 注意：mc.gui.screen() 在很多高版本里直接点 mc.screen 即可

        if (!screen.getTitle().getString().contains("Enter the answer!")) return;

        long now = System.currentTimeMillis();
        if (now - lastActionTime < (long) delay.getInput()) return;
        lastActionTime = now;

        AbstractContainerMenu handler = screen.getMenu();

        if (!readyToSubmit && digitIndex < digits.size()) {
            String targetName = "Enter " + digits.get(digitIndex);

            for (Slot slot : handler.slots) {
                ItemStack stack = slot.getItem();
                if (stack.isEmpty()) continue;

                if (!stack.getHoverName().getString().equals(targetName)) continue;

                clickSlot(slot.index);
                digitIndex++;
                if (digitIndex >= digits.size()) readyToSubmit = true;
                return;
            }
            return;
        }

        if (readyToSubmit) {
            for (Slot slot : handler.slots) {
                ItemStack stack = slot.getItem();
                if (stack.isEmpty()) continue;

                if (!stack.getHoverName().getString().contains("SUBMIT")) continue;

                clickSlot(slot.index);
                reset();
                return;
            }
        }
    }

    private void clickSlot(int slotId) {
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) return;
        AbstractContainerMenu handler = screen.getMenu();

        if (mc.gameMode != null) {
            mc.gameMode.handleContainerInput(
                    handler.containerId, slotId, 2,
                    ContainerInput.CLONE, mc.player);
        }
    }

    private int evaluate(String expr) {
        expr = expr.replaceAll("\\bmod\\b", "%")
                .replaceAll("[x×]", "*");

        Matcher pm = PAREN_PATTERN.matcher(expr);
        if (pm.find()) {
            int inner = applyOp(
                    Integer.parseInt(pm.group(1)), pm.group(2),
                    Integer.parseInt(pm.group(3)));
            return applyOp(inner, pm.group(4), Integer.parseInt(pm.group(5)));
        }

        Matcher bm = BINARY_PATTERN.matcher(expr);
        if (bm.find()) {
            return applyOp(
                    Integer.parseInt(bm.group(1)), bm.group(2),
                    Integer.parseInt(bm.group(3)));
        }

        return 0;
    }

    private int applyOp(int a, String op, int b) {
        switch (op) {
            case "+":  return a + b;
            case "-":  return a - b;
            case "*":  return a * b;
            case "/":  return b != 0 ? a / b : 0;
            case "%":  return b != 0 ? a % b : 0;
            default:   return 0;
        }
    }

    private void reset() {
        active           = false;
        waitingForSolve  = false;
        waitingForBinary = false;
        readyToSubmit    = false;
        digitIndex       = 0;
        digits.clear();
    }

    private String appendAntiDupe(String original) {
        StringBuilder sb = new StringBuilder(original);

        sb.append(" [");
        for (int i = 0; i < 7; i++) {
            int index = random.nextInt(CHAR_POOL.length());
            sb.append(CHAR_POOL.charAt(index));
        }
        sb.append("]");

        return sb.toString();
    }
}
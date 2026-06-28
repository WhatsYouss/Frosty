package xyz.whatsyouss.frosty.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.joml.Matrix3x2fStack;
import xyz.whatsyouss.frosty.events.impl.ReceivePacketEvent;
import xyz.whatsyouss.frosty.events.impl.Render2DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.MathUtils;
import xyz.whatsyouss.frosty.utility.Theme;
import xyz.whatsyouss.frosty.utility.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;

public class TPS extends Module {

    private SelectSetting color;
    private String[] colors = new String[] {"Rainbow", "Cherry", "Cotton candy", "Flare", "Flower", "Gold", "Grayscale", "Royal", "Sky", "Vine"};

    private final ArrayDeque<Long> packetIntervals = new ArrayDeque<>(10);
    private long lastPacketTime;
    private int strColor;

    public TPS() {
        super("TPS", "服务器刻数", category.Render);
        this.registerSetting(color = new SelectSetting("Color", 0, colors));
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!Utils.nullCheck() || mc.gui.hud.isHidden()) {
            return;
        }

        Matrix3x2fStack matrices = event.drawContext.pose();
        matrices.pushMatrix();
        int x = 5;
        int y = mc.getWindow().getGuiScaledHeight() - 12;

        event.drawContext.text(mc.font, "TPS: " + getTPS(), x, y, strColor, true);
        matrices.popMatrix();
    }

    @Override
    public void onUpdate() {
        strColor = getCurrentColor();
    }

    public float getTPS() {
        if (packetIntervals.isEmpty()) {
            return 20.0f;
        }

        long totalTimeMs = 0;
        for (Long interval : packetIntervals) {
            totalTimeMs += interval;
        }

        long now = System.currentTimeMillis();
        long timeSinceLastPacket = now - lastPacketTime;

        if (lastPacketTime != 0L && timeSinceLastPacket > 1000L) {
            totalTimeMs += (timeSinceLastPacket - 1000L);
        }

        float totalTicks = packetIntervals.size() * 20.0f;
        float calculatedTps = totalTicks / ((float) totalTimeMs / 1000.0f);

        return round2(MathUtils.clamp(calculatedTps, 0.0f, 20.0f));
    }

    @EventHandler
    public void onPacketReceive(ReceivePacketEvent event) {
        if (event.getPacket() instanceof ClientboundSetTimePacket) {
            long now = System.currentTimeMillis();

            if (lastPacketTime != 0L) {
                long interval = now - lastPacketTime;

                if (interval < 50L) interval = 50L;

                if (packetIntervals.size() >= 10) {
                    packetIntervals.poll();
                }
                packetIntervals.add(interval);
            }

            lastPacketTime = now;
        }
    }

    public static float round2(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0f;
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    private int getCurrentColor() {
        int selectedIndex = (int) color.getValue();
        if (selectedIndex == 0) {
            return Theme.getChroma(1, 0);
        }
        Theme theme = Theme.values()[selectedIndex];
        return theme.getAnimatedColor(0, 255, 0.0002);
    }
}
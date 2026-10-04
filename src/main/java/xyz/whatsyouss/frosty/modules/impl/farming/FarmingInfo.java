package xyz.whatsyouss.frosty.modules.impl.farming;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import xyz.whatsyouss.frosty.events.impl.Render2DEvent;
import xyz.whatsyouss.frosty.gui.LiquidGlassStyle;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FarmingInfo extends Module {

    private ButtonSetting bps, profit, autoHide;
    private SliderSetting X, Y;

    public FarmingInfo() {
        super("FarmingInfo", "农业数据", category.Farming);

        this.registerSetting(bps = new ButtonSetting("BPS", "每秒破坏", true));
        this.registerSetting(profit = new ButtonSetting("Profit", "收益", true));
        this.registerSetting(X = new SliderSetting("X", 90, 0, 100, 1, "水平位置"));
        this.registerSetting(Y = new SliderSetting("Y", 90, 0, 100, 1, "竖直位置"));
        this.registerSetting(autoHide = new ButtonSetting("Auto Hide", "自动隐藏", false));
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (!Utils.nullCheck() || mc.options.hideGui) return;
        if (autoHide.isToggled() && (!ModuleManager.farmingMacro.isEnabled() || !ModuleManager.farmingMacro.running)) return;

        List<Component> lines = new ArrayList<>(2);
        if (bps.isToggled()) {
            float value = FarmingStats.getBps();
            ChatFormatting color = value >= 15.0f ? ChatFormatting.GREEN
                    : value >= 10.0f ? ChatFormatting.YELLOW : ChatFormatting.RED;
            lines.add(Component.literal("BPS: ").withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(String.format(Locale.ROOT, "%.1f", value)).withStyle(color)));
        }
        if (profit.isToggled()) {
            lines.add(Component.literal("Profit: ").withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(String.format(Locale.ROOT, "%,d", FarmingStats.getProfitPerHour()))
                            .withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("/h").withStyle(ChatFormatting.WHITE)));
        }
        if (lines.isEmpty()) return;

        int paddingX = 10;
        int paddingY = 7;
        int lineGap = 3;
        int contentWidth = 0;
        for (Component line : lines) contentWidth = Math.max(contentWidth, mc.font.width(line));
        int width = contentWidth + paddingX * 2;
        int height = lines.size() * mc.font.lineHeight + (lines.size() - 1) * lineGap + paddingY * 2;
        int x = (int) Math.round((event.screenWidth - width - 3) * X.getInput() / 100.0);
        int y = (int) Math.round((event.screenHeight - height - 3) * Y.getInput() / 100.0);

        LiquidGlassStyle.drawGlass(event.drawContext, x, y, width, height, 9.0f,
                0xD72A466F);
        for (int i = 0; i < lines.size(); i++) {
            event.drawContext.text(mc.font, lines.get(i), x + paddingX,
                    y + paddingY + i * (mc.font.lineHeight + lineGap), 0xFFF4F8FF, true);
        }
    }
}

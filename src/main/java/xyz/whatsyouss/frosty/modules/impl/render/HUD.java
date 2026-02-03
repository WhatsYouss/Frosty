package xyz.whatsyouss.frosty.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import xyz.whatsyouss.frosty.events.impl.Render2DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Theme;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HUD extends Module {

    private SelectSetting color;
    private ButtonSetting flow, suffix, bar, background;
    private SliderSetting opacity, offset;

    private String[] colors = new String[] {"Rainbow", "Cherry", "Cotton candy", "Flare", "Flower", "Gold", "Grayscale", "Royal", "Sky", "Vine"};

    private static int MARGIN = 5;
    private static final int INFO_COLOR = 0xFFA0A0A0;
    private int strColor;
    private float SCALE;

    public HUD() {
        super("HUD", Module.category.Render);

        this.registerSetting(offset = new SliderSetting("Offset", 2, 0, 5, 1));
        this.registerSetting(color = new SelectSetting("Color", 0, colors));
        this.registerSetting(flow = new ButtonSetting("Gradient", false));
        this.registerSetting(suffix = new ButtonSetting("Suffix", true));
        this.registerSetting(bar = new ButtonSetting("Bar", true));
        this.registerSetting(background = new ButtonSetting("Background", true));
        this.registerSetting(opacity = new SliderSetting("Opacity", "%", 50, 0, 100, 1));
    }

    @Override
    public void guiUpdate() {
        this.opacity.setVisibilityCondition(() -> background.isToggled());
    }

    @Override
    public void onUpdate() {
        strColor = getCurrentColor(0);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (!Utils.nullCheck() || mc.options.hudHidden) {
            return;
        }

        MARGIN = (int) offset.getInput();
        SCALE = (float) mc.getWindow().getScaleFactor();

        TextRenderer textRenderer = mc.textRenderer;
        Matrix3x2fStack matrices = event.drawContext.getMatrices();

        matrices.pushMatrix();

        List<Module> sortedModules = ModuleManager.organizedModules.stream()
                .filter(module -> module.isEnabled() && !module.isHidden())
                .sorted(Comparator.comparingInt(this::getModuleDisplayLength).reversed())
                .collect(Collectors.toList());

        int extraOffset = bar.isToggled() ? 2 : 0;
        int yPos = MARGIN;
        int scaledWidth = mc.getWindow().getScaledWidth();

        for (int i = 0; i < sortedModules.size(); i++) {
            Module module = sortedModules.get(i);
            String moduleName = module.getNameInHud();
            String info = module.getInfo();

            int totalWidth = textRenderer.getWidth(moduleName + (info.isEmpty() || !suffix.isToggled() ? "" : " " + info));
            int xPos = scaledWidth - totalWidth - MARGIN - extraOffset;

            int moduleColor;
            if (flow.isToggled()) {
                moduleColor = getCurrentColor(i);
            } else {
                moduleColor = strColor;
            }

            if (background.isToggled()) {
                int alpha = (int) (25 + (opacity.getInput() / 100.0) * (125 - 25));
                int backgroundColor = (alpha << 24) | 0x000000;
                event.drawContext.fill(
                        xPos - 2,
                        yPos - 1,
                        scaledWidth - MARGIN - extraOffset + 2,
                        yPos + textRenderer.fontHeight + 1,
                        backgroundColor
                );
            }

            event.drawContext.drawText(
                    textRenderer,
                    Text.literal(moduleName),
                    xPos,
                    yPos,
                    moduleColor,
                    true
            );

            if (!info.isEmpty() && suffix.isToggled()) {
                int nameWidth = textRenderer.getWidth(moduleName);
                event.drawContext.drawText(
                        textRenderer,
                        Text.literal(" " + info),
                        xPos + nameWidth,
                        yPos,
                        INFO_COLOR,
                        true
                );
            }

            yPos += textRenderer.fontHeight + 2;
        }

        if (bar.isToggled()) {
            yPos = MARGIN;
            for (int i = 0; i < sortedModules.size(); i++) {
                Module module = sortedModules.get(i);
                int moduleColor;
                if (flow.isToggled()) {
                    moduleColor = getCurrentColor(i);
                } else {
                    moduleColor = strColor;
                }

                int barX = scaledWidth - 2;
                int barY2 = yPos + textRenderer.fontHeight;
                event.drawContext.fill(barX, yPos - 1, scaledWidth, barY2 + 1, moduleColor);

                yPos += textRenderer.fontHeight + 2;
            }
        }

        matrices.popMatrix();
    }

    private int getModuleDisplayLength(Module module) {
        String displayText = module.getNameInHud() +
                (module.getInfo().isEmpty() || !suffix.isToggled() ? "" : " " + module.getInfo());
        return MinecraftClient.getInstance().textRenderer.getWidth(displayText);
    }

    private int getCurrentColor(int moduleIndex) {
        int selectedIndex = (int) color.getValue();
        Theme theme = Theme.values()[selectedIndex];

        if (flow.isToggled()) {
            double offset = moduleIndex * 0.1;
            double speed = 0.001;

            if (theme == Theme.Rainbow) {
                long delay = moduleIndex * 50L;
                return Theme.getChroma(2, delay);
            } else {
                return theme.getAnimatedColor(offset, 255, speed);
            }
        } else {
            if (theme == Theme.Rainbow) {
                return Theme.getChroma(2, 0);
            } else {
                return theme.getAnimatedColor(0, 255, 0.001);
            }
        }
    }
}
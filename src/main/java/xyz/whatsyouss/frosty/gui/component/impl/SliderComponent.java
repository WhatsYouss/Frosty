package xyz.whatsyouss.frosty.gui.component.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import xyz.whatsyouss.frosty.gui.LiquidGlassStyle;
import xyz.whatsyouss.frosty.gui.SmoothRoundedRenderer;
import xyz.whatsyouss.frosty.gui.component.Component;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.client.UI;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;

import java.awt.*;

import static xyz.whatsyouss.frosty.Frosty.mc;

public class SliderComponent extends Component {
    private final SliderSetting setting;
    private boolean draggingMin;
    private boolean draggingMax;
    private String cachedDisplayText;
    private String cachedName;
    private String cachedSuffix;
    private double cachedMin = Double.NaN;
    private double cachedMax = Double.NaN;
    private double cachedValue = Double.NaN;
    private boolean cachedRange;
    private boolean cachedGlass;

    public SliderComponent(SliderSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
        this.draggingMin = false;
        this.draggingMax = false;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!isVisible()) {
            return;
        }
        boolean isLight = UI.clickGuiColor.getValue() == 0;
        isHovered = isHovered(mouseX, mouseY);

        int sliderY = (int) (y + height - 5);
        float sliderStartX = x + width / 3;
        float sliderEndX = x + width - 5;
        float sliderWidth = sliderEndX - sliderStartX;
        if (LiquidGlassStyle.isEnabled()) {
            SmoothRoundedRenderer.fill(context, sliderStartX, sliderY, sliderWidth, 3, 2,
                    isLight ? 0x7091A5C8 : 0x705D7198);
        } else {
            context.fill((int) sliderStartX, sliderY, (int) sliderEndX, sliderY + 3, 0xFFB4B4B4);
        }

        if (setting.isRange()) {
            double minValue = setting.getInputMin();
            double maxValue = setting.getInputMax();
            double minPos = ((minValue - setting.getMin()) / (setting.getMax() - setting.getMin())) * sliderWidth;
            double maxPos = ((maxValue - setting.getMin()) / (setting.getMax() - setting.getMin())) * sliderWidth;

            if (maxValue == setting.getMax()) {
                maxPos = sliderWidth;
            }

            if (minValue == maxValue && minPos >= maxPos) {
                minPos = maxPos - 1;
            }

            // draw range
            drawSliderRange(context, sliderStartX + minPos, sliderStartX + maxPos, sliderY);
            drawSliderKnob(context, sliderStartX + minPos, sliderY);
            drawSliderKnob(context, sliderStartX + maxPos, sliderY);
        } else {
            double value = setting.getInput();
            double pos = ((value - setting.getMin()) / (setting.getMax() - setting.getMin())) * sliderWidth;

            if (value == setting.getMax()) {
                pos = sliderWidth;
            }

            drawSliderRange(context, sliderStartX, sliderStartX + pos, sliderY);
            drawSliderKnob(context, sliderStartX + pos, sliderY);
        }

        context.text(mc.font, displayText(), (int) (x + 2), (int) (y + height / 2 - 4), LiquidGlassStyle.isEnabled() ? LiquidGlassStyle.textColor() : isLight ? 0xFF000000 : 0xFFFFFFFF, false);
    }

    private String displayText() {
        double min = setting.getInputMin();
        double max = setting.getInputMax();
        double value = setting.getInput();
        boolean range = setting.isRange();
        boolean glass = LiquidGlassStyle.isEnabled();
        String name = setting.getTransName();
        String suffix = setting.getSuffix();
        if (cachedDisplayText == null || min != cachedMin || max != cachedMax || value != cachedValue
                || range != cachedRange || glass != cachedGlass || !name.equals(cachedName) || !suffix.equals(cachedSuffix)) {
            cachedDisplayText = name + ": " + (range
                    ? String.format("%.2f-%.2f", displayValue(min), displayValue(max))
                    : String.format("%.2f", displayValue(value))) + suffix;
            cachedMin = min;
            cachedMax = max;
            cachedValue = value;
            cachedRange = range;
            cachedGlass = glass;
            cachedName = name;
            cachedSuffix = suffix;
        }
        return cachedDisplayText;
    }

    private void drawSliderRange(GuiGraphicsExtractor context, double start, double end, int sliderY) {
        if (LiquidGlassStyle.isEnabled()) {
            float rangeWidth = (float) (end - start);
            SmoothRoundedRenderer.fill(context, (float) start, sliderY, rangeWidth, 3,
                    2, LiquidGlassStyle.accentColor());
        } else {
            context.fill((int) start, sliderY, (int) end, sliderY + 3, 0xFF6464FF);
        }
    }

    private void drawSliderKnob(GuiGraphicsExtractor context, double position, int sliderY) {
        if (LiquidGlassStyle.isEnabled()) {
            SmoothRoundedRenderer.fill(context, (float) position - 3, sliderY - 3, 6, 8,
                    3, 0xFFEAF2FF);
            SmoothRoundedRenderer.border(context, (float) position - 3, sliderY - 3, 6, 8,
                    3, LiquidGlassStyle.accentColor());
        } else {
            context.fill((int) position - 2, sliderY - 2, (int) position + 2, sliderY + 5, 0xFF0000FF);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible() || button != 0) return;

        int sliderY = (int) (y + height - 5);
        float sliderStartX = x + width / 3;
        float sliderEndX = x + width - 5;
        float sliderWidth = sliderEndX - sliderStartX;

        if (LiquidGlassStyle.isEnabled() && mouseX >= sliderStartX && mouseX <= sliderEndX
                && mouseY >= sliderY - 3 && mouseY <= sliderY + 6) {
            if (setting.isRange()) {
                double minPos = ((setting.getInputMin() - setting.getMin())
                        / (setting.getMax() - setting.getMin())) * sliderWidth;
                double maxPos = ((setting.getInputMax() - setting.getMin())
                        / (setting.getMax() - setting.getMin())) * sliderWidth;
                draggingMin = Math.abs(mouseX - (sliderStartX + minPos))
                        <= Math.abs(mouseX - (sliderStartX + maxPos));
                draggingMax = !draggingMin;
            } else {
                draggingMin = true;
            }
            updateDraggedValue(mouseX);
            return;
        }

        if (setting.isRange()) {
            double minValue = setting.getInputMin();
            double maxValue = setting.getInputMax();
            double minPos = ((minValue - setting.getMin()) / (setting.getMax() - setting.getMin())) * sliderWidth;
            double maxPos = ((maxValue - setting.getMin()) / (setting.getMax() - setting.getMin())) * sliderWidth;

            if (maxValue == setting.getMax()) {
                maxPos = sliderWidth;
            }

            if (minValue == maxValue && minPos >= maxPos) {
                minPos = maxPos - 1;
            }

            if (mouseX >= sliderStartX + minPos - 2 && mouseX <= sliderStartX + minPos + 2 && mouseY >= sliderY - 2 && mouseY <= sliderY + 5) {
                draggingMin = true;
            } else if (mouseX >= sliderStartX + maxPos - 2 && mouseX <= sliderStartX + maxPos + 2 && mouseY >= sliderY - 2 && mouseY <= sliderY + 5) {
                draggingMax = true;
            }
        } else {
            double value = setting.getInput();
            double pos = ((value - setting.getMin()) / (setting.getMax() - setting.getMin())) * sliderWidth;

            if (value == setting.getMax()) {
                pos = sliderWidth;
            }

            if (mouseX >= sliderStartX + pos - 2 && mouseX <= sliderStartX + pos + 2 && mouseY >= sliderY - 2 && mouseY <= sliderY + 5) {
                draggingMin = true;
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (!isVisible()) {
            return;
        }
        if (LiquidGlassStyle.isEnabled()) {
            snapDraggedValue();
        }
        draggingMin = false;
        draggingMax = false;
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!isVisible()) {
            return;
        }
        if (draggingMin || draggingMax) {
            updateDraggedValue(mouseX);
        }
    }

    private void updateDraggedValue(double mouseX) {
        float sliderStartX = x + width / 3;
        float sliderEndX = x + width - 5;
        float sliderWidth = sliderEndX - sliderStartX;
        double value = (mouseX - sliderStartX) / sliderWidth
                * (setting.getMax() - setting.getMin()) + setting.getMin();
        value = Math.max(setting.getMin(), Math.min(setting.getMax(), value));

        if (!LiquidGlassStyle.isEnabled() && setting.getIntervals() > 0) {
            value = snapToInterval(value);
        }

        if (setting.isRange()) {
            if (draggingMin) {
                setting.setInputMin(Math.min(value, setting.getInputMax()));
            } else if (draggingMax) {
                setting.setInputMax(Math.max(value, setting.getInputMin()));
            }
        } else {
            setting.setInput(value);
        }
    }

    private void snapDraggedValue() {
        if (setting.getIntervals() <= 0) {
            return;
        }
        if (setting.isRange()) {
            if (draggingMin) {
                setting.setInputMin(snapToInterval(setting.getInputMin()));
            } else if (draggingMax) {
                setting.setInputMax(snapToInterval(setting.getInputMax()));
            }
        } else if (draggingMin) {
            setting.setInput(snapToInterval(setting.getInput()));
        }
    }

    private double snapToInterval(double value) {
        double snapped = setting.getMin() + Math.round((value - setting.getMin())
                / setting.getIntervals()) * setting.getIntervals();
        return Math.max(setting.getMin(), Math.min(setting.getMax(),
                Math.round(snapped * 100.0) / 100.0));
    }

    private double displayValue(double value) {
        return LiquidGlassStyle.isEnabled() && setting.getIntervals() > 0
                ? snapToInterval(value) : value;
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}

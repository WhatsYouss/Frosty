package xyz.whatsyouss.frosty.gui.component.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import xyz.whatsyouss.frosty.gui.component.Component;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;

import java.awt.*;

import static xyz.whatsyouss.frosty.Frosty.mc;

public class SliderComponent extends Component {
    private final SliderSetting setting;
    private boolean draggingMin;
    private boolean draggingMax;

    public SliderComponent(SliderSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
        this.draggingMin = false;
        this.draggingMax = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!isVisible()) {
            return;
        }
        boolean isLight = ModuleManager.ui.clickGuiColor.getValue() == 0;
        isHovered = isHovered(mouseX, mouseY);

        int sliderY = (int) (y + height - 5);
        float sliderStartX = x + width / 3;
        float sliderEndX = x + width - 5;
        float sliderWidth = sliderEndX - sliderStartX;
        context.fill((int) sliderStartX, sliderY, (int) sliderEndX, sliderY + 3, new Color(180, 180, 180).getRGB());

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
            context.fill((int) (sliderStartX + minPos), sliderY, (int) (sliderStartX + maxPos), sliderY + 3, new Color(100, 100, 255).getRGB());

            context.fill((int) (sliderStartX + minPos - 2), sliderY - 2, (int) (sliderStartX + minPos + 2), sliderY + 5, Color.BLUE.getRGB());
            context.fill((int) (sliderStartX + maxPos - 2), sliderY - 2, (int) (sliderStartX + maxPos + 2), sliderY + 5, Color.BLUE.getRGB());
        } else {
            double value = setting.getInput();
            double pos = ((value - setting.getMin()) / (setting.getMax() - setting.getMin())) * sliderWidth;

            if (value == setting.getMax()) {
                pos = sliderWidth;
            }

            context.fill((int) sliderStartX, sliderY, (int) (sliderStartX + pos), sliderY + 3, new Color(100, 100, 255).getRGB());
            context.fill((int) (sliderStartX + pos - 2), sliderY - 2, (int) (sliderStartX + pos + 2), sliderY + 5, Color.BLUE.getRGB());
        }

        String displayText = setting.getName() + ": " + (setting.isRange() ?
                String.format("%.2f-%.2f", setting.getInputMin(), setting.getInputMax()) : String.format("%.2f", setting.getInput())) + setting.getSuffix();
        context.drawText(mc.textRenderer, Text.literal(displayText), (int) (x + 2), (int) (y + height / 2 - 4), isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false);
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
        draggingMin = false;
        draggingMax = false;
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!isVisible()) {
            return;
        }
        if (draggingMin || draggingMax) {
            float sliderStartX = x + width / 3;
            float sliderEndX = x + width - 5;
            float sliderWidth = sliderEndX - sliderStartX;
            double value = (mouseX - sliderStartX) / sliderWidth * (setting.getMax() - setting.getMin()) + setting.getMin();
            value = Math.max(setting.getMin(), Math.min(setting.getMax(), value));

            double intervals = setting.getIntervals();
            if (intervals > 0) {
                value = Math.round(value / intervals) * intervals;
                value = Math.round(value * 100.0) / 100.0; // Round to 2 decimal places
            }

            if (setting.isRange()) {
                if (draggingMin) {
                    double maxValue = setting.getInputMax();
                    setting.setInputMin(Math.min(value, maxValue));
                } else if (draggingMax) {
                    double minValue = setting.getInputMin();
                    setting.setInputMax(Math.max(value, minValue));
                }
            } else {
                setting.setInput(value);
            }
        }
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
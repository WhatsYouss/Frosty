package xyz.whatsyouss.frosty.gui.component.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import xyz.whatsyouss.frosty.gui.LiquidGlassStyle;
import xyz.whatsyouss.frosty.gui.component.Component;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.client.UI;

import java.awt.*;

import static xyz.whatsyouss.frosty.Frosty.mc;

public class CategoryComponent extends Component {
    private final Module.category category;
    private boolean selected;

    public CategoryComponent(Module.category category, float x, float y, float width, float height, boolean selected) {
        super(x, y, width, height);
        this.category = category;
        this.selected = selected;
    }

    private String getChineseName(Module.category cat) {
        if (cat == null) return "";

        switch (cat) {
            case Combat:   return "战斗";
            case Movement: return "移动";
            case Render:   return "渲染";
            case Other:    return "其他";
            case Client:   return "客户端";
            case Fishing:   return "钓鱼";
            case Foraging:   return "伐木";
            case Hunting:   return "狩猎";
            case Mining:   return "挖矿";
            case Farming:   return "农业";
            case Fun:   return "娱乐";
            default:       return cat.name();
        }
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        isHovered = isHovered(mouseX, mouseY);
        boolean isLight = ModuleManager.ui.clickGuiColor.getValue() == 0;

        int color;
        if (selected) {
            color = isLight ? 0xFF6464FF : 0xFF3C3CB4;
        } else if (isHovered) {
            color = isLight ? 0xFFC8C8C8 : 0xFF787878;
        } else {
            color = isLight ? 0xFFB4B4B4 : 0xFF505050;
        }

        if (LiquidGlassStyle.isEnabled()) {
            LiquidGlassStyle.drawControl(context, x, y, width, height, selected, isHovered);
        } else {
            context.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color);
        }
        String displayName = UI.lang.getValue() == 1 ? getChineseName(category) : category.name();

        context.text(mc.font, displayName,
                (int) (x + width / 2 - (float) mc.font.width(displayName) / 2),
                (int) (y + height / 2 - 4), LiquidGlassStyle.isEnabled()
                ? LiquidGlassStyle.textColor() : isLight ? 0xFF000000 : 0xFFFFFFFF, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY) && button == 0) {
            selected = true;
        }
    }

    public Module.category getCategory() {
        return category;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}

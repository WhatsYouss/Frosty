package xyz.whatsyouss.frosty.gui.component.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import xyz.whatsyouss.frosty.gui.component.Component;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;

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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        isHovered = isHovered(mouseX, mouseY);
        boolean isLight = ModuleManager.ui.clickGuiColor.getValue() == 0;

        int color;
        if (selected) {
            color = isLight ? new Color(100, 100, 255).getRGB() : new Color(60, 60, 180).getRGB();
        } else if (isHovered) {
            color = isLight ? new Color(200, 200, 200).getRGB() : new Color(120, 120, 120).getRGB();
        } else {
            color = isLight ? new Color(180, 180, 180).getRGB() : new Color(80, 80, 80).getRGB();
        }

        context.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color);
        context.drawText(mc.textRenderer, Text.literal(category.name()), (int) (x + width / 2 - mc.textRenderer.getWidth(category.name()) / 2),
                (int) (y + height / 2 - 4), isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false);
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
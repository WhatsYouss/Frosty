package xyz.whatsyouss.frosty.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xyz.whatsyouss.frosty.config.ConfigManager;
import xyz.whatsyouss.frosty.modules.impl.client.UI;

import java.awt.*;

import static xyz.whatsyouss.frosty.Frosty.mc;

public class LanguageSelectScreen extends Screen {
    private final Screen nextScreen;

    public LanguageSelectScreen(Screen nextScreen) {
        super(Component.literal("Choose your language"));
        this.nextScreen = nextScreen;
    }

    @Override
    protected void init() {
        int buttonWidth = 80;
        int buttonHeight = 20;
        int gap = 12;
        int y = height / 2 + 28;
        int leftX = width / 2 - buttonWidth - gap / 2;
        int rightX = width / 2 + gap / 2;

        addRenderableWidget(new Button.Builder(Component.literal("English"), button -> choose(0))
                .bounds(leftX, y, buttonWidth, buttonHeight)
                .build());
        addRenderableWidget(new Button.Builder(Component.literal("\u7b80\u4f53\u4e2d\u6587"), button -> choose(1))
                .bounds(rightX, y, buttonWidth, buttonHeight)
                .build());
    }

    private void choose(int language) {
        if (UI.lang != null) {
            UI.lang.setValue(language);
        }
        ConfigManager.completeLanguagePrompt();
        ConfigManager.saveConfig();
        mc.gui.setScreen(nextScreen);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, new Color(12, 16, 22).getRGB());
        context.text(font, "Thank you for using §9§lF§b§lr§9§lo§b§ls§9§lt§b§ly", width / 2 - font.width("Thank you for using §9§lF§b§lr§9§lo§b§ls§9§lt§b§ly") / 2, height / 2 - 80, Color.WHITE.getRGB(), false);
        context.text(font, "~ Choose your language ~", width / 2 - font.width("~ Choose your language ~") / 2, height / 2 - 30, Color.WHITE.getRGB(), false);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}

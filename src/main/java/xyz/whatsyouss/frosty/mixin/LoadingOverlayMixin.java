package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
    private static final Identifier FROSTY_ICON = Identifier.fromNamespaceAndPath("frosty", "icon.png");

    private static boolean initialLaunchComplete = false;

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ReloadInstance reload;
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void frosty$extractLoadingOverlay(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (initialLaunchComplete) {
            return;
        }

        int width = context.guiWidth();
        int height = context.guiHeight();
        long now = Util.getMillis();

        if (this.fadeOutStart == -1L && this.reload.isDone()) {
            this.fadeOutStart = now;
        }

        float fadeOut = fadeOutStart > -1L ? (now - fadeOutStart) / 1000.0f : -1.0f;
        if (fadeOut >= 2.0f) {
            initialLaunchComplete = true;
            minecraft.gui.setOverlay(null);
            ci.cancel();
            return;
        }

        currentProgress = Mth.clamp(currentProgress * 0.95f + reload.getActualProgress() * 0.050000012f, 0.0f, 1.0f);
        float shownProgress = fadeOut > -1.0f ? 1.0f : currentProgress;
        float fade = fadeOut > -1.0f ? 1.0f - Mth.clamp(fadeOut, 0.0f, 1.0f) : 1.0f;

        context.fill(0, 0, width, height, new Color(12, 16, 22).getRGB());

        int iconSize = Math.min(128, Math.max(72, Math.min(width, height) / 5));
        int iconX = width / 2 - iconSize / 2;
        int iconY = height / 2 - iconSize / 2;
        int alpha = Math.round(255.0f * fade);
        context.blit(RenderPipelines.GUI_TEXTURED, FROSTY_ICON, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize, argb(alpha, 255, 255, 255));

        drawProgressFrame(context, width, height, shownProgress, fade);
        ci.cancel();
    }

    private void drawProgressFrame(GuiGraphicsExtractor context, int width, int height, float progress, float fade) {
        int left = 0;
        int top = 0;
        int right = width;
        int bottom = height;

        int perimeter = 2 * (width + height);
        int amount = Math.round(perimeter * progress);

        int r = 96, g = 190, b = 255;
        int maxAlpha = Math.round(255.0f * fade);
        int glowSize = 24;

        int bgAlpha = Math.round(35.0f * fade);
        context.fill(0, 0, width, 1, argb(bgAlpha, 58, 70, 87));
        context.fill(width - 1, 0, width, height, argb(bgAlpha, 58, 70, 87));
        context.fill(0, height - 1, width, height, argb(bgAlpha, 58, 70, 87));
        context.fill(0, 0, 1, height, argb(bgAlpha, 58, 70, 87));

        amount = drawGlowSegment(context, left, top, right, bottom, amount, width, true, glowSize, r, g, b, maxAlpha);
        amount = drawGlowSegment(context, left, top, right, bottom, amount, height, false, glowSize, r, g, b, maxAlpha);
        amount = drawGlowSegmentReverse(context, left, top, right, bottom, amount, width, true, glowSize, r, g, b, maxAlpha);
        drawGlowSegmentReverse(context, left, top, right, bottom, amount, height, false, glowSize, r, g, b, maxAlpha);
    }

    private int drawGlowSegment(GuiGraphicsExtractor context, int left, int top, int right, int bottom, int remaining, int length, boolean horizontal, int glowSize, int r, int g, int b, int maxAlpha) {
        if (remaining <= 0) return 0;
        int draw = Math.min(remaining, length);
        if (horizontal) {
            for (int i = 0; i < glowSize; i++) {
                float factor = 1.0f - (float) i / glowSize;
                int alpha = Math.round(factor * factor * maxAlpha);
                context.fill(left, top + i, left + draw, top + i + 1, argb(alpha, r, g, b));
            }
        } else {
            for (int i = 0; i < glowSize; i++) {
                float factor = 1.0f - (float) i / glowSize;
                int alpha = Math.round(factor * factor * maxAlpha);
                context.fill(right - 1 - i, top, right - i, top + draw, argb(alpha, r, g, b));
            }
        }
        return remaining - draw;
    }

    private int drawGlowSegmentReverse(GuiGraphicsExtractor context, int left, int top, int right, int bottom, int remaining, int length, boolean horizontal, int glowSize, int r, int g, int b, int maxAlpha) {
        if (remaining <= 0) return 0;
        int draw = Math.min(remaining, length);
        if (horizontal) {
            for (int i = 0; i < glowSize; i++) {
                float factor = 1.0f - (float) i / glowSize;
                int alpha = Math.round(factor * factor * maxAlpha);
                context.fill(right - draw, bottom - 1 - i, right, bottom - i, argb(alpha, r, g, b));
            }
        } else {
            for (int i = 0; i < glowSize; i++) {
                float factor = 1.0f - (float) i / glowSize;
                int alpha = Math.round(factor * factor * maxAlpha);
                context.fill(left + i, bottom - draw, left + i + 1, bottom, argb(alpha, r, g, b));
            }
        }
        return remaining - draw;
    }

    private int argb(int a, int r, int g, int b) {
        return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
    }
}
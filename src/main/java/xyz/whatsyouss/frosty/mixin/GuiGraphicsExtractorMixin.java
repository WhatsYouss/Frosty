package xyz.whatsyouss.frosty.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jetbrains.annotations.Nullable;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.render.ScrollableTooltips;

import java.util.List;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {

    @Final
    @Shadow
    private Matrix3x2fStack pose;

    @Inject(method = "tooltip", at = @At("HEAD"))
    private void onTooltipStart(Font font, List<ClientTooltipComponent> lines,
                                int xo, int yo, ClientTooltipPositioner positioner, @Nullable Identifier style, CallbackInfo ci) {
        ScrollableTooltips st = ModuleManager.scrollableTooltips;
        if (!st.isEnabled()) return;

        pose.pushMatrix();
        pose.translate(0, st.getOffset());
        float scale = st.getScale();
        pose.translate(xo, yo);
        pose.scale(scale, scale);
        pose.translate(-xo, -yo);
    }

    @Inject(method = "tooltip", at = @At("TAIL"))
    private void onTooltipEnd(Font font, List<ClientTooltipComponent> lines, int xo, int yo,
                              ClientTooltipPositioner positioner, @Nullable Identifier style, CallbackInfo ci) {
        if (!ModuleManager.scrollableTooltips.isEnabled()) return;

        pose.popMatrix();
    }
}

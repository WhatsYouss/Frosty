package xyz.whatsyouss.frosty.modules.impl.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;

public class ArmorHider extends Module {

    public ButtonSetting head, selfOnly;

    public ArmorHider() {
        super("ArmorHider", category.Render);

        this.registerSetting(head = new ButtonSetting("Head", true));
        this.registerSetting(selfOnly = new ButtonSetting("Self only", true));
    }
}
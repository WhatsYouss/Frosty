package xyz.whatsyouss.frosty.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import net.minecraft.world.entity.Entity;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.Color;

public class AxolotlESP extends Module {

    public AxolotlESP() {
        super("AxolotlESP","美西螈透视", category.Render);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Axolotl && isInRange((Axolotl) entity)) {
                renderAxolotlESP(event.getMatrix(), (Axolotl) entity);
            }
        }
    }

    private boolean isInRange(Axolotl entity) {
        double RANGE = mc.options.renderDistance().get().doubleValue() * 16;
        return mc.player.distanceToSqr(entity) <= RANGE * RANGE;
    }

    private void renderAxolotlESP(PoseStack matrices, Axolotl axolotl) {
        if (!axolotl.getVariant().equals(Axolotl.Variant.LUCY)) {
            return;
        }
        RenderUtils.outlineEntity(matrices, axolotl, Color.PINK, 1.0f, false);
    }
}
package xyz.whatsyouss.frosty.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.other.AntiBot;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.*;

public class PlayerESP extends Module {

    private ButtonSetting expand, fill;

    private static double RANGE;

    public PlayerESP() {
        super("PlayerESP", category.Render);

        this.registerSetting(expand = new ButtonSetting("Expand", true));
        this.registerSetting(fill = new ButtonSetting("Fill", false));
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        RANGE = mc.options.renderDistance().get() * 16;

        for (Player player : mc.level.players()) {
            if (player == mc.player || AntiBot.isBot(player)) continue;

            if (mc.player.distanceToSqr(player) > RANGE * RANGE) continue;

            renderPlayerESP(event.getMatrix(), player, event.getDelta());
        }
    }

    private void renderPlayerESP(PoseStack matrices, Player player, float partialTicks) {
        double x = Mth.lerp(partialTicks, player.xOld, player.getX());
        double y = Mth.lerp(partialTicks, player.yOld, player.getY());
        double z = Mth.lerp(partialTicks, player.zOld, player.getZ());

        AABB box = player.getBoundingBox().move(x - player.getX(), y - player.getY(), z - player.getZ());
        if (expand.isToggled()) {
            box = box.inflate(0.1, 0.1, 0.1);
        }

        int colorValue = Utils.getColorFromEntity(player);
        Color color = colorValue != -1 ?
                new Color(colorValue) :
                new Color(255, 255, 255, 150);

        if (fill.isToggled()) {
            RenderUtils.drawBoxFilled(matrices, box, new Color(color.getRed(), color.getGreen(), color.getBlue(), 50), false);
        }

        RenderUtils.drawBox(matrices, box, color, 2f, false);
    }
}
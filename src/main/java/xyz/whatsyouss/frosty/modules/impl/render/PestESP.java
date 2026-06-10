package xyz.whatsyouss.frosty.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.util.math.Box;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.*;

public class PestESP extends Module {

    public PestESP() {
        super("PestESP", category.Render);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        String sidebar = Utils.getScoreboardSidebar().toString().toLowerCase();
        if (!sidebar.contains("the garde") && !sidebar.contains("plot")) {
            return;
        }
        if (!sidebar.contains("ൠ")) {
            return;
        }
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof BatEntity || entity instanceof SilverfishEntity) {
                RenderUtils.outlineEntity(event.getMatrix(), entity, Color.CYAN, 2f);
            }
        }
    }
}

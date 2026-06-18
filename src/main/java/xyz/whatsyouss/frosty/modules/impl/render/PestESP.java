package xyz.whatsyouss.frosty.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.phys.AABB;
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
        String sidebar = Utils.stripColor(Utils.getScoreboardSidebarLines().toString().toLowerCase());
        if (!sidebar.contains("the garde") && !sidebar.contains("plot")) {
            return;
        }
        if (!sidebar.contains("ൠ")) {
            return;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Bat || entity instanceof Silverfish) {
                RenderUtils.outlineEntity(event.getMatrix(), entity, Color.CYAN, 2f, false);
            }
        }
    }
}

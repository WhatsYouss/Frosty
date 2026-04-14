package xyz.whatsyouss.frosty.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Box;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.*;

public class StarredMobESP extends Module {

    private static final String[] STARS = new String[]{"✯", "✰", "★", "☆"};

    public StarredMobESP() {
        super("StarredMobESP", category.Render);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck()) return;
        String sidebar = Utils.getScoreboardSidebar().toString().toLowerCase();
        if (!sidebar.contains("the catac") || !sidebar.contains("keys")) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ArmorStandEntity armorStand && entity.getCustomName() != null) {
                for (String star : STARS) {
                    if (Utils.getLiteralByText(entity.getCustomName()).contains(star)) {
                       Entity target = findTargetUnder(armorStand);
                       if (target instanceof LivingEntity living && living != mc.player) {
                           RenderUtils.outlineEntity(event.getMatrix(), living, Color.CYAN, 2.0f);
                       }
                    }
                }
            }
        }
    }


    private Entity findTargetUnder(Entity stand) {
        Box box = stand.getBoundingBox().expand(1.0, 3.0, 1.0).offset(0.0, -1.5, 0.0);
        Entity closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Entity e : mc.world.getOtherEntities(stand, box)) {
            if (e instanceof LivingEntity && e != mc.player) {
                double dist = stand.squaredDistanceTo(e);
                if (dist < minDistance) {
                    minDistance = dist;
                    closest = e;
                }
            }
        }
        return closest;
    }
}
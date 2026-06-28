package xyz.whatsyouss.frosty.modules.impl.hunting;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.MathUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.List;

public class AutoReel extends Module {

    private int tickCD;

    public AutoReel() {
        super("AutoReel", "自动收线", category.Hunting);
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) return;

        if (tickCD > 0) {
            tickCD--;
            return;
        }

        if (!Utils.isHeldItem("Abysmal Lasso", "Vinerip Lasso", "Entangler Lasso", "Everstretch Lasso")) {
            return;
        }

        Entity pulledEntity = getPulledEntity(mc.player);
        if (pulledEntity == null) return;

        detectStatusArmorStands(pulledEntity);
    }

    private Entity getPulledEntity(Player player) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Mob) {
                Mob mob = (Mob) entity;
                if (mob.isLeashed() && mob.getLeashHolder() == player) {
                    return mob;
                }
            }
        }
        return null;
    }

    private void interactWithLasso() {
        if (mc.gameMode != null) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }
    }


    private void detectStatusArmorStands(Entity pulledEntity) {
        Vec3 searchCenter = pulledEntity.position().add(0, 2, 0);

        AABB searchBox = new AABB(
                searchCenter.x() - 2.0, searchCenter.y() - 2.0, searchCenter.z() - 2.0,
                searchCenter.x() + 2.0, searchCenter.y() + 2.0, searchCenter.z() + 2.0
        );

        List<ArmorStand> armorStands = mc.level.getEntitiesOfClass(
                ArmorStand.class,
                searchBox,
                stand -> stand.isInvisible() || stand.isMarker()
        );

        for (ArmorStand stand : armorStands) {
            if (stand.getDisplayName() != null) {
                if (stand.getDisplayName().getString().endsWith("REEL")) {
                    interactWithLasso();
                    tickCD = 15;
                }
            }
        }
    }
}
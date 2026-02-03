package xyz.whatsyouss.frosty.modules.impl.hunting;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.MathUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.List;

public class AutoReel extends Module {

    private int tickCD;

    public AutoReel() {
        super("AutoReel", category.Hunting);
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

    private Entity getPulledEntity(PlayerEntity player) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof MobEntity) {
                MobEntity mob = (MobEntity) entity;
                if (mob.isLeashed() && mob.getLeashHolder() == player) {
                    return mob;
                }
            }
        }
        return null;
    }

    private void interactWithLasso() {
        if (mc.interactionManager != null) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }
    }


    private void detectStatusArmorStands(Entity pulledEntity) {
        Vec3d searchCenter = pulledEntity.getEntityPos().add(0, 2, 0);

        Box searchBox = new Box(
                searchCenter.getX() - 2.0, searchCenter.getY() - 2.0, searchCenter.getZ() - 2.0,
                searchCenter.getX() + 2.0, searchCenter.getY() + 2.0, searchCenter.getZ() + 2.0
        );

        List<ArmorStandEntity> armorStands = mc.world.getEntitiesByClass(
                ArmorStandEntity.class,
                searchBox,
                stand -> stand.isInvisible() || stand.isMarker()
        );

        for (ArmorStandEntity stand : armorStands) {
            if (stand.getDisplayName() != null) {
                if (stand.getDisplayName().getString().endsWith("REEL")) {
                    interactWithLasso();
                    tickCD = 15;
                }
            }
        }
    }
}
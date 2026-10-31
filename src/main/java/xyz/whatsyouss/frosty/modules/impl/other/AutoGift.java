package xyz.whatsyouss.frosty.modules.impl.other;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.EntityHitResult;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.Utils;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

public class AutoGift extends Module {

//    private ButtonSetting rotation;
    private long lastInteractTime = 0;

    public AutoGift() {
        super("AutoGift", "自动收礼", category.Other);

//        this.registerSetting(rotation = new ButtonSetting("Rotation", true));
    }

    @Override
    public void onEnable() {
        lastInteractTime = 0;
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        double interactRange = 4.5;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ArmorStand armorStand) {
                if (mc.player.distanceTo(armorStand) > interactRange) {
                    continue;
                }

                String standName = armorStand.getDisplayName().getString();

                if (standName.contains("CLICK TO")) {

//                    double dx = armorStand.getX() - mc.player.getX();
//                    double dy = (armorStand.getY() + armorStand.getBbHeight() / 2.0) - (mc.player.getY() + mc.player.getEyeHeight());
//                    double dz = armorStand.getZ() - mc.player.getZ();
//                    double dh = Math.sqrt(dx * dx + dz * dz);
//
//                    float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
//                    float targetPitch = (float) -(Math.atan2(dy, dh) * 180.0 / Math.PI);
//
//                    if (rotation.isToggled()) {
//                        Rotations.setRotate(this, targetYaw, targetPitch, 9, 3);
//
//                        float diffYaw = (targetYaw - Rotations.lastServerYaw) % 360;
//                        if (diffYaw < -180) diffYaw += 360;
//                        if (diffYaw > 180) diffYaw -= 360;
//                        diffYaw = Math.abs(diffYaw);
//
//                        float diffPitch = Math.abs(targetPitch - Rotations.lastServerPitch);
//
//                        if (diffYaw > 3.0f || diffPitch > 3.0f) {
//                            return;
//                        }
//                    }

                    if (System.currentTimeMillis() - lastInteractTime > 400) {
                        net.minecraft.world.phys.Vec3 hitVec = new net.minecraft.world.phys.Vec3(
                                armorStand.getX(),
                                armorStand.getY() + (armorStand.getBbHeight() / 2.0),
                                armorStand.getZ()
                        );

                        EntityHitResult hitResult = new net.minecraft.world.phys.EntityHitResult(armorStand, hitVec);

                        mc.gameMode.interact(mc.player, armorStand, hitResult, InteractionHand.MAIN_HAND);

                        lastInteractTime = System.currentTimeMillis();
                    }
                    return;
                }
            }
        }
    }
}
package xyz.whatsyouss.frosty.modules.impl.hunting;

import meteordevelopment.orbit.EventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.interfaces.ICameraOverriddenEntity;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.RotationUtils;
import xyz.whatsyouss.frosty.utility.Rotations;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Hideonleaf extends Module {

    private ButtonSetting esp, hit, silent;
    private SliderSetting offset, range;

    public Hideonleaf() {
        super("Shulkers", category.Hunting);

        this.registerSetting(esp = new ButtonSetting("ESP", true));
        this.registerSetting(hit = new ButtonSetting("Attack", true));
        this.registerSetting(silent = new ButtonSetting("Silent", true));
        this.registerSetting(offset = new SliderSetting("Height offset", "x", 1.2, 0.8, 1.6, 0.1));
        this.registerSetting(range = new SliderSetting("Range", 2, 10, 2, 12, 0.5));
    }

    @Override
    public String getDesc() {
        return "Shulkers on Galatea";
    }

    @Override
    public void guiUpdate() {
        this.silent.setVisibilityCondition(() -> hit.isToggled());
        this.offset.setVisibilityCondition(() -> hit.isToggled());
        this.range.setVisibilityCondition(() -> hit.isToggled());
    }


    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck() || !esp.isToggled()) {
            return;
        }

        Map<String, String> location = Utils.getCurrentLocation();
        if (!Objects.equals(location.get("Area"), "Galatea")) {
            return;
        }


        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Shulker && isInRange(entity) && isGreenShulker((Shulker) entity)) {
                renderShulkerESP(event.getMatrix(), (Shulker) entity);
            }
        }
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck() || !hit.isToggled()) {
            return;
        }

        Map<String, String> location = Utils.getCurrentLocation();
        if (!Objects.equals(location.get("Area"), "Galatea")) {
            return;
        }


        List<ShulkerBullet> bullets = new ArrayList<>();
        List<Shulker> shulkers = new ArrayList<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ShulkerBullet) {
                if (mc.player.distanceTo(entity) <= range.getInputMax()) {
                    bullets.add((ShulkerBullet) entity);
                }
            } else if (entity instanceof Shulker && isGreenShulker((Shulker) entity)) {
                if (mc.player.distanceTo(entity) >= range.getInputMin()
                        && mc.player.distanceTo(entity) <= range.getInputMax()) {
                    shulkers.add((Shulker) entity);
                }
            }
        }


        ShulkerBullet nearestBullet = bullets.stream()
                .min(Comparator.comparingDouble(b -> mc.player.distanceTo(b)))
                .orElse(null);

        Shulker nearestShulker = shulkers.stream()
                .min(Comparator.comparingDouble(s -> mc.player.distanceTo(s)))
                .orElse(null);

        if (nearestBullet != null && nearestShulker != null) {

            Vec3 shulkerPos = nearestShulker.position().add(0, nearestShulker.getBbHeight() * offset.getInput(), 0);

            float[] angles = RotationUtils.getYawPitchTo(mc.player.getEyePosition(), shulkerPos);

            if (!silent.isToggled()) {
                RotationUtils.aimByPos(shulkerPos);
            } else {
                Rotations.setRotate(this, angles[0], angles[1], 5);
            }

            if (mc.player.getItemInHand(InteractionHand.MAIN_HAND).getCustomName() != null && Utils.getFirstLiteral(mc.player.getItemInHand(InteractionHand.MAIN_HAND).getCustomName().toString()).contains("Fishing Net")) {
                rightClick();
            } else {
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        } else {
            if (Rotations.rotating) {
                Rotations.cancelRotate(this);
            }
        }
    }

    private boolean isInRange(Entity entity) {
        double RANGE = mc.options.renderDistance().get() * 16;
        return mc.player.distanceToSqr(entity) <= RANGE * RANGE;
    }

    private void rightClick() {
        if (mc.gameMode == null) {
            return;
        }
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
    }

    private boolean isGreenShulker(Shulker shulker) {
        try {
            return shulker.getColor() != null && shulker.getColor().name().equalsIgnoreCase("green");
        } catch (Exception e) {
            return false;
        }
    }

    private void renderShulkerESP(PoseStack matrices, Shulker shulker) {
        RenderUtils.outlineEntity(matrices, shulker, Color.GREEN, 1.0f, false);
    }
}
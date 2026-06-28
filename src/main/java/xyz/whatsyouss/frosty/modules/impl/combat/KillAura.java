package xyz.whatsyouss.frosty.modules.impl.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.MouseButtonEvent;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import xyz.whatsyouss.frosty.modules.impl.other.AntiBot;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.*;

public class KillAura extends Module {

    private ButtonSetting player, requirePress, autoblockRequirePress;
    private SliderSetting aps, attackRange, swingRange;

    private boolean lcing, rcing, blocking;
    private int attackCooldown = 0;
    private Entity currentTarget = null;

    public KillAura() {
        super("KillAura", "杀戮光环", category.Combat);

        this.registerSetting(aps = new SliderSetting("APS", 14, 1, 20, 1));
        this.registerSetting(swingRange = new SliderSetting("Swing range", 4.5, 3, 7, 0.1));
        this.registerSetting(attackRange = new SliderSetting("Attack range", 3.2, 3, 5, 0.1));
        this.registerSetting(player = new ButtonSetting("Player", false));
        this.registerSetting(requirePress = new ButtonSetting("Require press", false));
    }

    @Override
    public void onEnable() {
        attackCooldown = 0;
        currentTarget = null;
        lcing = false;
        rcing = false;
        blocking = false;
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        lcing = false;
        rcing = false;
        blocking = false;
        Rotations.cancelRotate(this);
    }

    @EventHandler
    public void onMouseButton(MouseButtonEvent event) {
        if (event.button == 0) {
            if (event.action == KeyAction.Press) {
                lcing = true;
            } else if (event.action == KeyAction.Release) {
                lcing = false;
            }
            if (currentTarget != null && mc.gui.screen() == null) {
                event.cancel();
            }
        }
        if (event.button == 1) {
            if (event.action == KeyAction.Press) {
                rcing = true;
            } else if (event.action == KeyAction.Release) {
                rcing = false;
            }
            if (currentTarget != null && mc.gui.screen() == null) {
                event.cancel();
            }
        }
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            currentTarget = null;
            return;
        }
        if (currentTarget == null) {
            Rotations.cancelRotate(this);
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        Entity closestEntity = findClosestTarget();

        if (closestEntity != null && closestEntity.isAlive()) {
            currentTarget = closestEntity;

            if (requirePress.isToggled() && !lcing) return;
            float[] angle = RotationUtils.getYawPitchTo(mc.player.getEyePosition(), currentTarget.getEyePosition());
            Rotations.setRotate(this, angle[0], angle[1], 10, 2);

            handleAttack();
        } else {
            currentTarget = null;
        }
    }

    private Entity findClosestTarget() {
        Entity closestEntity = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isValidTarget(entity)) continue;

            double distance = mc.player.distanceTo(entity);
            if (distance <= swingRange.getInput() && distance < closestDistance) {
                closestDistance = distance;
                closestEntity = entity;
            }
        }

        return closestEntity;
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player) return false;
        if (!entity.isAlive()) return false;
        if (entity instanceof Mob) return true;
        if (player.isToggled() && entity instanceof Player p && !AntiBot.isBot(p)) return true;
        return false;
    }

    private void handleAttack() {
        if (attackCooldown > 0) return;
        if (mc.player.distanceTo(currentTarget) > attackRange.getInput()) return;

        if (!RotationUtils.isPossibleToHit(currentTarget, attackRange.getInput(),
                new float[]{Rotations.serverYaw, Rotations.serverPitch})) return;

        mc.player.attack(currentTarget);

        Vec3 hitVec = currentTarget.position().subtract(mc.player.position());
        mc.player.connection.send(new ServerboundAttackPacket(currentTarget.getId()));
        mc.player.swing(InteractionHand.MAIN_HAND);

        attackCooldown = Utils.getAPSToTicks(aps, 20);
    }

    private void sendInteractItemPacket() {
        if (autoblockRequirePress != null && autoblockRequirePress.isToggled() && !rcing) {
            return;
        }
        mc.player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0,
                Rotations.serverYaw, Rotations.serverPitch));
        blocking = true;
    }

    private void sendReleaseUseItem() {
        if (!blocking) {
            return;
        }
        mc.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ZERO, Direction.DOWN));blocking = false;
    }

    public Entity getCurrentTarget() {
        return currentTarget;
    }

    public void setCurrentTarget(Entity target) {
        this.currentTarget = target;
    }

    public Mob getCurrentTargetAsMob() {
        return currentTarget instanceof Mob m ? m : null;
    }
}
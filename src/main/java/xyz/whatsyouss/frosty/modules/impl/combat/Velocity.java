package xyz.whatsyouss.frosty.modules.impl.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Vec3d;
import xyz.whatsyouss.frosty.events.impl.ReceivePacketEvent;
import xyz.whatsyouss.frosty.mixin.accessor.EntityVelocityUpdateS2CPacketAccessor;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;

public class Velocity extends Module {
    private SliderSetting horizontal;
    private SliderSetting vertical;
    private ButtonSetting cancelExplosion;


    public Velocity() {
        super("Velocity", category.Combat);
        this.registerSetting(horizontal = new SliderSetting("Horizontal", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(vertical = new SliderSetting("Vertical", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(cancelExplosion = new ButtonSetting("00 Explosion", true));
    }

    @EventHandler
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!Utils.nullCheck() || e.isCancelled()) {
            return;
        }

        if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getEntityId() == mc.player.getId()) {
                if (cancel()) {
                    e.setCancelled(true);
                    return;
                }

                Vec3d currentVel = mc.player.getVelocity();

                double deltaX = (packet.getVelocity().x - currentVel.x) * horizontal.getInput() / 100.0;
                double deltaY = (packet.getVelocity().y - currentVel.y) * vertical.getInput()   / 100.0;
                double deltaZ = (packet.getVelocity().z - currentVel.z) * horizontal.getInput() / 100.0;

                Vec3d newVelocity = new Vec3d(
                        currentVel.x + deltaX,
                        currentVel.y + deltaY,
                        currentVel.z + deltaZ
                );

                ((EntityVelocityUpdateS2CPacketAccessor) packet).frosty$setVelocity(newVelocity);
            }
        }
        else if (e.getPacket() instanceof ExplosionS2CPacket) {
            if (cancelExplosion.isToggled()) {
                e.setCancelled(true);
            }
        }
    }

    private static void getCancel(ReceivePacketEvent e) {
        e.cancel();
    }

    private boolean cancel() {
        return (vertical.getInput() == 0 && horizontal.getInput() == 0);
    }

    @Override
    public String getInfo() {
        return (int) horizontal.getInput() + "%" + " " + (int) vertical.getInput() + "%";
    }
}
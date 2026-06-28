package xyz.whatsyouss.frosty.modules.impl.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.ReceivePacketEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;

public class Velocity extends Module {
    private SliderSetting horizontal;
    private SliderSetting vertical;
    private ButtonSetting cancelExplosion;


    public Velocity() {
        super("Velocity", "反击退", category.Combat);
        this.registerSetting(horizontal = new SliderSetting("Horizontal", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(vertical = new SliderSetting("Vertical", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(cancelExplosion = new ButtonSetting("00 Explosion", true));
    }

    @EventHandler
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!Utils.nullCheck() || e.isCancelled()) {
            return;
        }

        if (e.getPacket() instanceof ClientboundSetEntityMotionPacket packet) {
            if (packet.id() == mc.player.getId()) {

                if (cancel()) {
                    e.setCancelled(true);
                    return;
                }

                Vec3 currentVel = mc.player.getDeltaMovement();
                Vec3 packetVel = packet.movement();

                double deltaX = (packetVel.x - currentVel.x) * horizontal.getInput() / 100.0;
                double deltaY = (packetVel.y - currentVel.y) * vertical.getInput()   / 100.0;
                double deltaZ = (packetVel.z - currentVel.z) * horizontal.getInput() / 100.0;

                Vec3 newVelocity = new Vec3(currentVel.x + deltaX, currentVel.y + deltaY, currentVel.z + deltaZ);

                e.setCancelled(true);

                mc.player.setDeltaMovement(newVelocity);
            }
        }
        else if (e.getPacket() instanceof ClientboundExplodePacket) {
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
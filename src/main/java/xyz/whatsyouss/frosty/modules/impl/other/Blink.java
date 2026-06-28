package xyz.whatsyouss.frosty.modules.impl.other;

import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.events.impl.SendPacketEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Blink extends Module {

    private SliderSetting maxTime, releaseTimer;
    private ButtonSetting renderServerPos;

    private record QueuedPacket(ServerboundMovePlayerPacket packet, Vec3 capturedPos) {}

    private final List<QueuedPacket> packets = new ArrayList<>();

    private boolean sending = false;
    private int aliveTicks = 0;
    private int releaseTickTimer = 0;

    private Vec3 serverPos = null;

    public Blink() {
        super("Blink", "瞬移", category.Other);

        this.registerSetting(maxTime = new SliderSetting("Max time", "s", 0.7, 0.1, 20, 0.1));
        this.registerSetting(releaseTimer = new SliderSetting("Release timer", "s", 0.01, 0, 0.25, 0.01));
        this.registerSetting(renderServerPos = new ButtonSetting("Render server pos", true));
    }

    @Override
    public void onEnable() {
        if (!Utils.nullCheck()) {
            this.disable();
            return;
        }

        packets.clear();
        aliveTicks = 0;
        releaseTickTimer = 0;
        serverPos = mc.player.position();
    }

    @Override
    public void onDisable() {
        if (!Utils.nullCheck()) return;
        flushAll();
        serverPos = null;
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) return;

        aliveTicks++;

        int maxTicks = secondsToTicks(maxTime.getInput());
        if (maxTicks > 0 && aliveTicks >= maxTicks) {
            this.disable();
            return;
        }

        int releaseTicks = secondsToTicks(releaseTimer.getInput());
        if (releaseTicks <= 0) return;

        releaseTickTimer++;
        if (releaseTickTimer >= releaseTicks) {
            releaseTickTimer = 0;
            flushOne();
        }
    }

    @EventHandler
    public void onSendPacket(SendPacketEvent event) {
        if (!Utils.nullCheck() || sending) return;
        if (!(event.packet instanceof ServerboundMovePlayerPacket p)) return;

        event.cancel();

        QueuedPacket prev = packets.isEmpty() ? null : packets.get(packets.size() - 1);
        if (prev != null &&
                p.isOnGround() == prev.packet().isOnGround() &&
                p.getYRot(-1) == prev.packet().getYRot(-1) &&
                p.getXRot(-1) == prev.packet().getXRot(-1) &&
                p.getX(-1) == prev.packet().getX(-1) &&
                p.getY(-1) == prev.packet().getY(-1) &&
                p.getZ(-1) == prev.packet().getZ(-1)
        ) return;

        synchronized (packets) {
            packets.add(new QueuedPacket(p, mc.player.position()));
        }
    }

    private void flushOne() {
        synchronized (packets) {
            if (packets.isEmpty()) return;
            sending = true;
            QueuedPacket qp = packets.remove(0);
            mc.player.connection.send(qp.packet());
            serverPos = qp.capturedPos();
            sending = false;
        }
    }

    private void flushAll() {
        sending = true;
        synchronized (packets) {
            for (QueuedPacket qp : packets) {
                mc.player.connection.send(qp.packet());
                serverPos = qp.capturedPos();
            }
            packets.clear();
        }
        sending = false;
        aliveTicks = 0;
        releaseTickTimer = 0;
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck() || !this.isEnabled()) return;
        if (!renderServerPos.isToggled() || serverPos == null) return;

        PoseStack stack = event.getMatrix();

        double halfW = mc.player.getBbWidth() / 2.0;
        double height = mc.player.getBbHeight();

        AABB box = new AABB(
                serverPos.x - halfW, serverPos.y, serverPos.z - halfW,
                serverPos.x + halfW, serverPos.y + height, serverPos.z + halfW
        );

        RenderUtils.drawBoxFilled(stack, box, Color.green, false);
    }

    private int secondsToTicks(double seconds) {
        return (int) Math.round(seconds * 20.0);
    }

    @Override
    public String getInfo() {
        return String.format("%.1fs", aliveTicks / 20f);
    }
}
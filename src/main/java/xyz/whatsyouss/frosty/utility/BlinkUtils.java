package xyz.whatsyouss.frosty.utility;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.Packet;
import xyz.whatsyouss.frosty.events.impl.SendPacketEvent;

import java.util.concurrent.ConcurrentLinkedQueue;

public class BlinkUtils {
    private static ConcurrentLinkedQueue<Packet> blinkedPackets = new ConcurrentLinkedQueue();

    @EventHandler
    public void onSendPacket(SendPacketEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
    }
}

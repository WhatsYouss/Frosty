package xyz.whatsyouss.frosty.events.impl;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import xyz.whatsyouss.frosty.events.Cancellable;

public class ReceivePacketEvent extends Cancellable {
    public Packet<?> packet;
    public ClientConnection connection;

    public ReceivePacketEvent(Packet<?> packet, ClientConnection connection) {
        this.setCancelled(false);
        this.packet = packet;
        this.connection = connection;
    }

    public Packet<?> getPacket() {
        return packet;
    }
}

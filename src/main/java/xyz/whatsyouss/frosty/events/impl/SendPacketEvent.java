package xyz.whatsyouss.frosty.events.impl;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import xyz.whatsyouss.frosty.events.Cancellable;

public class SendPacketEvent extends Cancellable {
    public Packet<?> packet;
    public Connection connection;
    public boolean isCommand;

    public SendPacketEvent(Packet<?> packet, Connection connection) {
        this.setCancelled(false);
        this.packet = packet;
        this.connection = connection;
        this.isCommand = isCommandPacket(packet);
    }

    public Packet<?> getPacket() {
        return packet;
    }

    private boolean isCommandPacket(Packet<?> packet) {
        try {
            String packetClass = packet.getClass().getSimpleName();
            if (packetClass.equals("ChatMessageC2SPacket")) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
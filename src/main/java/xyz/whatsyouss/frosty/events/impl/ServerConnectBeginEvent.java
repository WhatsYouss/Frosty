package xyz.whatsyouss.frosty.events.impl;

import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;

public class ServerConnectBeginEvent {
    private static final ServerConnectBeginEvent INSTANCE = new ServerConnectBeginEvent();
    public ServerAddress address;
    public ServerData info;

    public static ServerConnectBeginEvent get(ServerAddress address, ServerData info) {
        INSTANCE.address = address;
        INSTANCE.info = info;
        return INSTANCE;
    }
}
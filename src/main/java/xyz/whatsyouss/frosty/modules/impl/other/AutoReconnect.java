package xyz.whatsyouss.frosty.modules.impl.other;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import xyz.whatsyouss.frosty.events.impl.ServerConnectBeginEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;

public class AutoReconnect extends Module {

    public SliderSetting delay;

    public Pair<ServerAddress, ServerData> lastServerConnection;

    public AutoReconnect() {
        super("AutoReconnect", "自动重连", category.Other);

        this.registerSetting(delay = new SliderSetting("Delay", 1000, 500, 5000, 100));
    }

    @EventHandler
    private void onServerConnectBegin(ServerConnectBeginEvent event) {
        lastServerConnection = new ObjectObjectImmutablePair<>(event.address, event.info);
    }
}

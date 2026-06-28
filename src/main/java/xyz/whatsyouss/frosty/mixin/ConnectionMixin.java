package xyz.whatsyouss.frosty.mixin;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.network.EventLoopGroupHolder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.commands.Command;
import xyz.whatsyouss.frosty.commands.CommandManager;
import xyz.whatsyouss.frosty.events.impl.*;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.utility.Utils;

import java.net.InetSocketAddress;
import java.util.Iterator;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void onHandlePacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            for (Iterator<Packet<? super ClientGamePacketListener>> it = bundle.subPackets().iterator(); it.hasNext(); ) {
                if (Frosty.EVENT_BUS.post(new ReceivePacketEvent(it.next(), (Connection) (Object) this)).isCancelled())
                    it.remove();
            }
        } else if (Frosty.EVENT_BUS.post(new ReceivePacketEvent(packet, (Connection) (Object) this)).isCancelled())
            ci.cancel();
    }

    @Inject(method = "connect(Ljava/net/InetSocketAddress;Lnet/minecraft/server/network/EventLoopGroupHolder;Lnet/minecraft/network/Connection;)Lio/netty/channel/ChannelFuture;", at = @At("HEAD"))
    private static void onConnect(InetSocketAddress address, EventLoopGroupHolder eventLoopGroupHolder, Connection connection, CallbackInfoReturnable<ChannelFuture> cir) {
        Frosty.EVENT_BUS.post(ServerConnectEndEvent.get(address));
    }

    @Inject(at = @At("HEAD"), method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", cancellable = true)
    private void onSendPacketHead(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        if (Frosty.EVENT_BUS.post(new SendPacketEvent(packet, (Connection) (Object) this)).isCancelled()) {
            ci.cancel();
        }
        if (packet instanceof ServerboundChatPacket && ModuleManager.commands.isEnabled()) {
            String message = ((ServerboundChatPacket) packet).message();
            if (message.startsWith(".")) {
                handleCommand(message.substring(1));
                ci.cancel();
            }
        }
    }

    @Unique
    private void handleCommand(String input) {
        String[] parts = input.split(" ");
        if (parts.length == 0) return;

        String commandName = parts[0];
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        Command command = CommandManager.getCommandByName(commandName.toLowerCase());

        if (command != null) {
            try {
                command.execute(args);
            } catch (Exception e) {
                Utils.addChatMessage("§cError executing command: " + e.getMessage());
            }
        } else {
            Utils.addChatMessage("§cUnknown command: " + commandName);
        }
    }
}
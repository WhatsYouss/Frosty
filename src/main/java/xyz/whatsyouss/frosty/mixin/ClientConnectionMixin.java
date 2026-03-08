package xyz.whatsyouss.frosty.mixin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.commands.Command;
import xyz.whatsyouss.frosty.commands.CommandManager;
import xyz.whatsyouss.frosty.events.impl.ReceivePacketEvent;
import xyz.whatsyouss.frosty.events.impl.SendPacketEvent;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.Iterator;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;handlePacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void onHandlePacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof BundleS2CPacket bundle) {
            for (Iterator<Packet<? super ClientPlayPacketListener>> it = bundle.getPackets().iterator(); it.hasNext(); ) {
                if (Frosty.EVENT_BUS.post(new ReceivePacketEvent(it.next(), (ClientConnection) (Object) this)).isCancelled())
                    it.remove();
            }
        } else if (Frosty.EVENT_BUS.post(new ReceivePacketEvent(packet, (ClientConnection) (Object) this)).isCancelled())
            ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;)V", cancellable = true)
    private void onSendPacketHead(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        if (Frosty.EVENT_BUS.post(new SendPacketEvent(packet, (ClientConnection) (Object) this)).isCancelled()) {
            ci.cancel();
        }
        if (packet instanceof ChatMessageC2SPacket && ModuleManager.commands.isEnabled()) {
            String message = ((ChatMessageC2SPacket) packet).chatMessage();
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
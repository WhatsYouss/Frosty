package xyz.whatsyouss.frosty.modules.impl.mining;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.chat.Component;
import xyz.whatsyouss.frosty.events.impl.ReceivePacketEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.Utils;

public class NoBreakReset extends Module {

    private String holding;

    public NoBreakReset() {
        super("NoBreakReset", "无破坏重置", category.Mining);
    }

    @EventHandler
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.getPacket() instanceof ClientboundContainerSetSlotPacket packet) {
            ItemStack stack = packet.getItem();

            if (!stack.isEmpty() && stack.getCustomName() != null) {
                holding = Utils.getLiteralByText(Component.literal(stack.getCustomName().toString()));
            }

            if (stack.getItem() == Items.STONE_AXE && holding.contains("Fig") || stack.getItem() == Items.PRISMARINE_SHARD && holding.contains("Drill") || stack.getItem() == Items.DIAMOND_PICKAXE) {
                event.setCancelled(true);
            }
        }
    }
}
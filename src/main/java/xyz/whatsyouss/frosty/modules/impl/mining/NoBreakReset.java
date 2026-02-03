package xyz.whatsyouss.frosty.modules.impl.mining;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.text.Text;
import xyz.whatsyouss.frosty.events.impl.ReceivePacketEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.Utils;

public class NoBreakReset extends Module {

    private String holding;

    public NoBreakReset() {
        super("NoBreakReset", category.Mining);
    }

    @EventHandler
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket packet) {
            ItemStack stack = packet.getStack();

            if (!stack.isEmpty() && stack.getCustomName() != null) {
                holding = Utils.getLiteralByText(Text.of(stack.getCustomName().toString()));
            }

            if (stack.getItem() == Items.STONE_AXE && holding.contains("Fig") || stack.getItem() == Items.PRISMARINE_SHARD && holding.contains("Drill") || stack.getItem() == Items.DIAMOND_PICKAXE) {
                event.setCancelled(true);
            }
        }
    }
}
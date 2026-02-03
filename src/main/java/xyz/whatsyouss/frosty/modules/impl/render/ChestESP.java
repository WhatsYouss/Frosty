package xyz.whatsyouss.frosty.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.Color;

public class ChestESP extends Module {

    public ChestESP() {
        super("ChestESP", category.Render);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        BlockPos playerPos = mc.player.getBlockPos();
        int renderDistance = mc.options.getViewDistance().getValue();
        ChunkPos centerChunk = new ChunkPos(playerPos);
        int chunkRadius = Math.min(renderDistance, 8);

        for (int x = centerChunk.x - chunkRadius; x <= centerChunk.x + chunkRadius; x++) {
            for (int z = centerChunk.z - chunkRadius; z <= centerChunk.z + chunkRadius; z++) {
                WorldChunk chunk = mc.world.getChunkManager().getChunk(x, z, net.minecraft.world.chunk.ChunkStatus.FULL, false);
                if (chunk != null) {
                    for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                        if (blockEntity instanceof ChestBlockEntity chest) {
                            BlockPos pos = chest.getPos();
                            Block block = mc.world.getBlockState(pos).getBlock();

                            if (block instanceof ChestBlock) {
                                Box box = adjustChestBox(pos);
                                renderChestESP(event.getMatrix(), box);
                            }
                        }
                    }
                }
            }
        }
    }

    private Box adjustChestBox(BlockPos pos) {
        double scale = 0.875;
        double singleWidth = 1.0 * scale;
        double singleMinX = pos.getX() + (1.0 - scale) / 2;
        double singleMinZ = pos.getZ() + (1.0 - scale) / 2;
        Box box = new Box(
                singleMinX, pos.getY(), singleMinZ,
                singleMinX + singleWidth, pos.getY() + singleWidth, singleMinZ + singleWidth
        );

        BlockState state = mc.world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return box;
        }

        Direction facing = state.get(ChestBlock.FACING);
        ChestType chestType = state.get(ChestBlock.CHEST_TYPE);

        if (chestType != ChestType.SINGLE) {
            BlockPos connectedPos;
            if (chestType == ChestType.RIGHT) {
                connectedPos = pos.offset(facing.rotateYCounterclockwise());
            } else {
                connectedPos = pos.offset(facing.rotateYClockwise());
            }

            if (mc.world.getBlockState(connectedPos).getBlock() instanceof ChestBlock) {
                double minX = Math.min(pos.getX(), connectedPos.getX()) + (1.0 - scale) / 2;
                double minZ = Math.min(pos.getZ(), connectedPos.getZ()) + (1.0 - scale) / 2;
                double doubleWidthX = (facing.getAxis() == Direction.Axis.X) ? 2.0 * scale : singleWidth;
                double doubleWidthZ = (facing.getAxis() == Direction.Axis.Z) ? 2.0 * scale : singleWidth;
                return new Box(
                        minX, pos.getY(), minZ,
                        minX + doubleWidthX, pos.getY() + singleWidth, minZ + doubleWidthZ
                );
            }
        }

        return box;
    }

    private void renderChestESP(MatrixStack matrices, Box box) {
        RenderUtils.drawBox(matrices, box, new Color(200, 125, 0), 3f);
    }
}
package xyz.whatsyouss.frosty.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.Color;

public class ChestESP extends Module {

    public ChestESP() {
        super("ChestESP", "容器透视", category.Render);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        Vec3 cam = new Vec3(event.getOffsetX(), event.getOffsetY(), event.getOffsetZ());

        BlockPos playerPos = mc.player.blockPosition();
        int renderDistance = mc.options.renderDistance().get();

        ChunkPos centerChunk = new ChunkPos(playerPos.getX() >> 4, playerPos.getZ() >> 4);
        int chunkRadius = Math.min(renderDistance, 8);

        for (int x = centerChunk.x() - chunkRadius; x <= centerChunk.x() + chunkRadius; x++) {
            for (int z = centerChunk.z() - chunkRadius; z <= centerChunk.z() + chunkRadius; z++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk != null) {
                    for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                        if (blockEntity instanceof ChestBlockEntity chest) {
                            BlockPos pos = chest.getBlockPos();
                            Block block = mc.level.getBlockState(pos).getBlock();

                            if (block instanceof ChestBlock) {
                                AABB box = adjustChestBox(pos);
                                renderChestESP(event.getMatrix(), box);
                            }
                        }
                    }
                }
            }
        }
    }

    private AABB adjustChestBox(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return new AABB(pos);
        }

        double scale = 0.875;
        double offset = (1.0 - scale) / 2.0;

        Direction facing = state.getValue(ChestBlock.FACING);
        ChestType chestType = state.getValue(ChestBlock.TYPE);

        if (chestType == ChestType.SINGLE) {
            return new AABB(
                    pos.getX() + offset,         pos.getY(),
                    pos.getZ() + offset,
                    pos.getX() + offset + scale, pos.getY() + scale,
                    pos.getZ() + offset + scale
            );
        }

        BlockPos connectedPos = (chestType == ChestType.RIGHT)
                ? pos.relative(facing.getClockWise())
                : pos.relative(facing.getCounterClockWise());

        if (!(mc.level.getBlockState(connectedPos).getBlock() instanceof ChestBlock)) {
            return new AABB(
                    pos.getX() + offset,         pos.getY(),
                    pos.getZ() + offset,
                    pos.getX() + offset + scale, pos.getY() + scale,
                    pos.getZ() + offset + scale
            );
        }

        double minX = Math.min(pos.getX(), connectedPos.getX()) + offset;
        double minZ = Math.min(pos.getZ(), connectedPos.getZ()) + offset;

        double sizeX, sizeZ;
        if (facing.getAxis() == Direction.Axis.Z) {
            sizeX = scale * 2.0;
            sizeZ = scale;
        } else {
            sizeX = scale;
            sizeZ = scale * 2.0;
        }

        return new AABB(
                minX,         pos.getY(),
                minZ,
                minX + sizeX, pos.getY() + scale,
                minZ + sizeZ
        );
    }

    private void renderChestESP(PoseStack matrices, AABB box) {
        RenderUtils.drawBox(matrices, box, new Color(200, 125, 0), 5f, false);
    }
}
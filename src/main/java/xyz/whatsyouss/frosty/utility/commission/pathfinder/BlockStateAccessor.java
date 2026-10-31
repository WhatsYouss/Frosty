package xyz.whatsyouss.frosty.utility.commission.pathfinder;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class BlockStateAccessor {
    public final ClientLevel world;
    public BlockStateAccessor(ClientLevel world) { this.world = world; }
    public BlockState get(int x, int y, int z) { return isBlockInLoadedChunks(x, z) ? world.getBlockState(new BlockPos(x, y, z)) : Blocks.AIR.defaultBlockState(); }
    public boolean isBlockInLoadedChunks(int x, int z) { return world.getChunkSource().getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false) != null; }
}

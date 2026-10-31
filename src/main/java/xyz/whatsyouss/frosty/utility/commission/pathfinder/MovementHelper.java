package xyz.whatsyouss.frosty.utility.commission.pathfinder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

public final class MovementHelper {
    private MovementHelper() { }
    public static double collisionMaxY(BlockState state, BlockGetter world, BlockPos pos) { try { var shape=state.getCollisionShape(world,pos); return shape.isEmpty() ? 0 : shape.bounds().maxY; } catch (UnsupportedOperationException ignored) { return 0; } }
    public static boolean canWalkThrough(BlockStateAccessor bsa, int x, int y, int z) { return canWalkThrough(bsa,x,y,z,bsa.get(x,y,z)); }
    public static boolean canWalkThrough(BlockStateAccessor bsa, int x, int y, int z, BlockState state) { Boolean value=canWalkThroughBlockState(state,bsa); return value != null ? value : canWalkThroughPosition(bsa,x,y,z,state); }
    public static Boolean canWalkThroughBlockState(BlockState state, BlockStateAccessor bsa) {
        Block block=state.getBlock();
        if (state.isAir()) return true;
        if (block==Blocks.FIRE || block==Blocks.TRIPWIRE || block==Blocks.COBWEB || block==Blocks.END_PORTAL || block==Blocks.COCOA || block instanceof TrapDoorBlock) return false;
        if (block instanceof DoorBlock) return state.getValue(DoorBlock.OPEN);
        if (block instanceof FenceGateBlock) return state.getValue(FenceGateBlock.OPEN);
        if (block instanceof CarpetBlock || block instanceof SnowLayerBlock) return null;
        if (!state.getFluidState().isEmpty()) return state.getFluidState().isSource() ? null : false;
        if (block instanceof CauldronBlock || block==Blocks.LADDER) return false;
        try { return bsa == null ? null : state.getCollisionShape(bsa.world, BlockPos.ZERO).isEmpty(); } catch (Throwable ignored) { return null; }
    }
    public static boolean canWalkThroughPosition(BlockStateAccessor bsa, int x, int y, int z, BlockState state) {
        Block block=state.getBlock();
        if (block instanceof CarpetBlock) return canStandOn(bsa,x,y-1,z);
        if (block instanceof SnowLayerBlock) return !bsa.isBlockInLoadedChunks(x,z) || state.getValue(SnowLayerBlock.LAYERS) < 1 && canStandOn(bsa,x,y-1,z);
        if (!state.getFluidState().isEmpty()) { if (isFlowing(x,y,z,state,bsa)) return false; BlockState up=bsa.get(x,y+1,z); return up.getFluidState().isEmpty() && up.getBlock()!=Blocks.LILY_PAD && state.getFluidState().is(FluidTags.WATER); }
        return state.getCollisionShape(bsa.world,new BlockPos(x,y,z)).isEmpty();
    }
    public static boolean canStandOn(BlockStateAccessor bsa, int x, int y, int z) { return canStandOn(bsa,x,y,z,bsa.get(x,y,z)); }
    public static boolean canStandOn(BlockStateAccessor bsa, int x, int y, int z, BlockState state) {
        Block block=state.getBlock();
        if (state.isSolidRender() || block==Blocks.REDSTONE_BLOCK || block==Blocks.LADDER || block==Blocks.FARMLAND || block==Blocks.GRASS_BLOCK || block==Blocks.ENDER_CHEST || block==Blocks.CHEST || block==Blocks.TRAPPED_CHEST || block==Blocks.GLASS || block==Blocks.STAINED_GLASS.white() || block instanceof StairBlock || block==Blocks.SEA_LANTERN || block instanceof SlabBlock || block instanceof SnowLayerBlock) return true;
        if (isWotah(state)) { Block up=bsa.get(x,y+1,z).getBlock(); return up==Blocks.LILY_PAD || up instanceof CarpetBlock; }
        return false;
    }
    public static boolean possiblyFlowing(BlockState state) { return !state.getFluidState().isEmpty() && !state.getFluidState().isSource(); }
    public static boolean isFlowing(int x,int y,int z,BlockState state,BlockStateAccessor bsa) { return !state.getFluidState().isEmpty() && (!state.getFluidState().isSource() || possiblyFlowing(bsa.get(x+1,y,z)) || possiblyFlowing(bsa.get(x-1,y,z)) || possiblyFlowing(bsa.get(x,y,z+1)) || possiblyFlowing(bsa.get(x,y,z-1))); }
    public static boolean isWotah(BlockState state) { return state.getFluidState().is(FluidTags.WATER); }
    public static boolean isLava(BlockState state) { return state.getFluidState().is(FluidTags.LAVA); }
    public static boolean isBottomSlab(BlockState state) { return state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE)==SlabType.BOTTOM; }
    public static boolean isValidStair(BlockState state,int dx,int dz) { if (dx==dz || !(state.getBlock() instanceof StairBlock) || state.getValue(StairBlock.HALF)!=Half.BOTTOM) return false; Direction facing=state.getValue(StairBlock.FACING); return dz==-1 ? facing==Direction.NORTH : dz==1 ? facing==Direction.SOUTH : dx==-1 ? facing==Direction.WEST : facing==Direction.EAST; }
    public static boolean isValidReversedStair(BlockState state,int dx,int dz) { if (dx==dz || !(state.getBlock() instanceof StairBlock) || state.getValue(StairBlock.HALF)!=Half.BOTTOM) return false; Direction facing=state.getValue(StairBlock.FACING); return dz==1 ? facing==Direction.NORTH : dz==-1 ? facing==Direction.SOUTH : dx==1 ? facing==Direction.WEST : facing==Direction.EAST; }
    public static boolean hasTop(BlockState state,int dx,int dz) { return !(isBottomSlab(state)||isValidStair(state,dx,dz)); }
    public static boolean avoidWalkingInto(BlockState state) { Block block=state.getBlock(); return !state.getFluidState().isEmpty() || block==Blocks.FIRE || block==Blocks.CACTUS || block==Blocks.END_PORTAL || block==Blocks.COBWEB; }
    public static Direction getFacing(int dx,int dz) { return dx==0&&dz==0 ? Direction.UP : dx>0 ? Direction.EAST : dx<0 ? Direction.WEST : dz>0 ? Direction.SOUTH : Direction.NORTH; }
    public static boolean isLadder(BlockState state) { return state.getBlock()==Blocks.LADDER; }
    public static boolean canWalkIntoLadder(BlockState state,int dx,int dz) { return isLadder(state) && state.getValue(LadderBlock.FACING)!=getFacing(dx,dz); }
}

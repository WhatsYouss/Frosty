package xyz.whatsyouss.frosty.utility.commission.pathfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/** Line-of-sight smoothing and standability checks from the original Kotlin pathfinder. */
public final class BlockUtil {
    private BlockUtil() { }
    public static boolean canWalkOnBlock(BlockPos pos) { var level=Minecraft.getInstance().level; if(level==null)return false; BlockState state=level.getBlockState(pos); return state.isSolidRender()&&state.getFluidState().isEmpty()&&level.getBlockState(pos.above()).isAir(); }
    public static List<BlockPos> neighbourGenerator(BlockPos origin,int x1,int x2,int y1,int y2,int z1,int z2){List<BlockPos> result=new ArrayList<>();for(int x=x1;x<=x2;x++)for(int y=y1;y<=y2;y++)for(int z=z1;z<=z2;z++)result.add(origin.offset(x,y,z));return result;}
    public static boolean isStairSlab(BlockPos pos){var level=Minecraft.getInstance().level;return level!=null&&(level.getBlockState(pos).getBlock() instanceof StairBlock||level.getBlockState(pos).getBlock() instanceof SlabBlock);}
    public static Direction getDirectionToWalkOnStairs(BlockState state){return state.getValue(StairBlock.HALF)==Half.TOP?Direction.DOWN:state.getValue(StairBlock.FACING);}
    public static Direction getPlayerDirectionToBeAbleToWalkOnBlock(BlockPos start,BlockPos end){int dx=end.getX()-start.getX(),dz=end.getZ()-start.getZ();return Math.abs(dx)>Math.abs(dz)?dx>0?Direction.EAST:Direction.WEST:dz>0?Direction.SOUTH:Direction.NORTH;}
    public static boolean canWalkOn(CalculationContext c,BlockPos start,BlockPos end){BlockState from=c.get(start.getX(),start.getY(),start.getZ()),to=c.get(end.getX(),end.getY(),end.getZ());if(!to.isSolidRender())return end.getY()-start.getY()<=1;double a=MovementHelper.collisionMaxY(from,c.world,start),b=MovementHelper.collisionMaxY(to,c.world,end);return to.getBlock() instanceof StairBlock&&b-a>1?MovementHelper.isValidStair(to,end.getX()-start.getX(),end.getZ()-start.getZ()):b-a<=.5;}
    public static boolean bresenham(CalculationContext c,BlockPos start,BlockPos end){return bresenham(c,Vec3.atCenterOf(start),Vec3.atCenterOf(end));}
    public static boolean bresenham(CalculationContext c,Vec3 start,Vec3 end){
        Vec3 current=start;int x1=Mth.floor(end.x),y1=Mth.floor(end.y),z1=Mth.floor(end.z),x0=Mth.floor(current.x),y0=Mth.floor(current.y),z0=Mth.floor(current.z);BlockState last=c.bsa.get(x0,y0,z0);BlockPos lastPos=new BlockPos(x0,y0,z0);
        for(int remaining=200;remaining>=0;remaining--){if(x0==x1&&y0==y1&&z0==z1)return true;double dx=end.x-current.x,dy=end.y-current.y,dz=end.z-current.z;double sx=step(current.x,dx,x0,x1),sy=step(current.y,dy,y0,y1),sz=step(current.z,dz,z0,z1);Direction direction;if(sx<sy&&sx<sz){direction=x1>x0?Direction.WEST:Direction.EAST;current=new Vec3(x1>x0?x0+1:x0,current.y+dy*sx,current.z+dz*sx);}else if(sy<sz){direction=y1>y0?Direction.DOWN:Direction.UP;current=new Vec3(current.x+dx*sy,y1>y0?y0+1:y0,current.z+dz*sy);}else{direction=z1>z0?Direction.NORTH:Direction.SOUTH;current=new Vec3(current.x+dx*sz,current.y+dy*sz,z1>z0?z0+1:z0);}x0=Mth.floor(current.x)-(direction==Direction.EAST?1:0);y0=Mth.floor(current.y)-(direction==Direction.UP?1:0);z0=Mth.floor(current.z)-(direction==Direction.SOUTH?1:0);int offset=validStandingOffset(c,x0,y0,z0);if(offset==Integer.MIN_VALUE)return false;BlockState now=c.bsa.get(x0,y0+offset,z0);int delta=y0+offset-lastPos.getY();if(delta>1)return false;if(delta>0&&!canWalkOn(c,lastPos,new BlockPos(x0,y0+offset,z0)))return false;last=now;lastPos=new BlockPos(x0,y0+offset,z0);}
        return false;
    }
    private static double step(double current,double delta,int cell,int target){if(cell==target)return Double.POSITIVE_INFINITY;double boundary=target>cell?cell+1:cell;double value=(boundary-current)/delta;return value==0?-1e-4:value;}
    private static int validStandingOffset(CalculationContext c,int x,int y,int z){for(int offset=-3;offset<=3;offset++){if(offset==0||MovementHelper.canStandOn(c.bsa,x,y+offset,z,c.bsa.get(x,y+offset,z))&&MovementHelper.canWalkThrough(c.bsa,x,y+offset+1,z)&&MovementHelper.canWalkThrough(c.bsa,x,y+offset+2,z))return offset;}return Integer.MIN_VALUE;}
    public static boolean canWalkBetween(CalculationContext c,BlockPos start,BlockPos end){return validDestination(c,end)&&bresenham(c,start,end);}
    public static boolean canWalkBetween(CalculationContext c,Vec3 start,Vec3 end){BlockPos destination=BlockPos.containing(end);return validDestination(c,destination)&&bresenham(c,start,end);}
    private static boolean validDestination(CalculationContext c,BlockPos p){return MovementHelper.canStandOn(c.bsa,p.getX(),p.getY(),p.getZ(),c.get(p.getX(),p.getY(),p.getZ()))&&MovementHelper.canWalkThrough(c.bsa,p.getX(),p.getY()+1,p.getZ(),c.get(p.getX(),p.getY()+1,p.getZ()))&&MovementHelper.canWalkThrough(c.bsa,p.getX(),p.getY()+2,p.getZ(),c.get(p.getX(),p.getY()+2,p.getZ()));}
    public static boolean canStandOn(BlockPos p){var level=Minecraft.getInstance().level;if(level==null)return false;BlockStateAccessor bsa=new BlockStateAccessor(level);return MovementHelper.canStandOn(bsa,p.getX(),p.getY(),p.getZ(),bsa.get(p.getX(),p.getY(),p.getZ()))&&MovementHelper.canWalkThrough(bsa,p.getX(),p.getY()+1,p.getZ(),bsa.get(p.getX(),p.getY()+1,p.getZ()))&&MovementHelper.canWalkThrough(bsa,p.getX(),p.getY()+2,p.getZ(),bsa.get(p.getX(),p.getY()+2,p.getZ()));}
}

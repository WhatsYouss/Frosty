package xyz.whatsyouss.frosty.utility.commission.pathfinder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class Path {
    public final BlockPos start, end; public final Goal goal; public final CalculationContext ctx; public final List<BlockPos> path; public final List<PathNode> nodes;
    private List<BlockPos> smoothedPath=List.of();
    public Path(PathNode start,PathNode end,Goal goal,CalculationContext ctx){this.start=start.getBlock();this.end=end.getBlock();this.goal=goal;this.ctx=ctx;LinkedList<BlockPos> positions=new LinkedList<>();LinkedList<PathNode> chain=new LinkedList<>();for(PathNode node=end;node!=null;node=node.parentNode){positions.addFirst(node.getBlock());chain.addFirst(node);}path=List.copyOf(positions);nodes=List.copyOf(chain);}
    public List<BlockPos> getSmoothedPath(){if(!smoothedPath.isEmpty())return smoothedPath;List<BlockPos> result=new ArrayList<>();if(!path.isEmpty()){result.add(path.getFirst());for(int current=0;current+1<path.size();){int next=current+1;for(int index=path.size()-1;index>=next;index--)if(BlockUtil.bresenham(ctx,path.get(current),path.get(index))){next=index;break;}result.add(path.get(next));current=next;}}smoothedPath=Collections.unmodifiableList(result);return smoothedPath;}
    public List<BlockPos> reconstructPath(PathNode target){LinkedList<BlockPos> result=new LinkedList<>();for(PathNode node=target;node!=null;node=node.parentNode)result.addFirst(node.getBlock());result.removeIf(pos->ctx.world.getBlockState(pos).getBlock()==Blocks.AIR);return result;}
}


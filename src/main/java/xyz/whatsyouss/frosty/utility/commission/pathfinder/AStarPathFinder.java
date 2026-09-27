package xyz.whatsyouss.frosty.utility.commission.pathfinder;

import net.minecraft.core.BlockPos;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class AStarPathFinder {
    private static final int MAX_ITERATIONS = 50_000;
    private final int startX,startY,startZ; private final Goal goal; private final CalculationContext ctx;
    private final Map<Long,PathNode> closedSet=new HashMap<>(); private volatile boolean calculating; private volatile SearchTelemetry telemetry=new SearchTelemetry(0,0,0,"not_started");
    public AStarPathFinder(int x,int y,int z,Goal goal,CalculationContext ctx){startX=x;startY=y;startZ=z;this.goal=goal;this.ctx=ctx;}
    public SearchTelemetry getLastTelemetry(){return telemetry;}
    public Path calculatePath(){calculating=true;closedSet.clear();Heap open=new Heap();PathNode start=new PathNode(startX,startY,startZ,goal);start.costSoFar=0;start.totalCost=start.costToEnd;open.add(start);int expanded=0,iterations=0,peak=1;MovementResult result=new MovementResult();while(!open.isEmpty()&&calculating&&iterations<MAX_ITERATIONS){iterations++;PathNode current=open.poll();expanded++;if(goal.isAtGoal(current.x,current.y,current.z)){calculating=false;telemetry=new SearchTelemetry(iterations,expanded,peak,"goal_reached");return new Path(start,current,goal,ctx);}for(Moves move:Moves.values()){result.reset();move.calculate(ctx,current.x,current.y,current.z,result);double cost=result.cost;if(!isChunkLoaded(result.x,result.y,result.z))cost=ctx.cost.INF_COST/2;if(cost>=ctx.cost.INF_COST)continue;PathNode next=getNode(result.x,result.y,result.z);double nextCost=current.costSoFar+cost;if(next.costSoFar>nextCost){next.parentNode=current;next.costSoFar=nextCost;next.totalCost=nextCost+next.costToEnd;if(next.heapPosition==-1)open.add(next);else open.relocate(next);peak=Math.max(peak,open.size);}}}telemetry=new SearchTelemetry(iterations,expanded,peak,!calculating?"stop_requested":iterations>=MAX_ITERATIONS?"max_iterations":"open_set_exhausted");calculating=false;return null;}
    public PathNode getNode(int x,int y,int z){return closedSet.computeIfAbsent(PathNode.longHash(x,y,z),ignored->new PathNode(x,y,z,goal));}
    public void requestStop(){calculating=false;}
    private boolean isChunkLoaded(int x,int y,int z){BlockPos p=new BlockPos(x,y,z);return ctx.world.isLoaded(p)||ctx.world.isLoaded(p.offset(1,0,0))||ctx.world.isLoaded(p.offset(-1,0,0))||ctx.world.isLoaded(p.offset(0,0,1))||ctx.world.isLoaded(p.offset(0,0,-1));}
    public record SearchTelemetry(int iterations,int expandedNodes,int openSetPeak,String terminationReason) { }
    private static final class Heap { PathNode[] items=new PathNode[1024];int size;boolean isEmpty(){return size==0;}void add(PathNode n){if(size>=items.length-1)items=Arrays.copyOf(items,items.length<<1);n.heapPosition=++size;items[size]=n;relocate(n);}void relocate(PathNode n){int parent=n.heapPosition>>>1;while(n.heapPosition>1&&n.totalCost<items[parent].totalCost){items[n.heapPosition]=items[parent];items[parent]=n;n.heapPosition=parent;parent>>>=1;}}PathNode poll(){PathNode out=items[1];out.heapPosition=-1;PathNode swap=items[size--];if(size==0)return out;swap.heapPosition=1;items[1]=swap;int parent=1,child=2;while(child<=size){if(child+1<=size&&items[child+1].totalCost<items[child].totalCost)child++;if(items[child].totalCost>=swap.totalCost)break;PathNode next=items[child];next.heapPosition=parent;items[parent]=next;swap.heapPosition=child;items[child]=swap;parent=child;child=parent<<1;}return out;}}
}


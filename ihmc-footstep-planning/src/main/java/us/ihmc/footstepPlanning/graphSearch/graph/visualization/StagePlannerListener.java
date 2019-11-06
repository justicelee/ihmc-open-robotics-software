package us.ihmc.footstepPlanning.graphSearch.graph.visualization;

import org.apache.commons.lang3.mutable.MutableInt;
import us.ihmc.concurrent.ConcurrentCopier;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapper;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.graph.LatticeNode;
import us.ihmc.footstepPlanning.graphSearch.listeners.BipedalFootstepPlannerListener;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class StagePlannerListener implements BipedalFootstepPlannerListener
{
   private final FootstepNodeSnapper snapper;

   private final ConcurrentSet<PlannerCell> occupancyMapCellsSinceLastReport = new ConcurrentSet<>();
   private final ConcurrentSet<FootstepNode> expandedNodesSinceLastReport = new ConcurrentSet<>();
   private final ConcurrentList<PlannerNodeData> fullGraphSinceLastReport = new ConcurrentList<>();

   private final List<FootstepNode> lowestCostPlan = new ArrayList<>();

   private final List<PlannerNodeData> incomingNodeDataThisTick = new ArrayList<>();
   private final HashSet<PlannerCell> incomingOccupiedCellsThisTick = new HashSet<>();
   private final HashSet<FootstepNode> incomingExpandedNodes = new HashSet<>();
   private final HashMap<FootstepNode, PlannerNodeData> nodeDataThisTick = new HashMap<>();

   private final ConcurrentCopier<PlannerNodeDataList> concurrentLowestCostNodeDataList = new ConcurrentCopier<>(PlannerNodeDataList::new);

   private final AtomicBoolean hasOccupiedCells = new AtomicBoolean(false);
   private final AtomicBoolean hasExpandedNodes = new AtomicBoolean(false);
   private final AtomicBoolean hasFullGraph = new AtomicBoolean(false);
   private final AtomicBoolean hasLowestCostPlan = new AtomicBoolean(false);

   private final long occupancyMapUpdateDt;
   private long lastUpdateTime = -1;
   private final EnumMap<BipedalFootstepPlannerNodeRejectionReason, MutableInt> rejectionCount = new EnumMap<>(BipedalFootstepPlannerNodeRejectionReason.class);
   private int totalNodeCount = 0;

   public StagePlannerListener(FootstepNodeSnapper snapper, long occupancyMapUpdateDt)
   {
      this.snapper = snapper;
      this.occupancyMapUpdateDt = occupancyMapUpdateDt;

      for(BipedalFootstepPlannerNodeRejectionReason rejectionReason : BipedalFootstepPlannerNodeRejectionReason.values)
      {
         rejectionCount.put(rejectionReason, new MutableInt(0));
      }
   }

   @Override
   public void addNode(FootstepNode node, FootstepNode previousNode)
   {
      int previousNodeDataIndex;
      if (previousNode == null)
      {
         reset();
         previousNodeDataIndex = -1;
      }
      else
      {
         previousNodeDataIndex = previousNode.getNodeIndex();
         incomingExpandedNodes.add(previousNode);
      }

      RigidBodyTransform nodePose = snapper.snapFootstepNode(node).getOrComputeSnappedNodeTransform(node);

      node.setNodeIndex(totalNodeCount);
      PlannerNodeData nodeData = new PlannerNodeData(previousNodeDataIndex, node, nodePose, null);

//      nodeDataThisTick.put(node, nodeData);
      incomingNodeDataThisTick.add(nodeData);
      incomingOccupiedCellsThisTick.add(new PlannerCell(node.getXIndex(), node.getYIndex()));
      totalNodeCount++;
   }

   public void reset()
   {
      nodeDataThisTick.clear();
      incomingNodeDataThisTick.clear();
      incomingOccupiedCellsThisTick.clear();
      incomingExpandedNodes.clear();

      occupancyMapCellsSinceLastReport.clear();
      expandedNodesSinceLastReport.clear();

      lowestCostPlan.clear();
      totalNodeCount = 0;
      rejectionCount.values().forEach(count -> count.setValue(0));
   }

   @Override
   public void reportLowestCostNodeList(List<FootstepNode> plan)
   {
      lowestCostPlan.clear();
      lowestCostPlan.addAll(plan);
   }

   @Override
   public void rejectNode(FootstepNode rejectedNode, FootstepNode parentNode, BipedalFootstepPlannerNodeRejectionReason reason)
   {
//      nodeDataThisTick.get(rejectedNode).setRejectionReason(reason);
      rejectionCount.get(reason).increment();
   }

   @Override
   public void tickAndUpdate()
   {
      long currentTime = System.currentTimeMillis();

      if (lastUpdateTime == -1)
         lastUpdateTime = currentTime;

      boolean isTimeForUpdate = currentTime - lastUpdateTime > occupancyMapUpdateDt;
      if (!isTimeForUpdate)
         return;

      updateOccupiedCells();
      updateExpandedNodes();
//      updateLowestCostPlan();
//      updateFullGraph();

      lastUpdateTime = currentTime;
   }

   @Override
   public void plannerFinished(List<FootstepNode> plan)
   {
      updateOccupiedCells();
   }

   private void updateOccupiedCells()
   {
      occupancyMapCellsSinceLastReport.addAll(incomingOccupiedCellsThisTick);
      hasOccupiedCells.set(hasOccupiedCells.get() || !incomingOccupiedCellsThisTick.isEmpty());

      incomingOccupiedCellsThisTick.clear();
   }

   private void updateExpandedNodes()
   {
      expandedNodesSinceLastReport.addAll(incomingExpandedNodes);
      hasExpandedNodes.set(hasExpandedNodes.get() || !incomingExpandedNodes.isEmpty());

      incomingExpandedNodes.clear();
   }

   private void updateLowestCostPlan()
   {
      PlannerNodeDataList concurrentNodeDataList = this.concurrentLowestCostNodeDataList.getCopyForWriting();
      concurrentNodeDataList.clear();
      for (int i = 0; i < lowestCostPlan.size(); i++)
      {
         FootstepNode node = lowestCostPlan.get(i);
         RigidBodyTransform nodePose = snapper.snapFootstepNode(node).getOrComputeSnappedNodeTransform(node);
         concurrentNodeDataList.addNode(-1, i, node.getLatticeNode(), node.getRobotSide(), nodePose, null);
      }
      this.concurrentLowestCostNodeDataList.commit();
      lowestCostPlan.clear();

      hasLowestCostPlan.set(!concurrentNodeDataList.getNodeData().isEmpty());
   }

   private void updateFullGraph()
   {
      fullGraphSinceLastReport.addAll(incomingNodeDataThisTick);
      hasFullGraph.set(hasFullGraph.get() || !incomingNodeDataThisTick.isEmpty());

      nodeDataThisTick.clear();
      incomingNodeDataThisTick.clear();
   }

   public boolean hasOccupiedCells()
   {
      return hasOccupiedCells.get();
   }

   public boolean hasExpandedNodes()
   {
      return hasExpandedNodes.get();
   }

   public boolean hasLowestCostPlan()
   {
      return hasLowestCostPlan.get();
   }

   public boolean hasFullGraph()
   {
      return hasFullGraph.get();
   }

   PlannerOccupancyMap getOccupancyMap()
   {
      PlannerOccupancyMap occupancyMap = new PlannerOccupancyMap();
      for (PlannerCell plannerCell : occupancyMapCellsSinceLastReport.getCopyForReading())
         occupancyMap.addOccupiedCell(plannerCell);

      hasOccupiedCells.set(false);
      occupancyMapCellsSinceLastReport.clear();

      return occupancyMap;
   }

   PlannerLatticeMap getExpandedNodes()
   {
      if (!hasExpandedNodes.get())
         updateExpandedNodes();

      PlannerLatticeMap latticeMap = new PlannerLatticeMap();
      for (FootstepNode latticeNode : expandedNodesSinceLastReport.getCopyForReading())
         latticeMap.addFootstepNode(latticeNode);

      hasExpandedNodes.set(false);
      expandedNodesSinceLastReport.clear();

      return latticeMap;
   }

   PlannerNodeDataList getLowestCostPlan()
   {
      hasLowestCostPlan.set(false);

      return concurrentLowestCostNodeDataList.getCopyForReading();
   }

   PlannerNodeDataList getFullGraph()
   {
      if (!hasFullGraph.get())
         updateFullGraph();

      PlannerNodeDataList plannerNodeDataList = new PlannerNodeDataList();
      plannerNodeDataList.setIsFootstepGraph(true);

      for (PlannerNodeData nodeData : fullGraphSinceLastReport.getCopyForReading())
         plannerNodeDataList.addNode(nodeData);

      hasFullGraph.set(false);
      fullGraphSinceLastReport.clear();

      return plannerNodeDataList;
   }

   public int getTotalNodeCount()
   {
      return totalNodeCount;
   }

   public int getRejectionReasonCount(BipedalFootstepPlannerNodeRejectionReason rejectionReason)
   {
      return rejectionCount.get(rejectionReason).getValue();
   }

   private class ConcurrentList<T> extends ConcurrentCopier<List<T>>
   {
      public ConcurrentList()
      {
         super(ArrayList::new);
      }

      public void clear()
      {
         getCopyForWriting().clear();
         commit();
      }

      public void addAll(Collection<? extends T> collection)
      {
         List<T> currentSet = getCopyForReading();
         List<T> updatedSet = getCopyForWriting();
         updatedSet.clear();
         if (currentSet != null)
            updatedSet.addAll(currentSet);
         updatedSet.addAll(collection);
         commit();
      }

      public T[] toArray(T[] ts)
      {
         List<T> currentSet = getCopyForReading();
         return currentSet.toArray(ts);
      }

      public boolean isEmpty()
      {
         List<T> currentList = getCopyForReading();
         if (currentList == null)
            return true;

         return currentList.isEmpty();
      }

      public int size()
      {
         List<T> currentList = getCopyForReading();
         if (currentList == null)
            return 0;

         return currentList.size();
      }
   }

   private class ConcurrentSet<T> extends ConcurrentCopier<Set<T>>
   {
      public ConcurrentSet()
      {
         super(HashSet::new);
      }

      public void clear()
      {
         getCopyForWriting().clear();
         commit();
      }

      public void addAll(Collection<? extends T> collection)
      {
         Set<T> currentSet = getCopyForReading();
         Set<T> updatedSet = getCopyForWriting();
         updatedSet.clear();
         if (currentSet != null)
            updatedSet.addAll(currentSet);
         updatedSet.addAll(collection);
         commit();
      }

      public T[] toArray(T[] ts)
      {
         Set<T> currentSet = getCopyForReading();
         return currentSet.toArray(ts);
      }

      public boolean isEmpty()
      {
         Set<T> currentList = getCopyForReading();
         if (currentList == null)
            return true;

         return currentList.isEmpty();
      }

      public int size()
      {
         Set<T> currentList = getCopyForReading();
         if (currentList == null)
            return 0;

         return currentList.size();
      }

   }
}

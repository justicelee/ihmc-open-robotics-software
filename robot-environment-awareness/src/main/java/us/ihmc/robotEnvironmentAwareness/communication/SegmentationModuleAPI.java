package us.ihmc.robotEnvironmentAwareness.communication;

import controller_msgs.msg.dds.PlanarRegionsListMessage;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.jOctoMap.ocTree.NormalOcTree;
import us.ihmc.messager.MessagerAPIFactory;
import us.ihmc.messager.MessagerAPIFactory.Category;
import us.ihmc.messager.MessagerAPIFactory.CategoryTheme;
import us.ihmc.messager.MessagerAPIFactory.MessagerAPI;
import us.ihmc.messager.MessagerAPIFactory.Topic;
import us.ihmc.robotEnvironmentAwareness.communication.packets.BoxMessage;
import us.ihmc.robotEnvironmentAwareness.communication.packets.LineSegment3DMessage;
import us.ihmc.robotEnvironmentAwareness.communication.packets.NormalOcTreeMessage;
import us.ihmc.robotEnvironmentAwareness.communication.packets.PlanarRegionSegmentationMessage;
import us.ihmc.robotEnvironmentAwareness.geometry.ConcaveHullFactoryParameters;
import us.ihmc.robotEnvironmentAwareness.planarRegion.*;
import us.ihmc.robotEnvironmentAwareness.ui.graphicsBuilders.OcTreeMeshBuilder;
import us.ihmc.robotEnvironmentAwareness.ui.graphicsBuilders.OcTreeMeshBuilder.ColoringType;

public class SegmentationModuleAPI
{
   private static final MessagerAPIFactory apiFactory = new MessagerAPIFactory();
   private static final Category Root = apiFactory.createRootCategory("PlanarSegmentationRoot");
   private static final CategoryTheme PlanarSegmentation = apiFactory.createCategoryTheme("PlanarSegmentation");

   public static final MessagerAPI API = apiFactory.getAPIAndCloseFactory();

   public static final Topic<Integer> UIOcTreeDepth = topic("UIOctTreeDepth");
   public static final Topic<ColoringType> UIOcTreeColoringMode = topic("UIOctTreeDepth");
   public static final Topic<OcTreeMeshBuilder.DisplayType> UIOcTreeDisplayType = topic("UIOcTreeDisplayType");
   public static final Topic<Boolean> UIPlanarRegionHideNodes = topic("UIPlanarRegionHideNodes");

   public static final Topic<Boolean> OcTreeEnable = topic("OcTreeEnable");
   public static final Topic<Boolean> OcTreeClear = topic("OcTreeClear");
   public static final Topic<NormalOcTreeMessage> OcTreeState = topic("OcTreeState");
   public static final Topic<NormalOcTree> OcTree = topic("OcTree");
   public static final Topic<Pose3DReadOnly> SensorPose = topic("SensorPose");
   public static final Topic<PlanarRegionSegmentationMessage[]> PlanarRegionsSegmentationState = topic("PlanarRegionsSegmentationState");

   public static final Topic<SurfaceNormalFilterParameters> SurfaceNormalFilterParameters = topic("SurfaceNormalFilterParameters");

   public static final Topic<Boolean> PlanarRegionsSegmentationEnable = topic("PlanarRegionsSegmentationEnable");
   public static final Topic<Boolean> PlanarRegionsSegmentationClear = topic("PlanarRegionsSegmentationClear");
   public static final Topic<PlanarRegionSegmentationParameters> PlanarRegionsSegmentationParameters = topic("PlanarRegionsSegmentationParameters");

   public static final Topic<LineSegment3DMessage[]> PlanarRegionsIntersectionState = topic("PlanarRegionsIntersectionState");

   public static final Topic<Boolean> CustomRegionsMergingEnable = topic("CustomRegionsMergingEnable");
   public static final Topic<Boolean> CustomRegionsClear = topic("CustomRegionsClear");
   public static final Topic<CustomRegionMergeParameters> CustomRegionsMergingParameters = topic("CustomRegionsMergingParameters");

   public static final Topic<Boolean> PlanarRegionsPolygonizerEnable = topic("PlanarRegionsPolygonizerEnable");
   public static final Topic<Boolean> PlanarRegionsPolygonizerClear = topic("PlanarRegionsPolygonizerClear");
   public static final Topic<Boolean> PlanarRegionsIntersectionEnable = topic("PlanarRegionsIntersectionEnable");
   public static final Topic<ConcaveHullFactoryParameters> PlanarRegionsConcaveHullParameters = topic("PlanarRegionsConcaveHullParameters");
   public static final Topic<PolygonizerParameters> PlanarRegionsPolygonizerParameters = topic("PlanarRegionsPolygonizerParameters");
   public static final Topic<IntersectionEstimationParameters> PlanarRegionsIntersectionParameters = topic("PlanarRegionsIntersectionParameters");

   public static final Topic<PlanarRegionsListMessage> PlanarRegionsState = topic("PlanarRegionsState");

   public static final Topic<Boolean> RequestEntireModuleState = topic("RequestEntireModuleState");
   public static final Topic<Boolean> RequestOcTree = topic("RequestOcTree");
   public static final Topic<Boolean> RequestPlanarRegions = topic("RequestPlanarRegions");
   public static final Topic<Boolean> RequestPlanarRegionSegmentation = topic("RequestPlanarRegionSegmentation");
   public static final Topic<Boolean> RequestPlanarRegionsIntersections = topic("RequestPlanarRegionsIntersections");

   public static final Topic<Boolean> SaveUpdaterConfiguration = topic("SaveUpdaterConfiguration");

   private static <T> Topic<T> topic(String name)
   {
      return Root.child(PlanarSegmentation).topic(apiFactory.createTypedTopicTheme(name));
   }
}

package us.ihmc.footstepPlanning;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.trajectories.SwingOverPlanarRegionsTrajectoryExpander;
import us.ihmc.commonWalkingControlModules.trajectories.TwoWaypointSwingGenerator;
import us.ihmc.commons.MathTools;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.geometry.interfaces.Pose3DBasics;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersReadOnly;
import us.ihmc.footstepPlanning.icp.AreaBasedSplitFractionCalculator;
import us.ihmc.footstepPlanning.icp.PositionBasedSplitFractionCalculator;
import us.ihmc.footstepPlanning.icp.SplitFractionCalculatorParametersBasics;
import us.ihmc.footstepPlanning.swing.AdaptiveSwingTrajectoryCalculator;
import us.ihmc.footstepPlanning.swing.SwingPlannerParametersBasics;
import us.ihmc.footstepPlanning.swing.SwingPlannerType;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.trajectories.TrajectoryType;
import us.ihmc.yoVariables.registry.YoRegistry;

import java.util.function.Consumer;

public class FootstepPlanPostProcessHandler
{
   private final YoRegistry registry = new YoRegistry(getClass().getSimpleName());

   private final FootstepPlannerParametersReadOnly footstepPlannerParameters;
   private final SwingPlannerParametersBasics swingPlannerParameters;
   private final SplitFractionCalculatorParametersBasics splitFractionParameters;
   private final WalkingControllerParameters walkingControllerParameters;

   private final AdaptiveSwingTrajectoryCalculator adaptiveSwingTrajectoryCalculator;
   private final SwingOverPlanarRegionsTrajectoryExpander swingOverPlanarRegionsTrajectoryExpander;

   private final AreaBasedSplitFractionCalculator areaBasedSplitFractionCalculator;
   private final PositionBasedSplitFractionCalculator positionBasedSplitFractionCalculator;

   private Consumer<FootstepPlannerOutput> statusCallback = result ->
   {
   };
   private double nominalSwingTrajectoryLength;

   public FootstepPlanPostProcessHandler(FootstepPlannerParametersReadOnly footstepPlannerParameters,
                                         SwingPlannerParametersBasics swingPlannerParameters,
                                         SplitFractionCalculatorParametersBasics splitFractionParameters,
                                         WalkingControllerParameters walkingControllerParameters,
                                         SideDependentList<ConvexPolygon2D> footPolygons)

   {
      this.footstepPlannerParameters = footstepPlannerParameters;
      this.swingPlannerParameters = swingPlannerParameters;
      this.splitFractionParameters = splitFractionParameters;
      this.walkingControllerParameters = walkingControllerParameters;

      if (walkingControllerParameters == null)
      {
         this.adaptiveSwingTrajectoryCalculator = null;
         this.swingOverPlanarRegionsTrajectoryExpander = null;
      }
      else
      {
         this.adaptiveSwingTrajectoryCalculator = new AdaptiveSwingTrajectoryCalculator(swingPlannerParameters, footstepPlannerParameters, walkingControllerParameters);
         this.swingOverPlanarRegionsTrajectoryExpander = new SwingOverPlanarRegionsTrajectoryExpander(walkingControllerParameters,
                                                                                                      registry,
                                                                                                      new YoGraphicsListRegistry());
      }

      this.areaBasedSplitFractionCalculator = new AreaBasedSplitFractionCalculator(splitFractionParameters, footPolygons);
      this.positionBasedSplitFractionCalculator = new PositionBasedSplitFractionCalculator(splitFractionParameters);
   }

   public YoRegistry getYoVariableRegistry()
   {
      return registry;
   }

   public void handleRequest(FootstepPlannerRequest request, FootstepPlannerOutput output)
   {
      if (!request.getAssumeFlatGround())
      {
         performPostProcessing(request.getPlanarRegionsList(),
                               output.getFootstepPlan(),
                               request.getStartFootPoses(),
                               request.getStartFootholds(),
                               request.getSwingPlannerType(),
                               request.performPositionBasedSplitFractionCalculation(),
                               request.performAreaBasedSplitFractionCalculation());
      }

      statusCallback.accept(output);
   }

   public void performPostProcessing(PlanarRegionsList planarRegionsList,
                                     FootstepPlan footstepPlan,
                                     SideDependentList<? extends Pose3DReadOnly> startFootPoses,
                                     SideDependentList<ConvexPolygon2D> startFootholds,
                                     SwingPlannerType swingPlannerType,
                                     boolean performPositionBasedSplitFractionCalculation,
                                     boolean performAreaBasedSplitFractionCalculation)
   {
      if (planarRegionsList == null || planarRegionsList.isEmpty())
      {
         return;
      }

      if (swingPlannerType == SwingPlannerType.PROPORTION && adaptiveSwingTrajectoryCalculator != null)
      {
         adaptiveSwingTrajectoryCalculator.setPlanarRegionsList(planarRegionsList);
         adaptiveSwingTrajectoryCalculator.setSwingParameters(startFootPoses, footstepPlan);
      }
      else if (swingPlannerType == SwingPlannerType.POSITION && swingOverPlanarRegionsTrajectoryExpander != null)
      {
         computeSwingWaypoints(planarRegionsList, footstepPlan, startFootPoses);
      }

      if (performPositionBasedSplitFractionCalculation)
      {
         positionBasedSplitFractionCalculator.computeSplitFractions(footstepPlan, startFootPoses);
      }
      if (performAreaBasedSplitFractionCalculation)
      {
         areaBasedSplitFractionCalculator.computeSplitFractions(footstepPlan, startFootPoses, startFootholds);
      }
   }

   public void computeSwingWaypoints(FootstepPlannerRequest request, FootstepPlan footstepPlan)
   {
      computeSwingWaypoints(request.getPlanarRegionsList(), footstepPlan, request.getStartFootPoses());
   }

   // TODO make this a method of the swing trajectory solver after moving it to this package
   public void computeSwingWaypoints(PlanarRegionsList planarRegionsList,
                                     FootstepPlan footstepPlan,
                                     SideDependentList<? extends Pose3DReadOnly> startFootPoses)
   {
      swingOverPlanarRegionsTrajectoryExpander.setDoInitialFastApproximation(swingPlannerParameters.getDoInitialFastApproximation());
      swingOverPlanarRegionsTrajectoryExpander.setFastApproximationLessClearance(swingPlannerParameters.getFastApproximationLessClearance());
      swingOverPlanarRegionsTrajectoryExpander.setNumberOfCheckpoints(swingPlannerParameters.getNumberOfChecksPerSwing());
      swingOverPlanarRegionsTrajectoryExpander.setMaximumNumberOfTries(swingPlannerParameters.getMaximumNumberOfAdjustmentAttempts());
      swingOverPlanarRegionsTrajectoryExpander.setMinimumSwingFootClearance(swingPlannerParameters.getMinimumSwingFootClearance());
      swingOverPlanarRegionsTrajectoryExpander.setMinimumAdjustmentIncrementDistance(swingPlannerParameters.getMinimumAdjustmentIncrementDistance());
      swingOverPlanarRegionsTrajectoryExpander.setMaximumAdjustmentIncrementDistance(swingPlannerParameters.getMaximumAdjustmentIncrementDistance());
      swingOverPlanarRegionsTrajectoryExpander.setAdjustmentIncrementDistanceGain(swingPlannerParameters.getAdjustmentIncrementDistanceGain());
      swingOverPlanarRegionsTrajectoryExpander.setMaximumAdjustmentDistance(swingPlannerParameters.getMaximumWaypointAdjustmentDistance());
      swingOverPlanarRegionsTrajectoryExpander.setMinimumHeightAboveFloorForCollision(swingPlannerParameters.getMinimumHeightAboveFloorForCollision());

      computeNominalSwingTrajectoryLength();

      for (int i = 0; i < footstepPlan.getNumberOfSteps(); i++)
      {
         PlannedFootstep footstep = footstepPlan.getFootstep(i);
         FramePose3D swingEndPose = new FramePose3D(footstep.getFootstepPose());
         FramePose3D swingStartPose = new FramePose3D();
         FramePose3D stanceFootPose = new FramePose3D();

         RobotSide swingSide = footstep.getRobotSide();
         RobotSide stanceSide = swingSide.getOppositeSide();

         if (i == 0)
         {
            swingStartPose.set(startFootPoses.get(swingSide));
            stanceFootPose.set(startFootPoses.get(stanceSide));
         }
         else if (i == 1)
         {
            swingStartPose.set(startFootPoses.get(swingSide));
            stanceFootPose.set(footstepPlan.getFootstep(i - 1).getFootstepPose());
         }
         else
         {
            swingStartPose.set(footstepPlan.getFootstep(i - 2).getFootstepPose());
            stanceFootPose.set(footstepPlan.getFootstep(i - 1).getFootstepPose());
         }

         swingOverPlanarRegionsTrajectoryExpander.expandTrajectoryOverPlanarRegions(stanceFootPose,
                                                                                    swingStartPose,
                                                                                    swingEndPose,
                                                                                    planarRegionsList);
         if (swingOverPlanarRegionsTrajectoryExpander.wereWaypointsAdjusted())
         {
            footstep.setTrajectoryType(TrajectoryType.CUSTOM);
            Point3D waypointOne = new Point3D(swingOverPlanarRegionsTrajectoryExpander.getExpandedWaypoints().get(0));
            Point3D waypointTwo = new Point3D(swingOverPlanarRegionsTrajectoryExpander.getExpandedWaypoints().get(1));
            footstep.setCustomWaypointPositions(waypointOne, waypointTwo);

            double swingScale = Math.max(1.0, swingOverPlanarRegionsTrajectoryExpander.getExpandedTrajectoryLength() / nominalSwingTrajectoryLength);
            double swingTime = swingPlannerParameters.getAdditionalSwingTimeIfExpanded() + swingScale * swingPlannerParameters.getMinimumSwingTime();
            footstep.setSwingDuration(swingTime);
         }
         else
         {
            double swingScale = Math.max(1.0, swingOverPlanarRegionsTrajectoryExpander.getInitialTrajectoryLength() / nominalSwingTrajectoryLength);
            double swingTime = swingScale * swingPlannerParameters.getMinimumSwingTime();
            footstep.setSwingDuration(swingTime);
         }
      }
   }

   private void computeNominalSwingTrajectoryLength()
   {
      double nominalSwingHeight = walkingControllerParameters.getSteppingParameters().getDefaultSwingHeightFromStanceFoot();
      double nominalSwingProportion = TwoWaypointSwingGenerator.getDefaultWaypointProportions()[0];
      double nominalSwingLength = 2.0 * footstepPlannerParameters.getIdealFootstepLength();

      // trapezoidal approximation of swing trajectory
      nominalSwingTrajectoryLength = 2.0 * EuclidGeometryTools.pythagorasGetHypotenuse(nominalSwingProportion * nominalSwingLength, nominalSwingHeight)
                                     + (1.0 - 2.0 * nominalSwingProportion) * nominalSwingLength;
   }

   public void setStatusCallback(Consumer<FootstepPlannerOutput> statusCallback)
   {
      this.statusCallback = statusCallback;
   }

   public SwingPlannerParametersBasics getSwingPlannerParameters()
   {
      return swingPlannerParameters;
   }

   public SplitFractionCalculatorParametersBasics getSplitFractionParameters()
   {
      return splitFractionParameters;
   }

   public AdaptiveSwingTrajectoryCalculator getAdaptiveSwingTrajectoryCalculator()
   {
      return adaptiveSwingTrajectoryCalculator;
   }

   public SwingOverPlanarRegionsTrajectoryExpander getSwingOverPlanarRegionsTrajectoryExpander()
   {
      return swingOverPlanarRegionsTrajectoryExpander;
   }

   public AreaBasedSplitFractionCalculator getAreaBasedSplitFractionCalculator()
   {
      return areaBasedSplitFractionCalculator;
   }

   public PositionBasedSplitFractionCalculator getPositionBasedSplitFractionCalculator()
   {
      return positionBasedSplitFractionCalculator;
   }
}

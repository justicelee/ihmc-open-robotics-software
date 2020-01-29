package us.ihmc.humanoidBehaviors.lookAndStep;

import controller_msgs.msg.dds.WalkingStatusMessage;
import org.apache.commons.lang3.tuple.Pair;
import us.ihmc.commons.thread.Notification;
import us.ihmc.communication.RemoteREAInterface;
import us.ihmc.communication.packets.ExecutionMode;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.footstepPlanning.FootstepDataMessageConverter;
import us.ihmc.footstepPlanning.FootstepPlan;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapData;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapper;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.SimplePlanarRegionFootstepNodeSnapper;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.nodeChecking.FootstepNodeChecker;
import us.ihmc.footstepPlanning.graphSearch.nodeChecking.FootstepNodeCheckerOfCheckers;
import us.ihmc.footstepPlanning.graphSearch.nodeChecking.SnapBasedNodeChecker;
import us.ihmc.footstepPlanning.graphSearch.parameters.DefaultFootstepPlannerParameters;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersBasics;
import us.ihmc.humanoidBehaviors.BehaviorDefinition;
import us.ihmc.humanoidBehaviors.BehaviorInterface;
import us.ihmc.humanoidBehaviors.tools.BehaviorHelper;
import us.ihmc.humanoidBehaviors.tools.HumanoidRobotState;
import us.ihmc.humanoidBehaviors.tools.RemoteHumanoidRobotInterface;
import us.ihmc.humanoidRobotics.footstep.SimpleFootstep;
import us.ihmc.log.LogTools;
import us.ihmc.messager.MessagerAPIFactory;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.tools.thread.PausablePeriodicThread;
import us.ihmc.tools.thread.TypedNotification;

import java.util.ArrayList;
import java.util.Arrays;

import static us.ihmc.humanoidBehaviors.lookAndStep.LookAndStepBehavior.LookAndStepBehaviorAPI.*;
import static us.ihmc.humanoidBehaviors.navigation.NavigationBehavior.NavigationBehaviorAPI.FootstepPlanForUI;

public class LookAndStepBehavior implements BehaviorInterface
{
   public static final BehaviorDefinition DEFINITION = new BehaviorDefinition("Look and Step", LookAndStepBehavior::new, create());

   private final BehaviorHelper helper;
   private final SideDependentList<ConvexPolygon2D> footPolygons;
   private final Notification takeStep;
   private final PausablePeriodicThread mainThread;
   private final RemoteREAInterface rea;

   private final FootstepPlannerParametersBasics footstepPlannerParameters;
   private final RemoteHumanoidRobotInterface robot;

   public LookAndStepBehavior(BehaviorHelper helper)
   {
      this.helper = helper;
      footPolygons = helper.createFootPolygons();
      rea = helper.getOrCreateREAInterface();
      robot = helper.getOrCreateRobotInterface();
      takeStep = helper.createUINotification(TakeStep);
      mainThread = helper.createPausablePeriodicThread(getClass(), 0.1, this::lookAndStep);
      footstepPlannerParameters = helper.getRobotModel().getFootstepPlannerParameters();
   }

   @Override
   public void setEnabled(boolean enabled)
   {
      LogTools.info("Look and step behavior selected = {}", enabled);

      mainThread.setRunning(enabled);
      helper.setCommunicationCallbacksEnabled(enabled);
   }

   private void lookAndStep()
   {
      HumanoidRobotState latestHumanoidRobotState = robot.pollHumanoidRobotState();

      FramePose3D midFeetZUp = new FramePose3D();
      midFeetZUp.setToZero(latestHumanoidRobotState.getMidFeetZUpFrame());
      midFeetZUp.changeFrame(ReferenceFrame.getWorldFrame());

      FramePose3D targetFootstepPose = new FramePose3D();
      targetFootstepPose.setToZero(latestHumanoidRobotState.getMidFeetZUpFrame());
      targetFootstepPose.changeFrame(latestHumanoidRobotState.getFootFrame(RobotSide.LEFT));
      targetFootstepPose.prependTranslation(0.2, 0.0, 0.0);
      targetFootstepPose.changeFrame(ReferenceFrame.getWorldFrame());

      RobotSide initialStanceFootSide = null;
      FramePose3D initialStanceFootPose = null;
      FramePose3D leftSolePose = new FramePose3D();
      leftSolePose.setToZero(latestHumanoidRobotState.getSoleZUpFrame(RobotSide.LEFT));
      leftSolePose.changeFrame(ReferenceFrame.getWorldFrame());
      FramePose3D rightSolePose = new FramePose3D();
      rightSolePose.setToZero(latestHumanoidRobotState.getSoleZUpFrame(RobotSide.RIGHT));
      rightSolePose.changeFrame(ReferenceFrame.getWorldFrame());

      double idealFootstepWidth = footstepPlannerParameters.getIdealFootstepWidth();

      if (leftSolePose.getPosition().distance(targetFootstepPose.getPosition()) <= rightSolePose.getPosition().distance(targetFootstepPose.getPosition()))
      {
         initialStanceFootSide = RobotSide.LEFT;
         initialStanceFootPose = leftSolePose;

         targetFootstepPose.changeFrame(latestHumanoidRobotState.getFootFrame(RobotSide.LEFT));
         targetFootstepPose.prependTranslation(0.0, -idealFootstepWidth, 0.0);
         targetFootstepPose.changeFrame(ReferenceFrame.getWorldFrame());
      }
      else
      {
         initialStanceFootSide = RobotSide.RIGHT;
         initialStanceFootPose = rightSolePose;

         targetFootstepPose.changeFrame(latestHumanoidRobotState.getFootFrame(RobotSide.LEFT));
         targetFootstepPose.prependTranslation(0.0, idealFootstepWidth, 0.0);
         targetFootstepPose.changeFrame(ReferenceFrame.getWorldFrame());
      }

      FootstepNode targetFootstepNode = new FootstepNode(targetFootstepPose.getX(),
                                                   targetFootstepPose.getY(),
                                                   targetFootstepPose.getYaw(),
                                                   initialStanceFootSide.getOppositeSide());

      PlanarRegionsList latestPlanarRegionList = rea.getLatestPlanarRegionList();
      FootstepNodeSnapper snapper = new SimplePlanarRegionFootstepNodeSnapper(footPolygons);
      snapper.setPlanarRegions(latestPlanarRegionList);
      FootstepNodeSnapData footstepNodeSnapData = snapper.snapFootstepNode(targetFootstepNode);

      FramePose3D framePose3D = new FramePose3D();
      framePose3D.set(footstepNodeSnapData.getSnapTransform());

      SimpleFootstep simpleFootstep = new SimpleFootstep();
//      simpleFootstep.setFoothold(footstepNodeSnapData.getCroppedFoothold());
//      simpleFootstep.getSoleFramePose(framePose3D);
      simpleFootstep.setSoleFramePose(targetFootstepPose);
      simpleFootstep.setRobotSide(initialStanceFootSide.getOppositeSide());

      FootstepPlan footstepPlan = new FootstepPlan();
      footstepPlan.addFootstep(simpleFootstep);


//      FootstepNodeChecker snapBasedNodeChecker = new SnapBasedNodeChecker(footstepPlannerParameters, footPolygons, snapper);
//
//
//      // nodeChecker = new FootstepNodeCheckerOfCheckers(Arrays.asList(snapBasedNodeChecker, bodyCollisionNodeChecker, cliffAvoider));
//      FootstepNodeCheckerOfCheckers nodeChecker = new FootstepNodeCheckerOfCheckers(Arrays.asList(snapBasedNodeChecker));


      // send footstep plan to UI
      helper.publishToUI(FootstepPlanForUI, FootstepDataMessageConverter.reduceFootstepPlanForUIMessager(footstepPlan));


      if (takeStep.poll())
      {
         // take step

         LogTools.info("Requesting walk");
         TypedNotification<WalkingStatusMessage> walkingStatusNotification = robot.requestWalk(FootstepDataMessageConverter.createFootstepDataListFromPlan(
               footstepPlan,
               1.0,
               0.5,
               ExecutionMode.OVERRIDE));

         walkingStatusNotification.blockingPoll();
      }
   }

   public static class LookAndStepBehaviorAPI
   {
      private static final MessagerAPIFactory apiFactory = new MessagerAPIFactory();
      private static final MessagerAPIFactory.Category RootCategory = apiFactory.createRootCategory("LookAndStepBehavior");
      private static final MessagerAPIFactory.CategoryTheme LookAndStepTheme = apiFactory.createCategoryTheme("LookAndStep");

      public static final MessagerAPIFactory.Topic<Object> TakeStep = topic("TakeStep");
      public static final MessagerAPIFactory.Topic<ArrayList<Pair<RobotSide, Pose3D>>> FootstepPlanForUI = topic("FootstepPlan");

      private static final <T> MessagerAPIFactory.Topic<T> topic(String name)
      {
         return RootCategory.child(LookAndStepTheme).topic(apiFactory.createTypedTopicTheme(name));
      }

      public static final MessagerAPIFactory.MessagerAPI create()
      {
         return apiFactory.getAPIAndCloseFactory();
      }
   }
}

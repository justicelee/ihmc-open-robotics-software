package us.ihmc.quadrupedRobotics.controlModules.foot;

import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.PointFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.virtualModelControl.VirtualForceCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.virtualModelControl.VirtualModelControlCommand;
import us.ihmc.commons.MathTools;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.graphicsDescription.appearance.AppearanceDefinition;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.BagOfBalls;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControllerToolbox;
import us.ihmc.quadrupedRobotics.planning.YoQuadrupedTimedStep;
import us.ihmc.quadrupedRobotics.planning.trajectory.ThreeDoFSwingFootTrajectory;
import us.ihmc.quadrupedRobotics.util.TimeInterval;
import us.ihmc.robotics.math.filters.GlitchFilteredYoBoolean;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

public class QuadrupedSwingState extends QuadrupedFootState
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final boolean createSwingTrajectoryGraphics = false;

   private final ThreeDoFSwingFootTrajectory swingTrajectory;
   private final FramePoint3D goalPosition = new FramePoint3D();
   private final FramePoint3D initialPosition = new FramePoint3D();
   private final GlitchFilteredYoBoolean touchdownTrigger;

   private final FramePoint3D desiredPosition = new FramePoint3D();
   private final FrameVector3D desiredVelocity = new FrameVector3D();

   private final QuadrupedFootControlModuleParameters parameters;

   private final YoBoolean stepCommandIsValid;
   private final YoDouble timestamp;
   private final YoQuadrupedTimedStep currentStepCommand;

   private final VirtualForceCommand virtualForceCommand = new VirtualForceCommand();
   private final PointFeedbackControlCommand feedbackControlCommand = new PointFeedbackControlCommand();

   private final ReferenceFrame soleFrame;

   private final QuadrupedControllerToolbox controllerToolbox;
   private final RobotQuadrant robotQuadrant;

   // graphics
   private static final int pointsPerSecondOfSwingTime = 70;
   private static final AppearanceDefinition visualizationAppearance = YoAppearance.Blue();
   private final FramePoint3D swingPositionForVisualization = new FramePoint3D();
   private final BagOfBalls stepSequenceVisualization;

   private boolean triggerSupport;

   public QuadrupedSwingState(RobotQuadrant robotQuadrant, QuadrupedControllerToolbox controllerToolbox, YoBoolean stepCommandIsValid,
                              YoQuadrupedTimedStep currentStepCommand, YoGraphicsListRegistry graphicsListRegistry, YoVariableRegistry registry)
   {
      this.robotQuadrant = robotQuadrant;
      this.controllerToolbox = controllerToolbox;

      this.stepCommandIsValid = stepCommandIsValid;
      this.timestamp = controllerToolbox.getRuntimeEnvironment().getRobotTimestamp();
      this.currentStepCommand = currentStepCommand;

      this.parameters = controllerToolbox.getFootControlModuleParameters();

      soleFrame = controllerToolbox.getReferenceFrames().getSoleFrame(robotQuadrant);

      this.swingTrajectory = new ThreeDoFSwingFootTrajectory(this.robotQuadrant.getPascalCaseName(), registry);
      this.touchdownTrigger = new GlitchFilteredYoBoolean(this.robotQuadrant.getCamelCaseName() + "TouchdownTriggered", registry,
                                                          QuadrupedFootControlModuleParameters.getDefaultTouchdownTriggerWindow());

      RigidBody foot = controllerToolbox.getFullRobotModel().getFoot(robotQuadrant);
      FramePoint3D currentPosition = new FramePoint3D(soleFrame);
      currentPosition.changeFrame(foot.getBodyFixedFrame());

      feedbackControlCommand.set(controllerToolbox.getFullRobotModel().getBody(), foot);
      feedbackControlCommand.setBodyFixedPointToControl(currentPosition);

      // graphics
      if (createSwingTrajectoryGraphics)
      {
         String prefix = robotQuadrant.getPascalCaseName() + "SwingTrajectory";
         stepSequenceVisualization = new BagOfBalls(pointsPerSecondOfSwingTime, 0.005, prefix, visualizationAppearance, registry, graphicsListRegistry);
      }
      else
      {
         stepSequenceVisualization = null;
      }
   }

   @Override
   public void onEntry()
   {
      controllerToolbox.getFootContactState(robotQuadrant).clear();

      // initialize swing trajectory
      double groundClearance = currentStepCommand.getGroundClearance();
      TimeInterval timeInterval = currentStepCommand.getTimeInterval();

      initialPosition.setToZero(soleFrame);
      initialPosition.changeFrame(worldFrame);

      currentStepCommand.getGoalPosition(goalPosition);
      goalPosition.changeFrame(worldFrame);
      goalPosition.add(0.0, 0.0, parameters.getStepGoalOffsetZParameter());

      swingTrajectory.initializeTrajectory(initialPosition, goalPosition, groundClearance, timeInterval);

      if (createSwingTrajectoryGraphics)
         updateGraphics(timeInterval.getStartTime(), timeInterval.getEndTime());

      touchdownTrigger.set(false);
      triggerSupport = false;
   }

   private void updateGraphics(double startTime, double endTime)
   {
      stepSequenceVisualization.reset();
      double swingDuration = endTime - startTime;
      int numberOfPoints = MathTools.clamp((int) (pointsPerSecondOfSwingTime * swingDuration), 2, pointsPerSecondOfSwingTime);

      for (int i = 0; i < numberOfPoints; i++)
      {
         double t = startTime + (i + 1) * swingDuration / numberOfPoints;
         swingTrajectory.computeTrajectory(t);
         swingTrajectory.getPosition(swingPositionForVisualization);
         stepSequenceVisualization.setBall(swingPositionForVisualization);
      }
   }

   @Override
   public void doAction(double timeInState)
   {
      double currentTime = timestamp.getDoubleValue();
      double touchDownTime = currentStepCommand.getTimeInterval().getEndTime();

      // Compute current goal position.
      currentStepCommand.getGoalPosition(goalPosition);
      goalPosition.changeFrame(worldFrame);
      goalPosition.add(0.0, 0.0, parameters.getStepGoalOffsetZParameter());

      // Compute swing trajectory.
      if (touchDownTime - currentTime > parameters.getMinimumStepAdjustmentTimeParameter())
      {
         swingTrajectory.adjustTrajectory(goalPosition, currentTime);
      }
      swingTrajectory.computeTrajectory(currentTime);
      swingTrajectory.getPosition(desiredPosition);
      swingTrajectory.getVelocity(desiredVelocity);

      // Detect early touch-down. // FIXME do something else with this (trigger a loaded transition?)
      FrameVector3D soleForceEstimate = controllerToolbox.getTaskSpaceEstimates().getSoleVirtualForce(robotQuadrant);
      soleForceEstimate.changeFrame(worldFrame);
      double pressureEstimate = -soleForceEstimate.getZ();
      double relativeTimeInSwing = currentTime - currentStepCommand.getTimeInterval().getStartTime();
      double normalizedTimeInSwing = relativeTimeInSwing / currentStepCommand.getTimeInterval().getDuration();
      if (normalizedTimeInSwing > 0.5)
      {
         touchdownTrigger.update(pressureEstimate > parameters.getTouchdownPressureLimitParameter());
      }

      // Compute sole force.
      if (touchdownTrigger.getBooleanValue())
      {
         double pressureLimit = parameters.getTouchdownPressureLimitParameter();
         soleForceCommand.changeFrame(worldFrame);
         soleForceCommand.set(0, 0, -pressureLimit);
         soleForceCommand.changeFrame(soleFrame);
         virtualForceCommand.setLinearForce(soleFrame, soleForceCommand);

      }
      else
      {
         feedbackControlCommand.set(desiredPosition, desiredVelocity);
         feedbackControlCommand.setGains(parameters.getSolePositionGains());
      }

      if (createSwingTrajectoryGraphics)
         updateGraphics(currentTime, currentStepCommand.getTimeInterval().getEndTime());

      // Trigger support phase.
      if (currentTime >= touchDownTime)
      {
         if (stepTransitionCallback != null)
         {
            stepTransitionCallback.onTouchDown(robotQuadrant);
         }
         triggerSupport = true;
      }
   }

   @Override
   public QuadrupedFootControlModule.FootEvent fireEvent(double timeInState)
   {
      return triggerSupport ? QuadrupedFootControlModule.FootEvent.TIMEOUT : null;
   }

   @Override
   public void onExit()
   {
      stepCommandIsValid.set(false);

      if (createSwingTrajectoryGraphics)
         stepSequenceVisualization.hideAll();
      triggerSupport = false;
   }

   @Override
   public VirtualModelControlCommand<?> getVirtualModelControlCommand()
   {
      if (touchdownTrigger.getBooleanValue())
         return virtualForceCommand;
      else
         return null;
   }

   @Override
   public PointFeedbackControlCommand getFeedbackControlCommand()
   {
      if (touchdownTrigger.getBooleanValue())
         return null;
      else
         return feedbackControlCommand;
   }

   @Override
   public FeedbackControlCommand<?> createFeedbackControlTemplate()
   {
      return feedbackControlCommand;
   }
}

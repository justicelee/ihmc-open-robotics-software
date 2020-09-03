package us.ihmc.humanoidBehaviors.lookAndStep;

import us.ihmc.tools.property.StoredPropertySetReadOnly;

import static us.ihmc.humanoidBehaviors.lookAndStep.LookAndStepBehaviorParameters.*;

public interface LookAndStepBehaviorParametersReadOnly extends StoredPropertySetReadOnly
{
   default double getMaxPlanStrayDistance()
   {
      return get(maxPlanStrayDistance);
   }

   default int getMinimumNumberOfPlannedSteps()
   {
      return get(minimumNumberOfPlannedSteps);
   }

   default double getGoalSatisfactionRadius()
   {
      return get(goalSatisfactionRadius);
   }

   default double getGoalSatisfactionOrientationDelta()
   {
      return get(goalSatisfactionOrientationDelta);
   }

   default double getPlanarRegionsExpiration()
   {
      return get(planarRegionsExpiration);
   }

   default double getDirection()
   {
      return get(direction);
   }

   default double getWiggleInsideDeltaMinimumOverride()
   {
      return get(wiggleInsideDeltaMinimumOverride);
   }

   default double getWiggleInsideDeltaTargetOverride()
   {
      return get(wiggleInsideDeltaTargetOverride);
   }

   default boolean getWiggleWhilePlanningOverride()
   {
      return get(wiggleWhilePlanningOverride);
   }

   default boolean getEnableExpansionMaskOverride()
   {
      return get(enableExpansionMaskOverride);
   }

   default double getPlanHorizon()
   {
      return get(planHorizon);
   }

   default double getIdealFootstepLengthOverride()
   {
      return get(idealFootstepLengthOverride);
   }

   default double getCliffBaseHeightToAvoidOverride()
   {
      return get(cliffBaseHeightToAvoidOverride);
   }

   default boolean getEnableConcaveHullWigglerOverride()
   {
      return get(enableConcaveHullWigglerOverride);
   }

   default double getFootstepPlannerTimeout()
   {
      return get(footstepPlannerTimeout);
   }

   default double getSwingTime()
   {
      return get(swingTime);
   }

   default double getTransferTime()
   {
      return get(transferTime);
   }

   default double getWaitTimeAfterPlanFailed()
   {
      return get(waitTimeAfterPlanFailed);
   }

   default boolean getReturnBestEffortPlanOverride()
   {
      return get(returnBestEffortPlanOverride);
   }

   default double getPercentSwingToWait()
   {
      return get(percentSwingToWait);
   }

   default double getRobotConfigurationDataExpiration()
   {
      return get(robotConfigurationDataExpiration);
   }

   default int getAcceptableIncompleteFootsteps()
   {
      return get(acceptableIncompleteFootsteps);
   }

   default double getMinimumSwingFootClearanceOverride()
   {
      return get(minimumSwingFootClearanceOverride);
   }

   default double getNeckPitchForBodyPath()
   {
      return get(neckPitchForBodyPath);
   }

   default double getNeckPitchTolerance()
   {
      return get(neckPitchTolerance);
   }

   default double getResetDuration()
   {
      return get(resetDuration);
   }

   default int getSwingPlannerType()
   {
      return get(swingPlannerType);
   }
}

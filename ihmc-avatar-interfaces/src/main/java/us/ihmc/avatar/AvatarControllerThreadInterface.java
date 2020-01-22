package us.ihmc.avatar;

import us.ihmc.commonWalkingControlModules.barrierScheduler.context.HumanoidRobotContextData;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutputListBasics;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public interface AvatarControllerThreadInterface
{
   void run();

   YoVariableRegistry getYoVariableRegistry();

   FullHumanoidRobotModel getFullRobotModel();

   HumanoidRobotContextData getHumanoidRobotContextData();

   JointDesiredOutputListBasics getDesiredJointDataHolder();
}

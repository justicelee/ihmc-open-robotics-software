package us.ihmc.commonWalkingControlModules.controllerCore.command;

import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControllerCoreMode;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.InverseKinematicsCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.InverseKinematicsCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelOneDoFJointDesiredDataHolder;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelOneDoFJointDesiredDataHolderReadOnly;

public class ControllerCoreCommand implements ControllerCoreCommandInterface
{
   private final InverseDynamicsCommandList inverseDynamicsCommandList;
   private final FeedbackControlCommandList feedbackControlCommandList;
   private final InverseKinematicsCommandList inverseKinematicsCommandList;
   private final InverseDynamicsCommandList virtualModelCommandList;
   private final LowLevelOneDoFJointDesiredDataHolder lowLevelOneDoFJointDesiredDataHolder;
   private WholeBodyControllerCoreMode controllerCoreMode;

   public ControllerCoreCommand(WholeBodyControllerCoreMode controllerCoreMode)
   {
      this.controllerCoreMode = controllerCoreMode;

      inverseDynamicsCommandList = new InverseDynamicsCommandList();
      feedbackControlCommandList = new FeedbackControlCommandList();
      inverseKinematicsCommandList = new InverseKinematicsCommandList();
      virtualModelCommandList = new InverseDynamicsCommandList();
      lowLevelOneDoFJointDesiredDataHolder = new LowLevelOneDoFJointDesiredDataHolder();
   }

   public void clear()
   {
      inverseDynamicsCommandList.clear();
      feedbackControlCommandList.clear();
      inverseKinematicsCommandList.clear();
      virtualModelCommandList.clear();
      lowLevelOneDoFJointDesiredDataHolder.clear();
   }

   public void addInverseDynamicsCommand(InverseDynamicsCommand<?> inverseDynamicsCommand)
   {
      if (inverseDynamicsCommand != null)
         inverseDynamicsCommandList.addCommand(inverseDynamicsCommand);
   }

   public void addFeedbackControlCommand(FeedbackControlCommand<?> feedbackControlCommand)
   {
      if (feedbackControlCommand != null)
         feedbackControlCommandList.addCommand(feedbackControlCommand);
   }

   public void addInverseKinematicsCommand(InverseKinematicsCommand<?> inverseKinematicsCommand)
   {
      if (inverseKinematicsCommand != null)
         inverseKinematicsCommandList.addCommand(inverseKinematicsCommand);
   }

   public void addVirtualModelCommand(InverseDynamicsCommand<?> virtualModelCommand)
   {
      if (virtualModelCommand != null)
         virtualModelCommandList.addCommand(virtualModelCommand);
   }

   public void completeLowLevelJointData(LowLevelOneDoFJointDesiredDataHolderReadOnly lowLevelJointData)
   {
      if (lowLevelJointData != null)
         lowLevelOneDoFJointDesiredDataHolder.completeWith(lowLevelJointData);
   }

   public void setControllerCoreMode(WholeBodyControllerCoreMode controllerCoreMode)
   {
      this.controllerCoreMode = controllerCoreMode;
   }

   @Override
   public InverseDynamicsCommandList getInverseDynamicsCommandList()
   {
      return inverseDynamicsCommandList;
   }

   @Override
   public FeedbackControlCommandList getFeedbackControlCommandList()
   {
      return feedbackControlCommandList;
   }

   @Override
   public InverseKinematicsCommandList getInverseKinematicsCommandList()
   {
      return inverseKinematicsCommandList;
   }

   @Override
   public InverseDynamicsCommandList getVirtualModelCommandList()
   {
      return virtualModelCommandList;
   }

   @Override
   public LowLevelOneDoFJointDesiredDataHolder getLowLevelOneDoFJointDesiredDataHolder()
   {
      return lowLevelOneDoFJointDesiredDataHolder;
   }

   public void set(ControllerCoreCommand other)
   {
      controllerCoreMode = other.controllerCoreMode;
      inverseDynamicsCommandList.set(other.inverseDynamicsCommandList);
      feedbackControlCommandList.set(other.feedbackControlCommandList);
      inverseKinematicsCommandList.set(other.inverseKinematicsCommandList);
      virtualModelCommandList.set(other.virtualModelCommandList);
      lowLevelOneDoFJointDesiredDataHolder.overwriteWith(lowLevelOneDoFJointDesiredDataHolder);
   }

   @Override
   public WholeBodyControllerCoreMode getControllerCoreMode()
   {
      return controllerCoreMode;
   }
}

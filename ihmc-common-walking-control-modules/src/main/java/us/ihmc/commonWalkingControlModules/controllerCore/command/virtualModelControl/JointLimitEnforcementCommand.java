package us.ihmc.commonWalkingControlModules.controllerCore.command.virtualModelControl;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.commonWalkingControlModules.controllerCore.command.ControllerCoreCommandType;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.robotics.kinematics.JointLimitData;

public class JointLimitEnforcementCommand implements VirtualModelControlCommand<JointLimitEnforcementCommand>
{
   private final List<OneDoFJointBasics> joints;
   private final RecyclingArrayList<JointLimitData> jointLimitData;

   public JointLimitEnforcementCommand()
   {
      this(40);
   }

   public JointLimitEnforcementCommand(int initialCapacity)
   {
      joints = new ArrayList<>(initialCapacity);
      jointLimitData = new RecyclingArrayList<>(initialCapacity, JointLimitData.class);
   }

   public void clear()
   {
      joints.clear();

      for (int i = 0; i < jointLimitData.size(); i++)
         jointLimitData.get(i).clear();
      jointLimitData.clear();
   }

   @Override
   public void set(JointLimitEnforcementCommand other)
   {
      clear();

      for (int i = 0; i < other.getNumberOfJoints(); i++)
      {
         joints.add(other.joints.get(i));
         jointLimitData.add().set(other.jointLimitData.get(i));
      }
   }

   public void addJoint(OneDoFJointBasics joint, JointLimitData jointLimitData)
   {
      joints.add(joint);
      this.jointLimitData.add().set(jointLimitData);
   }

   public void addJoint(OneDoFJointBasics joint, double stiffness, double damping)
   {
      joints.add(joint);
      JointLimitData jointLimitData = this.jointLimitData.add();
      jointLimitData.setJointLimits(joint);
      jointLimitData.setPositionLimitStiffness(stiffness);
      jointLimitData.setPositionLimitDamping(damping);
   }

   public int getNumberOfJoints()
   {
      return joints.size();
   }

   public OneDoFJointBasics getJoint(int jointIndex)
   {
      return joints.get(jointIndex);
   }

   public JointLimitData getJointLimitData(int jointIndex)
   {
      return jointLimitData.get(jointIndex);
   }

   @Override
   public ControllerCoreCommandType getCommandType()
   {
      return ControllerCoreCommandType.JOINT_LIMIT_ENFORCEMENT;
   }
}

package us.ihmc.darpaRoboticsChallenge;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.ArmJointName;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotParameters;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.RotationFunctions;
import us.ihmc.utilities.screwTheory.OneDoFJoint;


public class DRCRobotWalkingControllerParameters implements WalkingControllerParameters
{
   private final SideDependentList<Transform3D> handControlFramesWithRespectToFrameAfterWrist = new SideDependentList<Transform3D>();
   private final SideDependentList<Transform3D> handPosesWithRespectToChestFrame = new SideDependentList<Transform3D>();
   private final double minElbowRollAngle = 0.5;

   public DRCRobotWalkingControllerParameters()
   {
      for(RobotSide robotSide : RobotSide.values)
      {
         Transform3D rotationPart = new Transform3D();
         double yaw = robotSide.negateIfRightSide(Math.PI / 2.0);
         double pitch = 0.0;
         double roll = 0.0;
         rotationPart.setEuler(new Vector3d(roll, pitch, yaw));
   
         Transform3D toHand = new Transform3D();
//         double x = 0.2;
//         double y = robotSide.negateIfRightSide(-0.03);
//         double z = 0.04;
         double x = 0.1;
         double y = 0.0;
         double z = 0.0;
         toHand.setTranslation(new Vector3d(x, y, z));
   
         Transform3D transform = new Transform3D();
         transform.mul(rotationPart, toHand);
         
         handControlFramesWithRespectToFrameAfterWrist.put(robotSide, transform);
      }
      
      
      for (RobotSide robotSide : RobotSide.values)
      {
         Transform3D transform = new Transform3D();

         double x = 0.20;
         double y = robotSide.negateIfRightSide(0.35); //0.30);
         double z = -0.40;
         transform.setTranslation(new Vector3d(x, y, z));

         Matrix3d rotation = new Matrix3d();
         double yaw = 0.0;//robotSide.negateIfRightSide(-1.7);
         double pitch = 0.7;
         double roll = 0.0;//robotSide.negateIfRightSide(-0.8);
         RotationFunctions.setYawPitchRoll(rotation, yaw, pitch, roll);
         transform.setRotation(rotation);

         handPosesWithRespectToChestFrame.put(robotSide, transform);
      }
   }
   
   public SideDependentList<Transform3D> getDesiredHandPosesWithRespectToChestFrame()
   {
      return handPosesWithRespectToChestFrame;
   }

   public Map<OneDoFJoint, Double> getDefaultArmJointPositions(FullRobotModel fullRobotModel, RobotSide robotSide)
   {
      Map<OneDoFJoint, Double> jointPositions = new LinkedHashMap<OneDoFJoint, Double>();

      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.SHOULDER_ROLL), robotSide.negateIfRightSide(-1.36));
      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.SHOULDER_PITCH), 0.34);
      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.ELBOW_ROLL), robotSide.negateIfRightSide(1.18));
      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.ELBOW_PITCH), 1.94);
      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.WRIST_PITCH), -0.19);
      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.WRIST_ROLL), robotSide.negateIfRightSide(-0.07));

//      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.SHOULDER_ROLL), robotSide.negateIfRightSide(0.0));
//      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.SHOULDER_PITCH), 0.0);
//      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.ELBOW_ROLL), robotSide.negateIfRightSide(0.0));
//      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.ELBOW_PITCH), 0.0);
//      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.WRIST_PITCH), 0.0);
//      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.WRIST_ROLL), robotSide.negateIfRightSide(0.0));

      return jointPositions;
   }


   public Map<OneDoFJoint, Double> getMinTaskspaceArmJointPositions(FullRobotModel fullRobotModel, RobotSide robotSide)
   {
      Map<OneDoFJoint, Double> ret = new LinkedHashMap<OneDoFJoint, Double>();
      if (robotSide == RobotSide.LEFT)
      {
         ret.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.ELBOW_ROLL), minElbowRollAngle);
      }
      return ret;
   }

   public Map<OneDoFJoint, Double> getMaxTaskspaceArmJointPositions(FullRobotModel fullRobotModel, RobotSide robotSide)
   {
      Map<OneDoFJoint, Double> ret = new LinkedHashMap<OneDoFJoint, Double>();
      if (robotSide == RobotSide.RIGHT)
      {
         ret.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.ELBOW_ROLL), -minElbowRollAngle);
      }
      return ret;
   }

   public boolean doToeOffIfPossible()
   {
      return true; 
//      return false; //TODO: JEP. Changed to false for now so we can try out the new ICP trajectory stuff. Need to do toe off a little more smoothly.
   }

   public String[] getDefaultHeadOrientationControlJointNames()
   {
      // Get rid of back_ubx to prevent hip roll jumps.
//      return new String[] {"back_lbz", "back_ubx", "neck_ay"}; // Pelvis will jump around with these setting.
      return new String[] {"back_lbz", "neck_ay"}; 
//      return new String[] {"neck_ay"};
   }
   
   public String[] getAllowableHeadOrientationControlJointNames()
   {
      return new String[] {"back_lbz", "back_ubx", "neck_ay"}; 
   }

   public String[] getDefaultChestOrientationControlJointNames()
   {
      //    return new String[] {"back_mby"};
      return new String[]{};
   }

   public String[] getAllowableChestOrientationControlJointNames()
   {
      return new String[] {"back_mby"};
   }

   public boolean checkOrbitalEnergyCondition()
   {
      return false;
   }

   // USE THESE FOR TASK 2 UNTIL WALKING WORKS BETTER WITH OTHERS.
   private double minimumHeightAboveGround = 0.68;                                       
   private double nominalHeightAboveGround = 0.76; 
   private double maximumHeightAboveGround = 0.82; 

//   // USE THESE FOR IMPROVING WALKING, BUT DONT CHECK THEM IN UNTIL IT IMPROVED WALKING THROUGH MUD.
//   private double minimumHeightAboveGround = 0.68;                                       
//   private double nominalHeightAboveGround = 0.80;  // NOTE: used to be 0.76, jojo        
//   private double maximumHeightAboveGround = 0.84;  // NOTE: used to be 0.82, jojo        
   
   public double minimumHeightAboveAnkle()
   {
      return minimumHeightAboveGround;
   }
   
   public double nominalHeightAboveAnkle()
   {
      return nominalHeightAboveGround;
   }
   
   public double maximumHeightAboveAnkle()
   {
      return maximumHeightAboveGround;
   }

   public void setNominalHeightAboveAnkle(double nominalHeightAboveAnkle)
   {
      this.nominalHeightAboveGround = nominalHeightAboveAnkle;
   }

   public double getGroundReactionWrenchBreakFrequencyHertz()
   {
      return 7.0;
   }

   public boolean resetDesiredICPToCurrentAtStartOfSwing()
   {
      return false;
   }

   public double getUpperNeckPitchLimit()
   {
      return DRCRobotParameters.DRC_ROBOT_NECK_PITCH_UPPER_LIMIT;
   }

   public double getLowerNeckPitchLimit()
   {
      return DRCRobotParameters.DRC_ROBOT_NECK_PITCH_LOWER_LIMIT;
   }

   public double getHeadYawLimit()
   {
      return DRCRobotParameters.DRC_ROBOT_HEAD_YAW_LIMIT;
   }

   public double getHeadRollLimit()
   {
      return DRCRobotParameters.DRC_ROBOT_HEAD_ROLL_LIMIT;
   }

   public String getJointNameForExtendedPitchRange()
   {
      return "back_mby";
   }


   public SideDependentList<Transform3D> getHandControlFramesWithRespectToFrameAfterWrist()
   {
      return handControlFramesWithRespectToFrameAfterWrist;
   }

   public boolean finishSwingWhenTrajectoryDone()
   {
      return false;
   }

   public double getFootForwardOffset()
   {
      return DRCRobotParameters.DRC_ROBOT_FOOT_FORWARD;
   }

   public double getFootBackwardOffset()
   {
      return DRCRobotParameters.DRC_ROBOT_FOOT_BACK;
   }
   
   public double getAnkleHeight()
   {
      return DRCRobotParameters.DRC_ROBOT_ANKLE_HEIGHT;
   }


   public double getFinalToeOffPitchAngularVelocity()
   {
      return 3.5;
   }

   public double getInPlaceWidth()
   {
      return 0.25;
   }

   public double getDesiredStepForward()
   {
      return 0.5; //0.35;
   }
  
   public double getMaxStepLength()
   {
       return 0.6; //0.5; //0.35;
   }

   public double getMinStepWidth()
   {
      return 0.15;
   }

   public double getMaxStepWidth()
   {
      return 0.6; //0.4;
   }

   public double getStepPitch()
   {
      return 0.0;
   }
}

package us.ihmc.avatar.reachabilityMap.example;

import us.ihmc.avatar.reachabilityMap.ReachabilityMapListener;
import us.ihmc.avatar.reachabilityMap.ReachabilitySphereMapCalculator;
import us.ihmc.graphicsDescription.Graphics3DObject;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.mecano.tools.MultiBodySystemTools;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.SimulationConstructionSetParameters;

public class ReachabilitySphereMapExample
{
   public ReachabilitySphereMapExample()
   {
      final RobotArm robot = new RobotArm();
      SimulationConstructionSetParameters parameters = new SimulationConstructionSetParameters(true, 16000);
      SimulationConstructionSet scs = new SimulationConstructionSet(robot, parameters);
      Graphics3DObject coordinate = new Graphics3DObject();
      coordinate.transform(robot.getElevatorFrameTransformToWorld());
      coordinate.addCoordinateSystem(1.0);
      scs.addStaticLinkGraphics(coordinate);
      scs.startOnAThread();

      OneDoFJointBasics[] armJoints = MultiBodySystemTools.filterJoints(robot.getJacobian().getJointsInOrder(), OneDoFJointBasics.class);
      ReachabilitySphereMapCalculator reachabilitySphereMapCalculator = new ReachabilitySphereMapCalculator(armJoints, scs);
//      reachabilitySphereMapCalculator.setControlFrameFixedInEndEffector(robot.getControlFrame());
      ReachabilityMapListener listener = new ReachabilityMapListener()
      {
         @Override
         public void hasReachedNewConfiguration()
         {
            robot.copyRevoluteJointConfigurationToPinJoints();
         }
      };
      reachabilitySphereMapCalculator.attachReachabilityMapListener(listener);
      reachabilitySphereMapCalculator.buildReachabilitySpace();
   }

   public static void main(String[] args)
   {
      new ReachabilitySphereMapExample();
   }
}

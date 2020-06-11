package us.ihmc.avatar.networkProcessor.stepConstraintToolboxModule;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.referenceFrame.FrameConvexPolygon2D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.tools.ReferenceFrameTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.footstepPlanning.tools.PlannerTools;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CapturabilityBasedPlanarRegionDeciderTest
{
   @AfterEach
   public void tearDown()
   {
      ReferenceFrameTools.clearWorldFrameTree();
   }

   @Test
   public void testFlatGround()
   {
      double stanceWidth = 0.2;
      double stepLength = 0.3;

      ConvexPolygon2D ground = new ConvexPolygon2D();
      ground.addVertex(10.0, 10.0);
      ground.addVertex(10.0, -10.0);
      ground.addVertex(-10.0, -10.0);
      ground.addVertex(-10.0, 10.0);
      ground.update();
      PlanarRegion groundPlane = new PlanarRegion(new RigidBodyTransform(), ground);

      PlanarRegionsList planarRegionsList = new PlanarRegionsList();
      planarRegionsList.addPlanarRegion(groundPlane);

      PoseReferenceFrame centerOfMassFrame = new PoseReferenceFrame("centerOfMassFrame", ReferenceFrame.getWorldFrame());
      centerOfMassFrame.translateAndUpdate(0.0, 0.0, 1.0);
      CapturabilityBasedPlanarRegionDecider constraintCalculator = new CapturabilityBasedPlanarRegionDecider(centerOfMassFrame,
                                                                                                             9.81,
                                                                                                             new YoVariableRegistry(""),
                                                                                                             null);
      constraintCalculator.setSwitchPlanarRegionConstraintsAutomatically(true);

      FramePose3D stepPose = new FramePose3D();
      stepPose.getPosition().set(stepLength, 0.5 * stanceWidth, 0.0);

      FrameConvexPolygon2D captureRegion = new FrameConvexPolygon2D();
      captureRegion.addVertex(stepLength + 0.05, 0.5 * stanceWidth + 0.05);
      captureRegion.addVertex(stepLength - 0.05, 0.5 * stanceWidth + 0.05);
      captureRegion.addVertex(stepLength - 0.05, 0.5 * stanceWidth - 0.05);
      captureRegion.addVertex(stepLength + 0.05, 0.5 * stanceWidth - 0.05);
      captureRegion.update();

      constraintCalculator.setOmega0(3.0);
      constraintCalculator.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());
      constraintCalculator.setCaptureRegion(captureRegion);

      constraintCalculator.updatePlanarRegionConstraintForStep(stepPose);

      assertTrue(groundPlane.epsilonEquals(constraintCalculator.getConstraintRegion(), 1e-8));
   }

   @Test
   public void testFlatGroundWithOtherRegion()
   {
      double stanceWidth = 0.2;
      double stepLength = 0.3;
      double stepWidth = 0.15;

      double comHeight = 1.0;
      double gravity = 9.81;
      double omega = Math.sqrt(gravity / comHeight);

      FramePose3D stepPose = new FramePose3D();
      stepPose.getPosition().set(stepLength, stepWidth, 0.0);

      ConvexPolygon2D ground = new ConvexPolygon2D();
      ground.addVertex(10.0, 10.0);
      ground.addVertex(10.0, -10.0);
      ground.addVertex(-10.0, -10.0);
      ground.addVertex(-10.0, 10.0);
      ground.update();
      PlanarRegion groundPlane = new PlanarRegion(new RigidBodyTransform(), ground);

      ConvexPolygon2D smallRegion = new ConvexPolygon2D();
      smallRegion.addVertex(0.2, 0.2);
      smallRegion.addVertex(0.2, -0.2);
      smallRegion.addVertex(-0.2, -0.2);
      smallRegion.addVertex(-0.2, 0.2);
      smallRegion.update();
      RigidBodyTransform smallRegionTransform = new RigidBodyTransform();
      smallRegionTransform.getTranslation().set(stepPose.getPosition());
      smallRegionTransform.getTranslation().setZ(0.1);
      PlanarRegion smallRegionPlane = new PlanarRegion(smallRegionTransform, smallRegion);

      PlanarRegionsList planarRegionsList = new PlanarRegionsList();
      planarRegionsList.addPlanarRegion(groundPlane);
      planarRegionsList.addPlanarRegion(smallRegionPlane);

      FrameConvexPolygon2D captureRegion = new FrameConvexPolygon2D();
      captureRegion.addVertex(stepLength + 0.05, 0.5 * stanceWidth + 0.05);
      captureRegion.addVertex(stepLength - 0.05, 0.5 * stanceWidth + 0.05);
      captureRegion.addVertex(stepLength - 0.05, 0.5 * stanceWidth - 0.05);
      captureRegion.addVertex(stepLength + 0.05, 0.5 * stanceWidth - 0.05);
      captureRegion.update();

      PoseReferenceFrame centerOfMassFrame = new PoseReferenceFrame("centerOfMassFrame", ReferenceFrame.getWorldFrame());
      CapturabilityBasedPlanarRegionDecider constraintCalculator = new CapturabilityBasedPlanarRegionDecider(centerOfMassFrame,
                                                                                                             9.81,
                                                                                                             new YoVariableRegistry(""),
                                                                                                             null);
      constraintCalculator.setSwitchPlanarRegionConstraintsAutomatically(true);

      centerOfMassFrame.setPositionAndUpdate(new FramePoint3D(ReferenceFrame.getWorldFrame(), 0.1, -0.05, comHeight));

      constraintCalculator.setOmega0(omega);
      constraintCalculator.setPlanarRegions(planarRegionsList.getPlanarRegionsAsList());
      constraintCalculator.setCaptureRegion(captureRegion);
      constraintCalculator.updatePlanarRegionConstraintForStep(stepPose);

      assertTrue(smallRegionPlane.epsilonEquals(constraintCalculator.getConstraintRegion(), 1e-8));

      captureRegion.clear();
      captureRegion.addVertex(stepLength + 0.05 + 0.05, -0.05 + 0.05);
      captureRegion.addVertex(stepLength + 0.05 - 0.05, -0.05 + 0.05);
      captureRegion.addVertex(stepLength + 0.05 - 0.05, -0.05 - 0.05);
      captureRegion.addVertex(stepLength + 0.05 + 0.05, -0.05 - 0.05);
      captureRegion.update();

      constraintCalculator.setCaptureRegion(captureRegion);

      constraintCalculator.updatePlanarRegionConstraintForStep(stepPose);
      assertTrue(groundPlane.epsilonEquals(constraintCalculator.getConstraintRegion(), 1e-8));
   }
}
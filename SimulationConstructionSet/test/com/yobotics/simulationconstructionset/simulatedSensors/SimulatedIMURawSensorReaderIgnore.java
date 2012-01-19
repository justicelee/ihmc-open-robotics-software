package com.yobotics.simulationconstructionset.simulatedSensors;

import static org.junit.Assert.assertEquals;

import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.junit.Before;
import org.junit.Test;

import com.yobotics.simulationconstructionset.rawSensors.RawIMUSensorsInterface;

import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.RigidBodyInertia;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistAndAccelerationCalculator;

public class SimulatedIMURawSensorReaderIgnore
{
   private final RawSensors rawSensors = new RawSensors();
   private final FullRobotModel fullRobotModel = new FullRobotModel();
   private final RigidBody rigidBody = fullRobotModel.getBodyLink();
   private final ReferenceFrame bodyFrame = fullRobotModel.getBodyFrame();
   
   private final Matrix3d actualIMUOrientation = new Matrix3d();
   private final Vector3d actualLinearAcceleration = new Vector3d();
   private final Vector3d actualAngularVelocity = new Vector3d();
   
   private final AxisAngle4d randomBodyAxisAngle = new AxisAngle4d();
   private final Transform3D randomTransformBodyToWorld = new Transform3D();
   private final FrameVector randomLinearVelocity = new FrameVector((ReferenceFrame) null);
   private final FrameVector randomAngularVelocity = new FrameVector((ReferenceFrame) null);
   private final FrameVector randomLinearAcceleration = new FrameVector((ReferenceFrame) null);
   private final FrameVector randomAngularAcceleration = new FrameVector((ReferenceFrame) null);
   
   private final Matrix3d expectedIMUOrientation = new Matrix3d();
   private final Vector3d expectedAngularVelocityInIMUFrame = new Vector3d();
   private final Vector3d expectedLinearAccelerationOfIMUInIMUFrame = new Vector3d();
   
   private final FrameVector jointToIMUOffset = new FrameVector(bodyFrame, 2.0*(Math.random()-0.5), 2.0*(Math.random()-0.5), 2.0*(Math.random()-0.5));
   
   //static
   {jointToIMUOffset.set(0.0, 0.0, 0.0);} // TODO
 
   //private final AxisAngle4d jointToIMURotation = new AxisAngle4d(2.0*(Math.random()-0.5), 2.0*(Math.random()-0.5), 2.0*(Math.random()-0.5), Math.random()*2.0*Math.PI);
   private final AxisAngle4d jointToIMURotation = new AxisAngle4d(0.0, 0.0, 0.0, 0.0); // TODO

   private final Transform3D transformIMUToJoint = new Transform3D();
   private final Transform3D transformJointToIMU = new Transform3D();
   {
      transformIMUToJoint.setRotation(jointToIMURotation);
      transformIMUToJoint.setTranslation(jointToIMUOffset.getVectorCopy());
      
      transformJointToIMU.invert(transformIMUToJoint);
   }
   
   public static final double GRAVITY = 0.0; // TODO (2.0 * Math.random() - 1) * 15.0; // random gravity between -15 and +15 m/s^2
   public static final int IMU_INDEX = (int) (10.0 * Math.random()); // random imu index between 0 and 10
   
   SimulatedIMURawSensorReader simulatedIMURawSensorReader = new SimulatedIMURawSensorReader(rawSensors, IMU_INDEX, rigidBody, transformIMUToJoint)
   {
      private static final long serialVersionUID = 6119697219409662317L;

      @Override
      protected void simulateIMU()
      {
         // Perfect IMU here for test
         m00.set(perf_m00.getDoubleValue());
         m01.set(perf_m01.getDoubleValue());
         m02.set(perf_m02.getDoubleValue());
         
         m10.set(perf_m10.getDoubleValue());
         m11.set(perf_m11.getDoubleValue());
         m12.set(perf_m12.getDoubleValue());
         
         m20.set(perf_m20.getDoubleValue());
         m21.set(perf_m21.getDoubleValue());
         m22.set(perf_m22.getDoubleValue());
         
         accel_x.set(perf_accel_x.getDoubleValue());
         accel_y.set(perf_accel_y.getDoubleValue());
         accel_z.set(perf_accel_z.getDoubleValue());
         
         gyro_x.set(perf_gyro_x.getDoubleValue());
         gyro_y.set(perf_gyro_y.getDoubleValue());
         gyro_z.set(perf_gyro_z.getDoubleValue());
         
         compass_x.set(perf_compass_x.getDoubleValue());
         compass_y.set(perf_compass_y.getDoubleValue());
         compass_z.set(perf_compass_z.getDoubleValue());
      }
      
      @Override
      protected TwistAndAccelerationCalculator createTwistAndAccelerationCalculator()
      {
         RigidBody rootBody = fullRobotModel.getElevator();
         ReferenceFrame rootBodyFrame = fullRobotModel.getElevatorFrame();
         Vector3d linearAcceleration = new Vector3d(0.0, 0.0, GRAVITY);
         Vector3d angularAcceleration = new Vector3d();
         SpatialAccelerationVector rootAcceleration = new SpatialAccelerationVector(rootBodyFrame, worldFrame, rootBodyFrame, linearAcceleration, angularAcceleration);
         return new TwistAndAccelerationCalculator(worldFrame, rootBody, rootAcceleration, true, true, false);
      }
   };
   
   @Before
   public void setUp() throws Exception
   {
      simulatedIMURawSensorReader.initialize();
   }

   @Test
   public void testRead()
   {
      for (int i = 0; i < 10000; i++)
      {  
         generateAppliedOrientation();
         generateAppliedVelocity();
         generateAppliedAcceleration();
         
         fullRobotModel.update(randomTransformBodyToWorld, randomLinearVelocity, randomAngularVelocity, randomLinearAcceleration, randomAngularAcceleration);
         simulatedIMURawSensorReader.read();
         
         rawSensors.packOrientation(actualIMUOrientation, IMU_INDEX);
         rawSensors.packAngularVelocity(actualAngularVelocity, IMU_INDEX);
         rawSensors.packAcceleration(actualLinearAcceleration, IMU_INDEX);
         
         generateExpectedOrientation();
         generateExpectedAngularVelocity();
         generateExpectedLinearAcceleration();

         assertEqualsRotationMatrix(expectedIMUOrientation, actualIMUOrientation, 1e-3);
         assertEqualsVector(expectedAngularVelocityInIMUFrame, actualAngularVelocity, 1e-3);
         assertEqualsVector(expectedLinearAccelerationOfIMUInIMUFrame, actualLinearAcceleration, 1e-3);
      }
   }

   void generateAppliedOrientation()
   {
      randomBodyAxisAngle.set(2.0*(Math.random()-0.5), 2.0*(Math.random()-0.5), 2.0*(Math.random()-0.5), Math.random()*2.0*Math.PI);
      
      randomBodyAxisAngle.set(0.0, 0.0, 0.0, 0.0); // TODO
      
      randomTransformBodyToWorld.set(randomBodyAxisAngle);
   }
   
   void generateAppliedVelocity()
   {
      randomLinearVelocity.set(bodyFrame, Math.random()-0.5, Math.random()-0.5, Math.random()-0.5);
      randomLinearVelocity.scale(10);
      randomLinearVelocity.set(1.0, 0.0, 0.0); // TODO
      
      randomAngularVelocity.set(bodyFrame, Math.random()-0.5, Math.random()-0.5, Math.random()-0.5);
      randomAngularVelocity.scale(10);
      randomAngularVelocity.set(0.0, 0.0, 1.0); // TODO
   }
   
   void generateAppliedAcceleration()
   {
      randomLinearAcceleration.set(fullRobotModel.getWorldFrame(), Math.random()-0.5, Math.random()-0.5, Math.random()-0.5); // in world frame
      randomLinearAcceleration.scale(40);
      randomLinearAcceleration.set(0.0, 0.0, 0.0); // TODO
      
      randomAngularAcceleration.set(bodyFrame, Math.random()-0.5, Math.random()-0.5, Math.random()-0.5);
      randomAngularAcceleration.scale(20);
      randomAngularAcceleration.set(0.0, 0.0, 0.0); // TODO
   }
   
   void generateExpectedOrientation()
   {
      Matrix3d randomTransformBodyToWorldMatrix = new Matrix3d();
      Matrix3d transformIMUToJointMatrix = new Matrix3d();
      randomTransformBodyToWorld.get(randomTransformBodyToWorldMatrix);
      transformIMUToJoint.get(transformIMUToJointMatrix);
      
      expectedIMUOrientation.mul(randomTransformBodyToWorldMatrix, transformIMUToJointMatrix);
   }
   
   void generateExpectedAngularVelocity()
   {
      expectedAngularVelocityInIMUFrame.set(randomAngularVelocity.getVectorCopy()); // in joint/body frame
      transformJointToIMU.transform(expectedAngularVelocityInIMUFrame);
   }
   
   void generateExpectedLinearAcceleration()
   {
      FrameVector centerAccelerationPart = new FrameVector(randomLinearAcceleration);
      centerAccelerationPart.setZ(centerAccelerationPart.getZ() + GRAVITY);
      centerAccelerationPart.changeFrame(bodyFrame);
      
      FrameVector centripedalAccelerationPart = new FrameVector(bodyFrame);
      centripedalAccelerationPart.cross(randomAngularVelocity, jointToIMUOffset);
      centripedalAccelerationPart.cross(randomAngularVelocity, centripedalAccelerationPart);
      
//      FrameVector angularAccelerationPart = new FrameVector(bodyFrame);
//      angularAccelerationPart.cross(randomAngularVelocity, jointToIMUOffset);
      
      expectedLinearAccelerationOfIMUInIMUFrame.set(centerAccelerationPart.getVectorCopy());
      expectedLinearAccelerationOfIMUInIMUFrame.add(centripedalAccelerationPart.getVectorCopy());
      //expectedLinearAccelerationOfIMUInIMUFrame.add(angularAccelerationPart.getVectorCopy());
      
      transformJointToIMU.transform(expectedLinearAccelerationOfIMUInIMUFrame);
   }
   
   void assertEqualsVector(Vector3d expected, Vector3d actual, double delta)
   {
      assertEquals(expected.getX(), actual.getX(), delta);
      assertEquals(expected.getY(), actual.getY(), delta);
      assertEquals(expected.getZ(), actual.getZ(), delta);
   }
   
   private void assertEqualsRotationMatrix(Matrix3d expected, Matrix3d actual, double delta)
   {
      Matrix3d differenceMatrix = new Matrix3d();
      differenceMatrix.mulTransposeLeft(expected, actual);
      
      AxisAngle4d differenceAxisAngle = new AxisAngle4d();
      differenceAxisAngle.set(differenceMatrix);
      
      double differenceAngle = differenceAxisAngle.getAngle();
      
      assertEquals(0.0, differenceAngle, delta);
   }

   class RawSensors implements RawIMUSensorsInterface
   {
      private Double r_imu_m00 = 0.0;
      private Double r_imu_m01 = 0.0;
      private Double r_imu_m02 = 0.0;

      private Double r_imu_m10 = 0.0;
      private Double r_imu_m11 = 0.0;
      private Double r_imu_m12 = 0.0;

      private Double r_imu_m20 = 0.0;
      private Double r_imu_m21 = 0.0;
      private Double r_imu_m22 = 0.0;

      private Double r_imu_accel_x = 0.0;
      private Double r_imu_accel_y = 0.0;
      private Double r_imu_accel_z = 0.0;

      private Double r_imu_gyro_x = 0.0;
      private Double r_imu_gyro_y = 0.0;
      private Double r_imu_gyro_z = 0.0;  

      private Double r_imu_compass_x = 0.0;
      private Double r_imu_compass_y = 0.0;
      private Double r_imu_compass_z = 0.0;  

      public void setOrientation(Matrix3d orientation, int imuIndex)
      {      
         r_imu_m00 = orientation.getM00();
         r_imu_m01 = orientation.getM01();
         r_imu_m02 = orientation.getM02();
         
         r_imu_m10 = orientation.getM10();
         r_imu_m11 = orientation.getM11();
         r_imu_m12 = orientation.getM12();

         r_imu_m20 = orientation.getM20();
         r_imu_m21 = orientation.getM21();
         r_imu_m22 = orientation.getM22();
      }

      public void setAcceleration(Vector3d acceleration, int imuIndex)
      {
         r_imu_accel_x = acceleration.getX();
         r_imu_accel_y = acceleration.getY();
         r_imu_accel_z = acceleration.getZ();
      }

      public void setAngularVelocity(Vector3d gyroscope, int imuIndex)
      {
         r_imu_gyro_x = gyroscope.getX();
         r_imu_gyro_y = gyroscope.getY();
         r_imu_gyro_z = gyroscope.getZ();
      }

      public void setCompass(Vector3d compass, int imuIndex)
      {
         r_imu_compass_x = compass.getX();
         r_imu_compass_y = compass.getY();
         r_imu_compass_z = compass.getZ();
      }

      public void packOrientation(Matrix3d orientationToPack, int imuIndex)
      {
         orientationToPack.setM00(r_imu_m00);
         orientationToPack.setM01(r_imu_m01);
         orientationToPack.setM02(r_imu_m02);
         
         orientationToPack.setM10(r_imu_m10);
         orientationToPack.setM11(r_imu_m11);
         orientationToPack.setM12(r_imu_m12);
         
         orientationToPack.setM20(r_imu_m20);
         orientationToPack.setM21(r_imu_m21);
         orientationToPack.setM22(r_imu_m22);
      }

      public void packAcceleration(Vector3d accelerationToPack, int imuIndex)
      {
         accelerationToPack.set(r_imu_accel_x, r_imu_accel_y, r_imu_accel_z);
      }

      public void packAngularVelocity(Vector3d angularVelocityToPack, int imuIndex)
      {
         angularVelocityToPack.set(r_imu_gyro_x, r_imu_gyro_y, r_imu_gyro_z);
      }

      public void packCompass(Vector3d compassToPack, int imuIndex)
      {
         compassToPack.set(r_imu_compass_x, r_imu_compass_y, r_imu_compass_z);
      }
   }

   class FullRobotModel
   {
      private final RigidBody elevator;
      private final RigidBody body;

      private final SixDoFJoint rootJoint;
      private final ReferenceFrame worldFrame;
      
      private final double Ixx = 1.0; //TODO Random
      private final double Iyy = 2.0;
      private final double Izz = 3.0;
      private final double mass = 4.0;
      private Vector3d comOffset = new Vector3d(0.0, 0.0, 0.0);
      
      private final ReferenceFrame elevatorFrame;
      private final ReferenceFrame bodyFrame;
      
      private final FrameVector linearAccelerationInBodyFrame = new FrameVector((ReferenceFrame) null);

      public FullRobotModel()
      {
         worldFrame = ReferenceFrame.getWorldFrame();
         elevatorFrame = ReferenceFrame.constructFrameWithUnchangingTransformToParent("elevator", worldFrame, new Transform3D());

         elevator = new RigidBody("elevator", elevatorFrame);

         rootJoint = new SixDoFJoint("rootJoint", elevator, elevatorFrame);
     
         body = addRigidBody("body", rootJoint, Ixx, Iyy, Izz, mass, comOffset);
         
         bodyFrame = rootJoint.getFrameAfterJoint();
      }

      public void update(Transform3D transformBodyToWorld, FrameVector linearVelocity, FrameVector angularVelocity, FrameVector linearAccelerationInWorldFrame, FrameVector angularAcceleration)
      {
         // Update Body Pose
         rootJoint.setPositionAndRotation(transformBodyToWorld); // TODO correct???
         updateFrames();
         
         // Update Body Velocity
         Twist bodyTwist = new Twist(bodyFrame, elevatorFrame, bodyFrame, linearVelocity.getVector(), angularVelocity.getVector());
         rootJoint.setJointTwist(bodyTwist);
         
         // Update Body Acceleration
         linearAccelerationInBodyFrame.setAndChangeFrame(linearAccelerationInWorldFrame);
         linearAccelerationInBodyFrame.changeFrame(bodyFrame);
   
         SpatialAccelerationVector accelerationOfChestWithRespectToWorld = new SpatialAccelerationVector(bodyFrame, worldFrame, bodyFrame, linearAccelerationInBodyFrame.getVector(), angularAcceleration.getVector());
         accelerationOfChestWithRespectToWorld.changeBaseFrameNoRelativeAcceleration(getElevatorFrame());
         rootJoint.setAcceleration(accelerationOfChestWithRespectToWorld);
         
         updateFrames();
      }
      
      public void updateFrames()
      {
         elevator.updateFramesRecursively();
      }
      
      public RigidBody getBodyLink()
      {
         return body;
      }

      public ReferenceFrame getWorldFrame()
      {
         return worldFrame;
      }
      
      public ReferenceFrame getBodyFrame()
      {
         return bodyFrame;
      }

      public ReferenceFrame getElevatorFrame()
      {
         return elevator.getBodyFixedFrame();
      }

      public RigidBody getElevator()
      {
         return elevator;
      }
      
      public FrameVector getReferenceFrameTransInWorldFrame(ReferenceFrame frame) {
         Vector3d trans = new Vector3d();
         frame.getTransformToDesiredFrame(worldFrame).get(trans);
         FrameVector ret = new FrameVector(worldFrame, trans);
         return ret;
      }
 
      private RigidBody addRigidBody(String name, InverseDynamicsJoint parentJoint, double Ixx, double Iyy, double Izz, double mass, Vector3d centerOfMassOffset)
      {
         String comFrameName = name + "CoM";
         ReferenceFrame comFrame = createOffsetFrame(parentJoint, centerOfMassOffset, comFrameName);
         RigidBodyInertia inertia = new RigidBodyInertia(comFrame, Ixx, Iyy, Izz, mass);
         RigidBody ret = new RigidBody(name, inertia, parentJoint);
         return ret;
      }

      private ReferenceFrame createOffsetFrame(InverseDynamicsJoint previousJoint, Vector3d offset, String frameName)
      {
         ReferenceFrame parentFrame = previousJoint.getFrameAfterJoint();
         Transform3D transformToParent = new Transform3D();
         transformToParent.set(offset);
         ReferenceFrame beforeJointFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent(frameName, parentFrame, transformToParent);
         return beforeJointFrame;
      }
   }
}
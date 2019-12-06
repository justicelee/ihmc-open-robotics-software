package us.ihmc.humanoidBehaviors.ui.mapping;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import controller_msgs.msg.dds.StereoVisionPointCloudMessage;
import gnu.trove.list.array.TDoubleArrayList;
import javafx.scene.paint.Color;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.transform.interfaces.RigidBodyTransformReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.jOctoMap.ocTree.NormalOcTree;
import us.ihmc.robotEnvironmentAwareness.geometry.ConcaveHullFactoryParameters;
import us.ihmc.robotEnvironmentAwareness.hardware.StereoVisionPointCloudDataLoader;
import us.ihmc.robotEnvironmentAwareness.planarRegion.CustomRegionMergeParameters;
import us.ihmc.robotEnvironmentAwareness.planarRegion.PlanarRegionPolygonizer;
import us.ihmc.robotEnvironmentAwareness.planarRegion.PlanarRegionSegmentationRawData;
import us.ihmc.robotEnvironmentAwareness.planarRegion.PolygonizerParameters;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.numericalMethods.GradientDescentModule;

public class IhmcSLAMTest
{
   @Test
   public void testViewer()
   {
      IhmcSLAMViewer viewer = new IhmcSLAMViewer();
      String stereoPath = "E:\\Data\\SimpleArea3\\PointCloud\\";
      List<StereoVisionPointCloudMessage> messagesFromFile = StereoVisionPointCloudDataLoader.getMessagesFromFile(new File(stereoPath));

      viewer.addStereoMessage(messagesFromFile.get(20), Color.GREEN, Color.GREEN);
      viewer.addStereoMessage(messagesFromFile.get(25), Color.BLUE, Color.BLUE);
      viewer.start("testViewer");

      ThreadTools.sleepForever();
   }

   @Test
   public void testSimulatedPointCloudFrameForStair()
   {
      double stairHeight = 0.3;
      double stairWidth = 0.5;
      double stairLength = 0.25;

      StereoVisionPointCloudMessage message = SimulatedStereoVisionPointCloudMessageLibrary.generateMessageSimpleStair(stairHeight, stairWidth, stairLength);
      IhmcSLAMViewer viewer = new IhmcSLAMViewer();
      viewer.addStereoMessage(message);
      viewer.start("testSimulatedPointCloudFrameForStair");

      ThreadTools.sleepForever();
   }

   @Test
   public void testSimulatedContinuousFramesForStair()
   {
      double movingForward = 0.05;

      double stairHeight = 0.3;
      double stairWidth = 0.5;
      double stairLength = 0.25;

      StereoVisionPointCloudMessage messageOne = SimulatedStereoVisionPointCloudMessageLibrary.generateMessageSimpleStair(stairHeight, stairWidth, stairLength,
                                                                                                                          stairLength);

      double translationX = movingForward / 2;
      double translationY = 0.0;
      double translationZ = 0.0;
      double rotateY = Math.toRadians(1.0);
      RigidBodyTransform preMultiplier = new RigidBodyTransform();
      preMultiplier.setTranslation(translationX, translationY, translationZ);
      preMultiplier.appendPitchRotation(rotateY);
      StereoVisionPointCloudMessage messageTwo = SimulatedStereoVisionPointCloudMessageLibrary.generateMessageSimpleStair(preMultiplier, stairHeight,
                                                                                                                          stairWidth,
                                                                                                                          stairLength - movingForward,
                                                                                                                          stairLength + movingForward, true);
      IhmcSLAMViewer viewer = new IhmcSLAMViewer();
      viewer.addStereoMessage(messageOne, Color.BLUE);
      viewer.addStereoMessage(messageTwo, Color.GREEN);
      viewer.start("testSimulatedContinuousFramesForStair");

      ThreadTools.sleepForever();
   }

   @Test
   public void testSimulatedBadFrame()
   {
      double stairHeight = 0.3;
      double stairWidth = 0.5;
      double stairLength = 0.25;

      StereoVisionPointCloudMessage messageOne = SimulatedStereoVisionPointCloudMessageLibrary.generateMessageSimpleStair(stairHeight, stairWidth, stairLength);

      double translationX = -0.02;
      double translationY = 0.03;
      double translationZ = 0.0;
      double rotateY = Math.toRadians(3.0);
      RigidBodyTransform preMultiplier = new RigidBodyTransform();
      preMultiplier.setTranslation(translationX, translationY, translationZ);
      preMultiplier.appendPitchRotation(rotateY);
      StereoVisionPointCloudMessage messageTwo = SimulatedStereoVisionPointCloudMessageLibrary.generateMessageSimpleStair(preMultiplier, stairHeight,
                                                                                                                          stairWidth, stairLength);

      IhmcSLAMViewer viewer = new IhmcSLAMViewer();
      viewer.addStereoMessage(messageOne, Color.BLUE);
      viewer.addStereoMessage(messageTwo, Color.GREEN);
      viewer.start("testSimulatedBadFrame");

      ThreadTools.sleepForever();
   }

   @Test
   public void testPlanarRegionsForSimulatedPointCloudFrame()
   {
      double stairHeight = 0.3;
      double stairWidth = 0.5;
      double stairLength = 0.25;

      double octreeResolution = 0.02;
      ConcaveHullFactoryParameters concaveHullFactoryParameters = new ConcaveHullFactoryParameters();
      PolygonizerParameters polygonizerParameters = new PolygonizerParameters();

      StereoVisionPointCloudMessage message = SimulatedStereoVisionPointCloudMessageLibrary.generateMessageSimpleStair(stairHeight, stairWidth, stairLength);
      Point3DReadOnly[] pointCloud = IhmcSLAMTools.extractPointsFromMessage(message);
      RigidBodyTransformReadOnly sensorPose = IhmcSLAMTools.extractSensorPoseFromMessage(message);

      List<PlanarRegionSegmentationRawData> rawData = IhmcSLAMTools.computePlanarRegionRawData(pointCloud, sensorPose.getTranslation(), octreeResolution);
      PlanarRegionsList planarRegionsMap = PlanarRegionPolygonizer.createPlanarRegionsList(rawData, concaveHullFactoryParameters, polygonizerParameters);

      IhmcSLAMViewer viewer = new IhmcSLAMViewer();
      viewer.addPlanarRegions(planarRegionsMap);
      viewer.start("testPlanarRegionsForSimulatedPointCloudFrame");

      ThreadTools.sleepForever();
   }

   @Test
   public void testPlanarRegionsForFlatGround()
   {
      double groundWidth = 1.5;
      double stairLength = 0.3;
      int numberOfMessages = 10;

      double octreeResolution = 0.02;
      ConcaveHullFactoryParameters concaveHullFactoryParameters = new ConcaveHullFactoryParameters();
      PolygonizerParameters polygonizerParameters = new PolygonizerParameters();
      CustomRegionMergeParameters customRegionMergeParameters = new CustomRegionMergeParameters();
      List<Point3DReadOnly[]> pointCloudMap = new ArrayList<>();

      StereoVisionPointCloudMessage firstMessage = SimulatedStereoVisionPointCloudMessageLibrary.generateMessageSimpleStair(0.0, groundWidth, stairLength,
                                                                                                                            stairLength, false);
      Point3DReadOnly[] firstPointCloud = IhmcSLAMTools.extractPointsFromMessage(firstMessage);
      RigidBodyTransformReadOnly firstSensorPose = IhmcSLAMTools.extractSensorPoseFromMessage(firstMessage);
      pointCloudMap.add(firstPointCloud);

      List<PlanarRegionSegmentationRawData> firstRawData = IhmcSLAMTools.computePlanarRegionRawData(firstPointCloud, firstSensorPose.getTranslation(),
                                                                                                    octreeResolution);
      PlanarRegionsList planarRegionsMap = PlanarRegionPolygonizer.createPlanarRegionsList(firstRawData, concaveHullFactoryParameters, polygonizerParameters);

      for (int i = 1; i < numberOfMessages; i++)
      {
         RigidBodyTransform preMultiplier = new RigidBodyTransform();
         preMultiplier.setTranslation(i * stairLength, 0.0, 0.0);
         StereoVisionPointCloudMessage message = SimulatedStereoVisionPointCloudMessageLibrary.generateMessageSimpleStair(preMultiplier, 0.0, groundWidth,
                                                                                                                          stairLength, stairLength, false);

         Point3DReadOnly[] pointCloud = IhmcSLAMTools.extractPointsFromMessage(message);
         RigidBodyTransformReadOnly sensorPose = IhmcSLAMTools.extractSensorPoseFromMessage(message);
         pointCloudMap.add(pointCloud);

         List<PlanarRegionSegmentationRawData> rawData = IhmcSLAMTools.computePlanarRegionRawData(pointCloud, sensorPose.getTranslation(), octreeResolution);

         planarRegionsMap = EnvironmentMappingTools.buildNewMap(rawData, planarRegionsMap, customRegionMergeParameters, concaveHullFactoryParameters,
                                                                polygonizerParameters);
      }

      IhmcSLAMViewer viewer = new IhmcSLAMViewer();
      viewer.addPlanarRegions(planarRegionsMap);
      for (int i = 0; i < pointCloudMap.size(); i++)
      {
         int redScaler = (int) (0xFF * (1 - (double) i / pointCloudMap.size()));
         int blueScaler = (int) (0xFF * ((double) i / pointCloudMap.size()));
         Color color = Color.rgb(redScaler, 0, blueScaler);
         viewer.addPointCloud(pointCloudMap.get(i), color);
      }

      viewer.start("testPlanarRegionsForFlatGround");

      ThreadTools.sleepForever();
   }

   @Test
   public void testInverseInterpolation()
   {
      double movingForward = 0.1;
      double fixedHeight = 1.0;

      double sensorPitchAngle = Math.toRadians(90.0 + 70.0);
      double stairHeight = 0.3;
      double stairWidth = 0.5;
      double stairLength = 0.25;

      double octreeResolution = 0.02;

      RigidBodyTransform sensorPoseOne = new RigidBodyTransform();
      sensorPoseOne.setTranslation(0.0, 0.0, fixedHeight);
      sensorPoseOne.appendPitchRotation(sensorPitchAngle);
      StereoVisionPointCloudMessage messageOne = SimulatedStereoVisionPointCloudMessageLibrary.generateMessageSimpleStair(sensorPoseOne,
                                                                                                                          new RigidBodyTransform(), stairHeight,
                                                                                                                          stairWidth, stairLength, stairLength,
                                                                                                                          false);

      double translationX = movingForward / 2;
      double translationY = 0.0;
      double translationZ = 0.0;
      double rotateY = Math.toRadians(0.0);
      RigidBodyTransform preMultiplier = new RigidBodyTransform();
      preMultiplier.setTranslation(translationX, translationY, translationZ);
      preMultiplier.appendPitchRotation(rotateY);

      RigidBodyTransform sensorPoseTwo = new RigidBodyTransform();
      sensorPoseTwo.setTranslation(movingForward, 0.0, fixedHeight);
      sensorPoseTwo.appendPitchRotation(sensorPitchAngle);

      RigidBodyTransform randomTransformer = createRandomDriftedTransform(new Random(0612L), 0.05, 5.0);
      preMultiplier.multiply(randomTransformer);
      sensorPoseTwo.multiply(randomTransformer);

      StereoVisionPointCloudMessage driftedMessageTwo = SimulatedStereoVisionPointCloudMessageLibrary.generateMessageSimpleStair(sensorPoseTwo, preMultiplier,
                                                                                                                                 stairHeight, stairWidth,
                                                                                                                                 stairLength - movingForward,
                                                                                                                                 stairLength + movingForward,
                                                                                                                                 false);

      IhmcSLAMFrame frameOne = new IhmcSLAMFrame(messageOne);
      IhmcSLAMFrame driftedFrameTwo = new IhmcSLAMFrame(frameOne, driftedMessageTwo);
      NormalOcTree overlappedOctreeNode = driftedFrameTwo.computeOctreeInPreviousView(octreeResolution);

      IhmcSLAMViewer viewer = new IhmcSLAMViewer();
      viewer.addSensorPose(frameOne.getSensorPose(), Color.BLUE);
      viewer.addSensorPose(driftedFrameTwo.getSensorPose(), Color.YELLOW);
      viewer.addPointCloud(frameOne.getPointCloud(), Color.BLUE);
      viewer.addPointCloud(driftedFrameTwo.getPointCloud(), Color.YELLOW);
      viewer.addOctree(overlappedOctreeNode, Color.GREEN, octreeResolution);
      viewer.start("testInverseInterpolation");

      ThreadTools.sleepForever();
   }

   @Test
   public void testDetectingSimilarPlanarRegions()
   {
      double movingForward = 0.1;
      double fixedHeight = 1.0;

      double sensorPitchAngle = Math.toRadians(90.0 + 70.0);
      double stairHeight = 0.3;
      double stairWidth = 0.5;
      double stairLength = 0.25;

      double octreeResolution = 0.02;
      ConcaveHullFactoryParameters concaveHullFactoryParameters = new ConcaveHullFactoryParameters();
      PolygonizerParameters polygonizerParameters = new PolygonizerParameters();

      RigidBodyTransform sensorPoseOne = new RigidBodyTransform();
      sensorPoseOne.setTranslation(0.0, 0.0, fixedHeight);
      sensorPoseOne.appendPitchRotation(sensorPitchAngle);
      StereoVisionPointCloudMessage messageOne = SimulatedStereoVisionPointCloudMessageLibrary.generateMessageSimpleStair(sensorPoseOne,
                                                                                                                          new RigidBodyTransform(), stairHeight,
                                                                                                                          stairWidth, stairLength, stairLength,
                                                                                                                          true);

      double translationX = movingForward / 2;
      double translationY = 0.0;
      double translationZ = 0.0;
      double rotateY = Math.toRadians(0.0);
      RigidBodyTransform preMultiplier = new RigidBodyTransform();
      preMultiplier.setTranslation(translationX, translationY, translationZ);
      preMultiplier.appendPitchRotation(rotateY);

      RigidBodyTransform sensorPoseTwo = new RigidBodyTransform();
      sensorPoseTwo.setTranslation(movingForward, 0.0, fixedHeight);
      sensorPoseTwo.appendPitchRotation(sensorPitchAngle);

      RigidBodyTransform randomTransformer = createRandomDriftedTransform(new Random(0612L), 0.15, 15.0);
      preMultiplier.multiply(randomTransformer);
      sensorPoseTwo.multiply(randomTransformer);

      StereoVisionPointCloudMessage driftedMessageTwo = SimulatedStereoVisionPointCloudMessageLibrary.generateMessageSimpleStair(sensorPoseTwo, preMultiplier,
                                                                                                                                 stairHeight, stairWidth,
                                                                                                                                 stairLength - movingForward,
                                                                                                                                 stairLength + movingForward,
                                                                                                                                 false);

      IhmcSLAMFrame frameOne = new IhmcSLAMFrame(messageOne);
      IhmcSLAMFrame driftedFrameTwo = new IhmcSLAMFrame(frameOne, driftedMessageTwo);

      List<PlanarRegionSegmentationRawData> rawData = IhmcSLAMTools.computePlanarRegionRawData(frameOne.getPointCloud(),
                                                                                               frameOne.getSensorPose().getTranslation(), octreeResolution);
      PlanarRegionsList planarRegionsMap = PlanarRegionPolygonizer.createPlanarRegionsList(rawData, concaveHullFactoryParameters, polygonizerParameters);
      System.out.println("planarRegionsMap " + planarRegionsMap.getNumberOfPlanarRegions());

      double validRatio = 0.0;
      double maximumDistance = 0.1;
      double maximumAngle = Math.toRadians(30.0);
      List<IhmcSurfaceElement> surfaceElements = IhmcSLAMTools.computeMergeableSurfaceElements(planarRegionsMap, driftedFrameTwo, octreeResolution, validRatio,
                                                                                               maximumDistance, maximumAngle);

      List<IhmcSurfaceElement> groupOne = new ArrayList<>();
      List<IhmcSurfaceElement> groupTwo = new ArrayList<>();
      for (IhmcSurfaceElement element : surfaceElements)
      {
         if (element.getMergeablePlanarRegionId() == planarRegionsMap.getPlanarRegion(0).getRegionId())
            groupOne.add(element);
         if (element.getMergeablePlanarRegionId() == planarRegionsMap.getPlanarRegion(1).getRegionId())
            groupTwo.add(element);
      }

      IhmcSLAMViewer viewer = new IhmcSLAMViewer();
      viewer.addSensorPose(frameOne.getSensorPose(), Color.BLUE);
      viewer.addSensorPose(driftedFrameTwo.getSensorPose(), Color.YELLOW);
      viewer.addPointCloud(frameOne.getPointCloud(), Color.BLUE);
      viewer.addPointCloud(driftedFrameTwo.getPointCloud(), Color.YELLOW);
      viewer.addOctree(groupOne, Color.CORAL);
      viewer.addOctree(groupTwo, Color.BLACK);

      viewer.start("testDetectingSimilarPlanarRegions");

      ThreadTools.sleepForever();
   }

   @Test
   public void testOptimization()
   {
      double movingForward = 0.1;
      double fixedHeight = 1.0;

      double sensorPitchAngle = Math.toRadians(90.0 + 70.0);
      double stairHeight = 0.3;
      double stairWidth = 0.5;
      double stairLength = 0.25;

      double octreeResolution = 0.02;
      ConcaveHullFactoryParameters concaveHullFactoryParameters = new ConcaveHullFactoryParameters();
      PolygonizerParameters polygonizerParameters = new PolygonizerParameters();

      RigidBodyTransform sensorPoseOne = new RigidBodyTransform();
      sensorPoseOne.setTranslation(0.0, 0.0, fixedHeight);
      sensorPoseOne.appendPitchRotation(sensorPitchAngle);
      StereoVisionPointCloudMessage messageOne = SimulatedStereoVisionPointCloudMessageLibrary.generateMessageSimpleStair(sensorPoseOne,
                                                                                                                          new RigidBodyTransform(), stairHeight,
                                                                                                                          stairWidth, stairLength, stairLength,
                                                                                                                          true);

      double translationX = movingForward / 2;
      double translationY = 0.0;
      double translationZ = 0.0;
      double rotateY = Math.toRadians(0.0);
      RigidBodyTransform preMultiplier = new RigidBodyTransform();
      preMultiplier.setTranslation(translationX, translationY, translationZ);
      preMultiplier.appendPitchRotation(rotateY);

      RigidBodyTransform sensorPoseTwo = new RigidBodyTransform();
      sensorPoseTwo.setTranslation(movingForward, 0.0, fixedHeight);
      sensorPoseTwo.appendPitchRotation(sensorPitchAngle);

      RigidBodyTransform randomTransformer = createRandomDriftedTransform(new Random(0612L), 0.05, 5.0);
//      RigidBodyTransform randomTransformer = new RigidBodyTransform();
//      randomTransformer.setTranslation(0.02, 0.05, 0.03);
//      randomTransformer.appendRollRotation(Math.toRadians(3.0));
      preMultiplier.multiply(randomTransformer);
      sensorPoseTwo.multiply(randomTransformer);

      StereoVisionPointCloudMessage driftedMessageTwo = SimulatedStereoVisionPointCloudMessageLibrary.generateMessageSimpleStair(sensorPoseTwo, preMultiplier,
                                                                                                                                 stairHeight, stairWidth,
                                                                                                                                 stairLength - movingForward,
                                                                                                                                 stairLength + movingForward,
                                                                                                                                 false);

      IhmcSLAMFrame frameOne = new IhmcSLAMFrame(messageOne);
      IhmcSLAMFrame driftedFrameTwo = new IhmcSLAMFrame(frameOne, driftedMessageTwo);

      List<PlanarRegionSegmentationRawData> rawData = IhmcSLAMTools.computePlanarRegionRawData(frameOne.getPointCloud(),
                                                                                               frameOne.getSensorPose().getTranslation(), octreeResolution);
      PlanarRegionsList planarRegionsMap = PlanarRegionPolygonizer.createPlanarRegionsList(rawData, concaveHullFactoryParameters, polygonizerParameters);

      double validRatio = 0.0;
      double maximumDistance = 0.1;
      double maximumAngle = Math.toRadians(30.0);

      List<IhmcSurfaceElement> surfaceElements = IhmcSLAMTools.computeMergeableSurfaceElements(planarRegionsMap, driftedFrameTwo, octreeResolution, validRatio,
                                                                                               maximumDistance, maximumAngle);

      IhmcSLAMViewer viewer = new IhmcSLAMViewer();
      viewer.addSensorPose(frameOne.getSensorPose(), Color.BLUE);
      viewer.addSensorPose(driftedFrameTwo.getSensorPose(), Color.YELLOW);
      viewer.addPointCloud(frameOne.getPointCloud(), Color.BLUE);
      viewer.addPointCloud(driftedFrameTwo.getPointCloud(), Color.YELLOW);
      viewer.addOctree(surfaceElements, Color.CORAL);
      viewer.start("testOptimization");

      IhmcSLAMViewer localViewer = new IhmcSLAMViewer();
      localViewer.addPointCloud(frameOne.getPointCloud(), Color.RED);
      localViewer.addPointCloud(driftedFrameTwo.getPointCloud(), Color.YELLOW);
      localViewer.addOctree(surfaceElements, Color.CORAL);
      localViewer.addPlanarRegions(planarRegionsMap);
      PreMultiplierOptimizerCostFunction function = new PreMultiplierOptimizerCostFunction(surfaceElements, driftedFrameTwo.getInitialSensorPoseToWorld());
      GradientDescentModule optimizer = new GradientDescentModule(function, IhmcSLAM.initialQuery);

      int maxIterations = 200;
      double convergenceThreshold = 10E-5;
      double optimizerStepSize = -0.1;
      double optimizerPerturbationSize = 0.0001;

      optimizer.setInputLowerLimit(IhmcSLAM.lowerLimit);
      optimizer.setInputUpperLimit(IhmcSLAM.upperLimit);
      optimizer.setMaximumIterations(maxIterations);
      optimizer.setConvergenceThreshold(convergenceThreshold);
      optimizer.setStepSize(optimizerStepSize);
      optimizer.setPerturbationSize(optimizerPerturbationSize);
      optimizer.setReducingStepSizeRatio(2);

      int run = optimizer.run();
      System.out.println(run + " " + function.getQuery(IhmcSLAM.initialQuery) + " " + optimizer.getOptimalQuery());
      TDoubleArrayList optimalInput = optimizer.getOptimalInput();
      System.out.println(optimalInput.get(0) + " " + optimalInput.get(1) + " " + optimalInput.get(2) + " " + optimalInput.get(3));

      RigidBodyTransform slamTransformer = new RigidBodyTransform();
      function.convertToSensorPoseMultiplier(optimalInput, slamTransformer);
      System.out.println("slam transformer");
      System.out.println(slamTransformer);
      
      System.out.println("randomTransformer");
      System.out.println(randomTransformer);

      driftedFrameTwo.updateSLAM(slamTransformer);
      
      System.out.println("Test Result : " + slamTransformer.geometricallyEquals(randomTransformer, 0.05));
      System.out.println("Position Diff    : " + slamTransformer.getTranslation().geometricallyEquals(randomTransformer.getTranslation(), 0.05));
      System.out.println("Orientation Diff : " + Math.toRadians(slamTransformer.getRotation().distance(randomTransformer.getRotation()))+" deg.");

      Color color = Color.rgb(0, 255, 0);
      localViewer.addPointCloud(driftedFrameTwo.getPointCloud(), color);
      localViewer.start("iteration ");
      
      ThreadTools.sleepForever();
   }

   @Test
   public void testHeightChangeBasedFlatGroundDetection()
   {

   }

   @Test
   public void testEndToEnd()
   {

   }

   private RigidBodyTransform createRandomDriftedTransform(Random random, double positionBound, double angleBoundDegree)
   {
      RigidBodyTransform randomTransform = new RigidBodyTransform();
      int positionAxis = random.nextInt(2);
      int angleAxis = random.nextInt(2);

      double randomAngle = 2 * Math.toRadians(angleBoundDegree) * (random.nextDouble() - 0.5);
      if (angleAxis == 0)
         randomTransform.appendRollRotation(randomAngle);
      if (angleAxis == 1)
         randomTransform.appendPitchRotation(randomAngle);
      else
         randomTransform.appendYawRotation(randomAngle);

      double randomTranslation = 2 * positionBound * (random.nextDouble() - 0.5);
      if (positionAxis == 0)
         randomTransform.appendTranslation(randomTranslation, 0.0, 0.0);
      if (positionAxis == 1)
         randomTransform.appendTranslation(0.0, randomTranslation, 0.0);
      else
         randomTransform.appendTranslation(0.0, 0.0, randomTranslation);

      System.out.println("positionAxis " + positionAxis + " " + randomTranslation);
      System.out.println("angleAxis " + angleAxis + " " + Math.toDegrees(randomAngle));

      return randomTransform;
   }
}
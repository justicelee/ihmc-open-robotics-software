
package us.ihmc.utilities.ros;

import static us.ihmc.robotics.Assert.*;

import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import sensor_msgs.PointCloud2;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.graphicsDescription.color.MutableColor;
import us.ihmc.utilities.ros.publisher.RosPointCloudPublisher;
import us.ihmc.utilities.ros.subscriber.RosPointCloudSubscriber;
import us.ihmc.utilities.ros.types.PointType;

@Disabled
public class RosPointCloudPublisherSubscriberTest extends IHMCRosTestWithRosCore
{
   @Test
   public void testPubSubSinglePointXYZICloud() throws URISyntaxException, InterruptedException
   {
      testPubSubSingleCloud(PointType.XYZI);
   }

   @Test
   public void testPubSubSinglePointXYZRGBCloud() throws URISyntaxException, InterruptedException
   {
      testPubSubSingleCloud(PointType.XYZRGB);
   }

   private void testPubSubSingleCloud(final PointType testPointType) throws URISyntaxException, InterruptedException
   {
      RosMainNode rosMainNode = new RosMainNode(rosMasterURI, "topicClientTestNode");

      // Test Data
      String testTopic = "/cloudTest";
      final String testFrameId = "/testFrame";
      final Point3D[] testPoints = new Point3D[] {new Point3D(1.0, 2.0, 3.0)};
      final float[] testIntensities = new float[] {1.0f};
      final MutableColor[] testColor = new MutableColor[] {new MutableColor(1.0f, 2.0f, 3.0f)};


      // setup publisher
      final RosPointCloudPublisher publisher = new RosPointCloudPublisher(testPointType, true);
      rosMainNode.attachPublisher(testTopic, publisher);

      // setup subscriber
      final CountDownLatch latch = new CountDownLatch(1);
      RosPointCloudSubscriber subscriber = new RosPointCloudSubscriber()
      {
         @Override
         public void onNewMessage(PointCloud2 pointCloud)
         {
            UnpackedPointCloud unpackedCloud = super.unpackPointsAndIntensities(pointCloud);
            assertEquals(testPointType, unpackedCloud.getPointType());
            assertArrayEquals(testPoints, unpackedCloud.getPoints());

            switch (unpackedCloud.getPointType())
            {
               case XYZI :
                  assertArrayEquals(testIntensities, unpackedCloud.getIntensities(), 1e-10f);

                  break;

               case XYZRGB :
                  assertArrayEquals(testColor, unpackedCloud.getPointColors());
                  break;
            }

            assertEquals(testFrameId, pointCloud.getHeader().getFrameId());
            latch.countDown();
         }
      };
      rosMainNode.attachSubscriber(testTopic, subscriber);

      // go
      rosMainNode.execute();
      subscriber.wailTillRegistered();
      publisher.waitTillRegistered();

      switch (testPointType)
      {
         case XYZI :
            publisher.publish(testPoints, testIntensities, testFrameId);

            break;

         case XYZRGB :
            publisher.publish(testPoints, testColor, testFrameId);
      }

      assertTrue(latch.await(2, TimeUnit.SECONDS));
   }
}

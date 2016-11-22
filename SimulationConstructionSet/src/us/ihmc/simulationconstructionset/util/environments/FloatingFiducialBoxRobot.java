package us.ihmc.simulationconstructionset.util.environments;

import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DDescription.Graphics3DObject;
import us.ihmc.graphics3DDescription.appearance.AppearanceDefinition;
import us.ihmc.graphics3DDescription.appearance.YoAppearance;
import us.ihmc.simulationconstructionset.FloatingJoint;
import us.ihmc.simulationconstructionset.Link;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.util.environments.FiducialsFlatGroundEnvironment.Fiducial;

public class FloatingFiducialBoxRobot extends Robot
{
   private final FloatingJoint qrCodeJoint;

   public FloatingFiducialBoxRobot(Fiducial fiducial)
   {
      super("FloatingFiducialBoxRobot");

      qrCodeJoint = new FloatingJoint("qrCode", "qrCode", new Vector3d(), this);
      Link qrCodeLink = new Link("qrCode");
      qrCodeLink.setMassAndRadiiOfGyration(1.0, 0.1, 0.1, 0.1);
      Graphics3DObject qrCodeLinkGraphics = new Graphics3DObject();
      //      qrCodeLinkGraphics.addCoordinateSystem(2.0);
      double cubeLength = 1.0;
      qrCodeLinkGraphics.translate(0.0, 0.0, -0.99 * cubeLength);
      AppearanceDefinition cubeAppearance = YoAppearance.Texture(fiducial.getPathString());
      qrCodeLinkGraphics.addCube(cubeLength * 0.98, cubeLength * 1.01, cubeLength * 0.98, YoAppearance.Yellow());

      boolean[] textureFaces = new boolean[] { true, true, false, false, false, false };
      qrCodeLinkGraphics.translate(0.0, 0.0, -0.01 * cubeLength);
      qrCodeLinkGraphics.addCube(cubeLength, cubeLength, cubeLength, cubeAppearance, textureFaces);

      qrCodeLink.setLinkGraphics(qrCodeLinkGraphics);
      qrCodeJoint.setLink(qrCodeLink);
      addRootJoint(qrCodeJoint);
      setGravity(0.0);
   }

   public void setPosition(Tuple3d position)
   {
      qrCodeJoint.setPosition(position);
   }

   public void setPosition(double x, double y, double z)
   {
      qrCodeJoint.setPosition(x, y, z);
   }

   public void setYawPitchRoll(double yaw, double pitch, double roll)
   {
      qrCodeJoint.setYawPitchRoll(yaw, pitch, roll);
   }

   public void setAngularVelocity(Vector3d angularVelocityInBody)
   {
      qrCodeJoint.setAngularVelocityInBody(angularVelocityInBody);
   }

   public void setLinearVelocity(Vector3d linearVelocityInWorld)
   {
      qrCodeJoint.setVelocity(linearVelocityInWorld);
   }
}

package us.ihmc.robotics.sensors;

import org.ejml.data.DMatrixRMaj;

import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.matrixlib.MatrixTools;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.mecano.spatial.Wrench;
import us.ihmc.mecano.spatial.interfaces.SpatialVectorReadOnly;
import us.ihmc.mecano.spatial.interfaces.WrenchReadOnly;
import us.ihmc.robotics.screwTheory.GenericCRC32;

public class ForceSensorData implements ForceSensorDataReadOnly, Settable<ForceSensorData>
{
   private final DMatrixRMaj wrench = new DMatrixRMaj(Wrench.SIZE, 1);

   private String sensorName;
   private ReferenceFrame measurementFrame;
   private RigidBodyBasics measurementLink;

   public ForceSensorData()
   {
   }

   public ForceSensorData(ForceSensorDefinition forceSensorDefinition)
   {
      setDefinition(forceSensorDefinition);
   }

   public void setDefinition(ForceSensorDefinition forceSensorDefinition)
   {
      setDefinition(forceSensorDefinition.getSensorName(), forceSensorDefinition.getSensorFrame(), forceSensorDefinition.getRigidBody());
   }

   public void setDefinition(String sensorName, ReferenceFrame measurementFrame, RigidBodyBasics measurementLink)
   {
      this.sensorName = sensorName;
      this.measurementFrame = measurementFrame;
      this.measurementLink = measurementLink;
   }

   public void setWrench(Vector3DReadOnly moment, Vector3DReadOnly force)
   {
      moment.get(0, wrench);
      force.get(3, wrench);
   }

   public void setWrench(DMatrixRMaj newWrench)
   {
      wrench.set(newWrench);
   }

   public void setWrench(float[] newWrench)
   {
      for (int i = 0; i < SpatialVectorReadOnly.SIZE; i++)
      {
         wrench.set(i, 0, newWrench[i]);
      }
   }

   public void setWrench(WrenchReadOnly newWrench)
   {
      measurementFrame.checkReferenceFrameMatch(newWrench.getReferenceFrame());
      measurementFrame.checkReferenceFrameMatch(newWrench.getBodyFrame());
      newWrench.get(wrench);
   }

   @Override
   public String getSensorName()
   {
      return sensorName;
   }

   @Override
   public ReferenceFrame getMeasurementFrame()
   {
      return measurementFrame;
   }

   @Override
   public RigidBodyBasics getMeasurementLink()
   {
      return measurementLink;
   }

   @Override
   public void getWrench(DMatrixRMaj wrenchToPack)
   {
      wrenchToPack.set(wrench);
   }

   @Override
   public void getWrench(Wrench wrenchToPack)
   {
      wrenchToPack.setBodyFrame(measurementFrame);
      wrenchToPack.setIncludingFrame(measurementFrame, wrench);
   }

   @Override
   public void getWrench(Vector3DBasics momentToPack, Vector3DBasics forceToPack)
   {
      momentToPack.set(0, wrench);
      forceToPack.set(3, wrench);
   }

   @Override
   public void getWrench(float[] wrenchToPack)
   {
      for (int i = 0; i < SpatialVectorReadOnly.SIZE; i++)
      {
         wrenchToPack[i] = (float) wrench.get(i, 0);
      }
   }

   @Override
   public void set(ForceSensorData other)
   {
      set((ForceSensorDataReadOnly) other);
   }

   public void set(ForceSensorDataReadOnly other)
   {
      sensorName = other.getSensorName();
      measurementFrame = other.getMeasurementFrame();
      measurementLink = other.getMeasurementLink();
      other.getWrench(wrench);
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
      {
         return true;
      }
      else if (obj instanceof ForceSensorData)
      {
         ForceSensorData other = (ForceSensorData) obj;
         if (sensorName == null ? other.sensorName != null : !sensorName.equals(other.sensorName))
            return false;
         if (measurementFrame != other.measurementFrame)
            return false;
         if (measurementLink != other.measurementLink)
            return false;
         if (!MatrixTools.equals(wrench, other.wrench))
            return false;
         return true;
      }
      else
      {
         return false;
      }
   }

   public void calculateChecksum(GenericCRC32 checksum)
   {
      checksum.update(wrench);
   }
}

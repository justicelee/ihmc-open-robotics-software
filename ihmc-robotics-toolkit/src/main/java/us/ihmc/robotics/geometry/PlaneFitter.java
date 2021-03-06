package us.ihmc.robotics.geometry;

import java.util.List;

import us.ihmc.euclid.geometry.Plane3D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;

public interface PlaneFitter
{
   /**
    *
    * @param pointList The list of points to fit
    * @param planeToPack the plane to pack
    * @return the average error of the points to the plane fit
    */
   public abstract double fitPlaneToPoints(List<? extends Point3DReadOnly> pointList, Plane3D planeToPack);

   /**
    *
    * @param center origin the plane at the center if possible
    * @param pointList The list of points to fit
    * @param planeToPack the plane to pack
    * @return the average error of the points to the plane fit
    */
   public abstract double fitPlaneToPoints(Point2DBasics center, List<? extends Point3DReadOnly> pointList, Plane3D planeToPack);
}

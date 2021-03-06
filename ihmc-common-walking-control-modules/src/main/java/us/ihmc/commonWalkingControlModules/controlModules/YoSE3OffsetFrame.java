package us.ihmc.commonWalkingControlModules.controlModules;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.exceptions.ReferenceFrameMismatchException;
import us.ihmc.euclid.referenceFrame.interfaces.FrameQuaternionReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameTuple3DReadOnly;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.mecano.frames.MovingReferenceFrame;
import us.ihmc.mecano.spatial.Twist;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFrameQuaternion;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFrameVector3D;
import us.ihmc.yoVariables.registry.YoRegistry;

public class YoSE3OffsetFrame extends MovingReferenceFrame
{
   private final YoFrameVector3D translationToParent;
   private final YoFrameQuaternion rotationToParent;

   /**
    * Creates a new reference frame with a mutable offset to its parent frame.
    * <p>
    * The offset to the parent frame is backed by a {@link YoFrameVector3D} and a
    * {@link YoFrameQuaternion} internally.
    * </p>
    * 
    * @param frameName the name of this reference frame.
    * @param parentFrame the frame to which this is attached.
    * @param registry the registry to which the internal {@code YoVariable}s are registered.
    */
   public YoSE3OffsetFrame(String frameName, ReferenceFrame parentFrame, YoRegistry registry)
   {
      super(frameName, parentFrame);

      translationToParent = new YoFrameVector3D(frameName, parentFrame, registry);
      rotationToParent = new YoFrameQuaternion(frameName, parentFrame, registry);
   }

   /**
    * Sets the offset to be a pure translation.
    * <p>
    * This method resets the orientation offset.
    * </p>
    * 
    * @param translationToParent the position of this frame origin expressed in its parent frame.
    *           Not modified.
    * @throws ReferenceFrameMismatchException if the argument is not expressed in
    *            {@code this.getParent()}.
    */
   public void setOffsetToParentToTranslationOnly(FrameTuple3DReadOnly translationToParent)
   {
      this.translationToParent.set(translationToParent);
      this.rotationToParent.setToZero();
      update();
   }

   /**
    * Sets the offset of this frame to its parent.
    * 
    * @param translationToParent the position of this frame origin expressed in its parent frame.
    *           Not modified.
    * @param rotationToParent the orientation of this frame expressed in its parent frame. Not
    *           modified.
    * @throws ReferenceFrameMismatchException if any of the two arguments is not expressed in
    *            {@code this.getParent()}.
    */
   public void setOffsetToParent(FrameTuple3DReadOnly translationToParent, FrameQuaternionReadOnly rotationToParent)
   {
      this.translationToParent.set(translationToParent);
      this.rotationToParent.set(rotationToParent);
      update();
   }

   /**
    * Resets the offset such that this frame is located and aligned with its parent frame.
    */
   public void setToZero()
   {
      translationToParent.setToZero();
      rotationToParent.setToZero();
   }

   @Override
   protected void updateTransformToParent(RigidBodyTransform transformToParent)
   {
      transformToParent.set(rotationToParent, translationToParent);
   }

   @Override
   protected void updateTwistRelativeToParent(Twist twistRelativeToParentToPack)
   {
      twistRelativeToParentToPack.setToZero(this, getParent(), this);
   }
}

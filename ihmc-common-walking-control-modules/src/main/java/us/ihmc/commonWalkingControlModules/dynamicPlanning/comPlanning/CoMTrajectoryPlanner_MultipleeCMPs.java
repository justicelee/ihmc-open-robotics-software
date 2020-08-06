package us.ihmc.commonWalkingControlModules.dynamicPlanning.comPlanning;

import java.util.ArrayList;
import java.util.List;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import us.ihmc.commonWalkingControlModules.capturePoint.CapturePointTools;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.euclid.geometry.LineSegment3D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FixedFramePoint3DBasics;
import us.ihmc.euclid.referenceFrame.interfaces.FixedFrameVector3DBasics;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;
import us.ihmc.log.LogTools;
import us.ihmc.matrixlib.NativeCommonOps;
import us.ihmc.robotics.math.trajectories.Trajectory3D;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoFramePoint3D;
import us.ihmc.yoVariables.variable.YoFrameVector3D;

/**
 * <p>
 *    This is the main class of the trajectory-based CoM Trajectory Planner.
 * </p>
 * <p>
 *    This class assumes that the final phase is always the "stopping" phase, where the CoM is supposed to come to rest.
 *    This means that the final VRP is the terminal DCM location
 *  </p>
 *  <p>
 *     The CoM has the following definitions:
 *     <li>      x(t) = c<sub>0</sub> e<sup>&omega; t</sup> + c<sub>1</sub> e<sup>-&omega; t</sup> + c<sub>2</sub> t<sup>3</sup> + c<sub>3</sub> t<sup>2</sup> +
 *     c<sub>4</sub> t + c<sub>5</sub></li>
 *     <li> d/dt x(t) = &omega; c<sub>0</sub> e<sup>&omega; t</sup> - &omega; c<sub>1</sub> e<sup>-&omega; t</sup> + 3 c<sub>2</sub> t<sup>2</sup> +
 *     2 c<sub>3</sub> t+ c<sub>4</sub>
 *     <li> d<sup>2</sup> / dt<sup>2</sup> x(t) = &omega;<sup>2</sup> c<sub>0</sub> e<sup>&omega; t</sup> + &omega;<sup>2</sup> c<sub>1</sub> e<sup>-&omega; t</sup>
 *     + 6 c<sub>2</sub> t + 2 c<sub>3</sub>  </li>
 *  </p>
 *
 *
 *    <p> From this, it follows that the VRP has the trajectory
 *    <li> v(t) =  c<sub>2</sub> t<sup>3</sup> + c<sub>3</sub> t<sup>2</sup> + (c<sub>4</sub> - 6/&omega;<sup>2</sup> c<sub>2</sub>) t - 2/&omega; c<sub>3</sub> + c<sub>5</sub></li>
 *    </p>
 */
public class CoMTrajectoryPlanner_MultipleeCMPs implements CoMTrajectoryProvider
{
   private static boolean verbose = false;
   
   private static final int maxCapacity = 10;
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final DMatrixRMaj coefficientMultipliers = new DMatrixRMaj(0, 0);
   private final DMatrixRMaj coefficientMultipliersInv = new DMatrixRMaj(0, 0);

   private final DMatrixRMaj xEquivalents = new DMatrixRMaj(0, 1);
   private final DMatrixRMaj yEquivalents = new DMatrixRMaj(0, 1);
   private final DMatrixRMaj zEquivalents = new DMatrixRMaj(0, 1);

   private final DMatrixRMaj xConstants = new DMatrixRMaj(0, 1);
   private final DMatrixRMaj yConstants = new DMatrixRMaj(0, 1);
   private final DMatrixRMaj zConstants = new DMatrixRMaj(0, 1);

   private final DMatrixRMaj vrpWaypointJacobian = new DMatrixRMaj(0, 1);

   private final DMatrixRMaj vrpXWaypoints = new DMatrixRMaj(0, 1);
   private final DMatrixRMaj vrpYWaypoints = new DMatrixRMaj(0, 1);
   private final DMatrixRMaj vrpZWaypoints = new DMatrixRMaj(0, 1);

   final DMatrixRMaj xCoefficientVector = new DMatrixRMaj(0, 1);
   final DMatrixRMaj yCoefficientVector = new DMatrixRMaj(0, 1);
   final DMatrixRMaj zCoefficientVector = new DMatrixRMaj(0, 1);

   private final YoDouble omega = new YoDouble("omegaForPlanning", registry);
   private final YoDouble comHeight = new YoDouble("comHeightForPlanning", registry);
   private final double gravityZ;

   private final CoMTrajectoryPlannerIndexHandler_MultipleeCMPs indexHandler = new CoMTrajectoryPlannerIndexHandler_MultipleeCMPs();

   private final FixedFramePoint3DBasics desiredCoMPosition = new FramePoint3D(worldFrame);
   private final FixedFrameVector3DBasics desiredCoMVelocity = new FrameVector3D(worldFrame);
   private final FixedFrameVector3DBasics desiredCoMAcceleration = new FrameVector3D(worldFrame);

   private final FixedFramePoint3DBasics desiredDCMPosition = new FramePoint3D(worldFrame);
   private final FixedFrameVector3DBasics desiredDCMVelocity = new FrameVector3D(worldFrame);

   private final FixedFramePoint3DBasics desiredVRPPosition = new FramePoint3D(worldFrame);
   private final FixedFramePoint3DBasics desiredECMPPosition = new FramePoint3D(worldFrame);
   
   private final FixedFramePoint3DBasics desiredECMPPosition_left = new FramePoint3D(worldFrame);
   private final FixedFramePoint3DBasics desiredECMPVelocity_left = new FramePoint3D(worldFrame);
   private final FixedFramePoint3DBasics desiredECMPPosition_right = new FramePoint3D(worldFrame);
   private final FixedFramePoint3DBasics desiredECMPVelocity_right = new FramePoint3D(worldFrame);
   
   private final FixedFramePoint3DBasics computedCoMPosition = new FramePoint3D(worldFrame);
   private final FixedFramePoint3DBasics computedCoMVelocity = new FramePoint3D(worldFrame);
   private final FixedFramePoint3DBasics computedCoMAcceleration = new FramePoint3D(worldFrame);

   private final RecyclingArrayList<FramePoint3D> startVRPPositions = new RecyclingArrayList<>(FramePoint3D::new);
   private final RecyclingArrayList<FramePoint3D> endVRPPositions = new RecyclingArrayList<>(FramePoint3D::new);

   private final YoFramePoint3D finalDCMPosition = new YoFramePoint3D("goalDCMPosition", worldFrame, registry);

   private final YoFramePoint3D currentCoMPosition = new YoFramePoint3D("currentCoMPosition", worldFrame, registry);
   private final YoFrameVector3D currentCoMVelocity = new YoFrameVector3D("currentCoMVelocity", worldFrame, registry);

   private final YoFramePoint3D yoFirstCoefficient = new YoFramePoint3D("comFirstCoefficient", worldFrame, registry);
   private final YoFramePoint3D yoSecondCoefficient = new YoFramePoint3D("comSecondCoefficient", worldFrame, registry);
   private final YoFramePoint3D yoThirdCoefficient = new YoFramePoint3D("comThirdCoefficient", worldFrame, registry);
   private final YoFramePoint3D yoFourthCoefficient = new YoFramePoint3D("comFourthCoefficient", worldFrame, registry);
   private final YoFramePoint3D yoFifthCoefficient = new YoFramePoint3D("comFifthCoefficient", worldFrame, registry);
   private final YoFramePoint3D yoSixthCoefficient = new YoFramePoint3D("comSixthCoefficient", worldFrame, registry);

   private final RecyclingArrayList<FramePoint3D> dcmCornerPoints = new RecyclingArrayList<>(FramePoint3D::new);
   private final RecyclingArrayList<FramePoint3D> comCornerPoints = new RecyclingArrayList<>(FramePoint3D::new);

   private final RecyclingArrayList<Trajectory3D> vrpTrajectoryPool = new RecyclingArrayList<>(() -> new Trajectory3D(4));
   private final RecyclingArrayList<LineSegment3D> vrpSegments = new RecyclingArrayList<>(LineSegment3D::new);
   private final List<Trajectory3D> vrpTrajectories = new ArrayList<>();

   private int numberOfConstraints = 0;

   private CornerPointViewer viewer = null;

   public CoMTrajectoryPlanner_MultipleeCMPs(double gravityZ, double nominalCoMHeight, YoVariableRegistry parentRegistry)
   {
      this.gravityZ = Math.abs(gravityZ);

      comHeight.addVariableChangedListener(v -> omega.set(Math.sqrt(Math.abs(gravityZ) / comHeight.getDoubleValue())));
      comHeight.set(nominalCoMHeight);
      
      CoMTrajectoryPlannerTools_MultipleeCMPs.setMatrixIndex(indexHandler.getMatrixIndex());
      parentRegistry.addChild(registry);
   }

   public void setCornerPointViewer(CornerPointViewer viewer)
   {
      this.viewer = viewer;
   }
   
   /** {@inheritDoc} */
   @Override
   public void setNominalCoMHeight(double nominalCoMHeight)
   {
      this.comHeight.set(nominalCoMHeight);
   }

   /** {@inheritDoc} */
   @Override
   public double getNominalCoMHeight()
   {
      return comHeight.getDoubleValue();
   }

   /** {@inheritDoc} */
   @Override
   public void solveForTrajectory(List<? extends ContactStateProvider> contactSequence)
   {
      if (!ContactStateProviderTools.checkContactSequenceIsValid(contactSequence))
         throw new IllegalArgumentException("The contact sequence is not valid.");

      indexHandler.update(contactSequence);

      resetMatrices();

      CoMTrajectoryPlannerTools_MultipleeCMPs.computeVRPWaypoints(comHeight.getDoubleValue(), gravityZ, omega.getValue(), currentCoMVelocity, contactSequence, startVRPPositions,
                                                    endVRPPositions);

      solveForCoefficientConstraintMatrix(contactSequence);

      xEquivalents.set(xConstants);
      yEquivalents.set(yConstants);
      zEquivalents.set(zConstants);

      CommonOps_DDRM.multAdd(vrpWaypointJacobian, vrpXWaypoints, xEquivalents);
      CommonOps_DDRM.multAdd(vrpWaypointJacobian, vrpYWaypoints, yEquivalents);
      CommonOps_DDRM.multAdd(vrpWaypointJacobian, vrpZWaypoints, zEquivalents);

      CommonOps_DDRM.mult(coefficientMultipliersInv, xEquivalents, xCoefficientVector);
      CommonOps_DDRM.mult(coefficientMultipliersInv, yEquivalents, yCoefficientVector);
      CommonOps_DDRM.mult(coefficientMultipliersInv, zEquivalents, zCoefficientVector);

      // update coefficient holders
      int firstCoefficientIndex = 0;
      int secondCoefficientIndex = 1;
      int thirdCoefficientIndex = 2;
      int fourthCoefficientIndex = 3;
      int fifthCoefficientIndex = 4;
      int sixthCoefficientIndex = 5;
      

      yoFirstCoefficient.setX(xCoefficientVector.get(firstCoefficientIndex));
      yoFirstCoefficient.setY(yCoefficientVector.get(firstCoefficientIndex));
      yoFirstCoefficient.setZ(zCoefficientVector.get(firstCoefficientIndex));

      yoSecondCoefficient.setX(xCoefficientVector.get(secondCoefficientIndex));
      yoSecondCoefficient.setY(yCoefficientVector.get(secondCoefficientIndex));
      yoSecondCoefficient.setZ(zCoefficientVector.get(secondCoefficientIndex));

      yoThirdCoefficient.setX(xCoefficientVector.get(thirdCoefficientIndex));
      yoThirdCoefficient.setY(yCoefficientVector.get(thirdCoefficientIndex));
      yoThirdCoefficient.setZ(zCoefficientVector.get(thirdCoefficientIndex));

      yoFourthCoefficient.setX(xCoefficientVector.get(fourthCoefficientIndex));
      yoFourthCoefficient.setY(yCoefficientVector.get(fourthCoefficientIndex));
      yoFourthCoefficient.setZ(zCoefficientVector.get(fourthCoefficientIndex));

      yoFifthCoefficient.setX(xCoefficientVector.get(fifthCoefficientIndex));
      yoFifthCoefficient.setY(yCoefficientVector.get(fifthCoefficientIndex));
      yoFifthCoefficient.setZ(zCoefficientVector.get(fifthCoefficientIndex));

      yoSixthCoefficient.setX(xCoefficientVector.get(sixthCoefficientIndex));
      yoSixthCoefficient.setY(yCoefficientVector.get(sixthCoefficientIndex));
      yoSixthCoefficient.setZ(zCoefficientVector.get(sixthCoefficientIndex));
      
      // add seventh, eighth, ninth, and tenth coefficients for Left/Right eCMPs
      updateCornerPoints(contactSequence);

      if (viewer != null)
      {
         viewer.updateDCMCornerPoints(dcmCornerPoints);
         viewer.updateCoMCornerPoints(comCornerPoints);
         viewer.updateVRPWaypoints(vrpSegments);
      }
   }

   private void solveForCoefficientConstraintMatrix(List<? extends ContactStateProvider> contactSequence)
   {
      int numberOfPhases = contactSequence.size();
      int numberOfTransitions = numberOfPhases - 1;

      numberOfConstraints = 0;

      // set initial constraint
      setCoMPositionConstraint(currentCoMPosition);
      setDynamicsInitialConstraint(contactSequence, 0);
      setComputedCoMPositionInitialConstraint(currentCoMPosition, 0);
      setComputedCoMDynamicsInitialConstraint(contactSequence, 0);

      // add transition continuity constraints
      for (int transition = 0; transition < numberOfTransitions; transition++)
      {
         int previousSequence = transition;
         int nextSequence = transition + 1;
         setCoMPositionContinuity(contactSequence, previousSequence, nextSequence);
         setCoMVelocityContinuity(contactSequence, previousSequence, nextSequence);
         setDynamicsFinalConstraint(contactSequence, previousSequence);
         setDynamicsInitialConstraint(contactSequence, nextSequence);
         
         setECMPConstraints(contactSequence, previousSequence, nextSequence);
         setComputedCoMPositionContinuity(contactSequence, previousSequence, nextSequence);
         setComputedCoMVelocityContinuity(contactSequence, previousSequence, nextSequence);
         setComputedCoMDynamicsInitialConstraint(contactSequence, nextSequence);
         setComputedCoMDynamicsFinalConstraint(contactSequence, previousSequence);
      }

      // set terminal constraint
      ContactStateProvider lastContactPhase = contactSequence.get(numberOfPhases - 1);
      finalDCMPosition.set(endVRPPositions.getLast());
      double finalDuration = lastContactPhase.getTimeInterval().getDuration();
      setDCMPositionConstraint(numberOfPhases - 1, finalDuration, finalDCMPosition);
      setDynamicsFinalConstraint(contactSequence, numberOfPhases - 1);
      setECMPConstraints(contactSequence, numberOfPhases - 1, numberOfPhases - 2);
      setComputedCoMDCMConstraint(numberOfPhases - 1, finalDuration, finalDCMPosition);
      setComputedCoMDynamicsFinalConstraint(contactSequence, numberOfPhases - 1);
      
      // coefficient constraint matrix stored in coefficientMultipliers, but math requires inverted matrix
      NativeCommonOps.invert(coefficientMultipliers, coefficientMultipliersInv);
   }


   private final FramePoint3D comPositionToThrowAway = new FramePoint3D();
   private final FramePoint3D dcmPositionToThrowAway = new FramePoint3D();

   private final FrameVector3D comVelocityToThrowAway = new FrameVector3D();
   private final FrameVector3D comAccelerationToThrowAway = new FrameVector3D();
   private final FrameVector3D dcmVelocityToThrowAway = new FrameVector3D();
   private final FramePoint3D vrpStartPosition = new FramePoint3D();
   private final FramePoint3D vrpEndPosition = new FramePoint3D();
   private final FramePoint3D ecmpPositionToThrowAway = new FramePoint3D();

   private void updateCornerPoints(List<? extends ContactStateProvider> contactSequence)
   {
      vrpTrajectoryPool.clear();
      vrpTrajectories.clear();

      comCornerPoints.clear();
      dcmCornerPoints.clear();
      vrpSegments.clear();

      boolean verboseBefore = verbose;
      verbose = false;
      for (int segmentId = 0; segmentId < Math.min(contactSequence.size(), maxCapacity + 1); segmentId++)
      {
         double duration = contactSequence.get(segmentId).getTimeInterval().getDuration();

         compute(segmentId, 0.0, comCornerPoints.add(), comVelocityToThrowAway, comAccelerationToThrowAway, dcmCornerPoints.add(),
                 dcmVelocityToThrowAway, vrpStartPosition, ecmpPositionToThrowAway);
         compute(segmentId, duration, comPositionToThrowAway, comVelocityToThrowAway, comAccelerationToThrowAway, dcmPositionToThrowAway,
                 dcmVelocityToThrowAway, vrpEndPosition, ecmpPositionToThrowAway);
         
         Trajectory3D trajectory3D = vrpTrajectoryPool.add();
         trajectory3D.setLinear(0.0, duration, vrpStartPosition, vrpEndPosition);
         vrpTrajectories.add(trajectory3D);
 
         vrpSegments.add().set(vrpStartPosition, vrpEndPosition);
      }

      verbose = verboseBefore;
   }
   
   /** {@inheritDoc} */
   @Override
   public void compute(int segmentId, double timeInPhase)
   {
      compute(segmentId, timeInPhase, desiredCoMPosition, desiredCoMVelocity, desiredCoMAcceleration, desiredDCMPosition, desiredDCMVelocity,
              desiredVRPPosition, desiredECMPPosition);
      computeECMPsandComputedCoM(segmentId, timeInPhase, desiredECMPPosition_left, desiredECMPVelocity_left, desiredECMPPosition_right, desiredECMPVelocity_right, computedCoMPosition);
      if (verbose)
      {
         LogTools.info("At time " + timeInPhase + ", Desired DCM = " + desiredDCMPosition + ", Desired CoM = " + desiredCoMPosition);
      }
   }

   private final FramePoint3D firstCoefficient =         new FramePoint3D();
   private final FramePoint3D secondCoefficient =        new FramePoint3D();
   private final FramePoint3D thirdCoefficient =         new FramePoint3D();
   private final FramePoint3D fourthCoefficient =        new FramePoint3D();
   private final FramePoint3D fifthCoefficient =         new FramePoint3D();
   private final FramePoint3D sixthCoefficient =         new FramePoint3D();
   private final FramePoint3D seventhCoefficient =       new FramePoint3D();
   private final FramePoint3D eighthCoefficient =        new FramePoint3D();
   private final FramePoint3D ninthCoefficient =         new FramePoint3D();
   private final FramePoint3D tenthCoefficient =         new FramePoint3D();
   private final FramePoint3D eleventhCoefficient =      new FramePoint3D();
   private final FramePoint3D twelfthCoefficient =       new FramePoint3D();
   private final FramePoint3D thirteenthCoefficient =    new FramePoint3D();
   private final FramePoint3D fourteenthCoefficient =    new FramePoint3D();
   private final FramePoint3D fifteenthCoefficient =     new FramePoint3D();
   private final FramePoint3D sixteenthCoefficient =     new FramePoint3D();
   private final FramePoint3D seventeenthCoefficient =   new FramePoint3D();
   private final FramePoint3D eighteenthCoefficient =    new FramePoint3D();

   /*
    * compute() method does not include Coefficients 6, 7, 8, and 9 at this time because these trajectories 
    * do not depend on eCMPs atm.
    */
   
   @Override
   public void compute(int segmentId, double timeInPhase, FixedFramePoint3DBasics comPositionToPack, FixedFrameVector3DBasics comVelocityToPack,
                       FixedFrameVector3DBasics comAccelerationToPack, FixedFramePoint3DBasics dcmPositionToPack, FixedFrameVector3DBasics dcmVelocityToPack,
                       FixedFramePoint3DBasics vrpPositionToPack, FixedFramePoint3DBasics ecmpPositionToPack)
   {
      int startIndex = indexHandler.getContactSequenceStartIndex(segmentId);
      firstCoefficient.setX(xCoefficientVector.get(startIndex));
      firstCoefficient.setY(yCoefficientVector.get(startIndex));
      firstCoefficient.setZ(zCoefficientVector.get(startIndex));

      int secondCoefficientIndex = startIndex + 1;
      secondCoefficient.setX(xCoefficientVector.get(secondCoefficientIndex));
      secondCoefficient.setY(yCoefficientVector.get(secondCoefficientIndex));
      secondCoefficient.setZ(zCoefficientVector.get(secondCoefficientIndex));

      int thirdCoefficientIndex = startIndex + 2;
      thirdCoefficient.setX(xCoefficientVector.get(thirdCoefficientIndex));
      thirdCoefficient.setY(yCoefficientVector.get(thirdCoefficientIndex));
      thirdCoefficient.setZ(zCoefficientVector.get(thirdCoefficientIndex));

      int fourthCoefficientIndex = startIndex + 3;
      fourthCoefficient.setX(xCoefficientVector.get(fourthCoefficientIndex));
      fourthCoefficient.setY(yCoefficientVector.get(fourthCoefficientIndex));
      fourthCoefficient.setZ(zCoefficientVector.get(fourthCoefficientIndex));

      int fifthCoefficientIndex = startIndex + 4;
      fifthCoefficient.setX(xCoefficientVector.get(fifthCoefficientIndex));
      fifthCoefficient.setY(yCoefficientVector.get(fifthCoefficientIndex));
      fifthCoefficient.setZ(zCoefficientVector.get(fifthCoefficientIndex));

      int sixthCoefficientIndex = startIndex + 5;
      sixthCoefficient.setX(xCoefficientVector.get(sixthCoefficientIndex));
      sixthCoefficient.setY(yCoefficientVector.get(sixthCoefficientIndex));
      sixthCoefficient.setZ(zCoefficientVector.get(sixthCoefficientIndex));

      double omega = this.omega.getValue();

      CoMTrajectoryPlannerTools_MultipleeCMPs.constructDesiredCoMPosition(comPositionToPack, firstCoefficient, secondCoefficient, thirdCoefficient, fourthCoefficient, fifthCoefficient,
                                  sixthCoefficient, timeInPhase, omega);
      CoMTrajectoryPlannerTools_MultipleeCMPs.constructDesiredCoMVelocity(comVelocityToPack, firstCoefficient, secondCoefficient, thirdCoefficient, fourthCoefficient, fifthCoefficient,
                                  sixthCoefficient, timeInPhase, omega);
      CoMTrajectoryPlannerTools_MultipleeCMPs.constructDesiredCoMAcceleration(comAccelerationToPack, firstCoefficient, secondCoefficient, thirdCoefficient, fourthCoefficient, fifthCoefficient,
                                      sixthCoefficient, timeInPhase, omega);
      
      CapturePointTools.computeCapturePointPosition(comPositionToPack, comVelocityToPack, omega, dcmPositionToPack);
      CapturePointTools.computeCapturePointVelocity(comVelocityToPack, comAccelerationToPack, omega, dcmVelocityToPack);
      CapturePointTools.computeCentroidalMomentumPivot(dcmPositionToPack, dcmVelocityToPack, omega, vrpPositionToPack);

      ecmpPositionToPack.set(vrpPositionToPack);
      ecmpPositionToPack.subZ(comHeight.getDoubleValue());
   }
   
   /*
    * computeECMPs() method does the same as the compute() method above, but takes care of multiple eCMPs. The reason for a
    * different method was to avoid making modifications to the CoMTrajectorPlannerInterface.java file.
    */
   
   public void computeECMPsandComputedCoM(int segmentId, double timeInPhase, FixedFramePoint3DBasics ecmpLeftPositionToPack, FixedFramePoint3DBasics ecmpLeftVelocityToPack, 
                            FixedFramePoint3DBasics ecmpRightPositionToPack, FixedFramePoint3DBasics ecmpRightVelocityToPack, FixedFramePoint3DBasics computedCoMPositionToPack) {
      int startIndex = indexHandler.getContactSequenceStartIndex(segmentId);
      
      // seventh, eighth, ninth, and tenth coefficients are the coefficients that the 
      int seventhCoefficientIndex = startIndex + 6;
      seventhCoefficient.setX(xCoefficientVector.get(seventhCoefficientIndex));
      seventhCoefficient.setY(yCoefficientVector.get(seventhCoefficientIndex));
      seventhCoefficient.setZ(zCoefficientVector.get(seventhCoefficientIndex));
      
      int eighthCoefficientIndex = startIndex + 7;
      eighthCoefficient.setX(xCoefficientVector.get(eighthCoefficientIndex));
      eighthCoefficient.setY(yCoefficientVector.get(eighthCoefficientIndex));
      eighthCoefficient.setZ(zCoefficientVector.get(eighthCoefficientIndex));
      
      int ninthCoefficientIndex = startIndex + 8;
      ninthCoefficient.setX(xCoefficientVector.get(ninthCoefficientIndex));
      ninthCoefficient.setY(yCoefficientVector.get(ninthCoefficientIndex));
      ninthCoefficient.setZ(zCoefficientVector.get(ninthCoefficientIndex));
      
      int tenthCoefficientIndex = startIndex + 9;
      tenthCoefficient.setX(xCoefficientVector.get(tenthCoefficientIndex));
      tenthCoefficient.setY(yCoefficientVector.get(tenthCoefficientIndex));
      tenthCoefficient.setZ(zCoefficientVector.get(tenthCoefficientIndex));
      
      int eleventhCoefficientIndex = startIndex + 10;
      eleventhCoefficient.setX(xCoefficientVector.get(eleventhCoefficientIndex));
      eleventhCoefficient.setY(yCoefficientVector.get(eleventhCoefficientIndex));
      eleventhCoefficient.setZ(zCoefficientVector.get(eleventhCoefficientIndex));
      
      int twelfthCoefficientIndex = startIndex + 11;
      twelfthCoefficient.setX(xCoefficientVector.get(twelfthCoefficientIndex));
      twelfthCoefficient.setY(yCoefficientVector.get(twelfthCoefficientIndex));
      twelfthCoefficient.setZ(zCoefficientVector.get(twelfthCoefficientIndex));
      
      int thirteenthCoefficientIndex = startIndex + 12;
      thirteenthCoefficient.setX(xCoefficientVector.get(thirteenthCoefficientIndex));
      thirteenthCoefficient.setY(yCoefficientVector.get(thirteenthCoefficientIndex));
      thirteenthCoefficient.setZ(zCoefficientVector.get(thirteenthCoefficientIndex));
      
      int fourteenthCoefficientIndex = startIndex + 13;
      fourteenthCoefficient.setX(xCoefficientVector.get(fourteenthCoefficientIndex));
      fourteenthCoefficient.setY(yCoefficientVector.get(fourteenthCoefficientIndex));
      fourteenthCoefficient.setZ(zCoefficientVector.get(fourteenthCoefficientIndex));
      
      int fifteenthCoefficientIndex = startIndex + 14;
      fifteenthCoefficient.setX(xCoefficientVector.get(fifteenthCoefficientIndex));
      fifteenthCoefficient.setY(yCoefficientVector.get(fifteenthCoefficientIndex));
      fifteenthCoefficient.setZ(zCoefficientVector.get(fifteenthCoefficientIndex));
      
      int sixteenthCoefficientIndex = startIndex + 15;
      sixteenthCoefficient.setX(xCoefficientVector.get(sixteenthCoefficientIndex));
      sixteenthCoefficient.setY(yCoefficientVector.get(sixteenthCoefficientIndex));
      sixteenthCoefficient.setZ(zCoefficientVector.get(sixteenthCoefficientIndex));
      
      int seventeenthCoefficientIndex = startIndex + 16;
      seventeenthCoefficient.setX(xCoefficientVector.get(seventeenthCoefficientIndex));
      seventeenthCoefficient.setY(yCoefficientVector.get(seventeenthCoefficientIndex));
      seventeenthCoefficient.setZ(zCoefficientVector.get(seventeenthCoefficientIndex));
      
      int eighteenthCoefficientIndex = startIndex + 17;
      eighteenthCoefficient.setX(xCoefficientVector.get(eighteenthCoefficientIndex));
      eighteenthCoefficient.setY(yCoefficientVector.get(eighteenthCoefficientIndex));
      eighteenthCoefficient.setZ(zCoefficientVector.get(eighteenthCoefficientIndex));
      
      
      double omega = this.omega.getValue();
      
      CoMTrajectoryPlannerTools_MultipleeCMPs.constructECMPPosition_left(ecmpLeftPositionToPack, seventhCoefficient, eighthCoefficient, ninthCoefficient, tenthCoefficient, timeInPhase, omega);
      CoMTrajectoryPlannerTools_MultipleeCMPs.constructECMPVelocity_left(ecmpLeftVelocityToPack, seventhCoefficient, eighthCoefficient, ninthCoefficient, timeInPhase, omega);
      CoMTrajectoryPlannerTools_MultipleeCMPs.constructECMPPosition_right(ecmpRightPositionToPack, eleventhCoefficient, twelfthCoefficient, thirteenthCoefficient, fourteenthCoefficient, timeInPhase, omega);
      CoMTrajectoryPlannerTools_MultipleeCMPs.constructECMPVelocity_right(ecmpRightVelocityToPack, eleventhCoefficient, twelfthCoefficient, thirteenthCoefficient, timeInPhase, omega);
      CoMTrajectoryPlannerTools_MultipleeCMPs.constructComputedCoM(computedCoMPositionToPack, fifteenthCoefficient, sixteenthCoefficient, seventeenthCoefficient, eighteenthCoefficient, timeInPhase, omega);
   }

   /** {@inheritDoc} */
   @Override
   public void setInitialCenterOfMassState(FramePoint3DReadOnly centerOfMassPosition, FrameVector3DReadOnly centerOfMassVelocity)
   {
      this.currentCoMPosition.setMatchingFrame(centerOfMassPosition);
      this.currentCoMVelocity.setMatchingFrame(centerOfMassVelocity);
   }

   /** {@inheritDoc} */
   @Override
   public FramePoint3DReadOnly getDesiredDCMPosition()
   {
      return desiredDCMPosition;
   }

   /** {@inheritDoc} */
   @Override
   public FrameVector3DReadOnly getDesiredDCMVelocity()
   {
      return desiredDCMVelocity;
   }

   /** {@inheritDoc} */
   @Override
   public FramePoint3DReadOnly getDesiredCoMPosition()
   {
      return desiredCoMPosition;
   }

   /** {@inheritDoc} */
   @Override
   public FrameVector3DReadOnly getDesiredCoMVelocity()
   {
      return desiredCoMVelocity;
   }

   /** {@inheritDoc} */
   @Override
   public FrameVector3DReadOnly getDesiredCoMAcceleration()
   {
      return desiredCoMAcceleration;
   }

   /** {@inheritDoc} */
   @Override
   public FramePoint3DReadOnly getDesiredVRPPosition()
   {
      return desiredVRPPosition;
   }

   /** {@inheritDoc} */
   @Override
   public FramePoint3DReadOnly getDesiredECMPPosition()
   {
      return desiredECMPPosition;
   }
   
   public FramePoint3DReadOnly getComputedCoMPosition()
   {
      return computedCoMPosition;
   }
   
   public FramePoint3DReadOnly getComputedCoMVelocity()
   {
      return computedCoMVelocity;
   }
   
   public FramePoint3DReadOnly getComputedCoMAcceleration()
   {
      return computedCoMAcceleration;
   }
   
   public FramePoint3DReadOnly getDesiredECMPPosition_left()
   {
      return desiredECMPPosition_left;
   }
   
   public FramePoint3DReadOnly getDesiredECMPVelocity_left()
   {
      return desiredECMPVelocity_left;
   }
   
   public FramePoint3DReadOnly getDesiredECMPPosition_right()
   {
      return desiredECMPPosition_right;
   }
   
   public FramePoint3DReadOnly getDesiredECMPVelocity_right()
   {
      return desiredECMPVelocity_right;
   } 

   /**
    * Resets and resizes the internal matrices.
    */
   private void resetMatrices()
   {
      int size = indexHandler.getTotalNumberOfCoefficients();
      int numberOfVRPWaypoints = indexHandler.getNumberOfVRPWaypoints();

      coefficientMultipliers.reshape(size, size);
      coefficientMultipliersInv.reshape(size, size);
      xEquivalents.reshape(size, 1);
      yEquivalents.reshape(size, 1);
      zEquivalents.reshape(size, 1);
      xConstants.reshape(size, 1);
      yConstants.reshape(size, 1);
      zConstants.reshape(size, 1);
      vrpWaypointJacobian.reshape(size, numberOfVRPWaypoints); // only position
      vrpXWaypoints.reshape(numberOfVRPWaypoints, 1);
      vrpYWaypoints.reshape(numberOfVRPWaypoints, 1);
      vrpZWaypoints.reshape(numberOfVRPWaypoints, 1);
      xCoefficientVector.reshape(size, 1);
      yCoefficientVector.reshape(size, 1);
      zCoefficientVector.reshape(size, 1);

      coefficientMultipliers.zero();
      coefficientMultipliersInv.zero();
      xEquivalents.zero();
      yEquivalents.zero();
      zEquivalents.zero();
      xConstants.zero();
      yConstants.zero();
      zConstants.zero();
      vrpWaypointJacobian.zero();
      vrpXWaypoints.zero();
      vrpYWaypoints.zero();
      vrpZWaypoints.zero();
      xCoefficientVector.zero();
      yCoefficientVector.zero();
      zCoefficientVector.zero();
   }

   /**
    * <p> Sets the continuity constraint on the initial CoM position. This DOES result in a initial discontinuity on the desired DCM location,
    * coming from a discontinuity on the desired CoM Velocity. </p>
    * <p> This constraint should be used for the initial position of the center of mass to properly initialize the trajectory. </p>
    * <p> Recall that the equation for the center of mass is defined by </p>
    * <p>
    *    x<sub>i</sub>(t<sub>i</sub>) = c<sub>0,i</sub> e<sup>&omega; t<sub>i</sub></sup> + c<sub>1,i</sub> e<sup>-&omega; t<sub>i</sub></sup> +
    *    c<sub>2,i</sub> t<sub>i</sub><sup>3</sup> + c<sub>3,i</sub> t<sub>i</sub><sup>2</sup> +
    *    c<sub>4,i</sub> t<sub>i</sub> + c<sub>5,i</sub>.
    * </p>
    * <p>
    *    This constraint defines
    * </p>
    * <p>
    *    x<sub>0</sub>(0) = x<sub>d</sub>,
    * </p>
    * <p>
    *    substituting in the coefficients into the constraint matrix.
    * </p>
    * @param centerOfMassLocationForConstraint x<sub>d</sub> in the above equations
    */
   private void setCoMPositionConstraint(FramePoint3DReadOnly centerOfMassLocationForConstraint)
   {
      CoMTrajectoryPlannerTools_MultipleeCMPs.addCoMPositionConstraint(centerOfMassLocationForConstraint, omega.getValue(), 0.0, 0, numberOfConstraints,
                                                         coefficientMultipliers, xConstants, yConstants, zConstants);
      numberOfConstraints++;
   }

   /**
    * <p> Sets a constraint on the desired DCM position. This constraint is useful for constraining the terminal location of the DCM trajectory. </p>
    * <p> Recall that the equation for the center of mass position is defined by </p>
    * <p>
    *    x<sub>i</sub>(t<sub>i</sub>) = c<sub>0,i</sub> e<sup>&omega; t<sub>i</sub></sup> + c<sub>1,i</sub> e<sup>-&omega; t<sub>i</sub></sup> +
    *    c<sub>2,i</sub> t<sub>i</sub><sup>3</sup> + c<sub>3,i</sub> t<sub>i</sub><sup>2</sup> +
    *    c<sub>4,i</sub> t<sub>i</sub> + c<sub>5,i</sub>.
    * </p>
    * <p> and the center of mass velocity is defined by </p>
    * <p>
    *    d/dt x<sub>i</sub>(t<sub>i</sub>) = &omega; c<sub>0,i</sub> e<sup>&omega; t<sub>i</sub></sup> -
    *    &omega; c<sub>1,i</sub> e<sup>-&omega; t<sub>i</sub></sup> + 3 c<sub>2,i</sub> t<sub>i</sub><sup>2</sup> +
    *     2 c<sub>3,i</sub> t<sub>i</sub> + c<sub>4,i</sub>
    * </p>
    * <p>
    *    This constraint is then combining these two, saying
    * </p>
    * <p> x<sub>i</sub>(t<sub>i</sub>) + 1 / &omega; d/dt x<sub>i</sub>(t<sub>i</sub>) = &xi;<sub>d</sub>,</p>
    * <p> substituting in the appropriate coefficients. </p>
    * @param sequenceId i in the above equations
    * @param time t<sub>i</sub> in the above equations
    * @param desiredDCMPosition desired DCM location. &xi;<sub>d</sub> in the above equations.
    */
   private void setDCMPositionConstraint(int sequenceId, double time, FramePoint3DReadOnly desiredDCMPosition)
   {
      CoMTrajectoryPlannerTools_MultipleeCMPs.addDCMPositionConstraint(sequenceId, numberOfConstraints, time, omega.getValue(), desiredDCMPosition, coefficientMultipliers,
                                                         xConstants, yConstants, zConstants);
      numberOfConstraints++;
   }

   /**
    * <p> Set a continuity constraint on the CoM position at a state change, aka a trajectory knot.. </p>
    * <p> Recall that the equation for the center of mass position is defined by </p>
    * <p>
    *    x<sub>i</sub>(t<sub>i</sub>) = c<sub>0,i</sub> e<sup>&omega; t<sub>i</sub></sup> + c<sub>1,i</sub> e<sup>-&omega; t<sub>i</sub></sup> +
    *    c<sub>2,i</sub> t<sub>i</sub><sup>3</sup> + c<sub>3,i</sub> t<sub>i</sub><sup>2</sup> +
    *    c<sub>4,i</sub> t<sub>i</sub> + c<sub>5,i</sub>.
    * </p>
    * <p> This constraint is then defined as </p>
    * <p> x<sub>i-1</sub>(T<sub>i-1</sub>) = x<sub>i</sub>(0), </p>
    * <p> substituting in the trajectory coefficients. </p>
    *
    * @param contactSequence current contact sequence.
    * @param previousSequence i-1 in the above equations.
    * @param nextSequence i in the above equations.
    */
   private void setCoMPositionContinuity(List<? extends ContactStateProvider> contactSequence, int previousSequence, int nextSequence)
   {
      double previousDuration = contactSequence.get(previousSequence).getTimeInterval().getDuration();
      CoMTrajectoryPlannerTools_MultipleeCMPs.addCoMPositionContinuityConstraint(previousSequence, nextSequence, numberOfConstraints, omega.getValue(), previousDuration,
                                                                   coefficientMultipliers);
      numberOfConstraints++;
   }

   /**
    * <p> Set a continuity constraint on the CoM velocity at a state change, aka a trajectory knot.. </p>
    * <p> Recall that the equation for the center of mass position is defined by </p>
    * <p>
    *    d/dt x<sub>i</sub>(t<sub>i</sub>) = &omega; c<sub>0,i</sub> e<sup>&omega; t<sub>i</sub></sup> -
    *    &omega; c<sub>1,i</sub> e<sup>-&omega; t<sub>i</sub></sup> + 3 c<sub>2,i</sub> t<sub>i</sub><sup>2</sup> +
    *     2 c<sub>3,i</sub> t<sub>i</sub> + c<sub>4,i</sub>.
    * </p>
    * <p> This constraint is then defined as </p>
    * <p> d / dt x<sub>i-1</sub>(T<sub>i-1</sub>) = d / dt x<sub>i</sub>(0), </p>
    * <p> substituting in the trajectory coefficients. </p>
    *
    * @param contactSequence current contact sequence.
    * @param previousSequence i-1 in the above equations.
    * @param nextSequence i in the above equations.
    */
   private void setCoMVelocityContinuity(List<? extends ContactStateProvider> contactSequence, int previousSequence, int nextSequence)
   {
      double previousDuration = contactSequence.get(previousSequence).getTimeInterval().getDuration();
      CoMTrajectoryPlannerTools_MultipleeCMPs.addCoMVelocityContinuityConstraint(previousSequence, nextSequence, numberOfConstraints, omega.getValue(), previousDuration,
                                                                   coefficientMultipliers);
      numberOfConstraints++;
   }

   private final FrameVector3D desiredVelocity = new FrameVector3D();

   /**
    * Used to enforce the dynamics at the beginning of the trajectory segment {@param sequenceId}.
    *
    * @param contactSequence current contact sequence.
    * @param sequenceId desired trajectory segment.
    */
   private void setDynamicsInitialConstraint(List<? extends ContactStateProvider> contactSequence, int sequenceId)
   {
      ContactStateProvider contactStateProvider = contactSequence.get(sequenceId);
      ContactState contactState = contactStateProvider.getContactState();
      if (contactState.isLoadBearing())
      {
         double duration = contactStateProvider.getTimeInterval().getDuration();
         desiredVelocity.sub(endVRPPositions.get(sequenceId), startVRPPositions.get(sequenceId));
         desiredVelocity.scale(1.0 / duration);
         constrainVRPPosition(sequenceId, indexHandler.getVRPWaypointStartPositionIndex(sequenceId), 0.0, startVRPPositions.get(sequenceId));
         constrainVRPVelocity(sequenceId, indexHandler.getVRPWaypointStartVelocityIndex(sequenceId), 0.0, desiredVelocity);
      }
      else
      {
         constrainCoMAccelerationToGravity(sequenceId, 0.0);
         constrainCoMJerkToZero(sequenceId, 0.0);
      }
   }

   /**
    * Used to enforce the dynamics at the end of the trajectory segment {@param sequenceId}.
    *
    * @param contactSequence current contact sequence.
    * @param sequenceId desired trajectory segment.
    */
   private void setDynamicsFinalConstraint(List<? extends ContactStateProvider> contactSequence, int sequenceId)
   {
      ContactStateProvider contactStateProvider = contactSequence.get(sequenceId);
      ContactState contactState = contactStateProvider.getContactState();
      double duration = contactStateProvider.getTimeInterval().getDuration();
      if (contactState.isLoadBearing())
      {
         desiredVelocity.sub(endVRPPositions.get(sequenceId), startVRPPositions.get(sequenceId));
         desiredVelocity.scale(1.0 / duration);
         constrainVRPPosition(sequenceId, indexHandler.getVRPWaypointFinalPositionIndex(sequenceId), duration, endVRPPositions.get(sequenceId));
         constrainVRPVelocity(sequenceId, indexHandler.getVRPWaypointFinalVelocityIndex(sequenceId), duration, desiredVelocity);
      }
      else
      {
         constrainCoMAccelerationToGravity(sequenceId, duration);
         constrainCoMJerkToZero(sequenceId, duration);
      }
   }

   /**
    * <p> Adds a constraint for the desired VRP position.</p>
    * <p> Recall that the VRP is defined as </p>
    * <p> v<sub>i</sub>(t<sub>i</sub>) =  c<sub>2,i</sub> t<sub>i</sub><sup>3</sup> + c<sub>3,i</sub> t<sub>i</sub><sup>2</sup> +
    * (c<sub>4,i</sub> - 6/&omega;<sup>2</sup> c<sub>2,i</sub>) t<sub>i</sub> - 2/&omega; c<sub>3,i</sub> + c<sub>5,i</sub></p>.
    * <p> This constraint then says </p>
    * <p> v<sub>i</sub>(t<sub>i</sub>) = J v<sub>d</sub> </p>
    * <p> where J is a Jacobian that maps from a vector of desired VRP waypoints to the constraint form, and </p>
    * <p> v<sub>d,j</sub> = v<sub>r</sub> </p>
    * @param sequenceId segment of interest, i in the above equations
    * @param vrpWaypointPositionIndex current vrp waypoint index, j in the above equations
    * @param time time in the segment, t<sub>i</sub> in the above equations
    * @param desiredVRPPosition reference VRP position, v<sub>r</sub> in the above equations.
    */
   private void constrainVRPPosition(int sequenceId, int vrpWaypointPositionIndex, double time, FramePoint3DReadOnly desiredVRPPosition)
   {
      CoMTrajectoryPlannerTools_MultipleeCMPs.addVRPPositionConstraint(sequenceId, numberOfConstraints, vrpWaypointPositionIndex, time, omega.getValue(), desiredVRPPosition,
                                                         coefficientMultipliers, vrpXWaypoints, vrpYWaypoints, vrpZWaypoints, vrpWaypointJacobian);
      numberOfConstraints++;
   }

   /**
    * <p> Adds a constraint for the desired VRP velocity.</p>
    * <p> Recall that the VRP velocity is defined as </p>
    * <p> d/dt v<sub>i</sub>(t<sub>i</sub>) =  3 c<sub>2,i</sub> t<sub>i</sub><sup>2</sup> + 2 c<sub>3,i</sub> t<sub>i</sub> +
    * (c<sub>4,i</sub> - 6/&omega;<sup>2</sup> c<sub>2,i</sub>).
    * <p> This constraint then says </p>
    * <p> d/dt v<sub>i</sub>(t<sub>i</sub>) = J v<sub>d</sub> </p>
    * <p> where J is a Jacobian that maps from a vector of desired VRP waypoints to the constraint form, and </p>
    * <p> v<sub>d,j</sub> = d/dt v<sub>r</sub> </p>
    * @param sequenceId segment of interest, i in the above equations
    * @param vrpWaypointVelocityIndex current vrp waypoint index, j in the above equations
    * @param time time in the segment, t<sub>i</sub> in the above equations
    * @param desiredVRPVelocity reference VRP veloctiy, d/dt v<sub>r</sub> in the above equations.
    */
   private void constrainVRPVelocity(int sequenceId, int vrpWaypointVelocityIndex, double time, FrameVector3DReadOnly desiredVRPVelocity)
   {
      CoMTrajectoryPlannerTools_MultipleeCMPs.addVRPVelocityConstraint(sequenceId, numberOfConstraints, vrpWaypointVelocityIndex, omega.getValue(), time, desiredVRPVelocity,
                                                         coefficientMultipliers, vrpXWaypoints, vrpYWaypoints, vrpZWaypoints, vrpWaypointJacobian);
      numberOfConstraints++;
   }

   /**
    * <p> Adds a constraint for the CoM trajectory to have an acceleration equal to gravity at time t.</p>
    * <p> Recall that the CoM acceleration is defined as </p>
    * d<sup>2</sup> / dt<sup>2</sup> x<sub>i</sub>(t<sub>i</sub>) = &omega;<sup>2</sup> c<sub>0,i</sub> e<sup>&omega; t<sub>i</sub></sup> +
    * &omega;<sup>2</sup> c<sub>1,i</sub> e<sup>-&omega; t<sub>i</sub></sup> + 6 c<sub>2,i</sub> t<sub>i</sub> + 2 c<sub>3,i</sub>
    * <p> This constraint then states that </p>
    * <p> d<sup>2</sup> / dt<sup>2</sup> x<sub>i</sub>(t<sub>i</sub>) = -g, </p>
    * <p> substituting in the appropriate coefficients. </p>
    * @param sequenceId segment of interest, i in the above equations.
    * @param time time for the constraint, t<sub>i</sub> in the above equations.
    */
   private void constrainCoMAccelerationToGravity(int sequenceId, double time)
   {
      CoMTrajectoryPlannerTools_MultipleeCMPs.constrainCoMAccelerationToGravity(sequenceId, numberOfConstraints, omega.getValue(), time, gravityZ, coefficientMultipliers,
                                                                  zConstants);
      numberOfConstraints++;
   }

   /**
    * <p> Adds a constraint for the CoM trajectory to have a jerk equal to 0.0 at time t.</p>
    * <p> Recall that the CoM jerk is defined as </p>
    * d<sup>3</sup> / dt<sup>3</sup> x<sub>i</sub>(t<sub>i</sub>) = &omega;<sup>3</sup> c<sub>0,i</sub> e<sup>&omega; t<sub>i</sub></sup> -
    * &omega;<sup>3</sup> c<sub>1,i</sub> e<sup>-&omega; t<sub>i</sub></sup> + 6 c<sub>2,i</sub>
    * <p> This constraint then states that </p>
    * <p> d<sup>3</sup> / dt<sup>3</sup> x<sub>i</sub>(t<sub>i</sub>) = 0.0, </p>
    * <p> substituting in the appropriate coefficients. </p>
    * @param sequenceId segment of interest, i in the above equations.
    * @param time time for the constraint, t<sub>i</sub> in the above equations.
    */
   private void constrainCoMJerkToZero(int sequenceId, double time)
   {
      CoMTrajectoryPlannerTools_MultipleeCMPs.constrainCoMJerkToZero(time, omega.getValue(), sequenceId, numberOfConstraints, coefficientMultipliers);
      numberOfConstraints++;
   }
   
   /**
    * <p> Adds four constraints for the desired left and right eCMP conditions depending on single/double support state.</p>
    * <p> Multiple eCMPs are a way to directly connect the end effector positions to their influence on the CoM. 
    *     This method handles single and double support conditions. Every single support state is followed by a double support state. </p>
    * <p> For a footstep, the total eCMP position (r<sub>ecmp</sub>) can be represented as follows:</p>
    * <p> r<sub>ecmp</sub>(t) = t/T(r<sub>ecmp,h</sub> - r<sub>ecmp,T</sub>) + r<sub>ecmp,T</sub>
    * <p> Where 
    *       T - duration,
    *       r<sub>ecmp,H</sub> - VRP/CoP position at the heel of the following foot, and 
    *       r<sub>ecmp,T</sub> - VRP/CoP position at the toe of the support foot. 
    * This function can also be represented as follows, </p>
    * <p> r<sub>ecmp</sub>(t) = - x(t) + r<sub>ecmp,l</sub>(t) + r<sub>ecmp,r</sub>(t),
    * <p> relating the left/right eCMP to the CoM, x(t). The left and right (i) eCMP are linear trajectories represented as,
    * <p> r<sub>ecmp,i</sub> = C<sub>0,i</sub>t + c<sub>1,i</sub>
    * <p> For a right-to-left footstep, the following conditions are set in this method. </p>
    * <p> C<sub>1,l</sub> = x(0), c<sub>1,r</sub> = r<sub>ecmp,T</sub>, c<sub>0,l</sub>T + c<sub>1,l</sub> = x(T), and 
    *     c<sub>0,r</sub>T + c<sub>1,r</sub> = r<sub>ecmp,H</sub>
    * <p> For a double support condition, the left/right eCMP will stay at the VRP/CoP point or follow the CoM, depending on the following footstep. </sub>
    * @param contactSequence current contact sequence.
    * @param sequenceId desired trajectory segment.
    * @param nextSequenceId next desired trajectory segment.
    */
   private void setECMPConstraints(List<? extends ContactStateProvider> contactSequence, int sequenceId, int basisSequenceId) 
   {
      ContactStateProvider contactStateProvider = contactSequence.get(sequenceId);
      List<String> bodiesInContact = contactStateProvider.getBodiesInContact();
      ContactState contactState = contactStateProvider.getContactState();
      double nextDuration = contactSequence.get(sequenceId).getTimeInterval().getDuration();
      
      if (contactState.isLoadBearing()) {
          // Double Support Condition
          if (contactStateProvider.getNumberOfBodiesInContact() > 1 && ((bodiesInContact.get(0) == "left" && bodiesInContact.get(1) == "right") 
                                                                      || bodiesInContact.get(0) == "right" && bodiesInContact.get(1) == "left")){
             boolean nextIsRight = handleDoubleECMPConditions(contactSequence, sequenceId, basisSequenceId);
             // Double Support for a following right step
             if (nextIsRight) {
                setECMPsToCoMEndPositionForLinearECMP(true, sequenceId, nextDuration);
                setECMPsToVRPEndPosition(false, sequenceId, nextDuration, endVRPPositions.get(sequenceId));
                setECMPsToVRPStartPosition(true, sequenceId, nextDuration, startVRPPositions.get(sequenceId));
                setECMPsToCoMStartPositionForLinearECMP(false, sequenceId, nextDuration);
                setECMPNonlinearConstraintsToZero(true, sequenceId);
                setECMPNonlinearConstraintsToZero(false, sequenceId);
             }
             // Double Support for a following left step
             else if (!nextIsRight) {
                setECMPsToCoMEndPositionForLinearECMP(false, sequenceId, nextDuration);
                setECMPsToVRPEndPosition(true, sequenceId, nextDuration, endVRPPositions.get(sequenceId));
                setECMPsToVRPStartPosition(false, sequenceId, nextDuration, startVRPPositions.get(sequenceId));
                setECMPsToCoMStartPositionForLinearECMP(true, sequenceId, nextDuration);
                setECMPNonlinearConstraintsToZero(true, sequenceId);
                setECMPNonlinearConstraintsToZero(false, sequenceId);
             }
          }
          // Single Support Condition
          else {
             // Left Support Condition
             if (bodiesInContact.get(0) == "left")
             {
                setECMPDerivativesToZero(true, sequenceId);
                setECMPsToVRPStartPosition(true, sequenceId, nextDuration, startVRPPositions.get(sequenceId));
                setECMPtoCoM(false, sequenceId, nextDuration);
             }
             // Right Support Condition
             else if (bodiesInContact.get(0) == "right")
             {
                setECMPDerivativesToZero(false, sequenceId);
                setECMPsToVRPStartPosition(false, sequenceId, nextDuration, startVRPPositions.get(sequenceId));
                setECMPtoCoM(true, sequenceId, nextDuration);
             }
          }
      }
      // Flight conditions
      else {
         // Nada ahora.
      }
   }
   
   
   /**
    * <p> Method is used to handle ECMP conditions during a double support phase. Looks at next or previous step to 
    *     determine next step depending on the condition. Also handles the final condition where it cannot look ahead 
    *     or behind by returning an arbitrary true. </p>
    * <p> 
    */
   
   private boolean handleDoubleECMPConditions(List<? extends ContactStateProvider> contactSequence, int sequenceId, int basisSequenceId) {
      // basisSequenceId is variable to 
      boolean nextIsRight = false;
      if (sequenceId == 0 && sequenceId > basisSequenceId) {
         basisSequenceId = sequenceId;
      }
      ContactStateProvider basisContactStateProvider = contactSequence.get(basisSequenceId);
      List<String> basisBodiesInContact = basisContactStateProvider.getBodiesInContact();
      if (sequenceId < basisSequenceId) {
         if (basisBodiesInContact.get(0) == "right" && basisContactStateProvider.getNumberOfBodiesInContact() < 2) {
            return nextIsRight = true;
         }
         else if (basisBodiesInContact.get(0) == "left" && basisContactStateProvider.getNumberOfBodiesInContact() < 2) {
            return nextIsRight = false;
         }
      }
      else {
         // Double Support for a following left step.
         if (basisBodiesInContact.get(0) == "right" && basisContactStateProvider.getNumberOfBodiesInContact() < 2) {
            return nextIsRight = false;
         }
         // Double Support for a following right step.
         else if (basisBodiesInContact.get(0) == "left" && basisContactStateProvider.getNumberOfBodiesInContact() < 2) {
            return nextIsRight = true;
         }
         // Handles final conditions
         else {
            return nextIsRight = true;
         }
      }
      return nextIsRight;
   }
   
   /**
    * <p> Sets the eCMP Derivatives to zero depending on constrainLeftECMP variable. </sub>
    * <p> This constraint is set during a single support phase, when one eCMP is following the CoM and the other stays at the CoP. </p>
    * <p> c<sub>0,i</sub> = c<sub>1,i</sub> = c<sub>2,i</sub> = 0 </p>
    * @param constrainLeftECMP
    * @param sequenceId
    */
   private void setECMPNonlinearConstraintsToZero(boolean constrainLeftECMP, int sequenceId) {
      CoMTrajectoryPlannerTools_MultipleeCMPs.constrainECMPNonlinearConstraintsToZero(constrainLeftECMP, sequenceId, numberOfConstraints, coefficientMultipliers);
      numberOfConstraints = numberOfConstraints + 2;
   }
   
   /**
    * <p> Sets eCMP coefficients equal to CoM coefficients during a single support phase. </p>
    * @param constrainLeftECMP
    * @param sequenceId
    * @param time
    */
   private void setECMPtoCoM(boolean constrainLeftECMP, int sequenceId, double time) {
      CoMTrajectoryPlannerTools_MultipleeCMPs.constrainECMPtoCoM(constrainLeftECMP, sequenceId, numberOfConstraints, coefficientMultipliers);
      numberOfConstraints = numberOfConstraints + 4;
   }
   
   /**
    * <p> Sets the eCMP nonlinear constraints equal to zero depending on the constrainLeftECMP variable. </p>
    * <p> During the double support phase, the eCMPs are transitions linearly. This constraint sets c<sub>0,i</sub> = c<sub>1,i</sub> = 0. </p>
    * @param constrainLeftECMP
    * @param sequenceId
    */
   private void setECMPDerivativesToZero(boolean constrainLeftECMP, int sequenceId) {
      CoMTrajectoryPlannerTools_MultipleeCMPs.constrainECMPDerivativesToZero(constrainLeftECMP, sequenceId, numberOfConstraints, coefficientMultipliers);
      numberOfConstraints = numberOfConstraints + 3;
   }
   
   /**
    * <p> Adds a constraint to the left or right eCMP depending on variable constrainLeftECMP. </p>
    * <p> Recall that the eCMP is defined as </p>
    * r<sub>eCMP,i</sub> = c<sub>0,i</sub> * e<sup>&omega;t</sup> + c<sub>1,i</sub> * e<sup>-&omega;t</sup> + c<sub>2,i</sub> * t + c<sub>3,i</sub>
    * <p> Where i represents left/right. This constraint is set for a double support phase, where both eCMPs are linear.
    * Therefore, c<sub>0,i</sub> = c<sub>1,i</sub> = 0 in this instance. </p>
    * <p> This constraint sets the desiredStartVRPPosition, so this constraint sets c<sub>3,i</sub> = desiredVRPStartPosition. </p>
    * @param constrainLeftECMP
    * @param sequenceId
    * @param time
    * @param VRPStartPositionforConstraint
    */
   private void setECMPsToVRPStartPosition(boolean constrainLeftECMP, int sequenceId, double time, FramePoint3DReadOnly VRPStartPositionforConstraint) {
      CoMTrajectoryPlannerTools_MultipleeCMPs.constrainECMPsToVRPStartPosition(constrainLeftECMP, sequenceId, numberOfConstraints, VRPStartPositionforConstraint, 
                                                                               xConstants, yConstants, zConstants, coefficientMultipliers);
      numberOfConstraints++;
   }
   
   /**
    * <p> Adds a constraint to the left or right eCMP depending on variable constrainLeftECMP. </p>
    * <p> Recall that the eCMP is defined as </p>
    * r<sub>eCMP,i</sub> = c<sub>0,i</sub> * e<sup>&omega;t</sup> + c<sub>1,i</sub> * e<sup>-&omega;t</sup> + c<sub>2,i</sub> * t + c<sub>3,i</sub>
    * <p> Where i represents left/right. This constraint is set for a double support phase, where both eCMPs are linear.
    * Therefore, c<sub>0,i</sub> = c<sub>1,i</sub> = 0 in this instance. </p>
    * <p> This constraint sets the desiredStartEndPosition, so this constraint sets c<sub>2,i</sub>*T + c<sub>3,i</sub> = desiredVRPEndPosition. </p>
    * @param constrainLeftECMP
    * @param sequenceId
    * @param time
    * @param VRPEndPositionforConstraint
    */
   private void setECMPsToVRPEndPosition(boolean constrainLeftECMP, int sequenceId, double time, FramePoint3DReadOnly VRPEndPositionforConstraint) {
      CoMTrajectoryPlannerTools_MultipleeCMPs.constrainECMPsToVRPEndPosition(constrainLeftECMP, time, sequenceId, numberOfConstraints, VRPEndPositionforConstraint, 
                                                                             xConstants, yConstants, zConstants, coefficientMultipliers);
      numberOfConstraints++;
   }
   
   /**
    * <p> Sets eCMP constraint based on the CoM Start position depending on constrainLeftECMP variable. </p>
    * <p> Recall that the eCMP is defined as </p>
    * r<sub>eCMP,i</sub> = c<sub>0,i</sub> * e<sup>&omega;t</sup> + c<sub>1,i</sub> * e<sup>-&omega;t</sup> + c<sub>2,i</sub> * t + c<sub>3,i</sub>
    * <p> Where i represents left/right. This constraint is set for a double support phase, where both eCMPs are linear. </p>
    * <p> Therefore, c<sub>0,i</sub> = c<sub>1,i</sub> = 0 in this instance. </p>
    * <p> This constraint sets c<sub>3,i</sub> = x<sub>i</sub>(0). </p>
    * @param constrainLeftECMP
    * @param sequenceId
    * @param time
    */
   private void setECMPsToCoMStartPositionForLinearECMP(boolean constrainLeftECMP, int sequenceId, double time) {
      CoMTrajectoryPlannerTools_MultipleeCMPs.constrainECMPsToCoMStartPosition(constrainLeftECMP, omega.getValue(), sequenceId, numberOfConstraints, 
                                                                               xConstants, yConstants, zConstants, coefficientMultipliers);
      numberOfConstraints++;
   }
   
   /**
    * <p> Sets eCMP constraint based on the CoM End position depending on constrainLeftECMP variable. </p>
    * <p> Recall that the eCMP is defined as </p>
    * r<sub>eCMP,i</sub> = c<sub>0,i</sub> * e<sup>&omega;t</sup> + c<sub>1,i</sub> * e<sup>-&omega;t</sup> + c<sub>2,i</sub> * t + c<sub>3,i</sub>
    * <p> Where i represents left/right. This constraint is set for a double support phase, where both eCMPs are linear. </p>
    * <p> Therefore, c<sub>0,i</sub> = c<sub>1,i</sub> = 0 in this instance. </p>
    * <p> This constraint sets c<sub>2,i</sub>*T + c<sub>3,i</sub> = x<sub>i</sub>(T). </p>
    * @param constrainLeftECMP
    * @param sequenceId
    * @param time
    */
   private void setECMPsToCoMEndPositionForLinearECMP(boolean constrainLeftECMP, int sequenceId, double time) {
      CoMTrajectoryPlannerTools_MultipleeCMPs.constrainECMPsToCoMEndPosition(constrainLeftECMP, time, omega.getValue(), sequenceId, numberOfConstraints,
                                                                             xConstants, yConstants, zConstants, coefficientMultipliers);
      numberOfConstraints++;
   }
   
   /**
    * <p> This constraint sets the CoM dynamics to a specific position during the double support phase when t = 0. </p>
    * @param contactSequence
    * @param sequenceId
    */
   private void setComputedCoMDynamicsInitialConstraint(List<? extends ContactStateProvider> contactSequence, int sequenceId) 
   {
      ContactStateProvider contactStateProvider = contactSequence.get(sequenceId);
      ContactState contactState = contactStateProvider.getContactState();
      
      if (contactState.isLoadBearing()) {
         setComputedCoMDynamicsPosition(sequenceId, 0.0);

      }
      // Flight condition
      else {
         // Nada ahora
      }
   }
   
   /**
    * <p> This constraint sets the CoM dynamics to a specific position during the double support phase when t = T. </p>
    * @param contactSequence
    * @param sequenceId
    */
   private void setComputedCoMDynamicsFinalConstraint(List<? extends ContactStateProvider> contactSequence, int sequenceId) 
   {
      ContactStateProvider contactStateProvider = contactSequence.get(sequenceId);
      ContactState contactState = contactStateProvider.getContactState();
      double duration = contactSequence.get(sequenceId).getTimeInterval().getDuration();
      
      if (contactState.isLoadBearing()) {
         setComputedCoMDynamicsPosition(sequenceId, duration);
      }
      // Flight condition
      else {
         // Nada ahora
      }
   }
   
   /**
    * <p> This constraint sets the CoM dynamics to a specific position during the double support phase when t = 0. </p>
    * <p> The Computed CoM depends on the ECMPs based on the following equation. </p>
    * <p> r<sub>eCMP</sub>(t) = -x(t) + r<sub>eCMP,l</sub>(t) + r<sub>eCMP,r</sub>(t). </p>
    * <p> r<sub>eCMP</sub>(t) = t/T * r<sub>eCMP,H</sub> + (1 - t/T)*r<sub>eCMP,T</sub> </p>
    * <p> where T represents the toe and H represents the heel. The left/right eCMP are represented as follows.</p>
    * <p> r<sub>eCMP,i</sub> = c<sub>0,i</sub> * e<sup>&omega;t</sup> + c<sub>1,i</sub> * e<sup>-&omega;t</sup> + c<sub>2,i</sub> * t + c<sub>3,i</sub> </p> 
    * <p> Based on this the first equation simplifies to the following. </p>
    * <p> 0 = -cx<sub>0,i</sub> * e<sup>&omega;t</sup> - cx<sub>1,i</sub> * e<sup>-&omega;t</sup> - 2cx<sub>2,i</sub> * t - 2cx<sub>3,i</sub> + r<sub>eCMP,l</sub>(t) + r<sub>eCMP,r</sub>(t)</p>
    * @param sequenceId
    * @param duration
    */
   private void setComputedCoMDynamicsPosition(int sequenceId, double duration) {
      CoMTrajectoryPlannerTools_MultipleeCMPs.constrainComputedCoMDynamicsPosition(duration, omega.getValue(), sequenceId, numberOfConstraints, xConstants, yConstants, zConstants, coefficientMultipliers);
      numberOfConstraints++;
   }

   /**
    * <p> Sets a constraint for the Computed CoM based on the initial position of the CoM. </p>
    * @param centerOfMassLocationForConstraint
    * @param sequenceId
    */
   private void setComputedCoMPositionInitialConstraint(FramePoint3DReadOnly centerOfMassLocationForConstraint, int sequenceId) {
      CoMTrajectoryPlannerTools_MultipleeCMPs.constrainComputedCoMPosition(centerOfMassLocationForConstraint, 0.0, omega.getValue(), sequenceId, numberOfConstraints, xConstants, yConstants, zConstants, coefficientMultipliers);
      numberOfConstraints++;
   }
   
   /**
    * <p> This constraint sets the DCM final position as a constraint. </p>
    * <p> The DCM and CoM are related via the following equation. </p>
    * <p> &xi;(t) = x(t) + 1/&omega; * dx/dt(t) </p>
    * <p> For the final DCM phase this comes to &xi;<sub>f</sub> = x(T) + 1/&omega; * dx/dt(T) </p>
    * <p> This simplifies to the following. </p>
    * <p> &xi;<sub>f</sub> = 2a<sub>0</sub>e<sup>&omega;T</sup> + a<sub>2</sub>(T + 1/&omega;) + a<sub>3</sub> </p>
    * @param sequenceId
    * @param time
    * @param DCMFinalPositionforConstraint
    */
   private void setComputedCoMDCMConstraint(int sequenceId, double time, FramePoint3DReadOnly DCMFinalPositionforConstraint) {
      CoMTrajectoryPlannerTools_MultipleeCMPs.constrainComputedCoMDCM(DCMFinalPositionforConstraint, time, omega.getValue(), sequenceId, numberOfConstraints, xConstants, yConstants, zConstants, coefficientMultipliers);
      numberOfConstraints++;
   }
   
   /**
    * <p> Maintains position continuity for Computed CoM between phases as follows. </p>
    * <p> x<sub>i-1</sub>(T) = x<sub>i</sub>(0) </p>
    * @param contactSequence
    * @param previousSequence
    * @param nextSequence
    */
   private void setComputedCoMPositionContinuity(List<? extends ContactStateProvider> contactSequence, int previousSequence, int nextSequence) {
      double previousDuration = contactSequence.get(previousSequence).getTimeInterval().getDuration();
      CoMTrajectoryPlannerTools_MultipleeCMPs.constrainComputedCoMPositionContinuity(previousSequence, nextSequence, omega.getValue(), numberOfConstraints, previousDuration, coefficientMultipliers);
      numberOfConstraints++;
   }
   
   /**
    * Maintains velocity continuity for Computed CoM between phases as follows. </p>
    * <p> dx/dt<sub>i-1</sub>(T) = dx/dt<sub>i</sub>(0) </p>
    * @param contactSequence
    * @param previousSequence
    * @param nextSequence
    */
   private void setComputedCoMVelocityContinuity(List<? extends ContactStateProvider> contactSequence, int previousSequence, int nextSequence) {
      double previousDuration = contactSequence.get(previousSequence).getTimeInterval().getDuration();
      CoMTrajectoryPlannerTools_MultipleeCMPs.constrainComputedCoMVelocityContinuity(previousSequence, nextSequence, omega.getValue(), numberOfConstraints, previousDuration, coefficientMultipliers);
      numberOfConstraints++;
   }
   
   @Override
   public List<Trajectory3D> getVRPTrajectories()
   {
      return vrpTrajectories;
   }
}

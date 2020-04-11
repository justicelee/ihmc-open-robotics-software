package us.ihmc.robotics.physics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;

import us.ihmc.mecano.algorithms.ForwardDynamicsCalculator;
import us.ihmc.mecano.algorithms.MultiBodyResponseCalculator;
import us.ihmc.mecano.algorithms.interfaces.RigidBodyTwistProvider;
import us.ihmc.mecano.multiBodySystem.interfaces.JointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointReadOnly;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;

public class RobotJointLimitImpulseBasedCalculator implements ImpulseBasedConstraintCalculator
{
   private static final int matrixInitialSize = 40;

   public enum ActiveLimit
   {
      LOWER(DoubleUnaryOperator.identity()), UPPER(value -> -value);

      private final DoubleUnaryOperator signOperator;

      ActiveLimit(DoubleUnaryOperator signOperator)
      {
         this.signOperator = signOperator;
      }

      double transform(double value)
      {
         return signOperator.applyAsDouble(value);
      }
   };

   private double springConstant = 0.0;
   private final List<OneDoFJointBasics> jointsAtLimit = new ArrayList<>();
   private final List<ActiveLimit> activeLimits = new ArrayList<>();

   private boolean isFirstUpdate;
   private boolean isImpulseZero = false;

   private JointStateProvider externalJointTwistModifier;
   private final ImpulseBasedRigidBodyTwistProvider rigidBodyTwistModifier;
   private final ImpulseBasedJointTwistProvider jointTwistModifier;

   private final DenseMatrix64F jointVelocityNoImpulse = new DenseMatrix64F(matrixInitialSize, 1);
   private final DenseMatrix64F jointVelocityDueToOtherImpulse = new DenseMatrix64F(matrixInitialSize, 1);
   private final DenseMatrix64F jointVelocity = new DenseMatrix64F(matrixInitialSize, 1);
   private final DenseMatrix64F jointVelocityPrevious = new DenseMatrix64F(matrixInitialSize, 1);
   private final DenseMatrix64F jointVelocityUpdate = new DenseMatrix64F(matrixInitialSize, 1);

   private final DenseMatrix64F solverInput_A = new DenseMatrix64F(matrixInitialSize, matrixInitialSize);
   private final DenseMatrix64F solverInput_b = new DenseMatrix64F(matrixInitialSize, 1);
   private final LinearComplementarityProblemSolver solver = new LinearComplementarityProblemSolver();

   private final DenseMatrix64F impulse = new DenseMatrix64F(matrixInitialSize, 1);
   private final DenseMatrix64F impulsePrevious = new DenseMatrix64F(matrixInitialSize, 1);
   private final DenseMatrix64F impulseUpdate = new DenseMatrix64F(matrixInitialSize, 1);

   private final RigidBodyBasics rootBody;
   private final ForwardDynamicsCalculator forwardDynamicsCalculator;
   private final MultiBodyResponseCalculator responseCalculator;

   public RobotJointLimitImpulseBasedCalculator(RigidBodyBasics rootBody, ForwardDynamicsCalculator forwardDynamicsCalculator)
   {
      this.rootBody = rootBody;
      this.forwardDynamicsCalculator = forwardDynamicsCalculator;

      responseCalculator = new MultiBodyResponseCalculator(forwardDynamicsCalculator);
      rigidBodyTwistModifier = new ImpulseBasedRigidBodyTwistProvider(responseCalculator.getTwistChangeProvider().getInertialFrame(), rootBody);
      jointTwistModifier = new ImpulseBasedJointTwistProvider(rootBody);
   }

   @Override
   public void initialize(double dt)
   {
      jointsAtLimit.clear();
      activeLimits.clear();

      for (JointBasics joint : rootBody.childrenSubtreeIterable())
      {
         if (joint instanceof OneDoFJointBasics)
         {
            OneDoFJointBasics oneDoFJoint = (OneDoFJointBasics) joint;
            // TODO This is not enough as other joints maybe driven to their limit due to the different impacts. 
            ActiveLimit activeLimit = computeActiveLimit(oneDoFJoint, dt);

            if (activeLimit != null)
            {
               jointsAtLimit.add(oneDoFJoint);
               activeLimits.add(activeLimit);
            }
         }
      }

      jointVelocityNoImpulse.reshape(jointsAtLimit.size(), 1);
      jointVelocityDueToOtherImpulse.reshape(jointsAtLimit.size(), 1);
      jointVelocity.reshape(jointsAtLimit.size(), 1);
      jointVelocityPrevious.reshape(jointsAtLimit.size(), 1);
      jointVelocityUpdate.reshape(jointsAtLimit.size(), 1);
      solverInput_A.reshape(jointsAtLimit.size(), jointsAtLimit.size());
      solverInput_b.reshape(jointsAtLimit.size(), 1);
      impulse.reshape(jointsAtLimit.size(), 1);
      impulsePrevious.reshape(jointsAtLimit.size(), 1);
      impulseUpdate.reshape(jointsAtLimit.size(), 1);

      for (int i = 0; i < jointsAtLimit.size(); i++)
      {
         OneDoFJointBasics joint = jointsAtLimit.get(i);
         ActiveLimit activeLimit = activeLimits.get(i);

         double qd = joint.getQd() + dt * forwardDynamicsCalculator.getComputedJointAcceleration(joint).get(0);

         if (activeLimit == ActiveLimit.LOWER)
         {
            double distanceToLowerLimit = joint.getQ() - joint.getJointLimitLower();
            qd += springConstant * distanceToLowerLimit;
         }
         else
         {
            double distanceToUpperLimit = joint.getQ() - joint.getJointLimitUpper();
            qd += springConstant * distanceToUpperLimit;
         }
         jointVelocityNoImpulse.set(i, qd);
      }

      isFirstUpdate = true;
   }

   @Override
   public void updateInertia(List<? extends RigidBodyBasics> rigidBodyTargets, List<? extends JointBasics> jointTargets)
   {
      rigidBodyTwistModifier.clear(jointsAtLimit.size());
      jointTwistModifier.clear(jointsAtLimit.size());
      if (rigidBodyTargets != null)
         rigidBodyTwistModifier.addAll(rigidBodyTargets);
      if (jointTargets != null)
         jointTwistModifier.addAll(jointTargets);

      responseCalculator.reset();
      RigidBodyTwistProvider twistChangeProvider = responseCalculator.getTwistChangeProvider();

      for (int i = 0; i < jointsAtLimit.size(); i++)
      {
         OneDoFJointBasics joint = jointsAtLimit.get(i);
         ActiveLimit activeLimit = activeLimits.get(i);
         responseCalculator.applyJointImpulse(joint, 1.0);

         for (int j = i; j < jointsAtLimit.size(); j++)
         {
            OneDoFJointBasics otherJoint = jointsAtLimit.get(j);
            ActiveLimit otherActiveLimit = activeLimits.get(j);
            double a = responseCalculator.getJointTwistChange(otherJoint);
            /*
             * The LCP solver only handles positive impulse/velocity, so we mirror the problem for joints on the
             * upper limit which need negative impulse/velocity.
             */
            a = activeLimit.transform(otherActiveLimit.transform(a));
            solverInput_A.set(j, i, a);
            solverInput_A.set(i, j, a); // Using symmetry property
         }

         for (RigidBodyBasics externalTarget : rigidBodyTwistModifier.getRigidBodies())
         {
            DenseMatrix64F externalInertiaMatrix = rigidBodyTwistModifier.getApparentInertiaMatrixInverse(externalTarget);
            twistChangeProvider.getTwistOfBody(externalTarget).get(0, i, externalInertiaMatrix);
         }

         for (JointBasics externalTarget : jointTwistModifier.getJoints())
         {
            DenseMatrix64F externalInertiaMatrix = jointTwistModifier.getApparentInertiaMatrixInverse(externalTarget);
            externalInertiaMatrix.set(responseCalculator.getJointTwistChange(externalTarget));
         }

         responseCalculator.reset();
      }
   }

   private ActiveLimit computeActiveLimit(OneDoFJointReadOnly joint, double dt)
   {
      double q = joint.getQ();
      double qd = joint.getQd();
      double qdd = forwardDynamicsCalculator.getComputedJointAcceleration(joint).get(0);
      double projected_q = q + dt * qd + 0.5 * dt * dt * qdd;
      if (projected_q <= joint.getJointLimitLower())
         return ActiveLimit.LOWER;
      else if (projected_q >= joint.getJointLimitUpper())
         return ActiveLimit.UPPER;
      else
         return null;
   }

   @Override
   public void updateImpulse(double dt, double alpha)
   {
      if (jointsAtLimit.isEmpty())
      {
         isImpulseZero = true;
         return;
      }

      if (externalJointTwistModifier != null)
      {
         for (int i = 0; i < jointsAtLimit.size(); i++)
         {
            jointVelocityDueToOtherImpulse.set(i, externalJointTwistModifier.getJointState(jointsAtLimit.get(i)));
         }
         CommonOps.add(jointVelocityNoImpulse, jointVelocityDueToOtherImpulse, jointVelocity);
      }
      else
      {
         jointVelocityDueToOtherImpulse.zero();
         jointVelocity.set(jointVelocityNoImpulse);
      }

      if (isFirstUpdate)
      {
         jointVelocityPrevious.set(jointVelocity);
         jointVelocityUpdate.set(jointVelocity);
      }
      else
      {
         CommonOps.subtract(jointVelocity, jointVelocityPrevious, jointVelocityUpdate);
         jointVelocityPrevious.set(jointVelocity);
      }

      for (int i = 0; i < jointsAtLimit.size(); i++)
      {
         ActiveLimit activeLimit = activeLimits.get(i);
         solverInput_b.set(i, activeLimit.transform(jointVelocity.get(i)));
      }

      DenseMatrix64F solverOutput_f = solver.solve(solverInput_A, solverInput_b);

      for (int i = 0; i < jointsAtLimit.size(); i++)
      {
         ActiveLimit activeLimit = activeLimits.get(i);
         impulse.set(i, activeLimit.transform(solverOutput_f.get(i)));
      }

      if (isFirstUpdate)
      {
         impulseUpdate.set(impulse);
      }
      else
      {
         CommonOps.add(1.0 - alpha, impulsePrevious, alpha, impulse, impulse);
      }

      isImpulseZero = NormOps.normP2(impulse) < 1.0e-12;

      if (isImpulseZero)
      {
         rigidBodyTwistModifier.setImpulseToZero();
         jointTwistModifier.setImpulseToZero();
      }
      else
      {
         rigidBodyTwistModifier.setImpulse(impulse);
         jointTwistModifier.setImpulse(impulse);
      }

      impulsePrevious.set(impulse);
      isFirstUpdate = false;
   }

   public void setSpringConstant(double springConstant)
   {
      this.springConstant = springConstant;
   }

   @Override
   public List<OneDoFJointBasics> getJointTargets()
   {
      return jointsAtLimit;
   }

   @Override
   public List<? extends RigidBodyBasics> getRigidBodyTargets()
   {
      return Collections.emptyList();
   }

   public List<ActiveLimit> getActiveLimits()
   {
      return activeLimits;
   }

   public DenseMatrix64F getImpulse()
   {
      return impulse;
   }

   @Override
   public double getImpulseUpdate()
   {
      return NormOps.normP2(impulseUpdate);
   }

   @Override
   public double getVelocityUpdate()
   {
      return NormOps.normP2(jointVelocityUpdate);
   }

   @Override
   public boolean isConstraintActive()
   {
      return !isImpulseZero;
   }

   @Override
   public void setExternalTwistModifier(JointStateProvider externalJointTwistModifier)
   {
      this.externalJointTwistModifier = externalJointTwistModifier;
   }

   @Override
   public int getNumberOfRobotsInvolved()
   {
      return 1;
   }

   @Override
   public RigidBodyBasics getRootBody(int index)
   {
      return rootBody;
   }

   @Override
   public RigidBodyTwistProvider getRigidBodyTwistChangeProvider(int index)
   {
      return rigidBodyTwistModifier;
   }

   @Override
   public JointStateProvider getJointTwistChangeProvider(int index)
   {
      return jointTwistModifier;
   }

   @Override
   public DenseMatrix64F getJointVelocityChange(int index)
   {
      if (!isConstraintActive())
         return null;

      DenseMatrix64F response = responseCalculator.propagateImpulse();
      if (response != null)
         return response;

      for (int i = 0; i < jointsAtLimit.size(); i++)
      {
         responseCalculator.applyJointImpulse(jointsAtLimit.get(i), impulse.get(i));
      }
      return responseCalculator.propagateImpulse();
   }

   public DenseMatrix64F getJointVelocityDueToOtherImpulse()
   {
      return jointVelocityDueToOtherImpulse;
   }

   public MultiBodyResponseCalculator getResponseCalculator()
   {
      return responseCalculator;
   }
}
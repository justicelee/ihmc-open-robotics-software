package us.ihmc.steppr.hardware.visualization;

import java.util.EnumMap;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.acsell.parameters.BonoRobotModel;
import us.ihmc.robotDataCommunication.YoVariableClient;
import us.ihmc.robotDataCommunication.visualizer.SCSYoVariablesUpdatedListener;
import us.ihmc.steppr.hardware.StepprActuator;
import us.ihmc.steppr.hardware.StepprDashboard;
import us.ihmc.steppr.hardware.StepprJoint;
import us.ihmc.steppr.hardware.configuration.StepprNetworkParameters;
import us.ihmc.yoUtilities.dataStructure.YoVariableHolder;
import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;

import com.yobotics.simulationconstructionset.IndexChangedListener;
import com.yobotics.simulationconstructionset.OneDegreeOfFreedomJoint;
import com.yobotics.simulationconstructionset.util.inputdevices.SliderBoardConfigurationManager;

public class StepprPDSliderboard extends SCSYoVariablesUpdatedListener implements IndexChangedListener
{

   private final SDFRobot robot;
   private final YoVariableRegistry sliderBoardRegistry = new YoVariableRegistry("StepprPDSliderBoard");
   private final EnumYoVariable<StepprJoint> selectedJoint = new EnumYoVariable<>("selectedJoint", sliderBoardRegistry, StepprJoint.class);

   private final BooleanYoVariable selectedJoint_enabled = new BooleanYoVariable("selectedJoint_enabled", sliderBoardRegistry);
   private final DoubleYoVariable selectedJoint_q = new DoubleYoVariable("selectedJoint_q", sliderBoardRegistry);
   private final DoubleYoVariable selectedJoint_qd = new DoubleYoVariable("selectedJoint_qd", sliderBoardRegistry);
   private final DoubleYoVariable selectedJoint_tau = new DoubleYoVariable("selectedJoint_tau", sliderBoardRegistry);
   private final DoubleYoVariable selectedJoint_tau_d = new DoubleYoVariable("selectedJoint_tau_d", sliderBoardRegistry);

   private final DoubleYoVariable selectedJoint_q_d = new DoubleYoVariable("selectedJoint_q_d", sliderBoardRegistry);
   private final DoubleYoVariable selectedJoint_qd_d = new DoubleYoVariable("selectedJoint_qd_d", sliderBoardRegistry);
   private final DoubleYoVariable selectedJoint_kp = new DoubleYoVariable("selectedJoint_kp", sliderBoardRegistry);
   private final DoubleYoVariable selectedJoint_kd = new DoubleYoVariable("selectedJoint_kd", sliderBoardRegistry);
   private final DoubleYoVariable selectedJoint_tauFF = new DoubleYoVariable("selectedJoint_tauFF", sliderBoardRegistry);
   private final DoubleYoVariable selectedJoint_damping = new DoubleYoVariable("selectedJoint_damping", sliderBoardRegistry);
   
   private volatile boolean started = false;

   private final EnumMap<StepprJoint, JointVariables> allJointVariables = new EnumMap<>(StepprJoint.class);

   public StepprPDSliderboard(SDFRobot robot, int bufferSize)
   {
      super(robot, bufferSize);

      this.robot = robot;
      scs.getDataBuffer().attachIndexChangedListener(this);
      registry.addChild(sliderBoardRegistry);
   }

   @Override
   public void start()
   {
      final SliderBoardConfigurationManager sliderBoardConfigurationManager = new SliderBoardConfigurationManager(scs);

      for (StepprJoint joint : StepprJoint.values)
      {
         JointVariables variables = new JointVariables(joint, registry);

         OneDegreeOfFreedomJoint oneDoFJoint = robot.getOneDegreeOfFreedomJoint(joint.getSdfName());
         sliderBoardConfigurationManager.setKnob(1, selectedJoint, 0, StepprJoint.values.length);
         sliderBoardConfigurationManager.setSlider(1, variables.q_d, oneDoFJoint.getJointLowerLimit(), oneDoFJoint.getJointUpperLimit());
         sliderBoardConfigurationManager.setSlider(2, variables.qd_d, -1, 1);
         sliderBoardConfigurationManager.setSlider(3, variables.kp, 0, 100);
         sliderBoardConfigurationManager.setSlider(4, variables.kd, 0, 1);
         
         if(Double.isNaN(oneDoFJoint.getTorqueLimit()) || Double.isInfinite(oneDoFJoint.getTorqueLimit()))
         {
            sliderBoardConfigurationManager.setSlider(5, variables.tauFF, -10, 10);            
         }
         else
         {
            sliderBoardConfigurationManager.setSlider(5, variables.tauFF, -oneDoFJoint.getTorqueLimit(), oneDoFJoint.getTorqueLimit());
         }
         sliderBoardConfigurationManager.setSlider(6, variables.damping, 0, 1);

         sliderBoardConfigurationManager.setButton(1, variables.enabled);
         sliderBoardConfigurationManager.saveConfiguration(joint.toString());
         allJointVariables.put(joint, variables);
      }

      selectedJoint.addVariableChangedListener(new VariableChangedListener()
      {

         @Override
         public void variableChanged(YoVariable<?> v)
         {
            sliderBoardConfigurationManager.loadConfiguration(selectedJoint.getEnumValue().toString());
         }
      });

      selectedJoint.set(StepprJoint.RIGHT_KNEE_Y);

      StepprDashboard.createDashboard(scs, registry);
      super.start();
      
      started = true;
   }

   private class JointVariables
   {
      private final BooleanYoVariable enabled;

      private final DoubleYoVariable q;
      private final DoubleYoVariable qd;
      private final DoubleYoVariable tau;
      private final DoubleYoVariable tau_d;

      private final DoubleYoVariable q_d;
      private final DoubleYoVariable qd_d;

      private final DoubleYoVariable kp;
      private final DoubleYoVariable kd;

      private final DoubleYoVariable tauFF;
      private final DoubleYoVariable damping;

      public JointVariables(final StepprJoint joint, final YoVariableHolder variableHolder)
      {
         final String prefix = joint.getSdfName();
         final String namespace = "StepprCommand." + prefix;
         enabled = new BooleanYoVariable(joint.getSdfName() + "_enabled", sliderBoardRegistry);
         enabled.addVariableChangedListener(new VariableChangedListener()
         {

            @Override
            public void variableChanged(YoVariable<?> v)
            {
               for (StepprActuator actuator : joint.getActuators())
               {
                  BooleanYoVariable actEnabled = (BooleanYoVariable) variableHolder.getVariable("StepprCommand." + actuator.getName(), actuator.getName()
                        + "Enabled");
                  actEnabled.set(enabled.getBooleanValue());
               }
            }
         });

         String stateNameSpace;
         String qStateVariable;
         String qdStateVariable;
         String tauStateVariable;
         switch (joint)
         {
         case LEFT_ANKLE_X:
            stateNameSpace = "Steppr.leftAnkle";
            qStateVariable = "leftAnkle_q_x";
            qdStateVariable = "leftAnkle_qd_x";
            tauStateVariable = "leftAnkle_tau_x";
            break;
         case LEFT_ANKLE_Y:
            stateNameSpace = "Steppr.leftAnkle";
            qStateVariable = "leftAnkle_q_y";
            qdStateVariable = "leftAnkle_qd_y";
            tauStateVariable = "leftAnkle_tau_y";
            break;
         case RIGHT_ANKLE_X:
            stateNameSpace = "Steppr.rightAnkle";
            qStateVariable = "rightAnkle_q_x";
            qdStateVariable = "rightAnkle_qd_x";
            tauStateVariable = "rightAnkle_tau_x";
            break;
         case RIGHT_ANKLE_Y:
            stateNameSpace = "Steppr.rightAnkle";
            qStateVariable = "rightAnkle_q_y";
            qdStateVariable = "rightAnkle_qd_y";
            tauStateVariable = "rightAnkle_tau_y";
            break;

         default:
            stateNameSpace = "Steppr." + prefix;
            qStateVariable = prefix + "_q";
            qdStateVariable = prefix + "_qd";
            tauStateVariable = prefix + "_tau";
         }
         q = (DoubleYoVariable) variableHolder.getVariable(stateNameSpace, qStateVariable);
         qd = (DoubleYoVariable) variableHolder.getVariable(stateNameSpace, qdStateVariable);
         tau = (DoubleYoVariable) variableHolder.getVariable(stateNameSpace, tauStateVariable);
         
         q_d = (DoubleYoVariable) variableHolder.getVariable("StepprPDJointController", prefix + "_q_d");
         qd_d = (DoubleYoVariable) variableHolder.getVariable("StepprPDJointController", prefix + "_qd_d");
         tauFF = (DoubleYoVariable) variableHolder.getVariable("StepprPDJointController", prefix + "_tau_ff");
         kp = (DoubleYoVariable) variableHolder.getVariable("StepprPDJointController", "kp_" + prefix);
         kd = (DoubleYoVariable) variableHolder.getVariable("StepprPDJointController", "kd_" + prefix);
         damping = (DoubleYoVariable) variableHolder.getVariable(namespace, prefix + "Damping");

         tau_d = (DoubleYoVariable) variableHolder.getVariable(namespace, prefix + "TauDesired");

      }

      public void update()
      {
         selectedJoint_enabled.set(enabled.getBooleanValue());
         selectedJoint_q.set(q.getDoubleValue());
         selectedJoint_qd.set(qd.getDoubleValue());
         selectedJoint_tau.set(tau.getDoubleValue());
         selectedJoint_tau_d.set(tau_d.getDoubleValue());
         selectedJoint_q_d.set(q_d.getDoubleValue());
         selectedJoint_qd_d.set(qd_d.getDoubleValue());
         selectedJoint_kp.set(kp.getDoubleValue());
         selectedJoint_kd.set(kd.getDoubleValue());
         selectedJoint_tauFF.set(tauFF.getDoubleValue());
         selectedJoint_damping.set(damping.getDoubleValue());
      }
   }

   @Override
   public void indexChanged(int newIndex, double newTime)
   {
      if(started)
      {
         StepprJoint joint = selectedJoint.getEnumValue();
         allJointVariables.get(joint).update();
      }
   }

   public static void main(String[] args)
   {
      System.out.println("Connecting to host " + StepprNetworkParameters.CONTROL_COMPUTER_HOST);
      BonoRobotModel robotModel = new BonoRobotModel(true, false);
      SDFRobot robot = robotModel.createSdfRobot(false);

      SCSYoVariablesUpdatedListener scsYoVariablesUpdatedListener = new StepprPDSliderboard(robot, 16384);

      YoVariableClient client = new YoVariableClient(StepprNetworkParameters.CONTROL_COMPUTER_HOST, StepprNetworkParameters.VARIABLE_SERVER_PORT,
            scsYoVariablesUpdatedListener, "remote", false);
      client.start();

   }

}

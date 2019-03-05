package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates;

import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsOptimizationSettingsCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.ControllerCoreOptimizationSettings;
import us.ihmc.yoVariables.parameters.DoubleParameter;
import us.ihmc.yoVariables.providers.DoubleProvider;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class ParameterizedControllerCoreOptimizationSettings
{
   private final YoVariableRegistry registry = new YoVariableRegistry(ControllerCoreOptimizationSettings.class.getSimpleName());

   private final DoubleProvider rhoMin;

   private final InverseDynamicsOptimizationSettingsCommand command = new InverseDynamicsOptimizationSettingsCommand();

   public ParameterizedControllerCoreOptimizationSettings(ControllerCoreOptimizationSettings defaultOptimizationSettings, YoVariableRegistry parentRegistry)
   {
      rhoMin = new DoubleParameter("RhoMin", registry, defaultOptimizationSettings.getRhoMin());
      parentRegistry.addChild(registry);
   }

   public InverseDynamicsOptimizationSettingsCommand getCommand()
   {
      command.setRhoMin(getRhoMin());
      return command;
   }

   public double getRhoMin()
   {
      return rhoMin.getValue();
   }
}

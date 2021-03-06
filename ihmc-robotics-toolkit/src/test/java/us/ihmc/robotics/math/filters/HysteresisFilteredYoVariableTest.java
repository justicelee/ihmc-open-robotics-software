package us.ihmc.robotics.math.filters;

import static us.ihmc.robotics.Assert.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

public class HysteresisFilteredYoVariableTest
{
   YoRegistry registry = new YoRegistry("TestHysteresisFilteredYoVariable");
   private YoDouble guideLineHysteresis = new YoDouble("guideLineHyst", registry);
   private HysteresisFilteredYoVariable filteredYoVariable = new HysteresisFilteredYoVariable("test", registry, guideLineHysteresis);
   private double epsilon = 1e-7;

   @BeforeEach
   public void setUp() throws Exception
   {
   }

   @AfterEach
   public void tearDown() throws Exception
   {
   }

   @Test
   public void testNoHysteresis()
   {
      guideLineHysteresis.set(0.0);

      double stepSize = Math.PI / 100.0;
      double maximumValue = Math.PI * 2.0;
      double[] x = getAbscissa(stepSize, maximumValue);

      double[] unfilteredValues = sin(x);

      double[] filteredValues = filter(unfilteredValues, filteredYoVariable);

      int n = filteredValues.length;
      for (int i = 0; i < n; i++)
      {
         assertTrue(Math.abs(filteredValues[i] - unfilteredValues[i]) < epsilon);
      }

      //    plot(x, new double[][]{unfilteredValues, filteredValues}, "No Hysteresis");
   }

   @Test
   public void testSomeHysteresis()
   {
      guideLineHysteresis.set(0.2);

      double stepSize = Math.PI / 50.0;
      double maximumValue = Math.PI * 4.0;
      double[] x = getAbscissa(stepSize, maximumValue);

      double[] unfilteredValues = sin(x);

      double[] filteredValues = filter(unfilteredValues, filteredYoVariable);

      @SuppressWarnings("unused")
      int n = filteredValues.length;

      //    plot(x, new double[][]{unfilteredValues, filteredValues}, "Hysteresis = " + guideLineHysteresis.val);
   }

   private double[] getAbscissa(double stepSize, double maximumValue)
   {
      int numberOfSteps = (int) (maximumValue / stepSize);
      double[] ret = new double[numberOfSteps];
      for (int i = 0; i < numberOfSteps; i++)
      {
         ret[i] = i * stepSize;
      }

      return ret;
   }

   private double[] sin(double[] abscissa)
   {
      int n = abscissa.length;
      double[] ret = new double[n];
      for (int i = 0; i < n; i++)
      {
         ret[i] = Math.sin(abscissa[i]);
      }

      return ret;
   }

   private double[] filter(double[] unfilteredValues, HysteresisFilteredYoVariable hysteresisFilteredYoVariable)
   {
      int n = unfilteredValues.length;
      double[] ret = new double[n];
      for (int i = 0; i < n; i++)
      {
         hysteresisFilteredYoVariable.update(unfilteredValues[i]);
         ret[i] = hysteresisFilteredYoVariable.getDoubleValue();
      }

      return ret;
   }

   // private void plot(double[] abscissa, double[][] ordinates, String name)
   // {
   //    double numberOfTrajectories = ordinates.length;
   //
   //    ArrayList<double[][]> listOfCurves1 = new ArrayList<double[][]> ();
   //    for (int i = 0; i < numberOfTrajectories; i++)
   //    {
   //       listOfCurves1.add(new double[][]
   //                         {abscissa, ordinates[i]});
   //    }
   //
   //    PlotGraph2d pg1 = PlotGraph2d.createPlotGraph2dMultipleCurves(listOfCurves1);
   //    pg1.plot();
   //    pg1.setGraphTitle(name);
   //
   //    sleepForever();
   // }
   //
   //   @SuppressWarnings("unused")
   //   private void sleepForever()
   //   {
   //      while (true)
   //      {
   //         try
   //         {
   //            Thread.sleep(1000);
   //         }
   //         catch (InterruptedException ex)
   //         {
   //         }
   //      }
   //   }
}

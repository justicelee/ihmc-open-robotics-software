package us.ihmc.robotics.functionApproximation;

import static us.ihmc.robotics.Assert.*;

import java.util.Random;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
public class OnlineLinearRegressionTest {

	@Test
	   public void toyExample()
	   {
		   int testSize=100;
		   int inputDim=10;
		   Random rnd = new Random(0);
		   
		   double[] trueCoefficient = new double[inputDim];
		   for(int i=0;i<inputDim;i++)
			   trueCoefficient[i] = rnd.nextDouble();
			   
		   
		   OnlineLinearRegression solver = new OnlineLinearRegression(100, inputDim);
		   for(int i=0;i<testSize;i++)
		   {
			   double y=0;
			   double[] x=new double[inputDim];
			   for(int j=0;j<inputDim;j++)
			   {
				   x[j]=rnd.nextDouble();
				   y+=trueCoefficient[j]*x[j];
			   }
			   solver.addEntry(y,x);
		   }
		   
		   double [] resultCoefficient = solver.getCoefficient();
		   assertArrayEquals(trueCoefficient, resultCoefficient, 1e-10);
	   }

}

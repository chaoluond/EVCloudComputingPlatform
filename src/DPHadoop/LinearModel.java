/**
 * 
 */
package DPHadoop;

import Jama.Matrix;





/**
 * @author CHAO LUO
 *
 */
public class LinearModel {
	
	public static Matrix[] H;
	
	public static double getNewCoefficients(double[] prices, double order, double error, 
			int horizon) {
		
		double maxDiff = 0;
		double[] realDemands = DemandSimulator.getRealDemand(prices);
		double[] estiDemands = DemandSimulator.getEstimateDemand(prices);
		int num = InputData.K; // Number of charging stations
		
		for (int i = 0; i < num; i++) {
			double[] newPrice = new double[num + 1];
			newPrice[0] = 1;
			
			// Calculate the maximum different between real demands andd estimated demands
			maxDiff = Math.max(maxDiff, Math.abs(realDemands[i] - estiDemands[i]) / realDemands[i]);
			
			
			for (int j = 1; j < num + 1; j++) {
				newPrice[j] = prices[j - 1];
			}
			
			
			/*
			 * b_t = b_{t - 1} + H_{t - 1} ^ (-1)(d_{t - 1} - x_{t - 1} ^ Tb_{t - 1})
			 * H_t = H_{t - 1} + x_{t - 1}x_{t - 1} ^ T
			 */
			Matrix priceInput = new Matrix(newPrice, num + 1); // construct a column vector
			
			double delta = realDemands[i] - estiDemands[i];
			double under = InputData.forgettingfactor + 
							priceInput.transpose().times(H[i]).times(priceInput).getRowPackedCopy()[0];
			
			Matrix g = H[i].times(priceInput).times(1 / under);
			Matrix w = new Matrix(InputData.estimateCoefficients[i], num + 1);
			
			w = w.plus(g.times(delta));
			InputData.estimateCoefficients[i] = w.getRowPackedCopy();
			
			//Update H[i]
			H[i] = H[i].times(1 / InputData.forgettingfactor).minus(g.times(priceInput.transpose()).times(1 / 
						InputData.forgettingfactor).times(H[i]));
			
			
										
			
			}
		
		double curInv = (double)InputData.currentInventory;
		double demandSum = 0;
		
		for (int k = 0; k < num; k++)
			demandSum += realDemands[k];
		
		InputData.currentInventory = (int)((curInv + order - 
					demandSum) * InputData.beta2);
		
		return maxDiff;
		
					
		
		
	}
	
	
}

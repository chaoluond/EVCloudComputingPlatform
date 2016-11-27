/**
 * 
 */
package DPHadoop;

import java.util.Random;


/**
 * @author CHAO LUO
 *
 */
public class DemandSimulator {
	//public DemandSimulator(){}
	
	
	// demands[][0] is the real charging demand without noise
	// demands[][1] is the real charging demand with noise
	public static double[][] getRealDemand(double[] prices, Random rd) {

		int num = InputData.K;
		double[][] coef = InputData.realCoefficients;
		double noise = 0;
		
		//double[] timeCoef = InputData.timeCoef;
		double[][] demands = new double[num][2];
		for (int i = 0; i < num; i++) {
			demands[i][0] = coef[i][0];
			for (int j = 0; j < num; j++) {
				demands[i][0] += (coef[i][j + 1] * prices[j]);
			}
			
			noise = rd.nextGaussian();
			while (noise + demands[i][0] < 0) {
				noise = rd.nextGaussian();
			}
			
			demands[i][1] = noise + demands[i][0];
			
			

		}
		
		return demands;
		
		
		
	}
	
	public static double[] getEstimateDemand(double[] prices) {
		int num = InputData.K;
		double[][] coef = InputData.estimateCoefficients;
		//System.out.println(Arrays.toString(coef[0]));
		double[] estimateDemands = new double[num];
		for (int i = 0; i < num; i++) {
			estimateDemands[i] = coef[i][0];
			for (int j = 0; j < num; j++) {
				estimateDemands[i] += (coef[i][j + 1] * prices[j]);
			}
		}
		
		return estimateDemands;
	}
}

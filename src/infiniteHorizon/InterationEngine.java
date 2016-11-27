package infiniteHorizon;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import DPHadoop.DemandSimulator;
import DPHadoop.InputData;
import Jama.Matrix;

public class InterationEngine {
	
	public static double[][] finalPolicy;
	public static double[] finalMaxU;
	public static double[] finalJ;
	
	
	public static void run() {
		
		
		int states = InputData.invenRange / InputData.interval;
		int numCtrl = InputData.groups + 1;

		// Step 1: Guess an initial stationary policy
		double[][] policy = new double[states][numCtrl];
		for (int i = 0; i < states; i++)
			policy[i] = InputData.policyChoice[i].get(2);
		
		
		boolean flag = true;
		int count = 0;
		double[] preMaxU = new double[states];
		double[] J = new double[states];
		while (flag) {
			
			
			
			try {
				writeToFile("policy.txt", policy);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// Step 2: Policy evaluation
			
		    /**** update the transition probability matrix and cost vector ********/
			double[][] transProb = new double[states][states];
			double[] costVector = new double[states];
			for (int i = 0; i < InputData.invenRange; i = i + InputData.interval) {
				double[] temp = Initialization.createXVector(policy[i / InputData.interval]);
				double[] prices = new double[InputData.K];
				for (int k = 0; k < InputData.K; k++)
					prices[k] = temp[k];
				
				double order = temp[InputData.K];
		
				double[] estiDemands = DemandSimulator.getEstimateDemand(prices);
			
			    /*** update transition probability **********************/
				double demandsum = 0;
				for (int k = 0; k < InputData.K; k++)
					demandsum += estiDemands[k];
				
				double unsold = i + order - demandsum;
				int newState = (int)(unsold / InputData.interval);
				transProb[i / InputData.interval][newState] = 1;
				/********************************************************/
			
				/**** update the cost vector  ***************************/
				
				costVector[i / InputData.interval] = computeCostPerStage(prices, order, unsold, estiDemands, 0);
			
				/********************************************************/
				
			
				
			}
			
			Matrix costs = new Matrix(costVector, states);
			Matrix trans = new Matrix(transProb);
			Matrix temp = Matrix.identity(states, states);
			temp = temp.minus(trans.times(InputData.utilitydisc));
			J = temp.inverse().times(costs).getRowPackedCopy();

			
			
			
			
			// Step 3: Policy improvement
			double[][] newPolicy = new double[states][numCtrl];
			
			double[] curMaxU = new double[states];
			for (int i = 0; i < InputData.invenRange; i = i + InputData.interval) {
				double maxU = -100000;
				double[] currentPol = new double[InputData.groups + 1];
				for (double[] pol : InputData.policyChoice[i / InputData.interval]) {
					double[] XVector = Initialization.createXVector(pol);
					double[] prices = new double[InputData.K];
					for (int k = 0; k < InputData.K; k++)
						prices[k] = XVector[k];
					
					double order = XVector[InputData.K];
					
					double[] estiDemands = DemandSimulator.getEstimateDemand(prices);
					
					double demandsum = 0;
					for (int k = 0; k < InputData.K; k++)
						demandsum += estiDemands[k];
					
					double unsold = i + order - demandsum;
					int newState = (int)(unsold / InputData.interval);
					
					double costPerStage = computeCostPerStage(prices, order, unsold, estiDemands, 0);
					
					
					if (costPerStage + InputData.utilitydisc * J[newState] > maxU) {
						maxU = costPerStage + InputData.utilitydisc * J[newState];
						currentPol = pol;
						//System.out.println("cost per stage is " + costPerStage);
						//System.out.println("next stage is " + InputData.utilitydisc * J[newState]);
					}
					
					
				}
				
				curMaxU[i / InputData.interval] = maxU;
				
				newPolicy[i / InputData.interval] = currentPol;
				
			}
			
			// Compare newPolicy and policy
			if (Arrays.deepEquals(newPolicy, policy))
				flag = false;
			policy = newPolicy;
			
			System.out.println("Count is " + count);
			count++;
			
			// Calculate improvement
			double preMaxSum = 0;
			double curMaxSum = 0;
			
			for (int i = 0; i < states; i++) {
				preMaxSum += preMaxU[i];
				curMaxSum += curMaxU[i];
			}
			double improvement = (curMaxSum - preMaxSum) / preMaxSum * 100;
			System.out.println("Improvement is " + improvement + "%");
			System.out.println(curMaxSum + " and " + preMaxSum);
			preMaxU = curMaxU;
			

			
			
			
		}
		
		finalPolicy = policy;
		finalMaxU = preMaxU;
		finalJ = J;
		
	}
	
	
	public static double computeCostPerStage(double[] prices, double order, 
			double unsold, double[] demands, int stage) {
		double cost = 0;
		for (int k = 0; k < InputData.K; k++)
			cost += prices[k] * demands[k];
		
		// cost of purchase
		cost -= InputData.wholePrice[stage] * order;
		
		// cost of storage
		cost -= (InputData.eta * unsold);
		
		// penalty of power grid
		cost -= InputData.mu * Math.pow(order - InputData.Oref, 2);
		
		
		return cost;
	}
	
	
	public static void writeToFile(String fileName, double[][] array) throws IOException {
		BufferedWriter out = null;
		
		try {
			out = new BufferedWriter(new FileWriter(fileName, true));
			for (int i = 0; i < array.length; i++) {
				for (int j = 0; j < array[0].length; j++) {
					String temp = String.format("%.3f", array[i][j]) + ' ';
					out.write(temp);;
				}
				
				out.newLine();
				
			}
			
				out.newLine();
				out.newLine();
				out.newLine();
				out.newLine();
			
			
			
		} catch (IOException e) {  
			   e.printStackTrace();  
		  } finally {    
		   if (out != null)  
		    out.close();  
		  }  
		
	}

}

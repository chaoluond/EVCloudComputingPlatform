/**
 * 
 */
package infiniteHorizon;

import java.io.IOException;
import java.util.Arrays;

import DPHadoop.DemandSimulator;
import DPHadoop.InputData;
import Jama.Matrix;

/**
 * @author Chao
 *
 */
public class BBIterationEngine2 {
	
	public BBIterationEngine2() {
	
	}
	
	public double[][][] run() {
		
		// The flag for all finished
		boolean allFinish = true;
		
		// The period
		int period = InputData.horizon;
		
		
		// The number of control variables
		int numVar = InputData.K + 1;
		
		// The number of states
		int states = InputData.invenRange / InputData.interval;
			
		// Tolerance
		double tol = InputData.tolerance;
		
		double[][][] solutions = new double[period][states][];
		double[][] preMaxU = new double[period][states];
		double[][] solution = new double[states][];
		double[][] prevJ = new double[period][states];
		
		// Step 1: Guess an initial stationary policy
		InterationEngine.run();
		double[][] initPolicy = InterationEngine.finalPolicy;
		double[] premaxU = InterationEngine.finalMaxU;
		double[] initJ = InterationEngine.finalJ;
		
		for (int i = 0; i < InputData.invenRange; i = i + InputData.interval)
			solution[i / InputData.interval] = Initialization.createXVector(initPolicy[i / InputData.interval]);
		
		for (int i = 0; i < period; i++) {
			solutions[i] = solution;
			preMaxU[i] = premaxU;
			prevJ[i] = initJ;
		}
		
		
		
		
		int count = 0;
		while (true) {
	
			for (int i = 0; i < period; i++) {
				String fileName = "solution" + Integer.toString(i) + ".txt";
				try {
					InterationEngine.writeToFile(fileName, solutions[i]);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
			

			
			// Step 2: Policy evaluation
			
		    /**** update the transition probability matrix and cost vector ********/
			double[][] J = new double[period][states];
			
			for (int i = 0; i < period; i++) {
				
				double[][] transProb = new double[states][states];
				double[] costVector = new double[states];
				for (int j = 0; j < states; j++) {
					
					double[] prices = new double[InputData.K];
					for (int k = 0; k < InputData.K; k++)
						prices[k] = solutions[i][j][k];
					
					double order = solutions[i][j][InputData.K];
			
					double[] estiDemands = DemandSimulator.getEstimateDemand(prices);
				
				    /*** update transition probability **********************/
					double demandsum = 0;
					for (int k = 0; k < InputData.K; k++)
						demandsum += estiDemands[k];
					
					double unsold = (j * InputData.interval) + order - demandsum;
					int newState = (int)(unsold / InputData.interval);
					transProb[j][newState] = 1;
					/********************************************************/
				
					/**** update the cost vector  ***************************/
					
					costVector[j] = InterationEngine.computeCostPerStage(prices, order, unsold, estiDemands, i);
				
					/********************************************************/
					
				
					
				}
				
				Matrix costs = new Matrix(costVector, states);
				Matrix trans = new Matrix(transProb);
				int nextStage = (i + 1) % InputData.horizon;
				Matrix preJ = new Matrix(prevJ[nextStage], states);
				Matrix temp = costs.plus(trans.times(preJ).times(InputData.utilitydisc));
				J[i] = temp.getRowPackedCopy();

			}
			
			prevJ = J;
			
			
			
			
			
			
			
			
			
			
			
			// Step 3: Policy improvement
			allFinish = true;
			
			for (int i = 0; i < period; i++) { // Iterate for each period
				
				double[][] newSolution = new double[states][numVar];
				double[] curMaxU = new double[states];
				
				
				for (int j = 0; j < states; j++) { // Iterate over each state

					System.out.println("count is " + count + " horizon is " + i  + " state is " + j * InputData.interval);
					BranchAndBoundSearch BBS = new BranchAndBoundSearch(numVar, tol, preMaxU[i][j], solutions[i][j], j, prevJ[i], i);
					BBS.search();
					curMaxU[j] = BBS.curMaxU;
					newSolution[j] = BBS.incumbent;
					
				}
				
				
				
				// Compare newPolicy and policy
				if (!Arrays.deepEquals(newSolution, solutions[i])) {
					allFinish = false;
				}
				
				
				solutions[i] = newSolution;
				preMaxU[i] = curMaxU;
				
				// Calculate improvement
				/*double preMaxSum = 0;
				double curMaxSum = 0;
				
				for (int k = 0; k < states; k++) {
					preMaxSum += preMaxU[i][k];
					curMaxSum += curMaxU[k];
				}
				double improvement = (curMaxSum - preMaxSum) / preMaxSum * 100;
				System.out.println("Improvement is " + improvement + "%");
				System.out.println(curMaxSum + " and " + preMaxSum);
				preMaxU[i] = curMaxU;*/
			}
			
			count++;
			
			if (allFinish) {
				
				for (int i = 0; i < period; i++) {
					String fileName = "finalSolution" + Integer.toString(i) + ".txt";
					try {
						InterationEngine.writeToFile(fileName, solutions[i]);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
				break;
			}
				
			
			
			
			
			

			
			
			
		}
		
		return solutions;
		
		
		
	}
	
}

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
public class BBIterationEngine {
	public double[][] finalSolution;
	public double[] finalMaxU;
	
	public BBIterationEngine() {
	
	}
	
	public void run() {
		
		
		// The number of control variables
		int numVar = InputData.K + 1;
		
		// The number of states
		int states = InputData.invenRange / InputData.interval;
		
		// Tolerance
		double tol = InputData.tolerance;
		
		double[][] solution = new double[states][];
		
		
		// Step 1: Guess an initial stationary policy
		InterationEngine.run();
		double[][] initPolicy = InterationEngine.finalPolicy;
		double[] preMaxU = InterationEngine.finalMaxU;
		
		for (int i = 0; i < InputData.invenRange; i = i + InputData.interval)
			solution[i / InputData.interval] = Initialization.createXVector(initPolicy[i / InputData.interval]);
		
		
		
		int count = 0;
		while (true) {
			
			
			
			try {
				InterationEngine.writeToFile("solution.txt", solution);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// Step 2: Policy evaluation
			
		    /**** update the transition probability matrix and cost vector ********/
			double[][] transProb = new double[states][states];
			double[] costVector = new double[states];
			for (int i = 0; i < states; i++) {
				
				double[] prices = new double[InputData.K];
				for (int k = 0; k < InputData.K; k++)
					prices[k] = solution[i][k];
				
				double order = solution[i][InputData.K];
		
				double[] estiDemands = DemandSimulator.getEstimateDemand(prices);
			
			    /*** update transition probability **********************/
				double demandsum = 0;
				for (int k = 0; k < InputData.K; k++)
					demandsum += estiDemands[k];
				
				double unsold = i * InputData.interval + order - demandsum;
				int newState = (int)(unsold / InputData.interval);
				transProb[i][newState] = 1;
				/********************************************************/
			
				/**** update the cost vector  ***************************/
				
				costVector[i] = InterationEngine.computeCostPerStage(prices, order, unsold, estiDemands, 0);
			
				/********************************************************/
				
			
				
			}
			
			Matrix costs = new Matrix(costVector, states);
			Matrix trans = new Matrix(transProb);
			Matrix temp = Matrix.identity(states, states);
			temp = temp.minus(trans.times(InputData.utilitydisc));
			double[] J = temp.inverse().times(costs).getRowPackedCopy();

			
			
			
			
			// Step 3: Policy improvement
			double[][] newSolution = new double[states][numVar];
			double[] curMaxU = new double[states];
			
			for (int i = 0; i < states; i++) {

				System.out.println("count is " + count + " state is " + i);
				BranchAndBoundSearch BBS = new BranchAndBoundSearch(numVar, tol, preMaxU[i], solution[i], i, J, 0);
				BBS.search();
				curMaxU[i] = BBS.curMaxU;
				newSolution[i] = BBS.incumbent;
				System.out.println(Arrays.toString(BBS.incumbent));
				
			}
			
			
			
			// Compare newPolicy and policy
			if (Arrays.deepEquals(newSolution, solution))
				break;
			
			
			solution = newSolution;
			
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
		
		finalSolution = solution;
		finalMaxU = preMaxU;
		
		
	}
	
}

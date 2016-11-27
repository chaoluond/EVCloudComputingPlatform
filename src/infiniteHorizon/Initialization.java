package infiniteHorizon;

import java.util.ArrayList;
import java.util.Arrays;

import DPHadoop.InputData;

public class Initialization {
	
	
	public static void init() {
		
		prepareConstraints();
		createGroupToSingleMap();
		
		int states = InputData.invenRange / InputData.interval;
		InputData.priceChoice = new double[InputData.priceNum];
		InputData.orderChoice = new double[InputData.orderNum];
		double priceInterval = (InputData.maxPrice - InputData.minPrice) * 1.0 / (InputData.priceNum - 1);
		double orderInterval = (InputData.maxOrder - InputData.minOrder) * 1.0 / (InputData.orderNum - 1);
		for (int i = 0; i < InputData.priceNum; i++) {
			InputData.priceChoice[i] = InputData.minPrice + i * priceInterval;
			InputData.orderChoice[i] = InputData.minOrder + i * orderInterval;
		}
		
		InputData.policyChoice = new ArrayList[InputData.invenRange];
		for (int i = 0; i < InputData.invenRange; i++) {
			InputData.policyChoice[i] = new ArrayList<double[]>();
			
		}
		
		double[][] policyCandidates = policyCandiGenerator(InputData.priceNum, InputData.groups + 1); // The policy candidates
		int num = (int)Math.pow(InputData.priceNum, InputData.groups + 1);
		for (int state = 0; state < states; state++) {
			for (int i = 0; i < num; i++) {
				double[] X = createXVector(policyCandidates[i]);
				//System.out.println(Arrays.toString(X));
				if (isValid(X, state)) {
					InputData.policyChoice[state].add(policyCandidates[i]);
				}
			}
		}
		
		
		
		
		
		
		
		
		
		
		
		
	}
	
	private static double[][] policyCandiGenerator(int numChoice, int entry){
		int num = (int)Math.pow(numChoice, entry);
		double[][] candidates = new double[num][entry];

		
		int index = 0;
		for (int i1 = 0; i1 < numChoice; i1++)
			for (int i2 = 0; i2 < numChoice; i2++)
				for (int i3 = 0; i3 < numChoice; i3++)
					for (int i4 = 0; i4 < numChoice; i4++)
						for (int i5 = 0; i5 < numChoice; i5++) 
							for (int i6 = 0; i6 < numChoice; i6++){
							double[] temp = new double[]{InputData.priceChoice[i1], InputData.priceChoice[i2],
									InputData.priceChoice[i3], InputData.priceChoice[i4], InputData.priceChoice[i5],
									InputData.orderChoice[i6]};
							candidates[index] = temp;
							index++;
						}
		
		
		return candidates;
		
		
		
		
	}
	
	public static boolean isValid(double[] X, int state) {
		for (int i = 1; i <= InputData.K; i++) {
			double sum = 0;
			for (int j = 0; j < InputData.K; j++)
				sum += InputData.G[i][j] * X[j];
			
			
			if (sum > InputData.h[i]) {
				return false;
			}
		}
		
		// Check the last two constraints
		double sum = 0;
		for (int j = 0; j < InputData.K + 1; j++) {
			sum += InputData.G[InputData.K + 1][j] * X[j];
		}
		
		if (sum > InputData.h[InputData.K + 1] + state * InputData.interval) {
			return false;
		}
		
		sum = 0;
		for (int j = 0; j < InputData.K + 1; j++) {
			sum += InputData.G[InputData.K + 2][j] * X[j];
		}
		
		if (sum > InputData.h[InputData.K + 2] + InputData.invenRange - state * InputData.interval) {
			return false;
		}
		
		return true;
		
	}
	
	private static void prepareConstraints() {
		
		// Update the G and h in InputData
		double[][] newEstCoefs = InputData.estimateCoefficients;
		
		// Update G in InputData
		int num = InputData.K;
		InputData.G[0][num] = 1;
		for (int i = 1; i < num + 1; i++) {
			for (int j = 0; j < num; j++) {
				InputData.G[i][j] = -newEstCoefs[i - 1][j + 1];
			}
		}
		for (int i = 0; i < num; i++) {
			double sum = 0;
			for (int j = 0; j < num; j++) {
				sum += newEstCoefs[i][j + 1];
			}
			InputData.G[num + 1][i] = sum;
			InputData.G[num + 2][i] = -sum;
		}
		InputData.G[num + 1][num] = -1;
		InputData.G[num + 2][num] = 1;
		
		
		
		
		// Update h in InputData

		InputData.h[0] = InputData.maxOrder;
		for (int i = 0; i < num; i++)
			InputData.h[i + 1] = newEstCoefs[i][0];
		double s = 0;
		for (int i = 0; i < num; i++) {
			s += newEstCoefs[i][0];
		}
		InputData.h[num + 1] = -s;
		InputData.h[num + 2] = s;
		
		
	}
	
	
	private static void createGroupToSingleMap() {
		int[] temp = new int[InputData.K];
		
		// For test
		int index = 0;
		
		for (int i = 0; i < InputData.groups; i++) {

			
			while (index < (i + 1) * 4) {
				temp[index] = i;
				index++;
			}
		}
		
		InputData.singleToGroup = temp;
		
		
	}
	
	
	public static double[] createXVector(double[] policy) {
		double[] X = new double[InputData.K + 1]; // Create the X vector
		
		for (int i = 0; i < InputData.K; i++) {
			X[i] = policy[InputData.singleToGroup[i]];
			
		}
		
		X[InputData.K] = policy[InputData.groups];
		
		return X;
 	}
	
	
}

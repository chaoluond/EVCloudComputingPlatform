/**
 * 
 */
package finiteHorizon;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import DPHadoop.InputData;
import DPHadoop.QPSolver;

/**
 * @author Chao Luo
 *
 */
public class IterationEngine {
	public int curHorizon;
	public int day;
	public int states; // the states for inventory
	public int statesRenewable;  // the states for renewable energy generation
	public double intervalRenewable; // the interval for renewable energy generation
	public double[][][] solutionsRenewable; // the solutions for renewable energy generation
	public double[][] newEstCoefs;
	public int num; // Number of charging stations
	public double[] Gamma;
	public double[][] P;
	public double[] q;
	public double r;
	public double[][] G;
	public double[] h;
	public double[][] Pcon; // The matrix for quadratic constraint
	public double[] qcon; // The vector for quadratic constraint
	public double rcon; // The constant term for quadratic constraint
	public double[] J;
	public double[] J2;
	public double[][] JStochastic;
	public double[][] JStochastic2;
	public double[][] solutions; 
	public boolean multitask;
	
	
	public IterationEngine(int day, int curHorizon) {
		newEstCoefs = InputData.estimateCoefficients;
		num = InputData.K; // Number of charing stations
		this.curHorizon = curHorizon;
		this.day = day;
		
		
		
		// Initialize the J and JStochastic vector
		states = InputData.invenRange / InputData.interval;
		statesRenewable = InputData.stateNum;
		intervalRenewable = InputData.maxSolarPower / statesRenewable;
		
		J = new double[states];
		JStochastic = new double[states][statesRenewable];
		int index = 0;
		
		for (int i = 0; i < InputData.invenRange; i = i + InputData.interval) {// terminal cost
			J[index] = i * InputData.marginalProfit;
			index++;
		}
		
		solutions = new double[states][num + 2];
		solutionsRenewable = new double[states][statesRenewable][num + 2];
		
	}
	
	
	// curCalHorizon is the current calculate horizon
	public synchronized void multithreadingStep(int curCalHorizon, boolean isRenewable) {
		multitask = false;
		ThreadController tc = new ThreadController(this, isRenewable, curCalHorizon);
		(new Thread(tc)).start();
		
		while (!multitask) {
			try{
				wait();
			}catch (InterruptedException e) {
				
			}
		}
			
	}
	

	
	
	public synchronized void setMultiTask() {
		multitask = true;
		notifyAll();
	}
	
	
	
	class ThreadController implements Runnable {
		public int numCPUs;
		public boolean[] CPUStatus;
		public IterationEngine engine;
		public boolean isRenewable;
		public int curCalHorizon;
		
		public ThreadController (IterationEngine engine, boolean isRenewable, int curCalHorizon) {
			numCPUs = Runtime.getRuntime().availableProcessors(); // Get the number of processors
			// Set all CPU status to 'free'
			//System.out.print("# of processors is " + numCPUs + "\n");
			CPUStatus = new boolean[numCPUs];
			for (int i = 0; i < numCPUs; i++) {
				CPUStatus[i] = true;
			}
			
			this.engine = engine;
			this.isRenewable = isRenewable;
			this.curCalHorizon = curCalHorizon;
			
		}
		
		
		
		@Override
		public void run() {
			
			J2 = new double[states];
			JStochastic2 = new double[states][statesRenewable];
			solutionsRenewable = new double[states][statesRenewable][num + 2];
			for (int i = 0; i < InputData.invenRange; i = i + InputData.interval) {
				// for curCalHorizon, we iterate over different inventory states
				boolean foundFreeCPU = false;
				// Determine if there is free CPUs so this thread can wait for one to become free
				
				while (!foundFreeCPU) {
					synchronized(this) {
						
						cpu: for (int j = 0; j < numCPUs; j++) {
							if (CPUStatus[j]) {
								// Start a new thread on this cpu and set its status to false
								foundFreeCPU = true;
								CPUStatus[j] = false;
								(new Thread(new CalEachStateThread(i, j, this, isRenewable, curCalHorizon))).start();
								break cpu;
							}
						}
					
					if (!foundFreeCPU) {
						waitForThread();
					}
					
					
					}
				}
			}
			
			boolean allFinish = false;
			while(!allFinish) {
				allFinish = true;
				synchronized(this) {
					cpus: for (int i = 0; i < numCPUs; i++) {
						if (!CPUStatus[i]) {
							allFinish = false;
							break cpus;
						}
					}
					
					if (!allFinish) {
						waitForThread();
					}
				
				
				}
			}
			
			InputData.decisionTable[curCalHorizon] = solutionsRenewable;
			engine.setMultiTask();	
				
		}
		
		
		
		private synchronized void waitForThread() {
			try {
				// System.out.println("ThreadController got no free cpus, waiting "+Arrays.toString(cpuStatus));
				wait();
				// System.out.println("NOTIFIED");
			} catch (InterruptedException e) {
			}
		}
		
		public synchronized void setCPUFree(int cupNumber) {
			CPUStatus[cupNumber] = true;
			notifyAll();
			
		}	
		
	}
	
	
	
	class CalEachStateThread implements Runnable {
		private ThreadController tc;
		private int cpuNum;
		private int inventory;
		private boolean isRenewable;
		private int curCalHorizon;
		
		// The scaling coefficients for profit, customer satisfaction, and impact on power grid
		private double lambda1 = InputData.lambdas[0]; 

		
		public CalEachStateThread(int inv, int cpu, ThreadController controller, boolean isRenewable, int curCalHorizon) {
			inventory = inv;
			tc = controller;
			cpuNum = cpu;	
			this.isRenewable = isRenewable;
			this.curCalHorizon = curCalHorizon;
		}
		
		@Override
		public void run() {
			
			double max = 0;// Store the max utility (isRenewable = false)
			double[] result = new double[num + 2]; // Store the prices and order (isRenewable = false)
			
			double[] maxVector = new double[statesRenewable]; // store the max utility for each combination of inv + rew
			double[][] resultArray = new double[statesRenewable][num + 2]; // Store the prices and order for each rew
			
			
			
			if (!isRenewable) {
				/***********************************************************************************************
				 * 
				 * 
				 * This does not consider renewable energy generation as a stochastic process
				 * 
				 * 
				 * 
				 */
				for (int inv = 0; inv < InputData.invenRange; inv = inv + InputData.interval) { // iterate over J[k + 1]
					double rTemp = -(r - InputData.eta * lambda1 * inventory + J[inv / InputData.interval]);
					double[] hTemp = h.clone();
					hTemp[num + 1] += (inventory - inv); // Update the last two rows of h vector
					hTemp[num + 2] += (inv + InputData.interval - inventory);
					double[] solution = QPSolver.solveQP(P, q, rTemp, Pcon, qcon, rcon, G, hTemp, null, null, true);
					if (solution[0] < max) {
						max = solution[0];
						result = solution;
						result[0] = -result[0];
						
					}
					
				}
				
				
				max = -max;
				
				int stateIndex = inventory / InputData.interval;
				J2[stateIndex] = max;
				solutions[stateIndex] = result;
			}
			else {
			
				/***************************************************************************************************
				 * 
				 * This part considers renewable energy generation as a stochastic process. The state variables are inventory
				 * and renewable energy generation for each planning horizon.
				 * 
				 */
				for (int inv = 0; inv < InputData.invenRange; inv = inv + InputData.interval) {
					// chop the entire optimization range into small pieces. 
					// We iterate over the inventory over the next planning horizon. 
					
					if (curCalHorizon >= 7 && curCalHorizon <= 17) {
						// we have renewable energy during this period
						// Actually, curCalHorizon = 7 means we consider 8:00 am in the dataset
						for (int renState = 0; renState < statesRenewable; renState++) {
							// consider state (inventory, renState)
							// for each curCalHorizon, for each inventory state, we iterate over different renewable energy states
							// renState means the renewable energy state for the current horizon.
							
							if (!InputData.renewableFlag[curCalHorizon - 7][renState])// this renewable energy is not possible
								continue;
							
							double renEnergy = (renState + 0.5) * intervalRenewable;
							double[] hTemp = h.clone();
							hTemp[num + 1] += (renEnergy + inventory - inv); // Update the last two rows of h vector
							hTemp[num + 2] += (inv + InputData.interval - inventory - renEnergy);
							
							double JNext = 0;
							double[][] probability = InputData.transition.get(curCalHorizon - 6);
							for (int flag = 0; flag < statesRenewable; flag++) {// average J_{k+1}(I_{k+1})
								
								
								/************************************************
								 * renState represents the renewable energy state at curCalHorizon
								 * flag represents the renewable energy state at curCalHorizon + 1 (previous state)
								 */
								JNext += probability[renState][flag] * JStochastic[inv / InputData.interval][flag];
							
							}
							
							double rTemp = -(r - InputData.eta * lambda1 * (inventory + renEnergy) + JNext);
							
							double rcon2 = rcon - InputData.eta * (Gamma[0] - inventory - renEnergy);
							
							double[] solution = QPSolver.solveQP(P, q, rTemp, Pcon, qcon, rcon2, 
									G, hTemp, null, null, true);
							
							if (solution[0] < maxVector[renState]) {
								maxVector[renState] = solution[0];
								resultArray[renState] = solution;
								resultArray[renState][0] = -resultArray[renState][0];	
							}
							
						}
						
					}
					else {// we do not consider renewable energy 
						// the renewable energy generation is 0
						double[] hTemp = h.clone();
						hTemp[num + 1] += (inventory - inv); // Update the last two rows of h vector
						hTemp[num + 2] += (inv + InputData.interval - inventory);
						double JNext = 0;
						if (curCalHorizon == 6) {// We are dealing with 7:00 am
							double[][] probability = InputData.transition.get(0);
							for (int j = 0; j < statesRenewable; j++)
								JNext += probability[0][j] * JStochastic[inv / InputData.interval][j];
							
							
						}
						else {
							JNext = JStochastic[inv / InputData.interval][0];
						}
						
						
						double rTemp = -(r - InputData.eta * lambda1 * inventory + JNext);
						
						double rcon2 = rcon - InputData.eta * (Gamma[0] - inventory);
						
						double[] solution = QPSolver.solveQP(P, q, rTemp, Pcon, qcon, rcon2, 
								G, hTemp, null, null, true);
						
						if (solution[0] < max) {
							max = solution[0];
							result = solution;
							result[0] = -result[0];
							
						}
						
						
					} // end if >= 7 <= 17
				} // end for loop
				
				
				if (curCalHorizon >= 7 && curCalHorizon <= 17) {
					for (int renState = 0; renState < statesRenewable; renState++) {
						int stateIndex = inventory / InputData.interval;
						if (InputData.renewableFlag[curCalHorizon - 7][renState]) {
							maxVector[renState] = -maxVector[renState];
							//System.out.println("max is " + maxVector[renState]);
							JStochastic2[stateIndex][renState] = maxVector[renState];
							solutionsRenewable[stateIndex][renState] = resultArray[renState];
						}
					}	
				}
				else {
					max = -max;
					int stateIndex = inventory / InputData.interval;
					JStochastic2[stateIndex][0] = max; // without renewable energy
					solutionsRenewable[stateIndex][0] = result; // without renewable energy
				}
						
			}// end if renewable
			
			
			
			
			tc.setCPUFree(cpuNum);
		}
		
			
				
	}
	
	
	
	public double[][][] calculateStochastic() {
		
		for (int i = InputData.horizon - 1; i >= curHorizon; i--) {// i starts from 23 and ends at 0
			
			prepareData(i, InputData.isRenewable);
			multithreadingStep(i, InputData.isRenewable);
			System.out.println("horizon " + i + " is done!");
			JStochastic = JStochastic2;
		}
		
		return solutionsRenewable;
	}
	
	
	public double[][] calculate() {
		int lastHorizon = InputData.horizon;
		J2 = new double[states];
		
		for (int i = lastHorizon - 1; i >= curHorizon; i--) { // Start from the terminating horizon,
															  // calculate the current horizon backward
			prepareData(i, InputData.isRenewable);
			multithreadingStep(i, InputData.isRenewable);
			System.out.println("horizon " + i + " is done!");
			J = J2;
			
		}
		
		try {
			writeToFile("solutionsRenewable.txt", solutions, day, curHorizon);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return solutions;
		
		
	}
	
	
	public double[] greedyCalculateStochastic(double curInventory, double curRenewable) {
		double inventory = curInventory;
		double renewable = curRenewable;
		double max = 0;
		double lambda1 = InputData.lambdas[0];
		
		double[] result = new double[num + 2];
		prepareData(curHorizon, InputData.isRenewable);
		
		double[] hTemp = h.clone();
		hTemp[num + 1] += (renewable + inventory); // Update the last two rows of h vector
		hTemp[num + 2] += (InputData.invenRange  - inventory - renewable);
		
		double rTemp = -(r - InputData.eta * lambda1 * (inventory + renewable));
		double rcon2 = rcon - InputData.eta * (Gamma[0] - inventory - renewable);
		
		double[] solution = QPSolver.solveQP(P, q, rTemp, Pcon, qcon, rcon2, G, hTemp, null, null, true);
		if (solution[0] < max) {
			max = solution[0];
			result = solution;
			result[0] = -result[0];
			
		}
		
		return result;
		
		
	}
	
	
	
	public double[] greedyCalculate(double curInventory) {
		
		double inventory = curInventory;
		double max = 0;
		double[] result = new double[num + 2];
		
		prepareData(curHorizon, false);
		double rTemp = -(r - InputData.eta * inventory); // Need to check this line
		double[] hTemp = h.clone();
		hTemp[num + 1] += inventory; // Need to check this two lines
		hTemp[num + 2] += (InputData.invenRange - inventory);
		
		double[] solution = QPSolver.solveQP(P, q, rTemp, Pcon, qcon, rcon, G, hTemp, null, null, true);
		if (solution[0] < max) {
			max = solution[0];
			result = solution;
			result[0] = -result[0];
			
		}
		
		return result;
		
		
	}
	
	
	
		
	
	public void prepareData(int horizon, boolean isRenewable) {
		double lambda1 = InputData.lambdas[0];
		double lambda2 = InputData.lambdas[1];
		double lambda3 = InputData.lambdas[2];
		int[] index = InputData.indInGrid;
		double[][] jacobian = InputData.Jacobian;
		
		
		// Prepare for Theta vector
		double[][] Theta = new double[num + 1][jacobian.length];
		for (int j = 0; j < jacobian.length; j++) {
			for (int k = 0; k < num + 1; k++) {
				double sum = 0;
				for (int i = 0; i < num; i++) {
					
					sum += jacobian[j][index[i]] * newEstCoefs[i][k];
				}
				
				Theta[k][j] = sum;
			}
		}
		
		
		
		// Prepare for Gamma vector. 
		Gamma = new double[num + 1];
		for (int i = 0; i < num + 1; i++) {
			double sum = 0;
			for (int j = 0; j < num; j++)
				sum += newEstCoefs[j][i];
			
			Gamma[i] = sum;
		}
		
		
		// Initialize P matrix. P matrix is for quadratic function. When we construct P, we actually construct -p 
		P = new double[num + 1][num + 1];
		for (int i = 0; i < num; i++) {
			for (int j = 0; j < num; j++) {
				double sum = 0;
				
				for (int k = 0; k < jacobian.length; k++) {
					sum += Theta[i + 1][k] * Theta[j + 1][k];
				}
				
				P[i][j] = -(newEstCoefs[i][j + 1] * lambda1 * 2) + InputData.alpha * lambda2 * 
				      Gamma[i + 1] * Gamma[j + 1] + 2 * lambda3 * sum;
			}
		}
				
		
		// Initialize P_con
		Pcon = new double[num + 1][num + 1];
		for (int i = 0; i < num; i++) {
			for (int j = 0; j < num; j++) {
				Pcon[i][j] = -newEstCoefs[i][j + 1];
			}
		}
		
		Pcon[num][num] = InputData.dumPara;
		
		
				
		// Initialize q vector. q vector is for quadratic function
		q = new double[num + 1];
		for (int i = 0; i < num; i++) {
			double sum = 0;
			
			for (int j = 0; j < num; j++) { 
				sum += newEstCoefs[i][j + 1];
			}
			
			
			double sum2 = 0;
			for (int k = 0; k < jacobian.length; k++) {
				sum2 += Theta[0][k] * Theta[i + 1][k];
			}
			
				
			
			q[i] = -(newEstCoefs[i][0] * lambda1 + (InputData.eta * lambda1 + lambda2 * InputData.omega) * sum
					- InputData.alpha * lambda2 * Gamma[0] * Gamma[i + 1] - 2 * lambda3 * sum2);
		}
		q[num] = -(-InputData.wholePrice[horizon] * lambda1 - InputData.eta * lambda1);
				
				
		
		// Initailize q_con
		qcon = new double[num + 1];
		double para = Math.sqrt(2 * Math.PI) * InputData.epsilon * InputData.sigma;
		for (int i = 0; i < num; i++) {
			qcon[i] = -newEstCoefs[i][0] - InputData.eta * Gamma[i + 1] - para;
		}
		qcon[num] = InputData.wholePrice[horizon] + InputData.eta;
		
		
				
		// Initialize InputData.r
		//double s = 0;
		//for (int i = 0; i < num; i++) 
		//	s += newEstCoefs[i][0];
		
		double sum3 = 0;
		for (int k = 0; k < jacobian.length; k++) {
			sum3 += Theta[0][k] * Theta[0][k];
		}
		
		
		double sum4 = InputData.sigma * num; // the sum of variance of noise from demand estimation
		
		double sum5 = 0;
		for (int j = 0; j < jacobian.length; j++) {
			for (int k = 0; k < num; k++) {
				sum5 += Math.pow(jacobian[j][index[k]], 2) * Math.pow(InputData.sigma, 2);
			}
		}
		
		r = (InputData.eta * lambda1 + lambda2 * InputData.omega) * Gamma[0]
			- InputData.alpha * lambda2 / 2 * (Gamma[0] * Gamma[0] + sum4) - lambda3 * (sum3 + sum5);
		
		// I made some changes here
		// Need to add some term each time
	
		
		if (!isRenewable) {
			r -= InputData.eta * lambda1 * InputData.renewable[horizon]; 
		}
		
		
		
		// Initialize r_con
		rcon = InputData.minProfit - InputData.epsilon * InputData.eta * Math.sqrt(2 * Math.PI) * num * InputData.sigma;
		
		
		
				
				
		// Initialize matrix G. G matrix is for constraint
		double s = 0;
		G = new double[num + 3][num + 1];
		G[0][num] = 1;
		for (int i = 1; i < num + 1; i++) {
			for (int j = 0; j < num; j++) {
				G[i][j] = -newEstCoefs[i - 1][j + 1];
			}
		}
		for (int i = 0; i < num; i++) {
			double sum = 0;
			for (int j = 0; j < num; j++) {
				sum += newEstCoefs[i][j + 1];
			}
			G[num + 1][i] = sum;
			G[num + 2][i] = -sum;
		}
		G[num + 1][num] = -1;
		G[num + 2][num] = 1;
		
		
		
		// Initialize h
		h = new double[num + 3];
		h[0] = InputData.maxOrder;
		for (int i = 0; i < num; i++)
			h[i + 1] = newEstCoefs[i][0];
		s = 0;
		for (int i = 0; i < num; i++) {
			s += newEstCoefs[i][0];
		}
		h[num + 1] = -s;
		h[num + 2] = s;
				
				
				
				
	}
	
	

	
		

	
	
	private void writeToFile(String fileName, double[][] array, int day, int horizon) throws IOException {
		BufferedWriter out = null;
		
		try {
			out = new BufferedWriter(new FileWriter(fileName, true));
			String title = "Day is " + Integer.toString(day) + ", current horizon is " + Integer.toString(horizon);
			out.write(title);
			out.newLine();
			for (int i = 0; i < array.length; i++) {
				for (int j = 0; j < array[0].length; j++) {
					String temp = String.format("%.3f", array[i][j]) + ' ';
					out.write(temp);
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

/**
 * 
 */
package infiniteHorizon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import cern.colt.Arrays;
import DPHadoop.DemandSimulator;
import DPHadoop.InputData;
import DPHadoop.QPSolver;

/**
 * @author Chao
 *
 */
public class BranchAndBoundSearch {
	// The previous J matrix
	double[] J;
	
	// The inventory state
	public int state;
	
	// The global maximum utility
	public double curMaxU;
	
	public double curMaxUCopy;
	
	// The global optimal solution. 
	public double[] incumbent;
	
	// The optimal tree node
	public TreeNode result;
	
	// The number of control variables
	public int numVar;
	
	// The current level in the Breadth-first search
	public int curLevel;
	
	// Number of price choices
	public int numPri;
	
	// Number of order choices
	public int numOrd;
	
	// The tolerance 
	public double tol;
	
	// The queue to store the intermediate tree nodes
	public LinkedList<TreeNode> queue;
	
	// The P matrix 
	public double[][] P;
	
	// The q vector
	public double[] q;
	
	// The r
	public double r;
	
	// The G matrix
	public double[][] G;
	
	// The h vector
	public double[] h;
	
	
	// The indicator for multithreading job
	public boolean multitask;
	
	// The current period
	public int period;
	
	
	
	
	public BranchAndBoundSearch(int numVar, double tol, double curMaxU, double[] incumbent, int state, double[] J, int period) {
		this.numVar = numVar;
		this.tol = tol;
		this.curMaxU = curMaxU; // Initial current max utility
		curMaxUCopy = curMaxU;
		this.incumbent = incumbent; // Initial solution 
		this.state = state;
		this.J = J;
		this.period = period;
		queue = new LinkedList<TreeNode>();
		numPri = InputData.priceNum;
		numOrd = InputData.orderNum;
		curLevel = 0;
		
		
		
		
		/*****************************************************************************
		 ********************  A, b, h, and r need to update each time ***************
		 ****************************************************************************/
		
		
		// NewEstCoefs
		double[][] newEstCoefs = InputData.estimateCoefficients;
		
		// Initialize P matrix
		P = new double[numVar][numVar];
		for (int i = 0; i < numVar - 1; i++) {
			for (int j = 0; j < numVar - 1; j++) {
				P[i][j] = -(newEstCoefs[i][j + 1] * 2);
			}
		}
		P[numVar - 1][numVar - 1] = 2 * InputData.mu;
		
		
		// Initialize q vector
		q = new double[numVar];
		for (int i = 0; i < numVar - 1; i++) {
			double sum = 0;
			for (int j = 0; j < numVar - 1; j++) 
				sum += newEstCoefs[i][j + 1];
			
			q[i] = -(newEstCoefs[i][0] + InputData.eta * sum);
		}
		q[numVar - 1] = -(-InputData.wholePrice[period] - InputData.eta + 2 * InputData.mu * InputData.Oref);
		
		
		// Initialize InputData.r
		double s = 0;
		for (int i = 0; i < numVar - 1; i++) 
			s += newEstCoefs[i][0];
		r = -InputData.mu * InputData.Oref * InputData.Oref + InputData.eta * s; // Need to add some term each time
		
		
		
		// Initialize matrix G
		G = new double[numVar + 2][numVar];
		G[0][numVar - 1] = 1;
		for (int i = 1; i < numVar; i++) {
			for (int j = 0; j < numVar - 1; j++) {
				G[i][j] = -newEstCoefs[i - 1][j + 1];
			}
		}
		for (int i = 0; i < numVar - 1; i++) {
			double sum = 0;
			for (int j = 0; j < numVar - 1; j++) {
				sum += newEstCoefs[i][j + 1];
			}
			G[numVar][i] = sum;
			G[numVar + 1][i] = -sum;
		}
		G[numVar][numVar - 1] = -1;
		G[numVar + 1][numVar - 1] = 1;
		
		
		
		
		// Initialize h
		h = new double[numVar + 2];
		h[0] = InputData.maxOrder;
		for (int i = 0; i < numVar - 1; i++)
			h[i + 1] = newEstCoefs[i][0];
		s = 0;
		for (int i = 0; i < numVar - 1; i++) {
			s += newEstCoefs[i][0];
		}
		h[numVar] = -s; // Need to add something later on
		h[numVar + 1] = s; // Need to add something later on
		
	}

	public synchronized TreeNode[] multithreadingStep(TreeNode par) {
		multitask = false;
		ThreadController tc = new ThreadController(par, this);
		(new Thread(tc)).start();
		
		while (!multitask) {
			try{
				wait();
			}catch (InterruptedException e) {
				
			}
		}
		
		return tc.nodeCandidates;
		
	}
	
	public synchronized void setMultiTask() {
		multitask = true;
		notifyAll();
	}
	
	class ThreadController implements Runnable {
		public int numCPUs;
		public boolean[] CPUStatus;
		public TreeNode[] nodeCandidates;
		public TreeNode par; // Parent tree node
		public BranchAndBoundSearch bbs;
		
		public ThreadController (TreeNode par, BranchAndBoundSearch b) {
			numCPUs = Runtime.getRuntime().availableProcessors(); // Get the number of processors
			// Set all CPU status to 'free'
			CPUStatus = new boolean[numCPUs];
			for (int i = 0; i < numCPUs; i++) {
				CPUStatus[i] = true;
			}
			nodeCandidates = new TreeNode[numPri];
			this.par = par;
			bbs = b;
		}
		
		
		
		@Override
		public void run() {
			
			for (int i = 0; i < numPri; i++) {
				boolean foundFreeCPU = false;
				// Determine if there is free CPUs so this thread can wait for one to become free
				
				while (!foundFreeCPU) {
					synchronized(this) {
						
						cpu: for (int cpuIndex = 0; cpuIndex < numCPUs; cpuIndex++) {
							if (CPUStatus[cpuIndex]) {
								// Start a new thread on this cpu and set its status to false
								foundFreeCPU = true;
								CPUStatus[cpuIndex] = false;
								(new Thread(new DigTreeThread(i, cpuIndex, curLevel, par, this))).start();
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
			
			bbs.setMultiTask();
			
			
			
			
				
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
	
	
	
	class DigTreeThread implements Runnable {
		private double[] tempSol;
		private TreeNode tempNode;
		private ThreadController tc;
		private int level;
		private int index;
		private int cpuNum;
		private TreeNode parent;
		
		public DigTreeThread(int i, int cpuIndex, int curLevel, TreeNode par,
				ThreadController controller) {
			
			index = i; // The current variable for the prices or orders
			level = curLevel;
			tempSol = new double[numVar];
			tc = controller;
			parent = par;
			cpuNum = cpuIndex;
			
		}
		
		@Override
		public void run() {
			if (level == 0) {// The first level---- The order variable
				tempSol[numVar - 1] = InputData.orderChoice[index];
				tempNode = estOptSolution(level, numVar, state, tempSol);
				tc.nodeCandidates[index] = tempNode;
				
			}
			else { // Consider the 2nd, 3rd, levels
				int nodeLevel = parent.level;
				tempSol = parent.solution.clone();
				tempSol[nodeLevel] = InputData.priceChoice[index];
				if (nodeLevel == numVar - 2) {
					tempNode = calOptSolution(tempSol, state);
				}
				else {
					tempNode = estOptSolution(nodeLevel + 1, numVar, state, tempSol);
					//System.out.println("curMaxU is " + curMaxU + " node value is " + tempNode.optValue);
					
				}
				
				tc.nodeCandidates[index] = tempNode;
				
						
						
				}
			
			tc.setCPUFree(cpuNum);
			}
		
			
				
		}

		

	
	
	public TreeNode search() {
		
		while ((!queue.isEmpty()) || (curLevel == 0)) {
			
			//System.out.println("current level is " + curLevel);
			
			if (curLevel == 0) { // The first level----- The order variable
				
				TreeNode[] nodecandidates = multithreadingStep(null);
				
				

				
				for (TreeNode node : nodecandidates) {
					if (node.isFeasible && node.optValue > curMaxU) {
						if (node.isFathoming) {
							curMaxU = node.optValue;
							incumbent = node.solution;
						}
						else {
							queue.add(node);
						}
					}
				}
				
				curLevel++;
				
				
				
			
			}
			else { // Consider the 2nd, 3rd, ....... levels
				
				while (!queue.isEmpty()) {
					TreeNode bud = queue.poll();
					//System.out.println("The queue size is " + queue.size());
					
					if (bud.optValue > curMaxU) { // This is a good bud node. We should continue
						
						TreeNode[] nodecandidates = multithreadingStep(bud);
						
						ArrayList<TreeNode> list = new ArrayList<TreeNode>();
						TreeNode maxChild = null;
						double maxValue = -1000;
						
						for (TreeNode node : nodecandidates) {
							if (node.isFeasible && node.optValue > curMaxU) {
								if (node.isFathoming) {
									curMaxU = node.optValue;
									incumbent = node.solution;
								}
								else {
									list.add(node);
									if (node.optValue > maxValue) {
										maxValue = node.optValue;
										maxChild = node;
									}
								}
							}
						}
						

						
						if (bud.level < InputData.cutoff) {
							Collections.shuffle(list);
							int cout = 0;
							while ((!list.isEmpty()) && (cout < InputData.remains)) {
								queue.add(list.remove(0));
								cout++;
							}
						}
						else {
							if (maxChild != null)
								queue.add(maxChild);
						}
						
						
						break;
						
					}
				}
				
				
			}
			
		}
		
		System.out.println("The queue is empty!! The curMaxU is " + curMaxU + "  The previous maxU is " + curMaxUCopy);
		System.out.println(Arrays.toString(incumbent));
		result = new TreeNode(curMaxU, true, true, incumbent, numVar - 1);
		return result;
		
		
	}
	
	
	public TreeNode estOptSolution(int nodeLevel, int numVar, int state, double[] prefixSol) {
		// prefixSol = [p1, p2, xxxxxxxxxx, pN, order]
		// Update r, h, A, and b
		double[][] A = new double[nodeLevel + 1][numVar];
		A[0][numVar - 1] = 1;
		for (int i = 1; i < nodeLevel + 1; i++) {
			A[i][i - 1] = 1;
		}
		
		double[] b = new double[nodeLevel + 1];
		b[0] = prefixSol[numVar - 1];
		for (int i = 1; i < nodeLevel + 1; i++) {
			b[i] = prefixSol[i - 1];
		}
		
		
		double min = 0;
		double max = 0;
		double[] res = new double[numVar]; // Store the prices and order res[p1, p2, p3, ......., order]
		TreeNode node;
		
		for (int inv = 0; inv < InputData.invenRange; inv = inv + InputData.interval) {
	
			double rTemp = -(r - InputData.eta * state * InputData.interval + InputData.utilitydisc * J[inv / InputData.interval]);
			double[] hTemp = h.clone();
			hTemp[numVar] += (state * InputData.interval - inv); // Update the last two rows of h vector
			hTemp[numVar + 1] += (inv + InputData.interval - state * InputData.interval);
			double[] sol = QPSolver.solveQP(P, q, rTemp, G, hTemp, A, b, false); // sol = [maxValue p1 p2 ....... pN order]
			
			if (sol[0] < min) {
				min = sol[0];
				for (int i = 0; i < numVar; i++)
					res[i] = sol[i + 1];
				
			}
			

			
			
		}
		
		max = -min; // Get the maximum value
		
		if (Math.abs(max) < tol) { // The current tree node is infeasible. Ignore it
			node = new TreeNode (-1, false, false, null, nodeLevel);
		}
		else { // The current bud node is feasible
			
			
			node = new TreeNode(max, false, true, res, nodeLevel);
			if (isFathoming(res))
				node.isFathoming = true;
		}
		
		return node;
	}
	
	
	public boolean isFathoming(double[] solution) {
		
		for (int i = 0; i < numVar - 1; i++) {
			if (!isFathomingHelper(InputData.priceChoice, solution[i]))
				return false;
		}
		
		if (!isFathomingHelper(InputData.orderChoice, solution[numVar - 1]))
			return false;
		
		return true;
		
	}
	
	public boolean isFathomingHelper(double[] array, double target) {
		int left = 0;
		int right = array.length - 1;
		
		while (left <= right) {
			int mid = (left + right) / 2;
			
			if (Math.abs(target - array[mid]) < tol) {
				return true;
			}
			else {
				if (target < array[mid])
					right = mid - 1;
				else
					left = mid + 1;
			}
		}
		
		return false;
	}
	
	
	public TreeNode calOptSolution(double[] solution, int state) { // Calculate the utility directly
		
		if (!Initialization.isValid(solution, state * InputData.interval)) { // The solution is not valid
			//System.out.println("Optimal value calculation is done!");
			return new TreeNode(-1, false, false, solution, numVar - 1);
		}
		
		else { // The solution is valid
		
		
			double[] prices = new double[numVar - 1];
			double order = solution[numVar - 1];
			for (int i = 0; i < numVar - 1; i++)
				prices[i] = solution[i];
		
			double[] estiDemands = DemandSimulator.getEstimateDemand(prices);
		
			double demandsum = 0;
			for (int k = 0; k < InputData.K; k++)
				demandsum += estiDemands[k];
			
			double unsold = state * InputData.interval + order - demandsum;
			int newState = (int)(unsold / InputData.interval);
			
			double costPerStage = InterationEngine.computeCostPerStage(prices, order, unsold, estiDemands, period);
			double optV = costPerStage + InputData.utilitydisc * J[newState];
			
			TreeNode result = new TreeNode(optV, true, true, solution, numVar - 1);
			//System.out.println("Optimal value calculation is done!");
			return result;
		}
		
		
		
		
		
		
	}
	
	
	
}

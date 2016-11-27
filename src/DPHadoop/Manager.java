/**
 * 
 */
package DPHadoop;

import finiteHorizon.IterationEngine;
import infiniteHorizon.BBIterationEngine;
import infiniteHorizon.BBIterationEngine2;
import infiniteHorizon.Initialization;
import infiniteHorizon.InterationEngine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

//import org.apache.hadoop.conf.Configuration;
//import org.apache.hadoop.util.GenericOptionsParser;
//import org.apache.hadoop.util.ToolRunner;





import com.joptimizer.functions.PDQuadraticMultivariateRealFunction;

/**
 * @author CHAO LUO
 *
 */
public class Manager {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		InputData.renewable = new double[24];
		int days = InputData.days;
		int horizon = InputData.horizon;
		int num = InputData.K; // Number of charging stations
		double[] tempdecision;
		double[] decision;
		int curInventory_dp = 0;
		double renewableEnergy = 0; // the renewable energy for current horizon
		double[][][] solutionsInf = null;
		
		Random rd = new Random(2);
		
		
		double[] etas = new double[]{0.0}; // the eta array we will iterate
		double[] betas = new double[]{5000}; // the beta we will iterate
		double[] minPros = new double[]{4000};
		
		
		/*double[] lambda1 = new double[]{0.8};
		double[] lambda2 = new double[8];
		double[] lambda3 = new double[8];
		
		for (int i = 1; i <= 1; i++) {
			lambda2[i - 1] = i * 1.0 / 10;
			lambda3[i - 1] = (2 - i) * 1.0 / 10;
		}*/
		
		
		InputData.maxSolarPower = (int)(InputData.maxSolarPower / InputData.powerAttenuation);
		
		
		
		
		CoefficientGenerator.genCoefficient();
		CoefficientGenerator.hardcode1();
		CoefficientGenerator.hardcode2();
		
		int[] index = InputData.indInGrid;
		double[][] jacobian = InputData.Jacobian;
		
		
		Logger.getRootLogger().setLevel(Level.OFF);
		if (!InputData.isFiniteHorizon) { // Read the stationary solutions from files
			Initialization.init();
			BBIterationEngine2 engine = new BBIterationEngine2();
			solutionsInf = engine.run();	
		}
		
		for (int lam1 = 19; lam1 <= 20; lam1++) {
			for (int lam2 = 0; lam2 <= 100 - lam1; lam2++) {
				
			InputData.lambdas[0] = lam1 * 1.0 / 100;
			InputData.lambdas[1] = lam2 * 1.0 / 100;
			InputData.lambdas[2] = (100 - lam1 - lam2) * 1.0 / 100;
			
			// Lambda / max for multi objective optimization
			for (int i = 0; i < 3; i++) {
				InputData.lambdas[i] /= InputData.max4Scale[i];
			}
			
		for (int minp = 0; minp < minPros.length; minp++) {
			InputData.minProfit = minPros[minp];
		for (int et = 0; et < etas.length; et++) {
			InputData.eta = etas[et];
		for (int be = 0; be < betas.length; be++){
			InputData.beta = betas[be];
			
		/*************************************************************************************
		 * 
		 * If we consider the renewable energy as a stochastic process and we assume that the price coefficients are
		 * perfectly estimated, we can store the decision table first. 
		 * 
		 */
		if (InputData.isRenewable) {
			MarkovChain.run();
			if (!InputData.isGreedy) {
				double[][][][] temp = new double[InputData.horizon][InputData.invenRange / InputData.interval][InputData.stateNum][InputData.K + 2];
				InputData.decisionTable = temp;
				IterationEngine engine = new IterationEngine(0, 0);
				engine.calculateStochastic();
	
			}
		}
		
		
		/*for (int hr = 0; hr < 24; hr++) {
			for (int in = 0; in < 20; in++) {
				try {
					
					//DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
					//String today = dateFormat.format(new Date()).toString();
					
					String fileName = "horizon" + Integer.toString(hr) + "inventory" + Integer.toString(in) + ".txt";
					
					writeToFile(fileName, InputData.decisionTable[hr][in]);
			} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
			} 
			}
		}*/
		
		

			
		for (int i = 0; i < days; i++) { // Loop over days
			InputData.currentInventory = 0;
			curInventory_dp = 0;
			for (int j = 0; j < horizon; j++) { // Loop over horizon
				
				if (InputData.isFiniteHorizon) {
					
					
					
					/*******************************************************************
					 * ****************************************************************
					 * *********************** The codes for finite horizons***************
					 **********************************************************************
					 ***********************************************************************/
					
					if (InputData.isRenewable) {
						
						/*************************************************************************
						 * 
						 * For decision.
						 * decision[0]: current inventory
						 * decision[1]: current renewable energy
						 * decision[2]: real charging demand
						 * decision[3]: current profit
						 * decision[4]: customer satisfaction
						 * decision[5]: impact on power grid
						 * decision[6]: current revenue;
						 * decision[7] to decision[num + 6]: charging prices
						 * decision[num + 7]: order in the current horizon
						 * 
						 **************************************************************************/
						 decision = new double[8 + num];
						 double renEng = 0;
						 int renState = 0;
						 
						// check if the renewable energy satisfy constraints
						 renEng = InputData.simRenewEng[i][j];
						 if (renEng < 0 || j <= 6 || j >= 18) {
							 renEng = 0;
						 }
						 
						 
						 
						 if (renEng > InputData.maxSolarPower) {
							 
							 renEng = InputData.maxSolarPower - 0.1;
						 }
						 
						 
						 double intervalRenewable = InputData.maxSolarPower / InputData.stateNum;
						 
						 tempdecision = new double[InputData.K + 2];
						 
						 
						 if (j == 7 && renEng > 10) {
							 renEng = 5;
						 }
						 
						 if (j == 9 && renEng > 20) {
							 renEng = 15;
						 }
						 
						 if (j == 8 && renEng > 18) {
							 renEng = 16;
						 }
						 
						 if (InputData.isGreedy) {
							 
							 IterationEngine engine = new IterationEngine(i, j);
							 tempdecision = engine.greedyCalculateStochastic(curInventory_dp, renEng);
		
						 }
						 else {
							 
							 renState = (int) Math.floor(renEng / intervalRenewable);
							 //System.out.println("j is " + j + " inventory is " + curInventory_dp / InputData.interval + 
							 //		 " renewable is " + renState);
							
							 
							
							
							tempdecision = InputData.decisionTable[j][curInventory_dp / InputData.interval][renState];
							 
						 }
						 
						 
						 //System.out.println("current inventory is " + curInventory_dp + 
						//		 " Current renewable energy is " + renEng);
						 
						 // set the current inventory
						 decision[0] = curInventory_dp;
						 
						 // set the renewable energy
						 decision[1] = renEng;
						 
						 // set total utility, prices, and purchase
						 for (int m = 6; m < num + 8; m++) {
							 decision[m] = tempdecision[m - 6];
						 }
						 
						 double[] prices = new double[num];
						 for (int m = 7; m < num + 7; m++)
								prices[m - 7] = decision[m];
						 
						 double order = decision[num + 7];
						 
						 
						 // demands[][0] is the real charging demand without noise
						 // demands[][1] is the real charging demand with noise
						 double[][] realDemands = DemandSimulator.getRealDemand(prices, rd);
						 
						 double demandsumDeter = 0;
						 double demandsumStoc = 0;
						 double demandsum = 0;
						 int selector = -1;
						 
						 double currentProfit = 0;
						 double currentRevenue = 0;
						 
						 for (int m = 0; m < num; m++) { // calculate the real demand and current profit
							 demandsumDeter += realDemands[m][0];
							 demandsumStoc += realDemands[m][1]; 
						 }
						 
						 if (demandsumStoc < curInventory_dp + order + renEng) {
							 demandsum = demandsumStoc;
							 selector = 1;
						 }
						 else {
							 demandsum = demandsumDeter;
							 selector = 0;
						 }
						 
						 for (int m = 0; m < num; m++) {
							 currentProfit += prices[m] * realDemands[m][selector];
						 }
						 
						 currentRevenue = currentProfit;
						 
						 
						 currentProfit -= (InputData.wholePrice[j] * order + 
								 InputData.eta * (curInventory_dp + renEng + order - demandsum));
						 
						 double usersatvalue = InputData.omega * demandsum - 
								 InputData.alpha / 2 * demandsum * demandsum;
						 
						 
						 double impact = 0;
						 for (int n1 = 0; n1 < jacobian.length; n1++) {
							 double sum = 0;
							 for (int n2 = 0; n2 < num; n2++) {
								 sum += jacobian[n1][index[n2]] * realDemands[n2][selector]; 
							 }
							 sum = sum * sum;
							 impact += sum;
						 }
						 
						 
						 
						 
						 
						 decision[2] = demandsum; // Store the real charging demand at the current horizon
						 
						 decision[3] = currentProfit; // Store the profit
							
						 decision[4] = usersatvalue; // Store the customer satisfaction
						 
						 decision[5] = impact; // Store the impact on power grid
							
						 decision[6] = currentRevenue; // Store the revenue \sum(p_jd_j)
						 
						 double unsold = curInventory_dp + order + renEng - demandsum;

						 curInventory_dp = (Math.min((int)(unsold / InputData.interval), 
								 InputData.invenRange / InputData.interval - 1)) * InputData.interval;
						 
						 curInventory_dp = Math.max(0, curInventory_dp);
						 
						 
						 
						 double[][] temp = new double[1][];
						 temp[0] = decision;
							
							
							
							
							
						try {
								
								//DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
								//String today = dateFormat.format(new Date()).toString();
								
								String fileName = "365_dp_customer_stochastic_renewable" + "_eta" + Double.toString(etas[et]) + 
								"_minProfit" + Integer.toString((int)InputData.minProfit) + "_lambda1" + 
								Integer.toString(lam1) + "_lambda2"+ Integer.toString(lam2) +  
								"_lambda3" + Integer.toString(100 - lam1 - lam2) + ".txt";
								
								BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
								String title = "Current day is " + Integer.toString(i) + ", current horizon is " + Integer.toString(j);
								out.write(title);
								out.newLine();
								out.close();
								writeToFile(fileName, temp);
						} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
						} 
						
						
						 
						 
					}
					else {
						/*************************************************************************
						 * 
						 * For decision.
						 * decision[0]: current inventory
						 * decision[1]: user satisfaction
						 * decision[2]: real charging demand
						 * decision[3]: profit earned in the current horizon
						 * decision[4] to decision[num + 3]: charging prices
						 * decision[num + 4]: order in the current horizon
						 * 
						 **************************************************************************/
						
						decision = new double[5 + num];
						IterationEngine engine = new IterationEngine(i, j);
						
						if (InputData.isGreedy) {
							tempdecision = engine.greedyCalculate(curInventory_dp);
						}
						else {
							
							double[][] solutions = engine.calculate();
							tempdecision = solutions[curInventory_dp / InputData.interval];

						}
						
						System.out.println("current inventory is " + curInventory_dp);
						decision[0] = curInventory_dp; // Store the current energy storage
						
						
						
						
						for (int n = 3; n < num + 5; n++) // Store the prices and order
							decision[n] = tempdecision[n - 3];
						
						
						
						
						
						double[] prices = new double[num];
						for (int k = 4; k < num + 4; k++)
							prices[k - 4] = decision[k];
						
						double order = decision[num + 4];
						
						
						
						
						
						// We need to check here later on!!
						//
						//
						//
						//
						//
						//
						//
						//
						//
						//
						
						
						double[] realDemands = new double[num]; //DemandSimulator.getRealDemand(prices, rd);!!!!!!!!!!!
						
						double demandsum = 0;
						double currentProfit = 0;
						for (int k = 0; k < num; k++) { // calculate the real demand and current profit
							demandsum += realDemands[k];
							currentProfit += prices[k] * realDemands[k]; 
						}
						
						currentProfit -= InputData.wholePrice[j] * order;
						
						
						double usersatvalue = InputData.beta * (InputData.omega * demandsum - InputData.alpha / 2 * demandsum * demandsum);
						
						decision[1] = usersatvalue; // Store the current user satisfaction
						
						decision[2] = demandsum; // Store the real charging demand;
						
						decision[3] = currentProfit; // Store the current profit
						
						

							
						double unsold = curInventory_dp + order + InputData.renewable[j] - demandsum;
						curInventory_dp = (Math.min((int)(unsold / InputData.interval), InputData.invenRange / InputData.interval - 1)) * InputData.interval;

					
						
						
						
						double[][] temp = new double[1][];
						temp[0] = decision;
						
						
						
						
						
						try {
							
							//DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
							//String today = dateFormat.format(new Date()).toString();
							
							String fileName = "greedy_eta05" + "_beta300" + ".txt";
							
							BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
							String title = "Current day is " + Integer.toString(i) + ", current horizon is " + Integer.toString(j);
							out.write(title);
							out.newLine();
							out.close();
							writeToFile(fileName, temp);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
						
					}
					
					
					
					
					
				
				}
				else {
					
					/*******************************************************************
					 * ****************************************************************
					 * *********************** The codes for infinite horizons***************
					 **********************************************************************
					 ***********************************************************************/
					System.out.println("current inventory is " + curInventory_dp);
					decision = solutionsInf[j][curInventory_dp / InputData.interval];
					decision[0] = curInventory_dp;
					double[][] temp = new double[1][];
					temp[0] = decision;
					
					try {
						
						BufferedWriter out = new BufferedWriter(new FileWriter("DecisionsInf.txt", true));
						String title = "Current day is " + Integer.toString(i) + ", current horizon is " + Integer.toString(j);
						out.write(title);
						out.newLine();
						out.close();
						writeToFile("DecisionsInf.txt", temp);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					double[] prices = new double[num];
					for (int k = 0; k < num; k++)
						prices[k - 1] = decision[k];
					
					double order = decision[num];
					
					double[] estiDemands = DemandSimulator.getEstimateDemand(prices);
					
					double demandsum = 0;
					for (int k = 0; k < InputData.K; k++)
						demandsum += estiDemands[k];
					
					double unsold = curInventory_dp + order - demandsum;
					curInventory_dp = ((int)(unsold / InputData.interval)) * InputData.interval;
					
					
					
					
				
				
				}
				
				
				
				
				
				
				
				
				
				
				
				
				
				
				

				
				
				
					
			}
		}
		}
		}
		}
		}
		}
		//Initialization.init();
		//BBIterationEngine BBEngine = new BBIterationEngine();
		//BBEngine.run();
		

		

	}

	private static void writeToFile(String fileName, double[][] array) throws IOException {
		BufferedWriter out = null;
		
		try {
			out = new BufferedWriter(new FileWriter(fileName, true));
			for (int i = 0; i < array.length; i++) {
				for (int j = 0; j < array[0].length; j++) {
					String temp = String.format("%.3f", array[i][j]) + ' ';
					out.write(temp);
				}
				
				out.newLine();
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



	private static String doubleArray2String() {
		String output = "";
		int num = InputData.K;
		double[][] coefs = InputData.estimateCoefficients;
		for (int i = 0; i < num; i++) {
			output += Double.toString(coefs[i][0]);
			for (int j = 1; j < num + 1; j++) {
				output = output + "," + Double.toString(coefs[i][j]);
			}
			output = output + "\n";
		}
		
		return output;
	}
}

/*System.out.println("Current day is " + i + "   Current horizon is " + j);
try {
	writeToFile("estimateCoefs.txt", InputData.estimateCoefficients);
} catch (IOException e) {
// TODO Auto-generated catch block
	e.printStackTrace();
}*/

	
// Record the H matrix
/*try {
	for (int y = 0; y < 20; y++)
		writeToFile("Hmatrix.txt", LinearModel.H[y].getArray());
} catch (IOException e) {
// TODO Auto-generated catch block
	e.printStackTrace();
}*/



//Test the learning module!!!!

/*Random rand2 = new Random();
double[] prices = new double[num];
for (int k = 0; k < num; k++) {
	prices[k] = rand2.nextDouble() * 10 + 30;
}


LinearModel.getNewCoefficients(prices, 10, 0, j);
if (j % 100 == 0) {
try {
	writeToFile("coefficients.txt", InputData.estimateCoefficients);
} catch (IOException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}
}*/


/*String newCoefs = doubleArray2String();
DPDriver driver = new DPDriver(j, i, newCoefs);
double[][] outputs = driver.run();
if (InputData.currentInventory > InputData.invenRange) {
	System.out.println("Too big is " + InputData.currentInventory);
	InputData.currentInventory = InputData.invenRange - 1;
}
else if (InputData.currentInventory < 0) {
	System.out.println("Too small is " + InputData.currentInventory);
	InputData.currentInventory = 0;
}
decision = outputs[InputData.currentInventory];


// Extract the prices of each charging station
double[] prices = new double[num];

// Add disturbance to the prices which is proportional to the error
for (int k = 0; k < num; k++)
	prices[k] = Math.abs(decision[k] + (rand.nextDouble() - 0.5) * InputData.disturbance);

double order = decision[num];

error = LinearModel.getNewCoefficients(prices, order, error, j);*/







/*try {
	writeToFile("tables.txt", outputs);
} catch (IOException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}*/
// Test the learning process



/*double totalDemand = 0;
for (int k = 0; k < InputData.K; k++) {
	totalDemand += realDemand[k];
}

inventory = (int)Math.round(InputData.discount * (inventory + preOrder - totalDemand));

System.out.println("Purchase decision is " + preOrder);
System.out.println("Pricing policy is " + prices.toString());*/	

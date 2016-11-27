/**
 * 
 */
package DPHadoop; 


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import Jama.CholeskyDecomposition;
import Jama.Matrix;

import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.FunctionsUtils;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.OptimizationRequest;

/**
 * @author CHAO LUO
 *
 */
public class QPSolver {
	// Return the x and maximum [p1 p2 ... pN Ok max]
	public static  double[] solveQP(double[][] P, double[] q, 
			double r, double[][] Pcon, double[] qcon, double rcon, 
			double[][] G, double[] h, double[][] A, double[] b, boolean isFinite) {
		
		int num = InputData.K; // the number of charging stations
		// Get the cholesky decomposition of P_con
		Matrix Pconm = new Matrix(Pcon);
		CholeskyDecomposition cd = new CholeskyDecomposition(Pconm); 
		Matrix L = cd.getL().transpose();
		Matrix Linv = L.inverse();
		
		
		Matrix qconm = new Matrix(qcon, num + 1);
		double[] qconNew = qconm.transpose().times(Linv).getRowPackedCopy();
		double rconNew = rcon;
		
		
		// get the centers and radius
		double[] centers = new double[num + 1];
		for (int i = 0; i < num; i++) {
			centers[i] = - qconNew[i] / 2;
		}
		
		double radius = 0;
		for (int i = 0; i < num; i++) {
			radius += Math.pow(qconNew[i], 2) / 4;
		}
		
		//System.out.println("radius is " + radius);
		
		radius = Math.sqrt(radius - rconNew);
		
		
		// Transform P matrix
		Matrix Pmatrix = new Matrix(P);
		Pmatrix = Linv.transpose().times(Pmatrix).times(Linv);
		double[][] PNew = Pmatrix.getArray();
		
		// Transform q vector
		Matrix qmatrix = new Matrix(q, q.length);
		double[] qNew = qmatrix.transpose().times(Linv).getRowPackedCopy();

		double rNew = r;
		
		
		
		
		// number of inequalities
		int len = G.length;
		int unknown = G[0].length;
		
		
		
		if(isFinite) {
		
			double[][] G2 = new double[InputData.K * 3 + 3][InputData.K + 1];
			double[] h2 = new double[InputData.K * 3 + 3];
			
			for (int i = 0; i < InputData.K + 3; i++) {
				G2[i] = G[i];
				h2[i] = h[i];
			}
			
			int count = 0;
			
			for (int i = InputData.K + 3; i < (InputData.K * 3 + 3); i = i + 2) {
				G2[i][count] = 1;
				G2[i + 1][count] = -1;
				h2[i] = InputData.maxPrice;
				h2[i + 1] = -InputData.minPrice;
				count++;
			}
			
			len = G2.length;
			G = G2;
			h = h2;
		}
		
		
		// Transform G matrix
		Matrix Gmatrix = new Matrix(G);
		G = Gmatrix.times(Linv).getArray();
		

		
		// Objective function
		PDQuadraticMultivariateRealFunction objectiveFunction = 
			new PDQuadraticMultivariateRealFunction(PNew, qNew, rNew);
		
		//inequalities
		ConvexMultivariateRealFunction[] inequalities = 
			new ConvexMultivariateRealFunction[len + unknown + 1];
		
		for (int i = 0; i < len; i++) {
			inequalities[i] = 
				new LinearMultivariateRealFunction(G[i], -h[i]);
		}
		
		for (int i = len; i < len + unknown; i++) {
			double[] temp = new double[unknown];
			temp[i - len] = -1;
			inequalities[i] = 
				new LinearMultivariateRealFunction(temp, 0);
		}
		
		
		inequalities[len + unknown] = FunctionsUtils.createCircle(num + 1, radius, centers);
		
		//optimization problem
		OptimizationRequest or = new OptimizationRequest();
		or.setF0(objectiveFunction);
		or.setFi(inequalities); 
		or.setA(A);
		or.setB(b);
		or.setToleranceFeas(1.E-3);
		or.setTolerance(1.E-3);
		or.setCheckKKTSolutionAccuracy(true);
		
		//optimization
		JOptimizer opt = new JOptimizer();
		//BarrierFunction bf = new LogarithmicBarrier(inequalities, 6);
		//BarrierMethod opt = new BarrierMethod(bf);
		opt.setOptimizationRequest(or);
		try {
			opt.optimize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			
		}
		
		double[] sol = new double[unknown];
		double[] result = new double[unknown + 1];
		for (int i = 0; i < unknown + 1; i++)
			result[i] = 1;
		
		
		try {
		    sol = opt.getOptimizationResponse().getSolution();
		} catch (Exception e) {
			return result;
		}
		
		Matrix solm = new Matrix(sol, unknown);
		sol = Linv.times(solm).getColumnPackedCopy();
		Matrix xm = new Matrix(sol, unknown);
		Matrix pm = new Matrix(P);
		Matrix qm = new Matrix(q, unknown);
		double max = xm.transpose().times(pm).times(xm).get(0, 0) * 0.5 + qm.transpose().times(xm).get(0, 0) + r;
		
		result[0] = max;
		for (int i = 1; i < sol.length + 1; i++)
			result[i] = sol[i - 1];
		
		
		return result;
		
		
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

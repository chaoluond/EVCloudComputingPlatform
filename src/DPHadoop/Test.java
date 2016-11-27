package DPHadoop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

import Jama.Matrix;


public class Test {

	/**
	 * @param args
	 */
	public static int[] d = new int[]{1, 2, 3, 4};
	public static void main(String[] args) {
		
		
		int test = 100;
		InputData.Jacobian = new double[InputData.Jsize][InputData.Jsize];
		CoefficientGenerator.hardcode1();
		CoefficientGenerator.hardcode2();
		
		System.out.println(Arrays.toString(InputData.Jacobian[105]));
		
		/*CoefficientGenerator.genCoefficient();
		
		int num = InputData.K;
		double max = 0;
		double[] result = new double[num + 2]; // Store the prices and order
		double[][] P = InputData.P;
		double[] q = InputData.q;
		double[][] G = InputData.G;
		
		double inventory = 0;
		
		double inv = 3;
		for (int i = 0; i < 1; i++) {
			for (int j = 0; j < 1; j++) {
				//System.out.println("Inventory is " + i + "  requirement is " + j);
		//for (int inv : InputData.preInven) {
			double rTemp = -(InputData.r - InputData.alpha * i);
			//System.out.println("r is " + r);
			
			double[] h = InputData.h.clone();
			h[num + 1] += (inventory - inv); // Update the last two rows of h vector
			h[num + 2] += (inv + 1 - inventory);
			//System.out.println(Arrays.toString(h));
			double[] solution = QPSolver.solveQP(P, q, rTemp, G, h);
			//if (solution[0] < max) {
				max = solution[0];
				result = solution;
				
			//}
		
		

		
		System.out.println(Arrays.toString(result));
			}
		}*/
		
		
		
		
	}
	
	
	public static double[][] readFromFile(String fileName) throws FileNotFoundException{
		Scanner input = new Scanner (new File(fileName));
		double[][] result = new double[InputData.Jsize][InputData.Jsize];
		
		int row = 0;
		int col = 0;
		while (input.hasNextLine()) {
			Scanner line = new Scanner(input.nextLine());
			col = 0;
			while (line.hasNext()) {
				result[row][col] = line.nextDouble();
				col++;
			}
			row++;
		}
		
		return result;
	}
	
	
	public static void writeToFile(String fileName, double[][] array) throws IOException {
		BufferedWriter out = null;
		String temp = "";
		
		try {
			out = new BufferedWriter(new FileWriter(fileName, true));
			for (int i = 0; i < array.length; i++) {
				temp = "{";
				
				for (int j = 0; j < array[0].length - 1; j++) {
					temp = temp + String.format("%.4f", array[i][j]) + ',';
				}
				temp = temp + String.format("%.4f", array[i][105]) + "},";
				out.write(temp);
				
			}
			
			
			
			
		} catch (IOException e) {  
			   e.printStackTrace();  
		  } finally {    
		   if (out != null)  
		    out.close();  
		  }  
		
	}
}
		
	
		





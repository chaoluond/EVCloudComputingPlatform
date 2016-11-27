package DPHadoop;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

public class DPreducer extends MapReduceBase
	implements Reducer<IntWritable, Table, IntWritable, Text> {
	
	private int inventory;
	private int num = InputData.K; // The number of charging stations
	public String newCoefs;
	public double[][] newEstCoefs;
	double[][] P;
	double[] q;
	double r;
	double[][] G;
	double[] h;
	
	public void configure(JobConf job) {
	     newCoefs = job.get("newCoefs");
	}
	
	private void string2DoubleArray() {
		String[] lines = newCoefs.split("\n");
		newEstCoefs = new double[num][num + 1];
		for (int i = 0; i < num; i++) {
			String[] temp = lines[i].split(",");
			for (int j = 0; j < num + 1; j++) {
				newEstCoefs[i][j] = Double.parseDouble(temp[j]);
			}
		}
		
		/*System.out.println("The received coefs:");
		for (int i = 0; i < num; i++)
			System.out.println(Arrays.toString(newEstCoefs[i]));*/
	}
	
	private void prepareData() {
		
		// Initialize P matrix
		P = new double[num + 1][num + 1];
		for (int i = 0; i < num; i++) {
			for (int j = 0; j < num; j++) {
				P[i][j] = -(newEstCoefs[i][j + 1] * 2);
			}
		}
		P[num][num] = 2 * InputData.mu;
		
		
		// Initialize q vector
		q = new double[num + 1];
		for (int i = 0; i < num; i++) {
			double sum = 0;
			for (int j = 0; j < num; j++) 
				sum += newEstCoefs[i][j + 1];
			
			q[i] = -(newEstCoefs[i][0] + InputData.eta * sum);
		}
		//q[num] = -(-InputData.marketPrice[1] - InputData.eta + 2 * InputData.mu * InputData.Oref);
		
		
		
		
		// Initialize InputData.r
		double s = 0;
		for (int i = 0; i < num; i++) 
			s += newEstCoefs[i][0];
		r = -InputData.mu * InputData.Oref * InputData.Oref + InputData.eta * s; // Need to add some term each time
	
		
		
		
		// Initialize matrix G
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
	
	public void reduce(IntWritable key, Iterator <Table> values,
		   OutputCollector<IntWritable, Text> output, Reporter reporter) throws IOException {
			
			// inventory = I_k
			inventory = key.get();
			//System.out.println("Reduce id is " + key);
			

			HashMap<Integer, Float> tables = new HashMap<Integer, Float>();
			while (values.hasNext()) {
				Table tab = values.next();
				tables.put(tab.state, tab.result);
			}
			
			// Convert newCoefs string to double[][]
			string2DoubleArray();
			
			
			// Initialize P, q, r, G, h
			prepareData();
			
			
			double max = 0;
			double[] result = new double[num + 2]; // Store the prices and order


			for (int inv = 0; inv < InputData.invenRange; inv++) {
				double rTemp = -(r - InputData.eta * inventory + tables.get(inv));
				double[] hTemp = h.clone();
				hTemp[num + 1] += (inventory - inv); // Update the last two rows of h vector
				hTemp[num + 2] += (inv + InputData.interval - inventory);
				double[] solution = QPSolver.solveQP(P, q, rTemp, G, hTemp, null, null, true);
				if (solution[0] < max) {
					max = solution[0];
					result = solution;
					
				}
				
				
			}
			
			max = -max;
			String out = Float.toString((float)max);
			for (int i = 1; i < num + 2; i++) {
				out = out + "," + Float.toString((float)result[i]); 
			}
			
			
			
			//Set<Integer> keys = tables.keySet();
			//System.out.println("Inventory is " + key.toString());
			//for (int i : keys) {
			//	System.out.println(i + ", " + tables.get(i));
			//}

			output.collect(key, new Text(out));
			//System.out.println("Output is " + key.toString() + ", " + result);
			

		  }
	

	
	

}

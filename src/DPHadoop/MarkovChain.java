/**
 * 
 */
package DPHadoop;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Chao
 *
 */
public class MarkovChain {

	/**
	 * @param args
	 */
	public static void run ()  {
		// TODO Auto-generated method stub
		int stateNum = InputData.stateNum;
		
		List<String> list = new ArrayList<String>();
		String[] fileName = new String[]{"6_1_to_8_31_2006.txt", "6_1_to_8_31_2007.txt", "6_1_to_8_31_2008.txt",
				"6_1_to_8_31_2009.txt", "6_15_to_8_31_2010.txt", "6_1_to_8_31_2011.txt", 
				"6_1_to_8_31_2012.txt", "6_1_to_8_31_2013.txt", "6_1_to_8_31_2014.txt", "6_1_to_8_31_2015.txt"};
		
		int[][][] state = new int[11][stateNum][stateNum]; // we only consider 8:00 to 18:00
		ArrayList<double[][]> transition = new ArrayList<double[][]>();
		boolean[][] flags = new boolean[11][stateNum];
		
		
		BufferedReader reader;
		String line;
		for (String file : fileName) {
			file = "solardata/" + file;
			
			try {
				reader = new BufferedReader(new FileReader(file));
				line = reader.readLine(); // skip the first line
				line = reader.readLine();
				while (line != null) {
					list.add(line);
					line = reader.readLine();
				}
				
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
			
		}
		
		double interval = InputData.maxSolarPower / stateNum;
		
		for(int i = 0; i < list.size(); i++) {
			String str = list.get(i);
			String[] data = str.split(",");
			int time1 = Integer.parseInt(data[1]);
			double power1 = Double.parseDouble(data[2]) / InputData.powerAttenuation;
			if (time1 >= 8 && time1 <= 18) {
				data = list.get(i + 1).split(",");
				int time2 = Integer.parseInt(data[1]);
				double power2 = Double.parseDouble(data[2]) / InputData.powerAttenuation;
				if (time2 != time1 + 1) {
					System.out.println("Error here");
					System.out.println("time1 is " + time1 + " time2 is " + time2 + " i is " + i);
					continue;
				}
				
				
				int state1 = (int) Math.floor(power1 / interval);
				if (state1 < 0)
					state1 = 0;
				if (state1 >= stateNum)
					state1 = stateNum - 1;
				
				
				int state2 = (int) Math.floor(power2 / interval);
				if (state2 < 0)
					state2 = 0;
				
				if (state2 >= stateNum)
					state2 = stateNum - 1;
				
				state[time1 - 8][state1][state2]++;
			}
			
			
			
		}
		
		
		
		// calculate the transition probability from 7:00 am (renewable energy is 0) to 8:00 am (renewable energy begins)
		int[] sumtemp = new int[stateNum];
		for (int i = 0; i < stateNum; i++) {
			for (int j = 0; j < stateNum; j++) {
				sumtemp[i] += state[0][i][j];
			}
				
		}
		int tot = 0;
		for (int i = 0; i < stateNum; i++)
			tot += sumtemp[i];
		
		double[][] specialPro = new double[1][stateNum];
		for (int i = 0; i < stateNum; i++)
			specialPro[0][i] = sumtemp[i] * 1.0 / tot;
		
		transition.add(specialPro);
		
		
		for (int i = 8; i <= 18; i++) {
			double[][] prob = new double[stateNum][stateNum];
			
			
			for (int j = 0; j < stateNum; j++) {
				int sum = 0;
				for (int k = 0; k < stateNum; k++)
					sum += state[i - 8][j][k];
				
				if (sum != 0) {
					flags[i - 8][j] = true; // flag represents if a current renewable energy exists
					for (int k = 0; k < stateNum; k++)
						prob[j][k] = state[i - 8][j][k] * 1.0 / sum;
				}
			}
			
			transition.add(prob);
		}
		
		/*for (int i = 7; i <= 18; i++) {
			String file = Integer.toString(i);
			if (i == 7)
				file += "special";
			file += ".txt";
			
			try {
				writeToFile(file, transition.get(i - 7));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
		
		InputData.transition = transition;
		InputData.renewableFlag = flags;
		
		
		InputData.simRenewEng = new double[InputData.days][24];
		
		
		
		for (int i = 0; i < InputData.days; i++) {
			// iterate over day
			for (int j = 0; j < 24; j++) {
				// iterate over hour
				String item = list.get((i + 100) * 24 + j);
				String[] data = item.split(",");
				InputData.simRenewEng[i][j] = Double.parseDouble(data[2]) / InputData.powerAttenuation;	
			}
		}
		
		
		/*try {
			writeToFile("SimRenewEng.txt", InputData.simRenewEng);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
		
		
		
		
		
	private static void writeToFile(String fileName, double[][] array) throws IOException {
			BufferedWriter out = null;
			
			try {
				out = new BufferedWriter(new FileWriter(fileName, true));
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

package DPHadoop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.KeyValueTextInputFormat;

public class DPDriver {
	public int stage;
	public double[][] output;
	public String newCoefs;
	public int day;
	
	public DPDriver(int s, int d, String str) {
		stage = s;
		output = new double[InputData.invenRange][InputData.K + 2];
		day = d;
		newCoefs = str;
	}
	
	public double[][] run() {
		int lastHorizon = InputData.horizon; // The last horizon in each day 
		String path = "/user/hadoop-user/";
		for (int i = lastHorizon - 1; i >= stage; i--) {
			JobClient client = new JobClient();
			JobConf conf = new JobConf(DPHadoop.DPDriver.class); // Need to check this!
			
			conf.setStrings("newCoefs", newCoefs);
			
			// Set input/output format
			conf.setInputFormat(KeyValueTextInputFormat.class);
			
			// TODO:specify output types
			conf.setOutputKeyClass(IntWritable.class);
			conf.setOutputValueClass(FloatWritable.class);
			
			// Specify the types of key and value for mapper
			conf.setMapOutputKeyClass(IntWritable.class);
			conf.setMapOutputValueClass(Table.class);

			// TODO: specify input and output DIRECTORIES (not files)
			
			String input = "";
			if(i == lastHorizon - 1) {
				input = "container24";
			}
			else {
				input = path + "/Day" + Integer.toString(day) + 
					"/stage" + Integer.toString(stage) + 
					"/container" + Integer.toString(i + 1);
			}
				
			String output = path + "/Day" + Integer.toString(day) + 
				"/stage" + Integer.toString(stage) + 
				"/container" + Integer.toString(i);
			
		    FileInputFormat.addInputPath(conf, new Path(input));
		    FileOutputFormat.setOutputPath(conf, new Path(output));

			// TODO: specify a mapper
			conf.setMapperClass(DPmapper.class);

			// TODO: specify a reducer
			conf.setReducerClass(DPreducer.class);

			client.setConf(conf);
			try {
				JobClient.runJob(conf);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		String srcPath = path + "/Day" + Integer.toString(day) + 
			"/stage" + Integer.toString(stage) + 
			"/container" + Integer.toString(stage);
		
		String dstPath = path + "/Day" + Integer.toString(day) + 
			"/stage" + Integer.toString(stage) + 
			"/container" + Integer.toString(stage) + "/merged_file.txt";
		
		Configuration conf = new Configuration(); 
		try { 
			FileSystem hdfs = FileSystem.get(conf); 
			FileUtil.copyMerge(hdfs, new Path(srcPath), hdfs, 
					new Path(dstPath), false, conf, null); 
			
			BufferedReader br=new BufferedReader(new InputStreamReader(hdfs.open(new Path(dstPath))));
		    String line;
		    line=br.readLine();
		    while (line != null){
		    	String[] temp1 = line.split("	");
		    	int inventory = Integer.parseInt(temp1[0]);
		    	String[] temp2 = temp1[1].split(",");
		    	for (int i = 0; i < InputData.K + 1; i++) {
		    		output[inventory][i] = Double.parseDouble(temp2[i + 1]);
		    	}
		    	line=br.readLine();
		 	}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return output;
		
		
		
       
	}
	

}

package DPHadoop;

import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;


public class DPmapper extends MapReduceBase implements 
		Mapper<Text, Text, IntWritable, Table> {
	
	public void map(Text key, Text values,
		OutputCollector<IntWritable, Table> output, Reporter reporter) throws IOException {
		//System.out.println("Key is " + key.toString());
		Float val = Float.parseFloat(values.toString().split(",")[0]);
		for (int i = 0; i < InputData.invenRange; i++) {
			// State = I_k
			IntWritable state = new IntWritable(i); 
			// Table -- (I_{k-1}, J_{k-1}(I_{k-1})
			Table tab = new Table(Integer.parseInt(key.toString()), val); 
			//System.out.println("Inventory is " + i + ", " + tab.toString());
			output.collect(state, tab);
		}
		
	}
	
	

}

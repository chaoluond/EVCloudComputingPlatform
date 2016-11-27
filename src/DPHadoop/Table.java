/**
 * 
 */
package DPHadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

/**
 * @author CHAO LUO
 *
 */
public class Table implements Writable{
	public int state;
	public float result;
	
	public Table(int s, float r) {
		state = s;
		result = r;
	}
	
	public Table() {
		this(0, 0);
	}
	
	public void write(DataOutput out) throws IOException {
		out.writeInt(state);
		out.writeFloat(result);
	}
	
	public void readFields(DataInput in) throws IOException {
		state = in.readInt();
		result = in.readFloat();
	}
	
	public String toString() {
		return Integer.toString(state) + ", " + Float.toString(result);
	}
	
	
}

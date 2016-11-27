/**
 * 
 */
package infiniteHorizon;

/**
 * @author Chao
 *
 */
public class TreeNode {
	
	// Optimal utility value in this node. Use the bounding function to calculate this value 
	public double optValue;
	
	// Flag for fathoming solution
	public boolean isFathoming;
	
	// Flag for feasible node
	public boolean isFeasible;
	
	// The current solution
	public double[] solution;
	
	// The current level
	public int level;
	
	
	
	
	public TreeNode() {
		
	}
	
	public TreeNode(double optValue, boolean isFathoming, boolean isFeasible, double[] solution, int level) {
		this.optValue = optValue;
		this.isFathoming = isFathoming;
		this.solution = solution;
		this.level = level;
		this.isFeasible = isFeasible;
	}
	
	
	
}

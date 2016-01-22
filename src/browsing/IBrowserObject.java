package browsing;

/**
 * Provides an access point for implementing objects to return cost and reward values
 * 
 * @author Brandon Kindred
 *
 */
public interface IBrowserObject {
	/**
	 * Computes and returns the cost of the current object
	 * 
	 * @return
	 */
	public double getCost();
	
	/**
	 * Computes and returns the reward for the current object
	 * 
	 * @return
	 */
	public double getReward();
}

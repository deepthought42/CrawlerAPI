package learning;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class QLearn {

	private double learning_rate = .001;
	private double discount_factor = .001;
	
	/**
	 * 
	 * @param learning_rate
	 * @param discount_factor
	 */
	public QLearn(double learning_rate, double discount_factor) {
		this.learning_rate = learning_rate;
		this.discount_factor = discount_factor;
	}
	
	/**
	 * 
	 * @param old_value
	 * @param actual_reward
	 * @param estimated_future_reward
	 * @return
	 */
	public double calculate(double old_value, double actual_reward, double estimated_future_reward){
		return (old_value + learning_rate * (actual_reward + (discount_factor * estimated_future_reward) - old_value ));

	}

}

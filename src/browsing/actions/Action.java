package browsing.actions;

import browsing.IBrowserObject;

/**
 * Defines an action in name only
 * 
 * @author Brandon Kindred
 *
 */
public class Action implements IBrowserObject {
	private final String name;
	
	/**
	 * 
	 * @param action_name
	 */
	public Action(String action_name) {
		this.name = action_name;
	}
	
	/**
	 * @return the name of this action
	 */
	public String getName(){
		return this.name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getCost() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getReward() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(){
		return this.name;
	}

}

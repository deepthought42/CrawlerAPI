package browsing.actions;

import browsing.IObjectValuationAccessor;
import browsing.PathObject;

/**
 * Defines an action in name only
 * 
 * @author Brandon Kindred
 *
 */
public class Action implements PathObject, IObjectValuationAccessor{
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
	public String toString(){
		return this.name;
	}
	
	@Override
	public int hashCode(){
		return this.name.hashCode();
	}

	public double getCost() {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getReward() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Action getData() {
		return this;
	}

}

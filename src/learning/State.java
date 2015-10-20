package learning;

/**
 * A state is a representation data in a state space. 
 * 
 * @author Brandon Kindred
 */
public interface State {
	Object obj = null;
	
	/**
	 * get object that defines the state space.
	 * @return
	 */
	public Object getObject();
	
	@Override
	public boolean equals(Object object);
}

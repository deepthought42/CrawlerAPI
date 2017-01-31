package com.qanairy.rl.learning;

/**
 * A state is a data representation in a state space. 
 * 
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

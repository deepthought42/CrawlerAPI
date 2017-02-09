package com.qanairy.rl.memory;

/**
 * Gets graph connection to OrientDB for reinforcement learning data
 */
public class OrientRLMemoryConnectionFactory{
	public OrientDbPersistor graph = null;
	
	/**
	 * Gets a graph connection for the Reinforcement Learning database
	 * 
	 * @return OrientDbPersistor connection
	 */
	public static OrientDbPersistor getGraph() {
		return new OrientDbPersistor("remote:localhost/Thoth", "brandon", "password");
	}
}

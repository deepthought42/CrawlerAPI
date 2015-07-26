package actors;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

/**
 * Agent that is responsible for persisting data in the graph database. 
 * 
 * @author Brandon Kindred
 *
 */
public class PersistorAgent {
	private OrientGraphFactory factory = null;
	private OrientGraph graph = null;
	
	/**
	 * Configure and start the database.
	 * 
	 * @param dbUrl location of the database
	 */
	public PersistorAgent(String dbUrl){
		this.factory = new OrientGraphFactory(dbUrl).setupPool(1,10);
		graph = factory.getTx();

		try {
		  
		} finally {
		  
		}
	}
	
	/**
	 * shutdown database connection.
	 */
	public void shutdown(){
		this.graph.shutdown();
	}
}

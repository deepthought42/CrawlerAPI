package actors;

import java.util.UUID;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

/**
 * Agent that is responsible for persisting data in the OrientDB graph database. 
 * 
 * @author Brandon Kindred
 *
 */
public class DatabaseAgent extends Thread implements Actor{
	private OrientGraphFactory factory = null;
	private OrientGraph graph = null;
	private String dbUrl = null;
	
	/**
	 * Configure and start the database.
	 * 
	 * @param dbUrl location of the database
	 */
	public DatabaseAgent(String dbUrl){
		this.dbUrl = dbUrl;
		
	}
	
	/**
	 * Creates connection to database pool and a new transaction
	 */
	public void startTransaction(){
		this.factory = new OrientGraphFactory(dbUrl).setupPool(1,100);
		graph = factory.getTx();
	}
	
	/**
	 * shutdown database connection.
	 */
	public void shutdown(){
		this.graph.shutdown();
	}

	@Override
	public UUID getActorId() {
		// TODO Auto-generated method stub
		return null;
	}
}

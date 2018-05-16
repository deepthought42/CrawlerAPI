package com.minion.actors;

import java.util.UUID;




import akka.actor.UntypedActor;

/**
 * Agent that is responsible for persisting data in the OrientDB graph database. 
 * 
 * @author Brandon Kindred
 *
 */
public class DatabaseAgent extends UntypedActor{
	private OrientGraphFactory factory = null;
	private OrientGraph graph = null;
	private String dbUrl = null;
	private UUID uuid = null;
	
	/**
	 * Configure and start the database.
	 * 
	 * @param dbUrl location of the database
	 */
	public DatabaseAgent(String dbUrl){
		this.dbUrl = dbUrl;
		this.uuid = UUID.randomUUID();
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

	public UUID getActorId() {
		return uuid;
	}

	@Override
	public void onReceive(Object arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
}

package com.qanairy.persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.FramedTransactionalGraph;

/**
 * Produces connections to the OrientDB instance
 *
 */
public class OrientConnectionFactory {
    private static Logger log = LogManager.getLogger(OrientConnectionFactory.class);
        
	FramedTransactionalGraph<OrientGraph> current_tx;
	
	private String username;
	private String password;
	 
	public OrientConnectionFactory(){
		log.info("Opening connection to orientdb");
		this.current_tx = getConnection();
		log.info("connection opened");
	}
	
	/**
	 * Opens connection to database
	 * @return
	 */
	private FramedTransactionalGraph<OrientGraph> getConnection(){
		log.info("framing graph factory");
		FramedGraphFactory factory = new FramedGraphFactory(); //Factories should be reused for performance and memory conservation.
		OrientGraphFactory graphFactory = new OrientGraphFactory("remote:localhost/Thoth", "root", "password").setupPool(1, 20);
	    OrientGraph instance = graphFactory.getTx();
	    log.info("Orientdb transaction created returning instance");
		return factory.create(instance);
	}

	
	/**
	 * Commits transaction
	 * 
	 * @param persistable_obj
	 * @return if save was successful
	 */
	public boolean save(){
		log.info("Saving current transaction to orientDB");
		try{
			current_tx.commit();
		}catch(Exception e){
			return false;
		}
		return true;
	}

	/**
	 * @return current graph database transaction
	 */
	public FramedTransactionalGraph<OrientGraph> getTransaction(){
		return this.current_tx;
	}
}

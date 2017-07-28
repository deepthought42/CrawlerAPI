package com.qanairy.persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.qanairy.config.ConfigService;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;

/**
 * Produces connections to the OrientDB instance
 *
 */
@Component
public class OrientConnectionFactory {
    private static Logger log = LogManager.getLogger(OrientConnectionFactory.class);
        
	FramedGraph<OrientGraphNoTx> current_tx;

	//private static String username = ConfigService.getProperty("db.username");
	//private static String password = ConfigService.getProperty("db.password");
	//private static String db_path = ConfigService.getProperty("db.serverurl");
	
	public OrientConnectionFactory(){
		this.current_tx = getConnection();
		log.info("Opened connection to OrientDB");
	}
	
	/**
	 * Opens connection to database
	 * @return
	 */
	private FramedGraph<OrientGraphNoTx> getConnection(){
		FramedGraphFactory factory = new FramedGraphFactory(); //Factories should be reused for performance and memory conservation.
		OrientGraphFactory graphFactory = new OrientGraphFactory("remote:67.205.165.64/thoth", "root", "BP6*g^Cw_Kb=28_y").setupPool(1, 50);
	    OrientGraphNoTx instance = graphFactory.getNoTx();
	    log.info("Orientdb transaction created. returning instance");
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
			//current_tx.commit();
		}catch(Exception e){
			log.error("failed to save record to OrientDB");
			return false;
		}
		return true;
	}

	/**
	 * @return current graph database transaction
	 */
	public FramedGraph<OrientGraphNoTx> getTransaction(){
		return this.current_tx;
	}
}

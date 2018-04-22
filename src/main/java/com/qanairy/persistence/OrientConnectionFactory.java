package com.qanairy.persistence;

import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
    private static Logger log = LoggerFactory.getLogger(OrientConnectionFactory.class);
        
	FramedGraph<OrientGraphNoTx> current_tx = null;
	OrientGraphFactory graphFactory;
	//private static String username = ConfigService.getProperty("db.username");
	//private static String password = ConfigService.getProperty("db.password");
	//private static String db_path = ConfigService.getProperty("db.serverurl");
	
	public OrientConnectionFactory(){
		if(this.current_tx == null){
			this.current_tx = getConnection();
		}
	}
	
	/**
	 * Opens connection to database
	 * @return
	 */
	private FramedGraph<OrientGraphNoTx> getConnection(){
		FramedGraphFactory factory = new FramedGraphFactory(); //Factories should be reused for performance and memory conservation.
		graphFactory = new OrientGraphFactory("remote:159.89.234.103/thoth", "root", "BP6*g^Cw_Kb=28_y").setupPool(1, 1000);
	    //graphFactory.setConnectionStrategy(OStorageRemote.CONNECTION_STRATEGY.ROUND_ROBIN_CONNECT.toString());
		OrientGraphNoTx instance = graphFactory.getNoTx();
		return factory.create(instance);
	}

	
	/**
	 * Commits transaction
	 * 
	 * @param persistable_obj
	 * @return if save was successful
	 */
	public boolean save(){
		try{
			//current_tx.commit();
		}catch(Exception e){
			log.error("failed to save record to OrientDB");
			return false;
		}
		return true;
	}
	
	public void close(){
		graphFactory.close();
	}
	
	/**
	 * @return current graph database transaction
	 */
	public FramedGraph<OrientGraphNoTx> getTransaction(){
		return this.current_tx;
	}
}

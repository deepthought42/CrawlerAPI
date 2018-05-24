package com.qanairy.persistence;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.qanairy.persistence.edges.HasDomain;
import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.FramedGraph;


/**
 * Produces connections to the OrientDB instance
 *
 */
@Component
public class OrientConnectionFactory {
    @SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(OrientConnectionFactory.class);
        
    DelegatingFramedGraph<OrientGraph> current_tx = null;
	OrientGraphFactory graphFactory;
	//private static String username = ConfigService.getProperty("db.username");
	//private static String password = ConfigService.getProperty("db.password");
	//private static String db_path = ConfigService.getProperty("db.serverurl");
	
	public OrientConnectionFactory(){
		this.current_tx = getConnection();
	}
	
	/**
	 * Opens connection to database
	 * @return
	 */
	private DelegatingFramedGraph<OrientGraph> getConnection(){
		Set<Class<?>> types = new HashSet<Class<?>>(Arrays.asList(new Class<?>[]{
            Account.class,
            Domain.class,
            HasDomain.class}));
		graphFactory = new OrientGraphFactory("remote:206.81.15.55/thoth", "root", "BP6*g^Cw_Kb=28_y").setupPool(10,1000);
		
		
		return new DelegatingFramedGraph<OrientGraph>(graphFactory.getNoTx(), true, types);
		//return new OrientTransactionFactoryImpl(factory, annotationsSupported, basePaths);

		
        //
	    //graphFactory.setConnectionStrategy(OStorageRemote.CONNECTION_STRATEGY.ROUND_ROBIN_CONNECT.toString());
		//return graphFactory.getTx();
	}

	
	/**
	 * Commits transaction
	 * 
	 * @param persistable_obj
	 * @return if save was successful
	 */
	public void save(){
		current_tx.tx().commit();
	}
	
	public void close(){
		current_tx.tx().close();
	}
	
	/**
	 * @return current graph database transaction
	 */
	public FramedGraph getTransaction(){
		return this.current_tx;
	}
}

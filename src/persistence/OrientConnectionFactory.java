package persistence;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.FramedTransactionalGraph;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class OrientConnectionFactory {
	FramedTransactionalGraph<OrientGraph> current_tx;
	
	public OrientConnectionFactory(){
		this.current_tx = getConnection();
	}
	
	private FramedTransactionalGraph<OrientGraph> getConnection(){
		FramedGraphFactory factory = new FramedGraphFactory(); //(1) Factories should be reused for performance and memory conservation.
		OrientGraphFactory graphFactory = new OrientGraphFactory("remote:localhost/Thoth", "brandon", "password");
	    OrientGraph instance = graphFactory.getTx();
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

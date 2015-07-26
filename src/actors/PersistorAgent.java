package actors;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class PersistorAgent {
	private OrientGraphFactory factory = null;
	private OrientGraph graph = null;
	
	public PersistorAgent(String dbUrl){
		this.factory = new OrientGraphFactory(dbUrl).setupPool(1,10);
		graph = factory.getTx();

		try {
		  
		} finally {
		  
		}
	}
	
	public void shutdown(){
		this.graph.shutdown();
	}
}

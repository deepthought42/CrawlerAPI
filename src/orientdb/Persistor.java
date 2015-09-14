package orientdb;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class Persistor {
	TransactionalGraph graph = null;
	public Persistor() {
		this.graph = new OrientGraph("local:test", "deepthought42", "oicu812");
	}
	
	public Vertex addVertex(){
		Vertex vertex = graph.addVertex("class:Person");
		vertex.setProperty("firstName", "John");
		vertex.setProperty("lastName", "Smith");
		
		return vertex;
	}
	
	public void addEdge(Vertex v1, Vertex v2){
		Edge edge = graph.addEdge(null, v1, v2, "lives");
	}

}

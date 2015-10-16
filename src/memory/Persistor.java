package memory;
import memory.ObjectDefinition;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class Persistor {
	public OrientGraph graph = null;
	
	public Persistor() {
		this.graph = new OrientGraph("remote:localhost/Thoth", "admin", "admin");
	}
	
	public Vertex addVertex(ObjectDefinition obj){
		
		if (graph.getVertexType(obj.getType()) == null){
            graph.createVertexType(obj.getType());
            System.out.println("Created objectDefinition vertex type");
        }

		return this.graph.addVertex("class:"+obj.getType());
	}
	
	public Vertex addVertex(String clazz){
		if (graph.getVertexType(clazz) == null){
            graph.createVertexType(clazz);
            System.out.println("Created objectDefinition vertex type");
        }

		return this.graph.addVertex("class:"+clazz);
	}
	
	public void save(){
		this.graph.commit();
	}
	
	public Edge addEdge(Vertex v1, Vertex v2, String clazz){
		return graph.addEdge("class:"+clazz, v1, v2, "lives");
	}

	public Iterable<Vertex> find(ObjectDefinition obj) {
		System.err.println("Retrieving object of type = ( " + obj.getType() + " ) from orientdb with value :: " + obj.getValue());
		Iterable<Vertex> objVertices = graph.getVertices("value", obj.getValue());
		
		return objVertices;
	}

}

package orientdb;


import memory.ObjectDefinition;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class Persistor {
	OrientGraph graph = null;
	
	public Persistor() {
		this.graph = new OrientGraph("plocal:Thoth", "admin", "admin");
		if (graph.getVertexType("Person") == null){
            graph.createVertexType("Person");
            System.out.println("Created person vertex type");
        }
	}
	
	public void persist(ObjectDefinition obj){
		//TODO :: Check if a matching node already exists
		
		if (graph.getVertexType(obj.getType()) == null){
            graph.createVertexType(obj.getType());
            System.out.println("Created objectDefinition vertex type");
        }

		this.addVertex(obj.getType());
	}
	
	private Vertex addVertex(String clazz){
		Vertex vertex = graph.addVertex("class:"+clazz);
		
		return vertex;
	}
	
	public void save(){
		this.graph.commit();
	}
	
	public Edge addEdge(Vertex v1, Vertex v2, String clazz){
		return graph.addEdge("class:"+clazz, v1, v2, "lives");
	}

}

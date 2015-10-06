package orientdb;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

public interface IOrientVertex {

	/**
	 * Saves vertex in graph
	 * 
	 * @return reference to created vertex
	 */
	public Vertex save(Graph graph);

}

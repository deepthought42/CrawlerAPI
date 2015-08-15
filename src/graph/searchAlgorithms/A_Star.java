package graph.searchAlgorithms;

import graph.Graph;
import graph.Vertex;
import structs.Path;

public class A_Star extends GraphSearch {

	public A_Star(Graph graph, Path path) {
		super(graph, path);
	}

	@Override
	public int getIndexOfNextBestVertexFromFrontier() {
		
		return 0;
	}

	@Override
	public Path findBestPath(int index) {
		return null;
	}

	@Override
	public Path findPathToClosestRoot(Vertex<?> startVertex) {
		return null;
	}

}

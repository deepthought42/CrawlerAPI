package graph.searchAlgorithms;

import graph.Graph;
import graph.Vertex;
import structs.Path;

public class Djikstra extends GraphSearch {

	public Djikstra(Graph graph, Path path) {
		super(graph, path);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getIndexOfNextBestVertexFromFrontier() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Path findBestPath(int start_idx, int goal_idx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path findPathToClosestRoot(Vertex<?> startVertex) {
		// TODO Auto-generated method stub
		return null;
	}

}

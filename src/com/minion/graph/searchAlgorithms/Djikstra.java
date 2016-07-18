package com.minion.graph.searchAlgorithms;

import com.minion.graph.Graph;
import com.minion.graph.Vertex;
import com.minion.structs.Path;

public class Djikstra extends GraphSearch {

	public Djikstra(Graph graph) {
		super(graph);
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

	@Override
	public Path findPathToClosestRoot(int start_idx) {
		// TODO Auto-generated method stub
		return null;
	}

}

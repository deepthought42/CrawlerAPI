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
		int lowestWeight = 999999999;
		int lowestIdx = -1;
		for(Integer key : super.frontier.keySet()){
			int weight = frontier.get(key);
			if(weight < lowestWeight){
				lowestWeight = weight;
				lowestIdx = key;
			}
		}
		return lowestIdx;
	}

	@Override
	public Path findBestPath(int start_idx, int end_idx) {
		
		return null;
	}

	@Override
	public Path findPathToClosestRoot(int start_idx) {
		Path path = new Path();
		int weight = 0;
		boolean isRootFound = false;
		
		do{
			for(Integer vertex_idx : graph.getFromIndices(start_idx)){
				super.frontier.put(vertex_idx, ++weight);
				Vertex<?> vertex = super.graph.getVertices().get(vertex_idx);
				path.add(vertex_idx);
				if(vertex.isRoot()){
					isRootFound = true;
					break;
				}				
			}
		}while(!isRootFound);
		
		return path;
	}
}

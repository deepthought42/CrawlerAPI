package graph.searchAlgorithms;

import graph.Graph;
import graph.Vertex;
import structs.Path;

public class A_Star extends GraphSearch {

	public A_Star(Graph graph) {
		super(graph);
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
				Vertex<?> vertex = graph.getVertices().get(vertex_idx);
				path.add(vertex_idx);
				System.err.println("VERTEX ADDED TO PATH with idx : "+vertex_idx);
				if(vertex.isRoot() || vertex_idx == 0){
					isRootFound = true;
					break;
				}				
			}
		}while(!isRootFound);
		
		return path;
	}

	@Override
	public Path findPathToClosestRoot(Vertex<?> startVertex) {
		System.out.println("FINDING CLOSEST PATH TO ROOT FOR VERTEX : "+startVertex);
		int vertex_idx = graph.findVertexIndex(startVertex);
		
		return findPathToClosestRoot(vertex_idx);
	}
}

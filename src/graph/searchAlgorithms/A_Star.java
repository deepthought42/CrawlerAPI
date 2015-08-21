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
	public Path findPathToClosestRoot(int start_idx) throws NullPointerException{
		Path path = new Path();
		int weight = 0;
		boolean isRootFound = false;
		//this implementation is wrong. Needs to be fixed.
		super.frontier.put(start_idx, 0);
		while(!isRootFound && super.frontier.size() > 0){
			int lowest_weight = 999999;
			int closest_index = -1;
			System.out.println(Thread.currentThread().getName() + " -> FRONTIER SIZE :: "+ super.frontier.size());
			for(int index : super.frontier.keySet()){
				if(super.frontier.get(index) < lowest_weight){
					System.out.println(Thread.currentThread().getName() + " -> Found candidate for shortest path on frontier");
					lowest_weight = super.frontier.get(index);
					closest_index = index;
				}
			}
			System.out.println(Thread.currentThread().getName() + " -> closest Index: "+closest_index);
			super.removeNodeFromFrontier(closest_index);
			super.visited.put(closest_index, super.frontier.get(closest_index));
			path.add(closest_index);
			for(Integer vertex_idx : graph.getFromIndices(closest_index)){
				System.out.println(Thread.currentThread().getName() + " -> adding from index : "+vertex_idx);
				super.frontier.put(vertex_idx, ++weight);
			}
			Vertex<?> vertex = graph.getVertices().get(closest_index);
			
			System.out.println(Thread.currentThread().getName() + " -> VERTEX ADDED TO PATH with idx : "+closest_index);
			if(vertex.isRoot() || closest_index == 0){
				isRootFound = true;
				break;
			}	
		};
		
		//invert path
		Path corrected_path = new Path();
		for(int i = path.getPath().size()-1; i > -1; i--){
			corrected_path.add(path.getPath().get(i));
		}
		return corrected_path;
	}

	@Override
	public Path findPathToClosestRoot(Vertex<?> startVertex) throws NullPointerException{
		System.out.println("FINDING CLOSEST PATH TO ROOT FOR VERTEX : "+startVertex);
		int vertex_idx = graph.findVertexIndex(startVertex);
		
		return findPathToClosestRoot(vertex_idx);
	}
}

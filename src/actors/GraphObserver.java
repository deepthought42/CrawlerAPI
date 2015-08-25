package actors;

import java.util.Observable;
import java.util.Observer;

import graph.Graph;

public class GraphObserver implements Observer{
	private Graph graph = null;
	
	public GraphObserver(Graph graph) {
		this.graph = graph;
		this.graph.addObserver(this);
	}

	public void update(Observable o, Object arg) {
		this.graph = (Graph)o;
		//System.err.println("+++++++++++THERE ARE NOW " + this.graph.getVertices().size() + " VERTICES EDGES IN GRAPH+++++++++++");
	}
	
	/**
	 * 
	 * @return
	 */
	public Graph getGraph(){
		return this.graph;
	}

}

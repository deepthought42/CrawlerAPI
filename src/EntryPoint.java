

import graph.Graph;
import graph.Vertex;

import java.net.MalformedURLException;

import actors.BrowserActor;
import actors.GraphObserver;
import actors.ResourceManagementActor;
import actors.WorkAllocationActor;
import observableStructs.ObservableHash;
import observableStructs.ObservableQueue;
import structs.Path;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class EntryPoint {
	public static void main(String[] args){
		ResourceManagementActor resourceManager = new ResourceManagementActor(2);
		ObservableHash<Integer, Path> hashQueue = new ObservableHash<Integer, Path>();
		Graph graph = new Graph();
		GraphObserver graphObserver = new GraphObserver(graph);
		String url = "http://127.0.0.1:3000";
		//String url = "http://brandonkindred.ninja/blog";
		//String url = "http://www.ideabin.io";
		System.out.print("INITIALIZING ACTOR...");
		System.out.println("TOTAL CORES AVAILABLE : "+Runtime.getRuntime().availableProcessors());
		
		System.out.print("Initializing page monitor...");
		WorkAllocationActor workAllocator = new WorkAllocationActor(hashQueue, resourceManager, graphObserver);
		BrowserActor browserActor;
		
		try {
			browserActor = new BrowserActor(url, new Path(), hashQueue, graph, resourceManager, workAllocator);
			browserActor.start();
		} catch (MalformedURLException e1) {
			System.out.println("MALFORMED URL EXCEPTION");
		}
		
	}
}

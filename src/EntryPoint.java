

import graph.Graph;
import graph.Vertex;

import java.net.MalformedURLException;

import actors.BrowserActor;
import actors.GraphObserver;
import actors.ResourceManagementActor;
import actors.WorkAllocationActor;
import observableStructs.ObservableQueue;
import structs.Path;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class EntryPoint {
	public static void main(String[] args){
		ResourceManagementActor resourceManager = new ResourceManagementActor(6);
		ObservableQueue<Path> pathQueue = new ObservableQueue<Path>();
		Graph graph = new Graph();
		GraphObserver graphObserver = new GraphObserver(graph);
		//String url = "http://127.0.0.1:3000";
		String production_url = "http://www.ideabin.io";
		System.out.print("INITIALIZING ACTOR...");
		System.out.println("TOTAL CORES AVAILABLE : "+Runtime.getRuntime().availableProcessors());
		
		System.out.print("Initializing page monitor...");
		WorkAllocationActor workAllocator = new WorkAllocationActor(pathQueue, resourceManager, graphObserver);
		BrowserActor browserActor;
		try {
			browserActor = new BrowserActor(production_url, new Path(), pathQueue, graph, resourceManager, workAllocator);
			browserActor.start();
		} catch (MalformedURLException e1) {
			System.out.println("MALFORMED URL EXCEPTION");
		}
		
	}
}

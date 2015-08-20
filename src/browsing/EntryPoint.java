package browsing;

import graph.Graph;
import graph.Vertex;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import actors.BrowserActor;
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
		ResourceManagementActor resourceManager = new ResourceManagementActor(5);
		ObservableQueue<Vertex<?>> vertexQueue = new ObservableQueue<Vertex<?>>();
		Graph graph = new Graph();
		
		String url = "localhost:3000";
		System.out.print("INITIALIZING ACTOR...");
		ExecutorService e = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		System.out.println("TOTAL CORES AVAILABLE : "+Runtime.getRuntime().availableProcessors());
		
		System.out.print("Initializing page monitor...");
		WorkAllocationActor workAllocator = new WorkAllocationActor(vertexQueue, graph, resourceManager);
		BrowserActor browserActor;
		try {
			browserActor = new BrowserActor(url, new Path(), vertexQueue, graph, resourceManager, workAllocator);
			browserActor.start();
		} catch (MalformedURLException e1) {
			System.out.println("MALFORMED URL EXCEPTION");
		}
		
	}
}

package browsing;

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
		//graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
		//registerShutdownHook( graphDb );
		ResourceManagementActor resourceManager = new ResourceManagementActor(1);
		ObservableQueue<Path> pathQueue = new ObservableQueue<Path>();
		String url = "localhost:3000";
		System.out.print("INITIALIZING ACTOR...");
		ExecutorService e = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		System.out.println("TOTAL CORES AVAILABLE : "+Runtime.getRuntime().availableProcessors());
		//e.execute(new BrowserActor(url, pageNode));
		
		WorkAllocationActor workAllocator = new WorkAllocationActor(pathQueue, resourceManager);
		BrowserActor browserActor = new BrowserActor(url, pathQueue, resourceManager, workAllocator);
		browserActor.start();
		
		System.out.println("Registered observer!");
		System.out.println("THREADS STILL RUNNING AT END :: "+Thread.activeCount());

	}
}

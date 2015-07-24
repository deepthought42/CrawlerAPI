package browsing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
		
		ObservableQueue<Path> pathQueue = new ObservableQueue<Path>();
		String url = "localhost:3000";
		System.out.print("INITIALIZING ACTOR...");
		ExecutorService e = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		System.out.println("TOTAL CORES AVAILABLE : "+Runtime.getRuntime().availableProcessors());
		//e.execute(new BrowserActor(url, pageNode));
		
		WorkAllocationActor workAllocator = new WorkAllocationActor(pathQueue);
		workAllocator.start();
		BrowserActor browserActor = new BrowserActor(url, pathQueue);
		browserActor.start();
		
		System.out.println("Registered observer!");
		System.out.println("THREADS STILL RUNNING AT END :: "+Thread.activeCount());

	}
}

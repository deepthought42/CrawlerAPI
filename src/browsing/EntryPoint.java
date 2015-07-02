package browsing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import observableStructs.ObservableQueue;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class EntryPoint {
	public static void main(String[] args){
		//graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
		//registerShutdownHook( graphDb );
		
		ObservableQueue<ConcurrentNode<Page>> pageQueue = new ObservableQueue<ConcurrentNode<Page>>();
		String url = "localhost:3000";
		System.out.print("INITIALIZING ACTOR...");
		ExecutorService e = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		System.out.println("TOTAL CORES AVAILABLE : "+Runtime.getRuntime().availableProcessors());
		//e.execute(new BrowserActor(url, pageNode));
		
		WorkAllocationActor workAllocator = new WorkAllocationActor(pageQueue);
		BrowserActor browserActor = new BrowserActor(url, pageQueue);
		browserActor.start();
		
		System.out.println("Registered observer!");

		//e.execute( new BrowserActor(url, pageNode));
		//BrowserActor actor = new BrowserActor(url, pageNode);
		//BrowserActor actor2 = new BrowserActor(url, pageNode);

		
		//actor.start();
		//actor2.start();
		
		
		//pageNode = actor.getPageNode();
		
	}
}

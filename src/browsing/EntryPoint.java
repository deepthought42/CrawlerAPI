package browsing;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class EntryPoint {
	public static void main(String[] args){
		//graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
		//registerShutdownHook( graphDb );
		
		String url = "localhost:3000";
		System.out.println("INITIALIZING ACTOR...");
		BrowserActor actor = new BrowserActor(url);
		System.out.println("ACTOR INITIALIZED!");
		actor.run();
	}
}

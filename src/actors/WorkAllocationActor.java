package actors;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import browsing.Page;
import browsing.PathObject;
import observableStructs.ObservableHash;
import scala.concurrent.Future;
import shortTerm.ShortTermMemoryRegistry;
import structs.Path;
import structs.PathRepresentation;

/**
 * Work Allocator has the responsibility of starting new Actors, monitoring
 * {@link Path}s traversed, and allotting work to Actors as work is requested.
 * 
 * @author Brandon Kindred
 *
 */
public class WorkAllocationActor extends UntypedActor {
	
	ObservableHash<Integer, Path> hash_queue = null;
	private static ShortTermMemoryRegistry shortTermMemory = new ShortTermMemoryRegistry();
	private static URL url = null;
	
	/**
	 * Finds smallest key in hash
	 * 
	 * @return <= 99999
	 */
	public synchronized Integer getSmallestKey(){
		int smallest_key = 99999;
		for(Integer key : hash_queue.getQueueHash().keySet()){
			if(key==null){
				continue;
			}
			if(key < smallest_key && hash_queue.getQueueHash().get(key) != null && !hash_queue.getQueueHash().get(key).isEmpty()){
				smallest_key = key;
			}
		}
		return smallest_key;
	}
	
	
	/**
	 * 
	 * @param path
	 * @param last_page
	 * @param current_page
	 * @param graphObserver
	 * @param browser_actor
	 */
	public void registerCrawlResult(Path path, 
								    Page last_page, 
								    Page current_page, 
								    BrowserActor browser_actor){		
		//resourceManager.punchOut(browser_actor);
		boolean isValuable = false;
		//If cycle exists we don't care, dishing it out for work was a mistake
		if(!Path.hasCycle(path) && !Path.hasPageCycle(path)){
			//if last page in path is different than the current page then register as valuable
			ArrayList<Path> pathExpansions = null;
			if(last_page.equals(current_page) && path.getPath().size() > 1){
				isValuable = false;
			}
			else if(path.getPath().size() == 0){
				isValuable = true;
				path.add(new PathObject<Page>(current_page));
				pathExpansions = Path.expandPath(path);
			}
			else if(last_page.equals(current_page) && path.getPath().size() == 1){
				isValuable = true;
				pathExpansions = Path.expandPath(path);
			}
			else if(!last_page.equals(current_page)){
				isValuable = true;
				pathExpansions = Path.expandPath(path);
			}
			
			shortTermMemory.registerPath(path, isValuable);
			//register path with tracker intended for front end communications
			
			if(pathExpansions != null){	
				for(Path expanded_path : pathExpansions){
					shortTermMemory.registerPath(expanded_path, null);
				}
				System.out.println("Added "+pathExpansions.size()+" paths to the unknown value queue");
			}
		}
		
		//System.err.println("RESOURCE MANAGEMENT :: "+resourceManager);
		// Allocates work to Browser Actors until available resource ceiling is reached. 
		//while(resourceManager.areResourcesAvailable()){
			System.out.println(Thread.currentThread().getName() + 
					" -> Path length being passed to browserActor = "+path.getPath().size());
			Path new_path = retrieveNextPath();
			final ActorRef new_browser_actor;
			try {
				//new_browser_actor = actor_system.actorOf(Props.create(BrowserActor.class), "browserActor");
				//new_browser_actor.tell(new Path(), getSelf());
				
				//new_browser_actor = new BrowserActor(current_page.getUrl().toString(), new_path);
				
				//WorkAllocationActor.resourceManager.punchIn(new_browser_actor);
				//new_browser_actor.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	//}
	
	/**
	 * 
	 * @return
	 */
	public static Path retrieveNextPath(){
		HashMap<String, PathRepresentation> path_representations = shortTermMemory.getUnknownPaths();
		String key = getRandomKey(path_representations);
		PathRepresentation path_rep = path_representations.remove(key);
		
		Path path = new Path();
		
		for(Integer hash : path_rep.getPathRepresentation()){
			path.add(shortTermMemory.getPathNodes().get(hash));
		}
		
		return path;
	}
	
	/**
	 * Finds random key that exists in hash
	 * 
	 * @return <= 99999
	 */
	public static String getRandomKey(HashMap<String, PathRepresentation> pathHash){
		Set<String> keys = pathHash.keySet();
		int total_keys = keys.size();
		Random rand = new Random();
		int rand_idx = rand.nextInt(total_keys);
		String key_object = null;
		int key_idx = 0;
		for(String key : keys){
			if(key_idx == rand_idx){
				key_object = key;
				break;
			}

			key_idx++;
		}
		
		return key_object;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Path){
			Path path = (Path)message;
			
			final ActorRef browser_actor = this.getContext().actorOf(Props.create(BrowserActor.class), "browserActor");
			browser_actor.tell(path, getSelf() );
		}
		else if(message instanceof URL){
			url = (URL)message;
			System.err.println("message is URL for workAllocator");
			final ActorRef browser_actor = this.getContext().actorOf(Props.create(BrowserActor.class), "browserActor");
			browser_actor.tell((URL)message, ActorRef.noSender());
		}
		else unhandled(message);	
	}
}

package actors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import browsing.Page;
import browsing.PathObject;
import observableStructs.ObservableHash;
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
public class WorkAllocationActor extends Thread {
	
	ObservableHash<Integer, Path> hash_queue = null;
	private static ResourceManagementActor resourceManager = null;
	private static ShortTermMemoryRegistry shortTermMemory = new ShortTermMemoryRegistry();
	
	/**
	 * Construct new {@link WorkAllocationActor} 
	 * @param queue
	 * @param resourceManager
	 */
	public WorkAllocationActor(ResourceManagementActor resourceMgr,
							   String url){
		resourceManager = resourceMgr;
		BrowserActor browserActor;
		
		try {
			ExecutorService es = Executors.newSingleThreadExecutor();
			browserActor = new BrowserActor(url, new Path());
			es.submit(browserActor);
			WorkAllocationActor.resourceManager.punchIn(browserActor);
			//browserActor.start();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
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
	public static void registerCrawlResult(Path path, 
							   Page last_page, 
							   Page current_page, 
							   BrowserActor browser_actor){		
		resourceManager.punchOut(browser_actor);
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
		
		// Allocates work to Browser Actors until available resource ceiling is reached. 
		while(resourceManager.areResourcesAvailable()){
			System.out.println(Thread.currentThread().getName() + 
					" -> Path length being passed to browserActor = "+path.getPath().size());
			Path new_path = retrieveNextPath();
			BrowserActor new_browser_actor;
			try {
				ExecutorService es = Executors.newSingleThreadExecutor();
				
				new_browser_actor = new BrowserActor("http://127.0.0.1:3000", new_path);
				Future<Boolean> browserActorResult = es.submit(new_browser_actor);
				WorkAllocationActor.resourceManager.punchIn(new_browser_actor);
				boolean didChangeOccur = browserActorResult.get();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
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
}

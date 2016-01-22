package actors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import browsing.Page;
import browsing.PageElement;
import observableStructs.ObservableHash;
import shortTerm.ShortTermMemoryRegistry;
import structs.Path;

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
			browserActor = new BrowserActor(url, new Path());
			WorkAllocationActor.resourceManager.punchIn(browserActor);
			browserActor.start();
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
	 * Evaluate 2 paths to determine if either of them end in pages that have 
	 *   the same visible elements. If they do, then determine which of the 
	 *   {@link ElementAction element action} nodes 
	 *   {@link PageElement page element} is the parent of the other.
	 *   
	 * @param path1 {@link Path path} to be evaluated against
	 * @param path2 {@link Path path} to be evaluated against
	 * @return 0 if path1 is parent, 1 if path2 is parent. -1 if they are unrelated
	 */
	private int evaluatePaths(Path path1, Path path2) throws NullPointerException{
		Page path1Page = (Page) path1.getLastPageVertex();
		Page path2Page = (Page) path2.getLastPageVertex();
		
		ArrayList<PageElement> path1Elements = path1Page.getElements();
		ArrayList<PageElement> path2Elements = path2Page.getElements();
		
		boolean allElementsEqual = true;
		if(path1Page.getElements().size() == path2Page.getElements().size()){
			for(int idx = 0; idx < path1Page.getElements().size(); idx++){
				if(!path1Elements.get(idx).equals(path2Elements.get(idx))){
					allElementsEqual = false;
				}
			}
		}
		
		//get previous node in both paths
		/*Vertex<?> path1PrevNode = graphObserver.getGraph().getVertices().get(path1.getPath().get(path1Idx - 1));
		Vertex<?> path2PrevNode = graphObserver.getGraph().getVertices().get(path2.getPath().get(path2Idx - 1));
		
		if(allElementsEqual 
				&& path1PrevNode.getData().getClass().getCanonicalName().equals("browsing.ElementAction") 
				&& path2PrevNode.getData().getClass().getCanonicalName().equals("browsing.ElementAction"))
		{
			//determine which node is the parent
			ElementAction path1ElemAction = (ElementAction) path1PrevNode.getData();
			ElementAction path2ElemAction = (ElementAction) path2PrevNode.getData();
			
			if(path1ElemAction.getPageElement().isChildElement(path2ElemAction.getPageElement())){
				return 0;
			}
			else if(path1ElemAction.getPageElement().isChildElement(path2ElemAction.getPageElement())){
				return 1;
			}
			else{
				System.err.println("NEITHER PATH 1 OR PATH 2 ARE PARENTS OF EACH OTHER");
			}
		}
		*/
		return -1;
	}
	
	public void run(){
		
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
		//if last page in path is different than the current page then register as valuable
		ArrayList<Path> pathExpansions = null;
		if(last_page.equals(current_page) && path.getPath().size() != 1){
			isValuable = false;
		}
		else if(path.getPath().size() == 0){
			isValuable = true;
			path.add(current_page);
			pathExpansions = Path.expandPath(path);
		}
		else{
			isValuable = true;
			pathExpansions = Path.expandPath(path);
		}
		
		shortTermMemory.registerPath(path, isValuable);
		
		if(pathExpansions != null){	
			for(Path expanded_path : pathExpansions){
				shortTermMemory.registerPath(expanded_path, null);
			}
		}
		while(resourceManager.areResourcesAvailable()){
			System.out.println(Thread.currentThread().getName() + " -> Path length being passed to browserActor = "+path.getPath().size());
			if(pathExpansions != null){
				Path new_path = retrieveNextPath();
				BrowserActor new_browser_actor;
				try {
					new_browser_actor = new BrowserActor("http://127.0.0.1:3000", new_path);
					WorkAllocationActor.resourceManager.punchIn(new_browser_actor);
					new_browser_actor.start();
		
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
				System.out.println("WORK ALLOCATOR :: BROWSER ACTOR STARTED!");
			}		
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public static Path retrieveNextPath(){
		HashMap<String, Path> paths = shortTermMemory.getUnknownPaths();
		String key = getRandomKey(paths);
		Path path = paths.remove(key);
		
		return path;
	}
	
	/**
	 * Finds random key that exists in hash
	 * 
	 * @return <= 99999
	 */
	public static String getRandomKey(HashMap<String, Path> pathHash){
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

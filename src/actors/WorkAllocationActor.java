package actors;

import graph.Graph;
import graph.Vertex;

import java.io.IOException;
import java.util.ArrayList;

import browsing.ElementAction;
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
	ResourceManagementActor resourceManager = null;
	GraphObserver graphObserver = null;
	private static ShortTermMemoryRegistry shortTermMemory = new ShortTermMemoryRegistry();
	
	/**
	 * Construct new {@link WorkAllocationActor} 
	 * @param queue
	 * @param resourceManager
	 */
	public WorkAllocationActor(ResourceManagementActor resourceManager,
							   String url){
		Graph graph = new Graph();
		this.graphObserver = new GraphObserver(graph);
		this.resourceManager = resourceManager;
		
		BrowserActor browserActor;
		
		try {
			browserActor = new BrowserActor(url, new Path(), graphObserver);
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
		Vertex<Page> path1Vertex = (Vertex<Page>) path1.getLastPageVertex(this.graphObserver.getGraph());
		Vertex<Page> path2Vertex = (Vertex<Page>) path2.getLastPageVertex(this.graphObserver.getGraph());

		Page path1Page = (Page)path1Vertex.getData();
		Page path2Page = (Page)path2Vertex.getData();
		
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
	
	public static void registerCrawlResult(Path path, Page last_page, Page current_page, GraphObserver graphObserver){
		
		boolean isValuable = false;
		//if last page in path is different than the current page then register as valuable
		
		if(last_page.equals(current_page)){
			isValuable = false;
		}
		else{
			isValuable = true;
		}
		
		shortTermMemory.registerPath(path, isValuable);
		
		  System.out.println(Thread.currentThread().getName() + " -> Path length being passed to browserActor = "+path.getPath().size());
	       BrowserActor browserActor;
			try {
				browserActor = new BrowserActor("http://127.0.0.1:3000", path, graphObserver);
				browserActor.start();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("WORK ALLOCATOR :: BROWSER ACTOR STARTED!");
	}
}

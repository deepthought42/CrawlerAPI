package actors;

import graph.Graph;
import graph.Vertex;
import graph.searchAlgorithms.A_Star;
import graph.searchAlgorithms.GraphSearch;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.openqa.selenium.NoSuchElementException;

import browsing.ElementAction;
import browsing.Page;
import browsing.PageElement;
import observableStructs.ObservableQueue;
import structs.Path;

/**
 * Graph Condensing Agent iterates over a graph of nodes and condenses
 * matching nodes down into a single node.
 * 
 * @author Brandon Kindred
 *
 */
public class WorkAllocationActor extends Thread implements Observer {
	
	ObservableQueue<Path> path_queue = null;
	ResourceManagementActor resourceManager = null;
	GraphObserver graphObserver = null;
	
	/**
	 * 
	 * @param queue
	 * @param resourceManager
	 */
	public WorkAllocationActor(ObservableQueue<Path> queue, 
							   ResourceManagementActor resourceManager,
							   GraphObserver graphObserver){
		this.path_queue = queue;
		this.path_queue.addObserver(this);
		this.graphObserver = graphObserver;
		this.resourceManager = resourceManager;
	}
	
	public void run(){
		allocateVertexProcessing();
	}
	
	/**
	 * Whenever an update is observed the current queue is updated
	 * 
	 * @param o
	 * @param arg
	 */
	public void update(Observable o, Object arg)
	{
		if(o instanceof ObservableQueue){
	    	path_queue = (ObservableQueue) o;
			allocateVertexProcessing();
		}
	}
	
	/**
	 * 
	 */
	public void allocateVertexProcessing(){
		if(path_queue.size() > 0){
			try{
				while( resourceManager.areResourcesAvailable() && !path_queue.isEmpty()){
					Path path = retrieveNextPath();
					
					if(path != null){
						System.out.println("WORK ALLOCATION ACTOR HAS RETRIEVED NEXT VERTEX.");
				        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++");
				        //int start_idx = graphObserver.getGraph().findVertexIndex(vertex);
						//GraphSearch graphSearch = new A_Star(graphObserver.getGraph());

					    //Path path = graphSearch.findPathToClosestRoot(start_idx);
					    
				        System.out.println(Thread.currentThread().getName() + " -> Path length being passed to browserActor = "+path.getPath().size());
				        BrowserActor browserActor = new BrowserActor(path_queue, graphObserver.getGraph(), path, this.resourceManager, this);
						browserActor.start();

						System.out.println("WORK ALLOCATOR :: BROWSER ACTOR STARTED!");
					}
					else{
						System.out.println("WORK ALLOCATOR :: PATH is null.");
					}
				}
	    	}
			catch(NoSuchElementException e){
	    	}
	    	catch(NullPointerException e){
	    	} 
			catch (MalformedURLException e) {
				System.out.println("MALFORMED URL EXCEPTION");
			}
		}
	}
	
	/**
	 * Returns next path to be explored.
	 * 
	 * @return {@link Path} to be explored
	 */
	public Path retrieveNextPath() throws NullPointerException{
		return path_queue.poll();
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
		int path1Idx = getFurthestPageIndex(path1);
		int path2Idx = getFurthestPageIndex(path2);
		if(path1Idx == 0 || path2Idx == 0){
			return -1;
		}
		Page path1Page = (Page)(graphObserver.getGraph().getVertices().get(path1.getPath().get(path1Idx))).getData();
		Page path2Page = (Page)(graphObserver.getGraph().getVertices().get(path2.getPath().get(path2Idx))).getData();
		
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
		Vertex<?> path1PrevNode = graphObserver.getGraph().getVertices().get(path1.getPath().get(path1Idx - 1));
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
		return -1;
	}
	
	/**
	 * Retreives the index for the page node closest to the end of the path
	 * 
	 * @param path
	 * @return
	 */
	public int getFurthestPageIndex(Path path){
		int pathSize = path.getPath().size();
		for(int i = pathSize-1; i >= 0; i--){
			Vertex<?> pathNode = graphObserver.getGraph().getVertices().get(path.getPath().get(i));
			if(pathNode.getData().getClass().getCanonicalName().equals("browsing.Page")){
				return i;
			}
		}
		return -1;
	}
}

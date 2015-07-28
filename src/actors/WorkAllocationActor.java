package actors;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import org.openqa.selenium.NoSuchElementException;

import browsing.ConcurrentNode;
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
public class WorkAllocationActor implements Observer{
	
	ObservableQueue<Path> queue = null;
	ResourceManagementActor resourceManager = null;
	
	/**
	 * 
	 * @param queue
	 * @param resourceManager
	 */
	public WorkAllocationActor(ObservableQueue<Path> queue, ResourceManagementActor resourceManager){
		this.queue = queue;
		this.queue.addObserver(this);
		this.resourceManager = resourceManager;
	}

	//Whenever an update is observed the current queue is updated
	public void update(Observable o, Object arg)
	{
	    if (o instanceof ObservableQueue){
	    	queue = (ObservableQueue) o;
	        //System.out.println("MyObserver1 says: path left is now : [" + queue.size() + "]");
	        allocatePathProcessing();
	    }else{
	        System.out.println("The observable object was not of the correct type");
	    }
	}
	
	/**
	 * 
	 */
	public void allocatePathProcessing(){
		if(queue.size() > 0){
			try{
				if( resourceManager.areResourcesAvailable()){

					Path path = (Path)queue.poll();
					if(path != null){
						System.out.println("WORK ALLOCATION ACTOR PATH LENGTH :: " + path.getPath().size());
				        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++");
				        
				        BrowserActor browserActor = new BrowserActor(queue, path, this.resourceManager, this);
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
		}
	}
	
	public Path retrieveNextPath(){
		return (Path)queue.poll();
	}
	
	/**
	 * Evaluate 2 paths to determine if either of them end in pages that have 
	 *   the same visible elements. If they do, then determine which of the 
	 *   {@link ElementAction element action} nodes 
	 *   {@link PageElement page element} is the parent of the other.
	 *   
	 * @param path1 {@link Path path} to be evaluated against
	 * @param path2 {@link Path path} to be evaluated against
	 * @return the parent {@link Path path} or null if they are unrelated
	 */
	public Path evaluatePaths(Path path1, Path path2){
		Page path1Page = getFurthestPage(path1);
		Page path2Page = getFurthestPage(path2);
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
		ConcurrentNode<?> path1PrevNode = (ConcurrentNode<?>) path1.getPath().get(path1.getPath().size()-2);
		ConcurrentNode<?> path2PrevNode = (ConcurrentNode<?>) path1.getPath().get(path2.getPath().size()-2);
		
		if(allElementsEqual 
				&& path1PrevNode.getData().getClass().getCanonicalName().equals("browsing.ElementAction") 
				&& path2PrevNode.getData().getClass().getCanonicalName().equals("browsing.ElementAction"))
		{
			//determine which node is the parent
			ElementAction path1ElemAction = (ElementAction) path1PrevNode.getData();
			ElementAction path2ElemAction = (ElementAction) path2PrevNode.getData();
			
			if(path1ElemAction.getPageElement().getXpath().contains(path2ElemAction.getPageElement().getXpath())){
				return path1;
			}
			else if(path1ElemAction.getPageElement().getXpath().contains(path2ElemAction.getPageElement().getXpath())){
				return path2;
			}
			else{
				System.err.println("NEITHER PATH 1 OR PATH 2 ARE PARENTS OF EACH OTHER");
			}
		}
		return null;
	}
	
	public Page getFurthestPage(Path path){
		int pathSize = path.getPath().size();
		for(int i = pathSize-1; i >= 0; i--){
			ConcurrentNode<?> pathNode = (ConcurrentNode<?>) path.getPath().get(i);
			if(pathNode.getClass().getCanonicalName().equals("browsing.Page")){
				return (Page)pathNode.getData();
			}
		}
		return null;
	}
}

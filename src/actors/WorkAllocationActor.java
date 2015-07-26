package actors;

import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import org.openqa.selenium.NoSuchElementException;

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
				        
				        BrowserActor browserActor = new BrowserActor(queue, path, this.resourceManager);
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
}

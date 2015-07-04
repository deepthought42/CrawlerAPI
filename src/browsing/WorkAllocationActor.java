package browsing;

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
public class WorkAllocationActor extends Thread implements Observer{
	
	ObservableQueue queue = null;
	public WorkAllocationActor(ObservableQueue queue){
		this.queue = queue;
		this.queue.addObserver(this);
		this.start();
	}

	//Whenever an update is observed the current queue is updated
	public void update(Observable o, Object arg)
	{
		//System.out.println("O CLASS :::: "+o.getClass());
	    if (o instanceof ObservableQueue){
	    	queue = (ObservableQueue) o;
	        System.out.println("MyObserver1 says: OUTPUTS size is now : [" + queue.size() + "]");
	    	
	    }else{
	        System.out.println("The observable object was not of the correct type");
	    }
	}
	
	public void run(){
		long tStart = System.currentTimeMillis();
		int threadCount = Thread.activeCount();
		while(true){
			if(threadCount < 4){
				try{
					if(queue.size() >0){
						try{
							Path path = (Path)queue.poll();
							ConcurrentNode<?> node = (ConcurrentNode<?>) path.getPath().getFirst();
					        System.out.println("Element outputs ::: " + node.getOutputs().size());
					        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++");
					        
					        System.out.print("STARTING NEW THREAD TO MAP PATH..");
					        BrowserActor browserActor = new BrowserActor(queue, path);
							browserActor.start();
					        System.out.println("STARTED!");
				    	}
						catch(NoSuchElementException e){
				    	}
				    	catch(NullPointerException e){
				    	}
					}
				}
				catch(NullPointerException e){
					
				}
			}
		}
	}
}

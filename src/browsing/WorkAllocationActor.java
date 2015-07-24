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
	
	ObservableQueue<Path> queue = null;
	public WorkAllocationActor(ObservableQueue<Path> queue){
		this.queue = queue;
		this.queue.addObserver(this);
		
	}

	//Whenever an update is observed the current queue is updated
	public void update(Observable o, Object arg)
	{
	    if (o instanceof ObservableQueue){
	    	queue = (ObservableQueue) o;
	        System.out.println("MyObserver1 says: path left is now : [" + queue.size() + "]");
	        this.run();
	    }else{
	        System.out.println("The observable object was not of the correct type");
	    }
	}
	
	public void run(){
		long tStart = System.currentTimeMillis();
		int threadCount = Thread.activeCount();
		int processorCount = Runtime.getRuntime().availableProcessors()*3;
		System.out.println("TOTAL PROCESSES ALLOWED..."+processorCount);
		//while(!queue.isEmpty() || ((System.currentTimeMillis()-tStart) / 1000.0) < 120){
			if(threadCount != Thread.activeCount()){
				threadCount = Thread.activeCount();
				System.out.println("!!!!!     CURRENT THREAD COUNT :: "+threadCount +   "!!!!!!!");
			}
			if(Thread.activeCount() < processorCount){
				try{
					if(queue.size() > 0){
						//System.out.println("QUEUE IS NOT EMPTY...");
						try{
							Path path = (Path)queue.poll();
							if(path != null){
								//System.out.println("PATH IS NOT NULL...");
								System.out.println("WORK ALLOCATION ACTOR PATH LENGTH :: " + path.getPath().size());
								ConcurrentNode<?> node = (ConcurrentNode<?>) path.getPath().getFirst();
						        System.out.println("Element outputs ::: " + node.getOutputs().size());
						        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++");
						        
						        BrowserActor browserActor = new BrowserActor(queue, path);
								browserActor.start();
						        System.out.println("BROWSER ACTOR STARTED!");
							}
							else{
								System.out.println(this.getName() + " :: PATH is null.");
							}
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
		//}
		System.err.println("EXITING WORK ALLOCATOR");
	}
}

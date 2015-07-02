package browsing;

import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import org.openqa.selenium.NoSuchElementException;

import observableStructs.ObservableQueue;

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
		while(true){
			try{
				if(queue.size() >0){
					try{
						ConcurrentNode<Page> element = (ConcurrentNode<Page>) queue.poll();
				        System.out.println("Element outputs ::: " + element.getOutputs().size());
				        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++");
			    	}
					catch(NoSuchElementException e){
			    	}
			    	catch(NullPointerException e){
			    	}
				}
			}
			catch(NullPointerException e){
				
			}
			if(System.currentTimeMillis() > (tStart + 200000)){
				break;
			}
		}
	}
}

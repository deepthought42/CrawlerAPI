package actors;

import java.net.URL;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import browsing.Page;
import observableStructs.ObservableHash;
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

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Path){
			Path path = (Path)message;
			final ActorRef browser_actor = this.getContext().actorOf(Props.create(BrowserActor.class), "browserActor"+UUID.randomUUID());
			browser_actor.tell(path, getSelf() );
		}
		else if(message instanceof URL){
			System.err.println("message is URL for workAllocator");
			final ActorRef browser_actor = this.getContext().actorOf(Props.create(BrowserActor.class), "browserActor");
			browser_actor.tell((URL)message, getSelf());
		}
		else unhandled(message);	
	}
}

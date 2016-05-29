package actors;

import java.net.URL;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import WorkManagement.WorkAllowanceStatus;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import observableStructs.ObservableHash;
import structs.Message;
import structs.Path;

/**
 * Responsible for starting new Actors, monitoring
 * {@link Path}s traversed, and allotting work to Actors as work is requested.
 * 
 * @author Brandon Kindred
 *
 */
public class WorkAllocationActor extends UntypedActor {
    private static final Logger log = Logger.getLogger(WorkAllocationActor.class);

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
		if(message instanceof Message){
			Message<?> acct_message = (Message<?>)message;
			
			if(WorkAllowanceStatus.checkStatus(acct_message.getAccountKey())){
	
				if(acct_message.getData() instanceof Path){
					
					Path path = (Path)acct_message.getData();
				
					Message<Path> path_msg = new Message<Path>(acct_message.getAccountKey(), path);
					final ActorRef browser_actor = this.getContext().actorOf(Props.create(BrowserActor.class), "browserActor"+UUID.randomUUID());
					browser_actor.tell(path_msg, getSelf() );
				}
				else if(acct_message.getData() instanceof URL){
					log.info("message is URL for workAllocator");
					final ActorRef browser_actor = this.getContext().actorOf(Props.create(BrowserActor.class), "browserActor");

					Message<URL> url_msg = new Message<URL>(acct_message.getAccountKey(), (URL)acct_message.getData());
					browser_actor.tell(url_msg, getSelf());
				}
			}
			else{
				log.log(Priority.WARN, "Work allocation actor did not start any work due to account key not having a runnable status");
			}
		}
	}
}

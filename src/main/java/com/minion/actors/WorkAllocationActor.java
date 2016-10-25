package com.minion.actors;

import java.net.URL;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.WorkManagement.WorkAllowanceStatus;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.minion.observableStructs.ObservableHash;
import com.minion.structs.Message;
import com.minion.structs.Path;
import com.minion.tester.Test;



/**
 * Responsible for starting new Actors, monitoring
 * {@link Path}s traversed, and allotting work to Actors as work is requested.
 * 
 * @author Brandon Kindred
 *
 */
public class WorkAllocationActor extends UntypedActor {
    private static final Logger log = LoggerFactory.getLogger(WorkAllocationActor.class);

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
			log.info("Checking status of account key...");
			if(WorkAllowanceStatus.checkStatus(acct_message.getAccountKey())){
				log.info("Approved account : "+acct_message.getAccountKey());
				if(acct_message.getData() instanceof Path){
					log.info("Path passed to work allocator");
					//Path path = (Path)acct_message.getData();
				
					//Message<Path> path_msg = new Message<Path>(acct_message.getAccountKey(), path);
					final ActorRef browser_actor = this.getContext().actorOf(Props.create(BrowserActor.class), "browserActor"+UUID.randomUUID());
					browser_actor.tell(acct_message, getSelf() );
				}
				else if(acct_message.getData() instanceof Test){
					log.info("Test passed to work allocator");
					//Test test = (Test)acct_message.getData();
					
					//Message<Test> test_msg = new Message<Test>(acct_message.getAccountKey(), test);
					final ActorRef browser_actor = this.getContext().actorOf(Props.create(BrowserActor.class), "browserActor"+UUID.randomUUID());
					browser_actor.tell(acct_message, getSelf() );
				}
				else if(acct_message.getData() instanceof URL){
					log.info("message is URL for workAllocator");
					final ActorRef browser_actor = this.getContext().actorOf(Props.create(BrowserActor.class), "browserActor"+UUID.randomUUID());

					//Message<URL> url_msg = new Message<URL>(acct_message.getAccountKey(), (URL)acct_message.getData());
					browser_actor.tell(acct_message, getSelf());
				}
			}
			else{
				log.warn("Work allocation actor did not start any work due to account key not having a runnable status");
			}
		}
	}
}

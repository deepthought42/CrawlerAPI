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

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_message = (Message<?>)message;
			log.info("Checking status of account key...");
			if(WorkAllowanceStatus.checkStatus(acct_message.getAccountKey())){
				log.info("Approved account : "+acct_message.getAccountKey());
				if(acct_message.getData() instanceof Path || 
						acct_message.getData() instanceof Test || 
						acct_message.getData() instanceof URL){
					log.info("Path passed to work allocator");
					final ActorRef browser_actor = this.getContext().actorOf(Props.create(BrowserActor.class), "browserActor"+UUID.randomUUID());
					browser_actor.tell(acct_message, getSelf() );
				}
			}
			else{
				log.warn("Work allocation actor did not start any work due to account key not having a runnable status");
			}
		}
	}
}

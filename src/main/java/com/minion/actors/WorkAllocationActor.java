package com.minion.actors;

import java.net.URL;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.minion.WorkManagement.WorkAllowanceStatus;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Path;
import com.qanairy.models.Test;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import com.minion.structs.Message;



/**
 * Responsible for starting new Actors, monitoring
 * {@link Path}s traversed, and allotting work to Actors as work is requested.
 *
 */
public class WorkAllocationActor extends UntypedActor {
    private static Logger log = LogManager.getLogger(WorkAllocationActor.class);

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_message = (Message<?>)message;
			log.info("Checking status of account key...");
			if(WorkAllowanceStatus.checkStatus(acct_message.getAccountKey())){
				log.info("Approved account : "+acct_message.getAccountKey());
				if(acct_message.getData() instanceof Path ||
						acct_message.getData() instanceof ExploratoryPath ||
						acct_message.getData() instanceof URL){
					log.info("Path passed to work allocator");
					final ActorRef browser_actor = this.getContext().actorOf(Props.create(BrowserActor.class), "BrowserActor"+UUID.randomUUID());
					browser_actor.tell(acct_message, getSelf() );
					getSender().tell("Status: ok", getSelf());
				}
				else if(acct_message.getData() instanceof Test){
					log.info("Test passed to work allocator");
					final ActorRef testing_actor = this.getContext().actorOf(Props.create(TestingActor.class), "TestingActor"+UUID.randomUUID());
					testing_actor.tell(acct_message, getSelf() );
					getSender().tell("Status: ok", getSelf());
				}
			}
			else{
				log.warn("Work allocation actor did not start any work due to account key not having a runnable status");
				getSender().tell("Account not allowed to run discovery", getSelf());
			}
		}
		else{
			getSender().tell("did not recieve Message Object", getSelf());
		}
	}
}

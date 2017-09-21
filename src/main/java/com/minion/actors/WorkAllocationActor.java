package com.minion.actors;

import java.net.URL;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.minion.WorkManagement.WorkAllowanceStatus;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Path;
import com.qanairy.models.Test;
import com.qanairy.models.dto.PathRepository;
import com.qanairy.persistence.OrientConnectionFactory;

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
					boolean record_exists = false;
					Path path = null;
					ExploratoryPath exp_path = null;
					if(acct_message.getData() instanceof Path){
						path = (Path)acct_message.getData();
						PathRepository repo = new PathRepository();
						Path path_record = repo.find(new OrientConnectionFactory(), repo.generateKey(path));
						if(path_record != null){
							record_exists = true;
							path = path_record;
						}
					}
					else if(acct_message.getData() instanceof ExploratoryPath){
						exp_path = (ExploratoryPath)acct_message.getData();
						PathRepository repo = new PathRepository();
						Path path_record = repo.find(new OrientConnectionFactory(), repo.generateKey(exp_path));
						if(path_record != null){
							record_exists = true;
							path = path_record;
						}
					}
					else if(acct_message.getData() instanceof URL){
						//THIS SHOULD STILL BE IMPLEMENTED, LEAVING EMPTY FOR NOW DUE TO NON TRIVIAL NATURE OF THIS PIECE
					}
					
					//if record doesn't exist then send for exploration, else expand the record
					if(!record_exists){
						System.err.println("Data passed to work allocator is of type : "+acct_message.getData().getClass().getSimpleName());
						final ActorRef browser_actor = this.getContext().actorOf(Props.create(BrowserActor.class), "BrowserActor"+UUID.randomUUID());
						browser_actor.tell(acct_message, getSelf() );
						getSender().tell("Status: ok", getSelf());
					}
					else {
						System.err.println("path found in records. Skipping discovery. Expanding path now");
						final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
						path_expansion_actor.tell(acct_message, getSelf() );
					}	
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

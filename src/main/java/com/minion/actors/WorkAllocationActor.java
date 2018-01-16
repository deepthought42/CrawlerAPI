package com.minion.actors;

import java.net.URL;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
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
    private static Logger log = LoggerFactory.getLogger(WorkAllocationActor.class);

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_message = (Message<?>)message;

			if(WorkAllowanceStatus.checkStatus(acct_message.getAccountKey())){
				if(acct_message.getData() instanceof Path ||
						acct_message.getData() instanceof ExploratoryPath ||
						acct_message.getData() instanceof URL){
					System.out.println("Object type for browsers key :: "+acct_message.getOptions().get("browsers").getClass().getSimpleName());
					String browser_name = acct_message.getOptions().get("browser").toString();
					Message<?> msg = acct_message.clone();	
					System.out.println("Browser being put in message :: "+browser_name);
					msg.getOptions().put("browser", browser_name);
					boolean record_exists = false;
					Path path = null;
					ExploratoryPath exp_path = null;
					OrientConnectionFactory connection = new OrientConnectionFactory();

					if(acct_message.getData() instanceof Path){
						path = (Path)acct_message.getData();
						PathRepository repo = new PathRepository();
						Path path_record = repo.find(connection, repo.generateKey(path));
						if(path_record != null){
							record_exists = true;
							path = path_record;
						}
						else{
							path.setKey(repo.generateKey(path));
						}
						
					}
					else if(acct_message.getData() instanceof ExploratoryPath){
						exp_path = (ExploratoryPath)acct_message.getData();
						PathRepository repo = new PathRepository();

						Path path_record = repo.find(connection, repo.generateKey(exp_path));
						if(path_record != null){
							record_exists = true;
							path = path_record;
						}
					}
					else if(acct_message.getData() instanceof URL){
						System.err.println("url needs to be implemented");
						//THIS SHOULD STILL BE IMPLEMENTED, LEAVING EMPTY FOR NOW DUE TO NON TRIVIAL NATURE OF THIS PIECE
					}
					connection.close();

					//if record doesn't exist then send for exploration, else expand the record
					if(!record_exists){
						final ActorRef browser_actor = this.getContext().actorOf(Props.create(BrowserActor.class), "BrowserActor"+UUID.randomUUID());
						browser_actor.tell(msg, getSelf() );
						getSender().tell("Status: ok", getSelf());
					}
					else if(!(acct_message.getData() instanceof ExploratoryPath)) {
						final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
						path_expansion_actor.tell(msg, getSelf() );
					}
				}
				else if(acct_message.getData() instanceof Test){					
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

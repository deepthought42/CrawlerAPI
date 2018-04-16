package com.minion.actors;

import java.net.URL;
import java.util.UUID;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
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
    @SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(WorkAllocationActor.class);

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_message = (Message<?>)message;
			//if(WorkAllowanceStatus.checkStatus(acct_message.getAccountKey())){
				if(acct_message.getData() instanceof Path ||
						acct_message.getData() instanceof ExploratoryPath ||
						acct_message.getData() instanceof URL){
					String browser_name = acct_message.getOptions().get("browser").toString();
					Message<?> msg = acct_message.clone();	
					msg.getOptions().put("browser", browser_name);
					boolean record_exists = false;
					Path path = null;
					OrientConnectionFactory connection = new OrientConnectionFactory();
					
					if(acct_message.getData() instanceof ExploratoryPath){
						System.err.println("Sending path to BrowserActor for exploration");
						final ActorRef exploratory_browser_actor = this.getContext().actorOf(Props.create(ExploratoryBrowserActor.class), "ExploratoryBrowserActor"+UUID.randomUUID());
						exploratory_browser_actor.tell(msg, getSelf() );
					}
					else if(acct_message.getData() instanceof Path){
						System.err.println("Account message received by work allocation actor contains a path");
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
						
						System.err.println("Sending path to BrowserActor for exploration");
						final ActorRef browser_actor = this.getContext().actorOf(Props.create(BrowserActor.class), "BrowserActor"+UUID.randomUUID());
						browser_actor.tell(msg, getSelf() );						
					}
					else if(acct_message.getData() instanceof URL){
						//System.err.println("url needs to be implemented");
						System.err.println("Sending path to BrowserActor for exploration");
						final ActorRef browser_actor = this.getContext().actorOf(Props.create(BrowserActor.class), "BrowserActor"+UUID.randomUUID());
						browser_actor.tell(msg, getSelf() );	
						System.err.println("Sending path to expansion actor");
						final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
						path_expansion_actor.tell(msg, getSelf() );
					}
					
					connection.close();
					
					if(!(acct_message.getData() instanceof ExploratoryPath)) {
						//System.err.println("Sending path to expansion actor");
						//final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
						//path_expansion_actor.tell(msg, getSelf() );
					}
				}
				else if(acct_message.getData() instanceof Test){					
					final ActorRef testing_actor = this.getContext().actorOf(Props.create(TestingActor.class), "TestingActor"+UUID.randomUUID());
					testing_actor.tell(acct_message, getSelf() );
				}
				getSender().tell("Status: ok", getSelf());

			/*}
			else{
				log.warn("Work allocation actor did not start any work due to account key not having a runnable status");
				getSender().tell("Account not allowed to run discovery", getSelf());
			}
			*/
		}
		else{
			getSender().tell("did not recieve Message Object", getSelf());
		}
	}
}

package com.minion.actors;

import static com.qanairy.models.SpringExtension.SpringExtProvider;

import java.net.URL;
import java.util.UUID;

import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Test;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import com.minion.structs.Message;
import static com.qanairy.models.SpringExtension.SpringExtProvider;


/**
 * Responsible for starting new Actors, monitoring
 * {@link Path}s traversed, and allotting work to Actors as work is requested.
 *
 */
@Component
@Scope("prototype")
public class WorkAllocationActor extends AbstractActor  {
	private static Logger log = LoggerFactory.getLogger(WorkAllocationActor.class);

	@Autowired
	ActorSystem actor_system;
	
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Message.class, acct_message -> {
					if(acct_message.getData() instanceof ExploratoryPath ||
							acct_message.getData() instanceof URL){
						String browser_name = acct_message.getOptions().get("browser").toString();
						Message<?> msg = acct_message.clone();	
						msg.getOptions().put("browser", browser_name);
						
						if(acct_message.getData() instanceof ExploratoryPath){
							final ActorRef exploratory_browser_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
									  .props("exploratoryBrowserActor"), "exploratory_browser_actor"+UUID.randomUUID());
							exploratory_browser_actor.tell(msg, getSelf() );
						}
						else if(acct_message.getData() instanceof URL){
							log.info("Sending URL to UrlBrowserActor");
							final ActorRef url_browser_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
									  .props("urlBrowserActor"), "urlBrowserActor"+UUID.randomUUID());
							//final ActorRef url_browser_actor = this.getContext().actorOf(Props.create(UrlBrowserActor.class), "UrlBrowserActor"+UUID.randomUUID());
							url_browser_actor.tell(msg, getSelf() );
						}
					}
					else if(acct_message.getData() instanceof Test){					
						final ActorRef testing_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("testingActor"), "testing_actor"+UUID.randomUUID());
						testing_actor.tell(acct_message, getSelf() );
					}
					getSender().tell("Status: ok", getSelf());
				})
				.matchAny(o -> log.info("received unknown message"))
				.build();
	}
}

package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.net.URL;
import java.util.UUID;

import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Test;
import com.qanairy.models.message.ExplorationPathMessage;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

import com.minion.structs.Message;


/**
 * Responsible for starting new Actors, monitoring
 * {@link Path}s traversed, and allotting work to Actors as work is requested.
 *
 */
@Component
@Scope("prototype")
public class WorkAllocationActor extends AbstractActor  {
	private static Logger log = LoggerFactory.getLogger(WorkAllocationActor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private ActorSystem actor_system;
		
	//subscribe to cluster changes
	@Override
	public void preStart() {
	  cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), 
	      MemberEvent.class, UnreachableMember.class);
	}

	//re-subscribe when restart
	@Override
    public void postStop() {
	  cluster.unsubscribe(getSelf());
    }

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
							ActorRef exploratory_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
									  .props("exploratoryBrowserActor"), "exploratory_browser_actor"+UUID.randomUUID());
							exploratory_actor.tell(msg, getSelf() );
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
					postStop();
				})
				.match(ExplorationPathMessage.class, message -> {
					final ActorRef exploratory_browser_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
							  .props("exploratoryBrowserActor"), "exploratory_browser_actor"+UUID.randomUUID());
					exploratory_browser_actor.tell(message, getSelf() );
				})
				.match(MemberUp.class, mUp -> {
					log.info("Member is Up: {}", mUp.member());
				})
				.match(UnreachableMember.class, mUnreachable -> {
					log.info("Member detected as unreachable: {}", mUnreachable.member());
				})
				.match(MemberRemoved.class, mRemoved -> {
					log.info("Member is Removed: {}", mRemoved.member());
				})	
				.matchAny(o -> {
					log.info("received unknown message");
				})
				.build();
	}
}

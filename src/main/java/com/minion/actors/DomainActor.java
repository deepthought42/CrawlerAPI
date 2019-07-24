package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.api.MessageBroadcaster;
import com.qanairy.models.Domain;
import com.qanairy.models.Test;
import com.qanairy.models.enums.DomainAction;
import com.qanairy.models.message.DiscoveryActionMessage;
import com.qanairy.models.message.DomainActionMessage;
import com.qanairy.services.DomainService;
import com.qanairy.services.TestService;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

@Component
@Scope("prototype")
public class DomainActor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(ExploratoryBrowserActor.class.getName());
	private Cluster cluster = Cluster.get(getContext().getSystem());
	private String host = null;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private ActorSystem actor_system;
	
	private ActorRef discovery_actor = null;
	
	//subscribe to cluster changes
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
				MemberEvent.class, UnreachableMember.class);
		discovery_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
				  .props("discoveryActor"), "discovery_actor"+UUID.randomUUID());
	}

	//re-subscribe when restart
	@Override
    public void postStop() {
	  cluster.unsubscribe(getSelf());
    }

	/**
	 * {@inheritDoc}
	 *
	 * NOTE: Do not change the order of the checks for instance of below. These are in this order because ExploratoryPath
	 * 		 is also a Test and thus if the order is reversed, then the ExploratoryPath code never runs when it should
	 * @throws NullPointerException
	 * @throws IOException
	 * @throws NoSuchElementException
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(DomainActionMessage.class, message-> {
					host = message.getDomain().getHost();
					
					if(message.getAction().equals(DomainAction.CREATE)){
						
					}
					else if(message.getAction().equals(DomainAction.DELETE)){
						
					}
				})
				.match(DiscoveryActionMessage.class, message-> {
					//pass message along to discovery actor
					discovery_actor.tell(message, getSelf());
				})
				.match(Test.class, test -> {
					Test test_record = test_service.save(test, host);
					Domain domain = domain_service.findByHost(host);
					domain.addTest(test_record);
					domain_service.save(domain);
					
					MessageBroadcaster.broadcastDiscoveredTest(test, host);
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
					log.info("received unknown message of type :: "+o.getClass().getName());
				})
				.build();
	}
}

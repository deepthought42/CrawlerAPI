package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.looksee.api.MessageBroadcaster;
import com.looksee.models.Domain;
import com.looksee.models.LookseeObject;
import com.looksee.models.Test;
import com.looksee.models.enums.DiscoveryAction;
import com.looksee.models.message.DiscoveryActionMessage;
import com.looksee.models.message.DiscoveryActionRequest;
import com.looksee.models.message.FormDiscoveryMessage;
import com.looksee.models.message.TestMessage;
import com.looksee.services.DomainService;
import com.looksee.services.TestService;

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
	private static Logger log = LoggerFactory.getLogger(DomainActor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());
	private Domain domain = null;
	private DiscoveryAction discovery_action;

	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private TestService test_service;

	@Autowired
	private ActorSystem actor_system;
	
	private ActorRef discovery_actor;
	
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
				.match(DiscoveryActionMessage.class, message-> {
					domain = message.getDomain();
					if(discovery_actor == null){
						discovery_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("discoveryActor"), "discovery_actor"+UUID.randomUUID());
					}
					discovery_action = message.getAction();
					
					log.warn("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
					log.warn("RUNNING DOMAIN ACTOR WITH HOST :: " + domain.getUrl() + " WITH ACTION   :: " + message.getAction());
					log.warn("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
					
					//pass message along to discovery actor
					discovery_actor.tell(message, getSelf());
				})
				.match(DiscoveryActionRequest.class, message-> {
					if(discovery_action == null) {

					}

					getSender().tell(discovery_action, getSelf());
				})
				.match(FormDiscoveryMessage.class, form_msg -> {
					//forward message to discovery actor
					log.warn("form message :: "+form_msg);
					form_msg.setDomainActor(getSelf());
					log.warn("discovery_actor :: " + discovery_actor);
					if(discovery_actor == null){
						discovery_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("discoveryActor"), "discovery_actor"+UUID.randomUUID());
					}
					discovery_actor.tell(form_msg, getSelf());
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

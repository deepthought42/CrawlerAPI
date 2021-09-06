package com.looksee.actors;


import java.io.IOException;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.message.ElementExtractionMessage;
import com.looksee.models.message.ElementProgressMessage;
import com.looksee.services.BrowserService;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

@Component
@Scope("prototype")
public class ElementStateExtractor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(ElementStateExtractor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private BrowserService browser_service;
	
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
				.match(ElementExtractionMessage.class, message-> {
					log.warn("Extracting element states from page");

					browser_service.buildPageElements(message.getPageState(), 
			                		  							message.getXpaths(),
			                		  							message.getAuditRecordId());
					log.warn("completed element state extraction for "+message.getXpaths().size() + "  xpaths");
					ElementProgressMessage element_message = new ElementProgressMessage(message.getAuditRecordId(), 
																						message.getPageState().getId(), 
																						message.getXpaths());
					getSender().tell(element_message, getSelf());
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

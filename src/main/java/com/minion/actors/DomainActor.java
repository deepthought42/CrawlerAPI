package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.minion.api.MessageBroadcaster;
import com.qanairy.models.Domain;
import com.qanairy.models.Form;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.message.DiscoveryActionMessage;
import com.qanairy.services.DomainService;
import com.qanairy.services.PageStateService;
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
	private static Logger log = LoggerFactory.getLogger(DomainActor.class.getName());
	private Cluster cluster = Cluster.get(getContext().getSystem());
	private String host = null;
	Map<String, PageState> page_state_map = new HashMap<>();
	
	@Autowired
	private PageStateService page_state_service;
	
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
				.match(DiscoveryActionMessage.class, message-> {
					host = message.getDomain().getUrl();
					log.warn("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
					log.warn("RUNNING DOMAIN ACTOR WITH HOST :: " + host + " WITH ACTION   :: " + message.getAction());
					log.warn("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
					
					//pass message along to discovery actor
					discovery_actor.tell(message, getSelf());
				})
				.match(Test.class, test -> {
					Test test_record = test_service.save(test);
					
					Domain domain = domain_service.findByHost(host);
					for(PathObject obj : test.getPathObjects()){
						if(obj.getKey().contains("pagestate")){
							domain.addPageState((PageState)obj);
						}
					}
					
					domain.addTest(test_record);
					domain.addPageState(test.getResult());
					domain_service.save(domain);
					
					for(PathObject path_obj : test.getPathObjects()){
						try {
							MessageBroadcaster.broadcastPathObject(path_obj, domain.getUrl());
						} catch (JsonProcessingException e) {
							log.error(e.getLocalizedMessage());
						}
					}
					
					try {
						log.warn("TEST BEING BROADCASTED :: " + test);
						log.warn("host url for broadcast :: " + domain.getUrl());
						MessageBroadcaster.broadcastDiscoveredTest(test, domain.getUrl());
					} catch (JsonProcessingException e) {
						log.error(e.getLocalizedMessage());
					}
				})
				.match(PageState.class, page_state -> {
					page_state_service.save(page_state);
					Domain domain = domain_service.findByHost(host);
					domain.addPageState(page_state);
					domain_service.save(domain);
					
					page_state_map.put(page_state.getKey(), page_state);
					for(Form form : page_state.getForms()){
						MessageBroadcaster.broadcastDiscoveredForm(form, host);
					}
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

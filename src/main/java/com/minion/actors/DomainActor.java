package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

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
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.enums.DiscoveryAction;
import com.qanairy.models.enums.DiscoveryStatus;
import com.qanairy.models.message.DiscoveryActionMessage;
import com.qanairy.models.message.DiscoveryActionRequest;
import com.qanairy.models.message.FormDiscoveryMessage;
import com.qanairy.models.message.FormMessage;
import com.qanairy.models.message.TestMessage;
import com.qanairy.services.AccountService;
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
	private static Logger log = LoggerFactory.getLogger(DomainActor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());
	private Domain domain = null;
	private DiscoveryAction discovery_action;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private AccountService account_service;
	
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
						DiscoveryStatus status = domain_service.getMostRecentDiscoveryRecord(message.getDomain().getUrl()).getStatus();
						if(status == DiscoveryStatus.RUNNING) {
							discovery_action = DiscoveryAction.START;
						}
						else {
							discovery_action = DiscoveryAction.STOP;
						}
					}

					getSender().tell(discovery_action, getSelf());
				})
				.match(TestMessage.class, test_msg -> {
					Test test = test_msg.getTest();
					
					Test test_record = test_service.findByKey(test.getKey());
					if(test_record == null) {
						test_record = test_service.save(test);
						account_service.addTest(test_record, test_msg.getAccount());
					}
					
					if(domain == null){
						String url = test_msg.getDomain().getUrl();
						domain = domain_service.findByUrl(url);
					}
					
					for(PathObject obj : test.getPathObjects()){
						if(obj.getKey().contains("pagestate")){
							domain.addPageState((PageState)obj);
						}
					}
					
					domain = domain_service.save(domain);					
					domain_service.addPageState(domain.getUrl(), test.getResult());	

					for(PathObject path_obj : test.getPathObjects()){
						try {
							MessageBroadcaster.broadcastPathObject(path_obj, domain.getHost());
						} catch (JsonProcessingException e) {
							log.error(e.getLocalizedMessage());
						}
					}
          
					try {
						MessageBroadcaster.broadcastDiscoveredTest(test, domain.getHost());
					} catch (JsonProcessingException e) {
						log.error(e.getLocalizedMessage());
					}
					domain_service.save(domain);
					domain_service.addTest(domain.getUrl(), test_record);
					domain_service.addPageState(domain.getUrl(), test.getResult());
					
					for(PathObject path_obj : test.getPathObjects()){
						try {
							MessageBroadcaster.broadcastPathObject(path_obj, domain.getHost());
						} catch (JsonProcessingException e) {
							log.error(e.getLocalizedMessage());
						}
					}					
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
				.match(PageState.class, page_state -> {
					page_state_service.save(page_state);
				})
				.match(FormMessage.class, form_msg -> {
					
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

package com.minion.actors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.Props;
import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.minion.api.MessageBroadcaster;
import com.minion.structs.Message;
import com.qanairy.models.Domain;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.Test;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.services.PageElementService;
import com.qanairy.services.PageStateService;
import com.qanairy.services.TestService;

/**
 * Handles the saving of records into orientDB
 *
 *
 */
@Component
@Scope("prototype")
public class MemoryRegistryActor extends AbstractActor{
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), MemoryRegistryActor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private DomainRepository domain_repo;
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private PageStateService page_service;
	
	@Autowired
	private PageElementService element_service;
	
	public static Props props() {
	  return Props.create(MemoryRegistryActor.class);
	}
	
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
				.match(Message.class, msg -> {
					log.info("Memory Registry    ::    MESSAGE DATA TYPE :: " + msg.getData().getClass().getName());
					if(msg.getData() instanceof PageState){
						log.info("Saving page state : " + ((PageState)msg.getData()).getKey());
						PageState page = (PageState)msg.getData();
						page_service.save(page);
					}
					else if(msg.getData() instanceof Test){
						log.info("Saving test (memory registry)  :  " + ((Test)msg.getData()).getKey());
						log.info("Test message received by memory registry actor");
						Test test = (Test)msg.getData();
						
						String host_url = msg.getOptions().get("host").toString();
						Test record = test_service.findByKey(test.getKey());
						
						if(record == null){
							log.info("Test REPO :: "+test_service);
							log.info("Test ::  "+test);
							test.setName("Test #" + (domain_repo.getTestCount(host_url)+1));
							Domain domain = domain_repo.findByHost(host_url);
							domain.addTest(test);
							domain_repo.save(domain);
							
							MessageBroadcaster.broadcastDiscoveredTest(test, host_url);
						}
						else{
							test = record;
							MessageBroadcaster.broadcastTest(test, host_url);
						}
					}
					else if(msg.getData() instanceof PageElement){
						PageElement elem = (PageElement)msg.getData();
						if(element_service.findByKey(elem.getKey()) == null){
							element_service.save(elem);
						}
					}
					postStop();

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
				.matchAny(o -> log.info("MemoryRegistry received unknown message of type : "+o.getClass().getName() + ";  toString : "+o.toString()))
				.build();	
		}
}

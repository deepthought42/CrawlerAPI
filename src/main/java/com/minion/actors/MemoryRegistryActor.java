package com.minion.actors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.Props;
import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
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
import com.qanairy.models.repository.PageElementRepository;
import com.qanairy.models.repository.PageStateRepository;
import com.qanairy.models.repository.TestRepository;
import com.qanairy.services.BrowserService;

/**
 * Handles the saving of records into orientDB
 *
 *
 */
@Component
@Scope("prototype")
public class MemoryRegistryActor extends AbstractActor{
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private DomainRepository domain_repo;
	
	@Autowired
	private TestRepository test_repo;
	
	@Autowired
	private PageStateRepository page_repo;
	
	@Autowired
	private PageElementRepository element_repo;
	
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
					if(msg.getData() instanceof PageState){
						PageState page = (PageState)msg.getData();
						if(page_repo.findByKey(page.getKey()) == null){
							page_repo.save(page);
						}
					}
					else if(msg.getData() instanceof Test){
						log.info("Test message received by memory registry actor");
						Test test = (Test)msg.getData();
						
						String host_url = msg.getOptions().get("host").toString();
						Test record = test_repo.findByKey(test.getKey());
						
						if(record == null){
							log.info("Test REPO :: "+test_repo);
							log.info("Test ::  "+test);
							test.setName("Test #" + (domain_repo.getTestCount(host_url)+1));
							Domain domain = domain_repo.findByHost(host_url);
							domain.addTest(test);
							domain_repo.save(domain);
							
							if(test.getBrowserStatuses() == null || test.getBrowserStatuses().isEmpty()){
								MessageBroadcaster.broadcastDiscoveredTest(test, host_url);
							}
							else {
								MessageBroadcaster.broadcastTest(test, host_url);
							}
						}
						else{
							test = record;
						}
					}
					else if(msg.getData() instanceof PageElement){
						PageElement elem = (PageElement)msg.getData();
						if(element_repo.findByKey(elem.getKey()) == null){
							element_repo.save(elem);
						}
					}
				})
				.matchAny(o -> log.info("received unknown message"))
				.build();	
		}
}

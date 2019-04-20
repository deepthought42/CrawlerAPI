package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

import com.minion.api.MessageBroadcaster;
import com.minion.structs.Message;
import com.qanairy.models.Test;
import com.qanairy.models.message.PageStateMessage;
import com.qanairy.models.repository.DiscoveryRecordRepository;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.PageState;
import com.qanairy.services.BrowserService;
import com.qanairy.services.PageService;
import com.qanairy.services.PageStateService;
import com.qanairy.services.TestCreatorService;
import com.qanairy.services.TestService;

/**
 * Manages a browser instance and sets a crawler upon the instance using a given path to traverse 
 *
 */
@Component
@Scope("prototype")
public class UrlBrowserActor extends AbstractActor {
	private static Logger log = LoggerFactory.getLogger(UrlBrowserActor.class.getName());
	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	DiscoveryRecordRepository discovery_repo;
	
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private TestCreatorService test_creator_service;
	
	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired 
	private PageService page_service;
	
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
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Message.class, message -> {
					if(message.getData() instanceof URL){
						
						String discovery_key = message.getOptions().get("discovery_key").toString();
						
						String url = ((URL)message.getData()).toString();
						String host = ((URL)message.getData()).getHost();
						String browser_name = message.getOptions().get("browser").toString();
						
						List<PageState> page_states = browser_service.buildPageStates(url, browser_name);

						//Page page = new Page(url);
						//page.getPageStates().addAll(page_states);
						//page_service.save(page);
						
						Test test = test_creator_service.createLandingPageTest(page_states.get(0), browser_name);
						test = test_service.save(test, host);
						
						Message<Test> test_msg = new Message<Test>(message.getAccountKey(), test, message.getOptions());
						
						/**  path expansion temporarily disabled
						 */
						
						final ActorRef path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
						path_expansion_actor.tell(test_msg, getSelf() );
						MessageBroadcaster.broadcastDiscoveredTest(test, host);

						final ActorRef form_discoverer = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("formDiscoveryActor"), "form_discovery"+UUID.randomUUID());
						
						DiscoveryRecord discovery_record = discovery_repo.findByKey( discovery_key);
						for(PageState page_state : page_states.subList(1, page_states.size())){
							if(!discovery_record.getExpandedPageStates().contains(page_state.getKey())){
								log.warn("discovery path does not have expanded page state");
								PageStateMessage page_state_msg = new PageStateMessage(message.getAccountKey(), page_state, discovery_record, message.getOptions());

								form_discoverer.tell(page_state_msg, getSelf() );
									
								//path_expansion_actor.tell(page_state_msg, getSelf() );
							}
						}
						
						discovery_record.setLastPathRanAt(new Date());
						discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
						
						discovery_record.setTestCount(discovery_record.getTestCount()+1);
						discovery_record = discovery_repo.save(discovery_record);
						MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
				   }
					//log.warn("Total Test execution time (browser open, crawl, build test, save data) : " + browserActorRunTime);
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
				.matchAny(o -> {
					log.info("received unknown message");
				})
				.build();
	}
}
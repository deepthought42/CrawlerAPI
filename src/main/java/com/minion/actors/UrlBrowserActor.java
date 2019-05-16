package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.net.URL;
import java.util.ArrayList;
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
import com.minion.browsing.Browser;
import com.minion.browsing.BrowserConnectionFactory;
import com.minion.structs.Message;
import com.qanairy.models.Test;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.message.PageStateMessage;
import com.qanairy.models.repository.DiscoveryRecordRepository;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Redirect;
import com.qanairy.services.BrowserService;
import com.qanairy.services.TestCreatorService;
import com.qanairy.services.TestService;
import com.qanairy.utils.BrowserUtils;

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
	
	private ActorRef path_expansion_actor;
	
	//subscribe to cluster changes
	@Override
	public void preStart() {
	  cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), 
	      MemberEvent.class, UnreachableMember.class);
	  path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
			  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
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
						log.warn("starting redirect detection");
						Redirect redirect = null;
						do{
							Browser browser = null;
							try{
								browser = BrowserConnectionFactory.getConnection(browser_name, BrowserEnvironment.DISCOVERY);
								browser.navigateTo(url);
								redirect = BrowserUtils.getPageTransition(url, browser, host);
								browser.waitForPageToLoad();
							}
							catch(Exception e){
								e.printStackTrace();
							}
							finally {
								if(browser != null){
									browser.close();
								}
							}
						}while(redirect == null);
						log.warn("redirect detection complete");
						List<PageState> page_states = browser_service.buildPageStates(url, browser_name, host);

						log.warn("Done building page states ");
						//Page page = new Page(url);
						//page.getPageStates().addAll(page_states);
						//page_service.save(page);
						Test test = test_creator_service.createLandingPageTest(page_states.get(0), browser_name);
						log.warn("finished creating landing page test");
						if(redirect.getUrls().size() > 0){
							
							List<String> path_keys = new ArrayList<>();
							path_keys.add(redirect.getKey());
							path_keys.addAll(test.getPathKeys());
							
							List<PathObject> path_objects = new ArrayList<PathObject>();
							path_objects.add(redirect);
							path_objects.addAll(test.getPathObjects());
							
							test.setPathKeys(path_keys);
							test.setPathObjects(path_objects);
						}
						test = test_service.save(test, host);
						log.warn("path keys :: " + test.getPathKeys());
						log.warn("path objects :: " + test.getPathObjects());
						
						Message<Test> test_msg = new Message<Test>(message.getAccountKey(), test, message.getOptions());
						
						/**  path expansion temporarily disabled
						 */
						
						path_expansion_actor.tell(test_msg, getSelf() );
						MessageBroadcaster.broadcastDiscoveredTest(test, host);
						
						DiscoveryRecord discovery_record = discovery_repo.findByKey( discovery_key);
						PageStateMessage page_state_msg = new PageStateMessage(message.getAccountKey(), page_states.get(0), discovery_record, message.getOptions());

						final ActorRef form_discoverer = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("formDiscoveryActor"), "form_discovery"+UUID.randomUUID());
						form_discoverer.tell(page_state_msg, getSelf() );
						
						for(PageState page_state : page_states.subList(1, page_states.size())){
							if(!discovery_record.getExpandedPageStates().contains(page_state.getKey())){
								log.warn("discovery path does not have expanded page state");
								page_state_msg = new PageStateMessage(message.getAccountKey(), page_state, discovery_record, message.getOptions());

								form_discoverer.tell(page_state_msg, getSelf() );
									
								path_expansion_actor.tell(page_state_msg, getSelf() );
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
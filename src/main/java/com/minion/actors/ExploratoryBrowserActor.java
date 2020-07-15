package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.browsing.Crawler;
import com.qanairy.api.exceptions.DiscoveryStoppedException;
import com.qanairy.models.ElementState;
import com.qanairy.models.Page;
import com.qanairy.models.PageState;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.enums.PathStatus;
import com.qanairy.models.message.PathMessage;
import com.qanairy.models.message.TestCandidateMessage;
import com.qanairy.services.BrowserService;
import com.qanairy.services.DomainService;
import com.qanairy.services.PageService;
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

/**
 *
 */
@Component
@Scope("prototype")
public class ExploratoryBrowserActor extends AbstractActor {
	private static Logger log = LoggerFactory.getLogger(ExploratoryBrowserActor.class.getName());
	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private ActorSystem actor_system;

	@Autowired
	private TestService test_service;

	@Autowired
	private BrowserService browser_service;

	@Autowired
	private PageService page_service;

	@Autowired
	private DomainService domain_service;

	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private Crawler crawler;

	private ActorRef parent_path_explorer = null;
	
	//subscribe to cluster changes
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
				MemberEvent.class, UnreachableMember.class);
		parent_path_explorer = actor_system.actorOf(SpringExtProvider.get(actor_system)
				.props("parentPathExplorer"), "parent_path_explorer"+UUID.randomUUID());
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
				.match(PathMessage.class, message-> {
					String browser_name = message.getDomain().getDiscoveryBrowserName();					
					
					if(message.getPathObjects() != null){
						PageState result_page = null;

						try {
							result_page = crawler.performPathExploratoryCrawl(message.getAccountId(), message.getDomain(), browser_name, message);
						} catch(DiscoveryStoppedException e) {
							return;
						}
						//get page states
						List<PageState> page_states = new ArrayList<PageState>();
						for(LookseeObject path_obj : message.getPathObjects()){
							if(path_obj instanceof PageState){
								PageState page_state = (PageState)path_obj;
								page_states.add(page_state);
							}
						}
						
						boolean isResultAnimatedState = isResultAnimatedState( page_states, result_page);
						//boolean is_duplicate_path = test_service.checkIfEndOfPathAlreadyExistsInAnotherTest(message.getKeys(), path_object_lists);
						boolean is_result_matches_other_page_in_path = test_service.checkIfEndOfPathAlreadyExistsInPath(result_page, message.getKeys());
						log.warn("does result match path object in path   ??   "+is_result_matches_other_page_in_path);
						if(is_result_matches_other_page_in_path || isResultAnimatedState) {
							PathMessage path = new PathMessage(message.getKeys(), message.getPathObjects(), message.getDiscoveryActor(), PathStatus.EXAMINED, message.getBrowser(), message.getDomainActor(), message.getDomain(), message.getAccountId());
					  		//send path message with examined status to discovery actor
							message.getDiscoveryActor().tell(path, getSelf());
							return;
						}
						else {
							Page page = browser_service.buildPage(message.getAccountId(), result_page.getUrl());
							page = page_service.saveForUser(message.getAccountId(), page);
							domain_service.addPage(message.getDomain().getUrl(), page, message.getAccountId());

							long start_time = System.currentTimeMillis();
							List<ElementState> elements = browser_service.extractElementStates(message, BrowserType.create(browser_name));
							long end_time = System.currentTimeMillis();
							log.warn("element state time to get all elements ::  "+(end_time-start_time));
							result_page.addElements(elements);
							result_page = page_state_service.saveUserAndDomain(message.getAccountId(), message.getDomain().getUrl(), result_page);
							page_service.addPageState(message.getAccountId(), page.getKey(), result_page);
							
							log.warn("DOM elements found :: "+elements.size());
							
							TestCandidateMessage msg = new TestCandidateMessage(message.getKeys(), message.getPathObjects(), message.getDiscoveryActor(), result_page, message.getBrowser(), message.getDomainActor(), message.getDomain(), message.getAccountId());
							parent_path_explorer.tell(msg, getSelf());
						}
					}

					//PLACE CALL TO LEARNING SYSTEM HERE
					//Brain.learn(test, test.getIsUseful());

					//log.warn("Total Test execution time (browser open, crawl, build test, save data) : " + browserActorRunTime);

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

	/**
	 * Checks if given {@PageState result} page has a image url that matches any animated image urls in the 
	 * 	list of PageStates provided
	 * 
	 * @param page_states
	 * @param result_page
	 * 
	 * @return
	 * 
	 * @pre page_states != null
	 * @pre !page_states	.isEmpty()
	 * @pre result_page != null
	 */
	private boolean isResultAnimatedState(List<PageState> page_states, PageState result_page) {
		assert page_states != null;
		assert !page_states.isEmpty();
		assert result_page != null;
		
		for(PageState page_state : page_states){
			if(!page_state.getAnimatedImageUrls().isEmpty()){
				for(String image_url : page_state.getAnimatedImageUrls()){
					//if result screenshot image url is the same as a previous animated state then return true
					if(result_page.getScreenshotUrl().equals(image_url)){
						return true;
					}
				}
			}
		}

		return false;
	}
}

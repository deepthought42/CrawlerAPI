package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.util.ArrayList;
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

import com.minion.browsing.Browser;
import com.minion.browsing.BrowserConnectionFactory;
import com.qanairy.models.Test;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.enums.PathStatus;
import com.qanairy.models.message.PathMessage;
import com.qanairy.models.message.UrlMessage;
import com.qanairy.models.repository.DiscoveryRecordRepository;
import com.qanairy.models.PageLoadAnimation;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Redirect;
import com.qanairy.services.BrowserService;
import com.qanairy.services.TestCreatorService;
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
				.match(UrlMessage.class, message -> {
					//String discovery_key = message.getOptions().get("discovery_key").toString();
					String url = message.getUrl().toString();
					
					String host = message.getUrl().getHost();
					String browser_name = message.getBrowser().toString();
					log.warn("starting transition detection");
					Redirect redirect = null;
					PageLoadAnimation animation = null;

					List<String> path_keys = null;
					List<PathObject> path_objects = null;
					
					do{
						path_keys = new ArrayList<>();
						path_objects = new ArrayList<>();
						Browser browser = null;
						
						try{
							browser = BrowserConnectionFactory.getConnection(browser_name, BrowserEnvironment.DISCOVERY);
							log.warn("navigating to url :: "+url);
							browser.navigateTo(url);

							redirect = BrowserUtils.getPageTransition(url, browser, host);
						  	if(redirect != null && ((redirect.getUrls().size() > 1 && BrowserUtils.doesHostChange(redirect.getUrls())) || (redirect.getUrls().size() > 2 && !BrowserUtils.doesHostChange(redirect.getUrls())))){
								path_keys.add(redirect.getKey());
								path_objects.add(redirect);
							}

						  	animation = BrowserUtils.getLoadingAnimation(browser, host);
							if(animation != null){
								path_keys.add(animation.getKey());
								path_objects.add(animation);
							}
							break;
						}
						catch(Exception e){
							e.printStackTrace();
						}
						finally {
							if(browser != null){
								browser.close();
							}
						}
						log.warn("Transition :: " + redirect);
						log.warn("Animation returned   :: " + animation);
					}while(redirect == null);
					
					log.warn("loading animation detection complete");
					List<PageState> page_states = browser_service.buildPageStates(url, browser_name, host, path_objects, path_keys);

					log.warn("Done building page states ");
					//send test to discovery actor
					
					Test test = test_creator_service.createLandingPageTest(page_states.get(0), browser_name, redirect, animation);
					message.getDiscoveryActor().tell(test, getSelf());

					log.warn("finished creating landing page test");

					final ActorRef animation_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
							  .props("animationDetectionActor"), "animation_detection"+UUID.randomUUID());

					for(PageState page_state : page_states){
						log.warn("discovery path does not have expanded page state");
						System.err.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
						System.err.println("page state  ::   " + page_state);
						System.err.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");

						List<String> new_path_keys = new ArrayList<String>(path_keys);
					  	List<PathObject> new_path_objects = new ArrayList<PathObject>(path_objects);
					  	
					  	new_path_keys.add(page_state.getKey());
					  	new_path_objects.add(page_state);

						PathMessage path_message = new PathMessage(new ArrayList<>(new_path_keys), new ArrayList<>(new_path_objects), message.getDiscoveryActor(), PathStatus.READY, BrowserType.create(browser_name));
						
						//send message to animation detection actor
						animation_actor.tell(path_message, getSelf() );
					}

					
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
					log.info("received unknown message");
				})
				.build();
	}
}

package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.openqa.selenium.Point;
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
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

import com.minion.browsing.Browser;
import com.qanairy.models.Test;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.enums.DiscoveryAction;
import com.qanairy.models.enums.PathStatus;
import com.qanairy.models.message.DiscoveryActionRequest;
import com.qanairy.models.message.PathMessage;
import com.qanairy.models.message.TestMessage;
import com.qanairy.models.message.UrlMessage;
import com.qanairy.helpers.BrowserConnectionHelper;
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
					Timeout timeout = Timeout.create(Duration.ofSeconds(120));
					Future<Object> future = Patterns.ask(message.getDomainActor(), new DiscoveryActionRequest(message.getDomain(), message.getAccount()), timeout);
					DiscoveryAction discovery_action = (DiscoveryAction) Await.result(future, timeout.duration());
					
					if(discovery_action == DiscoveryAction.STOP) {
						log.warn("path message discovery actor returning");
						return;
					}
					
					String url = message.getUrl().toString();
					String host = message.getUrl().getHost();
					String browser_name = message.getDomain().getDiscoveryBrowserName();
					Redirect redirect = null;
					PageLoadAnimation animation = null;
					BrowserType browser_type = BrowserType.create(browser_name);
					List<String> path_keys = null;
					List<PathObject> path_objects = null;
					PageState page_state = null;
					
					do{
						path_keys = new ArrayList<>();
						path_objects = new ArrayList<>();
						Browser browser = null;
						
						try{
							browser = BrowserConnectionHelper.getConnection(browser_type, BrowserEnvironment.DISCOVERY);
							log.warn("navigating to url :: "+url);
							browser.navigateTo(url);
							redirect = BrowserUtils.getPageTransition(url, browser, host);
							log.warn("redirect detected as :: " + redirect.getKey());
							log.warn("redirect urls :: "+redirect.getUrls().size());
							log.warn("redirect start url     ::  "+redirect.getStartUrl());
						  	
						  	if(redirect != null && ((redirect.getUrls().size() > 0 && BrowserUtils.doesHostChange(redirect.getUrls())) || (redirect.getUrls().size() > 2 && !BrowserUtils.doesHostChange(redirect.getUrls())))){
								log.warn("redirect added to path objects list");
						  		path_keys.add(redirect.getKey());
								path_objects.add(redirect);
							}

						  	animation = BrowserUtils.getLoadingAnimation(browser, host);
							if(animation != null){
								path_keys.add(animation.getKey());
								path_objects.add(animation);
							}
							browser.moveMouseToNonInteractive(new Point(300, 300));
							
							//log.warn("parent only list size :: " + all_elements_list.size());
							log.warn("building page...");
							page_state = browser_service.buildPageState(message.getAccount(), message.getDomain(), browser);
							log.warn("page state elements :: " + page_state.getElements().size());
							break;
						}
						catch(Exception e){
							log.warn("URL BROWSER ACTOR EXCEPTION :: "+e.getMessage());
							e.printStackTrace();
						}
						finally {
							if(browser != null){
								browser.close();
							}
						}
					}while(page_state == null);
					
					path_keys.add(page_state.getKey());
					path_objects.add(page_state);

					log.warn("creating landing page test");
					Test test = test_creator_service.createLandingPageTest(path_keys, path_objects, page_state, browser_name, message.getDomain(), message.getAccount());
					TestMessage test_message = new TestMessage(test, message.getDiscoveryActor(), message.getBrowser(), message.getDomainActor(), message.getDomain(), message.getAccount());
					message.getDiscoveryActor().tell(test_message, getSelf());
					
					final ActorRef animation_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
							  .props("animationDetectionActor"), "animation_detection"+UUID.randomUUID());

					PathMessage path_message = new PathMessage(new ArrayList<>(path_keys), new ArrayList<>(path_objects), message.getDiscoveryActor(), PathStatus.READY, BrowserType.create(browser_name), message.getDomainActor(), message.getDomain(), message.getAccount());
					
					//send message to animation detection actor
					animation_actor.tell(path_message, getSelf() );
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

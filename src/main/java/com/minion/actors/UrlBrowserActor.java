package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.minion.browsing.BrowserConnectionFactory;
import com.qanairy.models.Test;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.enums.DiscoveryAction;
import com.qanairy.models.enums.PathStatus;
import com.qanairy.models.message.DiscoveryActionRequest;
import com.qanairy.models.message.PathMessage;
import com.qanairy.models.message.TestMessage;
import com.qanairy.models.message.UrlMessage;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageLoadAnimation;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Redirect;
import com.qanairy.models.Template;
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
					Future<Object> future = Patterns.ask(message.getDomainActor(), new DiscoveryActionRequest(message.getDomain()), timeout);
					DiscoveryAction discovery_action = (DiscoveryAction) Await.result(future, timeout.duration());
					
					if(discovery_action == DiscoveryAction.STOP) {
						log.warn("path message discovery actor returning");
						return;
					}
					
					//String discovery_key = message.getOptions().get("discovery_key").toString();
					String url = message.getUrl().toString();
					
					String host = message.getUrl().getHost();
					String browser_name = message.getDomain().getDiscoveryBrowserName();
					log.warn("starting transition detection");
					Redirect redirect = null;
					PageLoadAnimation animation = null;
					BrowserType browser_type = BrowserType.create(browser_name);
					List<String> path_keys = null;
					List<PathObject> path_objects = null;
					Map<String, Template> template_elements = new HashMap<>();
					PageState page_state = null;
					
					do{
						path_keys = new ArrayList<>();
						path_objects = new ArrayList<>();
						Browser browser = null;
						
						try{
							browser = BrowserConnectionFactory.getConnection(browser_type, BrowserEnvironment.DISCOVERY);
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
							browser.moveMouseToNonInteractive(new Point(300, 300));

							String source = browser.getDriver().getPageSource();
							
							List<ElementState> all_elements_list = BrowserService.getAllElementsUsingJSoup(source);
							template_elements = browser_service.findTemplates(all_elements_list);
							template_elements = browser_service.reduceTemplatesToParents(template_elements);
							template_elements = browser_service.reduceTemplateElementsToUnique(template_elements);

							page_state = browser_service.buildPage(browser);

							page_state.setTemplates(new ArrayList<>(template_elements.values()));
							break;
						}
						catch(Exception e){
							log.warn(e.getMessage());
						}
						finally {
							if(browser != null){
								browser.close();
							}
						}
					}while(page_state == null);
					
					/*
					log.warn("loading animation detection complete");
					List<PageState> page_states = browser_service.buildPageStates(url, browser_type, host, path_objects, path_keys);
					log.warn("Done building page states ");
					//send test to discovery actor
					*/
					Test test = test_creator_service.createLandingPageTest(page_state, browser_name, redirect, animation, message.getDomain());
					TestMessage test_message = new TestMessage(test, message.getDiscoveryActor(), message.getBrowser(), message.getDomainActor(), message.getDomain());
					message.getDiscoveryActor().tell(test_message, getSelf());
					
					final ActorRef animation_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
							  .props("animationDetectionActor"), "animation_detection"+UUID.randomUUID());

					List<String> new_path_keys = new ArrayList<String>(path_keys);
				  	List<PathObject> new_path_objects = new ArrayList<PathObject>(path_objects);
				  	new_path_keys.add(page_state.getKey());
				  	new_path_objects.add(page_state);

					PathMessage path_message = new PathMessage(new ArrayList<>(new_path_keys), new ArrayList<>(new_path_objects), message.getDiscoveryActor(), PathStatus.READY, BrowserType.create(browser_name), message.getDomainActor(), message.getDomain());
					
					//send message to animation detection actor
					animation_actor.tell(path_message, getSelf() );
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

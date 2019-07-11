package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.Props;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.minion.browsing.Browser;
import com.minion.browsing.BrowserConnectionFactory;
import com.minion.browsing.Crawler;
import com.qanairy.models.Animation;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.message.PathMessage;
import com.qanairy.services.PageStateService;
import com.qanairy.utils.BrowserUtils;
import com.qanairy.utils.PathUtils;

/**
 * Handles the saving of records into orientDB
 *
 *
 */
@Component
@Scope("prototype")
public class AnimationDetectionActor extends AbstractActor{
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), AnimationDetectionActor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private PageStateService page_state_service;

	@Autowired
	private Crawler crawler;

	@Autowired
	private ActorSystem actor_system;

	public static Props props() {
	  return Props.create(AnimationDetectionActor.class);
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
				.match(PathMessage.class, msg -> {
					boolean err = false;
					do{
						err = false;
						try{
							Browser browser = BrowserConnectionFactory.getConnection(msg.getDiscovery().getBrowserName(), BrowserEnvironment.DISCOVERY);
							PageState first_page_state = PathUtils.getFirstPage(msg.getPathObjects());
							
							log.warning("navigating to url :: " + msg.getDiscovery().getDomainUrl());
							browser.navigateTo(first_page_state.getUrl());
							crawler.crawlPathWithoutBuildingResult(msg.getKeys(), msg.getPathObjects(), browser, msg.getDiscovery().getDomainUrl());

							Animation animation = BrowserUtils.getAnimation(browser, msg.getDiscovery().getDomainUrl());
							if(animation.getImageUrls().size() > 1){
								first_page_state.getAnimatedImageUrls().addAll(animation.getImageUrls());
								first_page_state.getAnimatedImageChecksums().addAll(animation.getImageChecksums());
								page_state_service.save(first_page_state);
							}

							final ActorRef form_discoverer = actor_system.actorOf(SpringExtProvider.get(actor_system)
									  .props("formDiscoveryActor"), "form_discovery"+UUID.randomUUID());
							ActorRef path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
									  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());

							PathMessage path_message = msg.clone();

							form_discoverer.tell(path_message, getSelf() );
							path_expansion_actor.tell(path_message, getSelf() );
						}catch(Exception e){
							log.warning("exception occurred during Animation Detection.....  "+e.getMessage());
							err = true;
						}
					}while(err);
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

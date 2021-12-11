package com.looksee.actors;

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

import com.looksee.browsing.Browser;
import com.looksee.browsing.Crawler;
import com.looksee.helpers.BrowserConnectionHelper;
import com.looksee.models.Animation;
import com.looksee.models.PageState;
import com.looksee.models.enums.BrowserEnvironment;
import com.looksee.models.enums.PathStatus;
import com.looksee.models.message.PathMessage;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.PathUtils;

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
	private Crawler crawler;

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
							Browser browser = BrowserConnectionHelper.getConnection(msg.getBrowser(), BrowserEnvironment.DISCOVERY);
							PageState first_page_state = PathUtils.getFirstPage(msg.getPathObjects());
							
							log.warning("navigating to url :: " + first_page_state.getUrl());
							browser.navigateTo(first_page_state.getUrl());
							browser.removeDriftChat();

							log.warning("crawling path without building result");
							//crawler.crawlPathWithoutBuildingResult(msg.getKeys(), msg.getPathObjects(), browser, first_page_state.getUrl(), msg.getAccountId());
							log.warning("getting animation...");

							PathMessage updated_path_msg = new PathMessage(msg.getKeys(), msg.getPathObjects(), msg.getDiscoveryActor(), PathStatus.EXAMINED, msg.getBrowser(), msg.getDomainActor(), msg.getDomain(), msg.getAccountId());
							msg.getDiscoveryActor().tell(updated_path_msg, getSelf());
							
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

package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.enums.BrowserType;
import com.looksee.models.enums.CrawlAction;
import com.looksee.models.message.PageCrawlActionMessage;
import com.looksee.models.message.PageDataExtractionMessage;
import com.looksee.models.message.PathMessage;
import com.looksee.models.message.UrlMessage;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

@Component
@Scope("prototype")
public class CrawlerActor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(AestheticAuditor.class.getName());

	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private ActorSystem actor_system;
	
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
	 * NOTE: Do not change the order of the checks for instance of below. These are in this order because ExploratoryPath
	 * 		 is also a Test and thus if the order is reversed, then the ExploratoryPath code never runs when it should
	 * @throws NullPointerException
	 * @throws IOException
	 * @throws NoSuchElementException
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(PageCrawlActionMessage.class, page_crawl_action_msg -> {
					if(CrawlAction.START.equals(page_crawl_action_msg.getAction())) {
						
						ActorRef page_state_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
								  .props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
						page_state_builder.tell(page_crawl_action_msg, getSelf());
					}
					else if(CrawlAction.STOP.equals(page_crawl_action_msg.getAction())) {
						
					}
				})
				.match(PageDataExtractionMessage.class, msg -> {
					//Add page state to frontier
					//Add page state to path
					//send path to path expansion actor
					
					ActorRef path_expansion_actor = getContext().actorOf(SpringExtProvider.get(actor_system)
							  .props("pathExpansionActor"), "pathExpansionActor"+UUID.randomUUID());
					path_expansion_actor.tell(msg, getSelf());
				})
				.match(PathMessage.class, msg -> {
					if( /*final PageState URL is an outside domain || pageState is in visited list*/) {
						getContext().getParent().tell(msg, getSelf());
					}
					
					if( /* is NOT PageState in frontier list */) {
						//add page state to frontier
					}
					
					//Remove page state from frontier
					//Add page state to visited list
					
					ActorRef path_expansion_actor = getContext().actorOf(SpringExtProvider.get(actor_system)
							  .props("pathExpansionActor"), "pathExpansionActor"+UUID.randomUUID());
					path_expansion_actor.tell(msg, getSelf());
					
				})
				.match(MemberUp.class, mUp -> {
					log.debug("Member is Up: {}", mUp.member());
				})
				.match(MemberUp.class, mUp -> {
					log.debug("Member is Up: {}", mUp.member());
				})
				.match(UnreachableMember.class, mUnreachable -> {
					log.debug("Member detected as unreachable: {}", mUnreachable.member());
				})
				.match(MemberRemoved.class, mRemoved -> {
					log.debug("Member is Removed: {}", mRemoved.member());
				})
				.matchAny(o -> {
					log.debug("received unknown message of type :: "+o.getClass().getName());
				})
				.build();
	}
}

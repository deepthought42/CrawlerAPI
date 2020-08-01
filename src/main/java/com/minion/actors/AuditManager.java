package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.qanairy.models.Account;
import com.qanairy.models.Page;
import com.qanairy.models.PageState;
import com.qanairy.models.RenderedPageState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.enums.CrawlAction;
import com.qanairy.models.message.AuditSet;
import com.qanairy.models.message.CrawlActionMessage;
import com.qanairy.models.message.PageAuditComplete;

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
 * 
 */
@Component
@Scope("prototype")
public class AuditManager extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(AuditManager.class.getName());

	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private ActorSystem actor_system;
	
	private ActorRef web_crawler_actor;
	private Account account;
	private int page_count;
	private int page_state_count;
	private int rendered_page_state_count;
	private int page_audits_completed;
	private List<Audit> audits;
	Map<String, Page> pages_experienced = new HashMap<>();
	//subscribe to cluster changes
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
				MemberEvent.class, UnreachableMember.class);
		page_count = 0;
		page_state_count = 0;
		rendered_page_state_count = 0;
		page_audits_completed = 0;
		audits = new ArrayList<>();
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
				.match(CrawlActionMessage.class, message-> {
					if(message.getAction().equals(CrawlAction.START_LINK_ONLY)){
						log.warn("Starting crawler");
						
						//send message to page data extractor
						ActorRef web_crawl_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("webCrawlerActor"), "webCrawlerActor"+UUID.randomUUID());
						CrawlActionMessage crawl_action_message = new CrawlActionMessage(CrawlAction.START_LINK_ONLY, message.getDomain(), message.getAccountId());
						web_crawl_actor.tell(crawl_action_message, getSelf());
					}
					else if(message.getAction().equals(CrawlAction.STOP)){
						stopAudit(message);
					}
					
				})
				.match(Page.class, page -> {
					log.warn("recieved page :: "+page.getUrl());
				//	if(!pages_experienced.containsKey(page.getKey())) {
						page_count++;
						pages_experienced.put(page.getKey(), page);
						log.warn("Page Count :: "+page_count);
						ActorRef page_data_extractor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("pageDataExtractor"), "pageDataExtractor"+UUID.randomUUID());
						page_data_extractor.tell(page, getSelf());						
					//}
				})
				.match(PageState.class, page_state -> {
					page_state_count++;
					log.warn("Page State Count :: "+page_state_count);
					ActorRef web_crawl_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
							.props("webCrawlerActor"), "webCrawlerActor"+UUID.randomUUID());
					web_crawl_actor.tell(page_state, getSelf());
				})
				.match(RenderedPageState.class, page_state -> {
					rendered_page_state_count++;
					log.warn("Rendered Page State Count :: "+rendered_page_state_count);
					ActorRef auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
							.props("auditor"), "auditor"+UUID.randomUUID());
					auditor.tell(page_state.getPageState(), getSelf());	
				})
				.match(PageAuditComplete.class, audit_complete -> {
					audits.addAll(audit_complete.getPageState().getAudits());
					page_audits_completed++;
					log.warn("Audit record received by audit manager. page cnt : "+page_count+"   ;    audit size  ::   "+page_audits_completed);
					if(page_audits_completed == page_count) {
						log.warn("Audit Manager is now ready to perform a domain audit");
						AuditSet audit_record_set = new AuditSet(audits);
						ActorRef auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("auditor"), "auditor"+UUID.randomUUID());
						auditor.tell(audit_record_set, getSelf());
					}
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
	
	private void stopAudit(CrawlActionMessage message) {		
		//stop all discovery processes
		if(web_crawler_actor != null){
			actor_system.stop(web_crawler_actor);
			web_crawler_actor = null;
		}
	}
	
	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public int getPageAuditsCompleted() {
		return page_audits_completed;
	}

	public void setPageAuditsCompleted(int page_audits_completed) {
		this.page_audits_completed = page_audits_completed;
	}
	
	
}

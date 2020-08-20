package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.Page;
import com.qanairy.models.PageState;
import com.qanairy.models.RenderedPageState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.AuditRecord;
import com.qanairy.models.enums.AuditStage;
import com.qanairy.models.enums.CrawlAction;
import com.qanairy.models.message.AuditSet;
import com.qanairy.models.message.CrawlActionMessage;
import com.qanairy.models.message.DomainAuditMessage;
import com.qanairy.models.message.PageStateAuditComplete;
import com.qanairy.services.AuditRecordService;
import com.qanairy.services.AuditService;
import com.qanairy.services.DomainService;
import com.qanairy.services.PageStateService;

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
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private AuditService audit_service;
	
	private ActorRef web_crawler_actor;
	private Account account;
	private int page_count;
	private int page_state_count;
	private int rendered_page_state_count;
	private int page_audits_completed;
	Map<String, Page> pages_experienced = new HashMap<>();
	Map<String, Page> page_states_experienced = new HashMap<>();
	private AuditRecord audit_record;

	//subscribe to cluster changes
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
				MemberEvent.class, UnreachableMember.class);
		page_count = 0;
		page_state_count = 0;
		rendered_page_state_count = 0;
		page_audits_completed = 0;
		audit_record = new AuditRecord();
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
					audit_record = message.getAuditRecord();
					if(message.getAction().equals(CrawlAction.START_LINK_ONLY)){
						log.warn("Starting crawler");
						
						//send message to page data extractor
						ActorRef web_crawl_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("webCrawlerActor"), "webCrawlerActor"+UUID.randomUUID());
						web_crawl_actor.tell(message, getSelf());
					}
					else if(message.getAction().equals(CrawlAction.STOP)){
						stopAudit(message);
					}
					
				})
				.match(Page.class, page -> {
					log.warn("recieved page :: "+page.getUrl());
					if(!pages_experienced.containsKey(page.getKey())) {
						page_count++;
						pages_experienced.put(page.getKey(), page);
						log.warn("Page Count :: "+page_count);
						ActorRef page_data_extractor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("pageDataExtractor"), "pageDataExtractor"+UUID.randomUUID());
						page_data_extractor.tell(page, getSelf());
						
						ActorRef web_crawl_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("webCrawlerActor"), "webCrawlerActor"+UUID.randomUUID());
						web_crawl_actor.tell(page, getSelf());
						log.warn("page state received by audit manager ::      "+page);
					}
				})
				.match(PageState.class, page_state -> {
					log.warn("Recieved page state :: "+page_state.getUrl());
					//send URL to JourneyExplorer actor
					if(!page_states_experienced.containsKey(page_state.getKey())) {
						//page_state = page_state_service.save(page_state);
						//add page state to page somehow?
						ActorRef journeyMapper = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("journeyMappingManager"), "journeyMappingManager"+UUID.randomUUID());
						journeyMapper.tell(new URL(page_state.getUrl()), getSelf());
						
						page_state_count++;
						/*log.warn("Page State Count :: "+page_state_count);
						ActorRef web_crawl_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("webCrawlerActor"), "webCrawlerActor"+UUID.randomUUID());
						web_crawl_actor.tell(page_state, getSelf());
						*/
						log.warn("page state received by audit manager ::      "+page_state);
						log.warn("page state recieved by audit manager. page cnt : "+page_count+"   ;    page state count  ::   "+page_state_count);
					}
					else {
						log.warn("Page state already processed for audit manager ..... "+page_state.getUrl());
					}
				})
				.match(RenderedPageState.class, page_state -> {
					ActorRef auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
							.props("auditor"), "auditor"+UUID.randomUUID());
					
					rendered_page_state_count++;
					log.warn("Rendered Page State Count :: "+rendered_page_state_count);
					auditor.tell(page_state.getPageState(), getSelf());	
				})
				.match(PageStateAuditComplete.class, audit_complete -> {
					page_audits_completed++;
					log.warn("Page Audit Complete message received by audit manager. page cnt : "+page_count+"   ;    audit size  ::   "+page_audits_completed);

					if(page_audits_completed == page_count) {
						log.warn("audit complete page state key :: "+audit_complete.getPageState().getKey());
						Domain domain = domain_service.findByPageState(audit_complete.getPageState().getKey());
						DomainAuditMessage domain_msg = new DomainAuditMessage( domain, AuditStage.RENDERED);
						log.warn("Audit Manager is now ready to perform a domain audit");
						//AuditSet audit_record_set = new AuditSet(audits);
						ActorRef auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("auditor"), "auditor"+UUID.randomUUID());
						auditor.tell(domain_msg, getSelf());
					}
				})
				.match(AuditSet.class, audit_list -> {
					//save all audits in audit list to database and add them to the audit record
					for(Audit audit : audit_list.getAudits()){
						audit = audit_service.save(audit);
						audit_record_service.addAudit( audit_record.getKey(), audit.getKey());
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

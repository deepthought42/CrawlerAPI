package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.api.MessageBroadcaster;
import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.PageVersion;
import com.qanairy.models.PageState;
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
import com.qanairy.utils.BrowserUtils;

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
	private AuditService audit_service;

	private ActorRef web_crawler_actor;
	private Account account;
	
	Map<String, PageVersion> pages_experienced = new HashMap<>();
	Map<String, PageState> page_states_experienced = new HashMap<>();
	Map<String, PageState> page_states_audited = new HashMap<>();

	private AuditRecord audit_record;

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
				.match(PageVersion.class, page -> {
					log.warn("recieved page :: "+page.getUrl());
					if(!pages_experienced.containsKey(page.getKey())) {
						pages_experienced.put(page.getKey(), page);
						/*
						ActorRef page_data_extractor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("pageDataExtractor"), "pageDataExtractor"+UUID.randomUUID());
						page_data_extractor.tell(page, getSelf());
						*/
						ActorRef web_crawl_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("webCrawlerActor"), "webCrawlerActor"+UUID.randomUUID());
						web_crawl_actor.tell(page, getSelf());
					}
				})
				.match(PageState.class, page_state -> {
					log.warn("Received page state :: "+page_state.getUrl());
					//send URL to JourneyExplorer actor
					if(!page_states_experienced.containsKey(page_state.getKey())) {
						page_states_experienced.put(page_state.getKey(), page_state);
						/*
						ActorRef journeyMapper = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("journeyMappingManager"), "journeyMappingManager"+UUID.randomUUID());
						journeyMapper.tell(new URL(page_state.getUrl()), getSelf());
						*/
						
						log.warn("Page State Count :: "+page_states_experienced.keySet().size());
						/*
						ActorRef web_crawl_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("webCrawlerActor"), "webCrawlerActor"+UUID.randomUUID());
						web_crawl_actor.tell(page_state, getSelf());
						*/
						ActorRef auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("auditor"), "auditor"+UUID.randomUUID());
						
						//rendered_page_state_count++;
						auditor.tell(page_state, getSelf());
					}
					else {
						log.warn("Page state already processed for audit manager ..... "+page_state.getUrl());
					}
				})
				.match(PageStateAuditComplete.class, audit_complete -> {
					Domain domain = domain_service.findByPageState(audit_complete.getPageState().getKey());
					page_states_audited.put(audit_complete.getPageState().getKey(), audit_complete.getPageState());

					List<PageVersion> pages = domain_service.getPages(domain.getHost());
					Set<PageState> page_states = domain_service.getPageStates(domain.getHost());

					if( pages.size() == page_states.size()) {						
						DomainAuditMessage domain_audit_msg = new DomainAuditMessage( domain, AuditStage.RENDERED);
						//AuditSet audit_record_set = new AuditSet(audits);
						ActorRef auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("auditor"), "auditor"+UUID.randomUUID());
						auditor.tell(domain_audit_msg, getSelf());
					}
				})
				.match(AuditSet.class, audit_list -> {
					String url_str = BrowserUtils.sanitizeUserUrl(audit_list.getUrl());
					
					
					/* NOTE:: if still exists after 4-1-2021 then remove
					if(!url_str.contains("http")) {
						url_str = "http://"+url_str;
					}

					URL url = new URL(url_str);
					String host = url.getHost();
					host.replace("www.", "");
					if(!host.contains("www.")) {
						host = "www."+host;
					}
					*/
					
					URL url = new URL(url_str);
					String host = url.getHost();
					
					log.warn("(AUDIT MANAGER) looking up audit record using host  :: "+host);
					
					//NOTE: Audit record can be null, need to handle that scenario
					AuditRecord audit_record = domain_service.getMostRecentDomainAuditRecord(host);
					log.warn("Audit record :: " + audit_record);
					//save all audits in audit list to database and add them to the audit record
					for(Audit audit : audit_list.getAudits()){
						log.warn("saving audit : "+audit);
						audit = audit_service.save(audit);
						log.warn("successfully saved audit : "+audit);
						log.warn("audit record ::  "+audit_record);
						audit_record_service.addAudit( audit_record.getKey(), audit.getKey() );
						
						//send pusher message to clients currently subscribed to domain audit channel
						MessageBroadcaster.broadcastAudit(host, audit);
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
			//actor_system.stop(web_crawler_actor);
			web_crawler_actor = null;
		}
	}
	
	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

}

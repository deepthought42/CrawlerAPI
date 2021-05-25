package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.api.MessageBroadcaster;
import com.looksee.models.Account;
import com.looksee.models.AuditStats;
import com.looksee.models.Domain;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditStage;
import com.looksee.models.enums.CrawlAction;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.message.DomainAuditMessage;
import com.looksee.models.message.PageAuditRecordMessage;
import com.looksee.models.message.PageStateAuditComplete;
import com.looksee.models.message.PageStateMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.DomainService;
import com.looksee.utils.BrowserUtils;

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
	
	Map<String, PageState> page_states_experienced = new HashMap<>();
	Map<String, PageState> page_states_audited = new HashMap<>();

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
					if(message.getAction().equals(CrawlAction.START)){
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
				.match(PageStateMessage.class, page_state_msg -> {
					log.warn("Received page state :: "+page_state_msg.getPageState().getUrl());
					//send URL to JourneyExplorer actor
					if(!page_states_experienced.containsKey(page_state_msg.getPageState().getKey())) {
						Optional<AuditRecord> record = audit_record_service.findById(page_state_msg.getAuditRecordId());
						
						if( record.isPresent() ) {
							if(record.get() instanceof DomainAuditRecord) {								
								DomainAuditRecord audit_record = (DomainAuditRecord)record.get();
								
								Set<PageAuditRecord> audit_records = audit_record_service.getPageAuditRecords(page_state_msg.getAuditRecordId());
								//get Page Count
								long page_count = audit_records.size();
								
								//get total content audit pages
								long content_audit_pages = getAuditCount(AuditCategory.CONTENT);
								
								//get total information architecture audit pages
								long info_arch_pages = getAuditCount(AuditCategory.INFORMATION_ARCHITECTURE);
								
								//get total aesthetic audit pages
								long aesthetic_pages = getAuditCount(AuditCategory.AESTHETICS);
								
								
								//build stats object
								AuditStats audit_stats = new AuditStats(audit_record.getId(), 
																		audit_record.getStartTime(), 
																		audit_record.getEndTime(), 
																		page_count, 
																		content_audit_pages, 
																		info_arch_pages, 
																		aesthetic_pages );
								
								MessageBroadcaster.sendAuditStatUpdate(page_state_msg.getAccountId(), audit_stats);
							}		
						}
						
						page_states_experienced.put(page_state_msg.getPageState().getKey(), page_state_msg.getPageState());
						/*
						ActorRef journeyMapper = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("journeyMappingManager"), "journeyMappingManager"+UUID.randomUUID());
						journeyMapper.tell(new URL(page_state.getUrl()), getSelf());
						*/
				   		ActorRef insight_auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("performanceAuditor"), "performanceAuditor"+UUID.randomUUID());
						
						//rendered_page_state_count++;
				   		insight_auditor.tell(page_state_msg.getPageState(), getSelf());
						
						log.warn("Page State Count :: "+page_states_experienced.keySet().size());
						/*
						ActorRef web_crawl_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("webCrawlerActor"), "webCrawlerActor"+UUID.randomUUID());
						web_crawl_actor.tell(page_state, getSelf());
						*/
						ActorRef auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("auditor"), "auditor"+UUID.randomUUID());
						
						//rendered_page_state_count++;
						auditor.tell(page_state_msg, getSelf());
					}
					else {
						log.warn("Page state already processed for audit manager ..... "+page_state_msg.getPageState().getUrl());
					}
				})
				.match(PageStateAuditComplete.class, audit_complete -> {
					Domain domain = domain_service.findByPageState(audit_complete.getPageState().getKey());
					page_states_audited.put(audit_complete.getPageState().getKey(), audit_complete.getPageState());

					Set<PageState> pages = domain_service.getPages(domain.getUrl());
					Set<PageState> page_states = domain_service.getPageStates(domain.getId());

					if( pages.size() == page_states.size()) {						
						DomainAuditMessage domain_audit_msg = new DomainAuditMessage( domain, AuditStage.RENDERED);
						//AuditSet audit_record_set = new AuditSet(audits);
						ActorRef auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("auditor"), "auditor"+UUID.randomUUID());
						auditor.tell(domain_audit_msg, getSelf());
					}
				})
				.match(PageAuditRecordMessage.class, audit_record -> {
					
					log.warn("page audit :: "+audit_record.getPageAuditRecord());
					log.warn("page audit page state :: "+audit_record.getPageAuditRecord().getPageState());
					PageState page_state = audit_record_service.getPageStateForAuditRecord(audit_record.getPageAuditRecord().getId());
					String url_str = BrowserUtils.sanitizeUserUrl(page_state.getUrl());
					
					URL url = new URL(url_str);
					String host = url.getHost();
					
					log.warn("(AUDIT MANAGER) looking up audit record using host  :: "+host);
					
					//NOTE: Audit record can be null, need to handle that scenario
					//Optional<DomainAuditRecord> audit_record_opt = domain_service.getMostRecentAuditRecord(audit_record.getDomainId());					
					audit_record_service.save(audit_record.getPageAuditRecord());
					log.warn("adding page audit with key = "+audit_record.getPageAuditRecord().getKey() + "   to domain audit record with id= "+audit_record.getAuditRecordId());
					audit_record_service.addPageAuditToDomainAudit(audit_record.getAuditRecordId(), audit_record.getPageAuditRecord().getKey());
					
					log.warn("Audit record :: " + audit_record);
					//save all audits in audit list to database and add them to the audit record
					for(Audit audit : audit_record.getPageAuditRecord().getAudits()){
						audit = audit_service.save(audit);
						audit_record_service.addAudit( audit_record.getPageAuditRecord().getKey(), audit.getKey() );
						
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
	
	private long getAuditCount(AuditCategory content) {
	// TODO Auto-generated method stub
	return 0;
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

package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.Account;
import com.looksee.models.Domain;
import com.looksee.models.PageState;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.CrawlAction;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.enums.SubscriptionPlan;
import com.looksee.models.message.AuditError;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.ElementsSaved;
import com.looksee.models.message.ExceededSubscriptionMessage;
import com.looksee.models.message.Message;
import com.looksee.models.message.PageAuditRecordMessage;
import com.looksee.models.message.PageCandidateFound;
import com.looksee.models.message.PageCrawlActionMessage;
import com.looksee.models.message.PageDataExtractionError;
import com.looksee.models.message.PageDataExtractionMessage;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.PageStateService;
import com.looksee.services.SendGridMailService;
import com.looksee.services.SubscriptionService;
import com.looksee.utils.AuditUtils;
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
 * Performs audits of individual pages
 * 
 */
@Component
@Scope("prototype")
public class SinglePageAuditManager extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(SinglePageAuditManager.class.getName());

	private Cluster cluster = Cluster.get(getContext().getSystem());

	private ActorRef web_crawler_actor;
	private Account account = null;
	
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private AccountService account_service;
	
	@Autowired
	private SendGridMailService mail_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private SubscriptionService subscription_service;

	private Map<String, Boolean> page_urls = new HashMap<>();
	private Domain domain = null;

	//subscription tracking
	
	//PROGRESS TRACKING VARIABLES
	double journey_mapping_progress = 0.0;
	
	//subscribe to cluster changes
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
				MemberEvent.class, UnreachableMember.class);
	}

	//re-subscribe when restart
	@Override
    public void postStop() {
		log.error("Something happened that caused AuditManager to stop");
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
				.match(PageCrawlActionMessage.class, message-> {
					//HANDLE SINGLE PAGE AUDIT ACTION
					if(message.getAction().equals(CrawlAction.START)){
						log.warn("starting single page audit for  :: "+message.getUrl());						
						
						ActorRef page_state_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
					   			.props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
						page_state_builder.tell(message, getSelf());					
					}
					else if(message.getAction().equals(CrawlAction.STOP)){
						stopAudit(message);
					}
				})
				/*
				.match(PageCandidateFound.class, message -> {
					log.warn("Page candidate found message recieved by AUDIT MANAGER");
					try {
						String url_without_protocol = BrowserUtils.getPageUrl(message.getUrl());
						
						if(!this.page_urls.containsKey(url_without_protocol)) {
							DomainAuditRecord domain_audit_record = (DomainAuditRecord)audit_record_service.findById(message.getAuditRecordId()).get();						
							domain_audit_record.setTotalPages( this.page_urls.keySet().size());
							audit_record_service.save(domain_audit_record, message.getAccountId(), message.getDomainId());
							
							if(this.account == null && message.getAccountId() != -1) {
								this.account = account_service.findById(message.getAccountId()).get();
							}

					    	SubscriptionPlan plan = SubscriptionPlan.create(account.getSubscriptionType());

							if(!subscription_service.hasExceededDomainPageAuditLimit(plan, page_urls.size())) {
								//Account is still within page limit. continue with mapping page 
								log.warn("building page audit record...");
								PageAuditRecord audit_record = new PageAuditRecord(ExecutionStatus.BUILDING_PAGE, 
																					new HashSet<>(), 
																					null, 
																					true);
								audit_record.setUrl(url_without_protocol);
								audit_record.setDataExtractionProgress(1/50.0);
								audit_record.setDataExtractionMsg("Creating page record for "+url_without_protocol);
								audit_record.setAestheticMsg("Waiting for data extraction ...");
								audit_record.setAestheticAuditProgress(0.0);
							   	audit_record.setContentAuditMsg("Waiting for data extraction ...");
							   	audit_record.setContentAuditProgress(0.0);
							   	audit_record.setInfoArchMsg("Waiting for data extraction ...");
							   	audit_record.setInfoArchitectureAuditProgress(0.0);
							   	
							   	audit_record = (PageAuditRecord)audit_record_service.save(audit_record, 
							   														   	  message.getAccountId(), 
							   														   	  message.getDomainId());
							   	
							   	audit_record_service.addPageAuditToDomainAudit(message.getAuditRecordId(), 
							   												   audit_record.getKey());
								
								PageCrawlActionMessage crawl_action_msg = new PageCrawlActionMessage(CrawlAction.START,
																									 message.getDomainId(),
																									 message.getAccountId(), 
																									 audit_record, 
																									 message.getUrl());
								
								ActorRef page_state_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
							   											  .props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
								page_state_builder.tell(crawl_action_msg, getSelf());
							}
							page_urls.put(url_without_protocol, Boolean.TRUE);
						}
					}catch(Exception e) {
						e.printStackTrace();
					}
				})
				*/
				.match(PageDataExtractionError.class, message -> {
					log.warn("Error occurred while extracting page state for url "+message.getUrl()+";    error = "+message.getErrorMessage());
				})
				.match(PageDataExtractionMessage.class, message -> {
					initiatePageAudits(message.getPageState(), message);
				})
				.match(AuditProgressUpdate.class, message -> {
					log.debug("AUDIT PROGRESS UPDATE recieved by Audit Manager");
					try {
						AuditRecord audit_record = audit_record_service.updateAuditProgress(message.getAuditRecordId(), 
																							message.getCategory(), 
																							message.getAccountId(), 
																							message.getDomainId(), 
																							message.getProgress(), 
																							message.getMessage());	
	
						if(message.getAudit() != null) {
							audit_record_service.addAudit( audit_record.getId(), message.getAudit().getId() );							
						}
						
						boolean is_page_audit_complete = AuditUtils.isPageAuditComplete(audit_record);						
						if(is_page_audit_complete) {
							log.warn("page audit is complete!");
							audit_record.setEndTime(LocalDateTime.now());
							audit_record.setStatus(ExecutionStatus.COMPLETE);
							audit_record = audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());	
						
							PageState page = audit_record_service.getPageStateForAuditRecord(audit_record.getId());								
							if(this.account == null && message.getAccountId() != -1) {
								this.account = account_service.findById(message.getAccountId()).get();
								log.warn("sending email to account :: "+account.getEmail());
								mail_service.sendPageAuditCompleteEmail(account.getEmail(), page.getUrl(), audit_record.getId());
							}
						}
					} catch(Exception e) {
						log.warn("failed to retrieve audit record with id : "+message.getAuditRecordId());
						e.printStackTrace();
					}
				})
				.match(ExceededSubscriptionMessage.class, message -> {
					log.warn("subscription limits exceeded.");
					stopAudit(message);
				})
				.match(AuditError.class, message -> {
					AuditRecord audit_record = audit_record_service.findById(message.getAuditRecordId()).get();
					audit_record.setStatus(ExecutionStatus.ERROR);

					if(AuditCategory.CONTENT.equals(message.getAuditCategory())) {
						audit_record.setContentAuditProgress( message.getProgress() );
						audit_record.setContentAuditMsg( message.getErrorMessage());
					}
					else if(AuditCategory.AESTHETICS.equals(message.getAuditCategory())) {
						audit_record.setAestheticAuditProgress( message.getProgress() );
						audit_record.setAestheticMsg(message.getErrorMessage());
					}
					else if(AuditCategory.INFORMATION_ARCHITECTURE.equals(message.getAuditCategory())) {
						audit_record.setInfoArchitectureAuditProgress( message.getProgress() );
						audit_record.setInfoArchMsg(message.getErrorMessage());
					}
					
					audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());
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
	
	/**
	 * Marks domain {@link AuditRecord} as complete and sends email to user
	 * @param audit_record
	 * @param domain2
	 * @param account2
	 */
	private void markDomainAuditComplete(AuditRecord audit_record, Message message) {
		log.warn("audit IS COMPLETE!");
		audit_record.setContentAuditProgress(1.0);
		audit_record.setAestheticAuditProgress(1.0);
		audit_record.setDataExtractionProgress(1.0);
		audit_record.setInfoArchitectureAuditProgress(1.0);
		audit_record.setEndTime(LocalDateTime.now());
		audit_record.setStatus(ExecutionStatus.COMPLETE);
		audit_record =  audit_record_service.save(audit_record, 
												  message.getAccountId(), 
												  message.getDomainId());	
	}

	private void initiatePageAudits(PageState page_state, Message message) {
		log.warn("initiating page audits");
		//Account is still within page limit. continue with mapping page 
		PageAuditRecord page_audit = new PageAuditRecord(ExecutionStatus.BUILDING_PAGE, 
															new HashSet<>(), 
															null, 
															true);
		
		page_audit.setUrl(page_state.getUrl());
		page_audit.setDataExtractionProgress(1/50.0);
		page_audit.setDataExtractionMsg("Creating page record for "+page_state.getUrl());
		page_audit.setAestheticMsg("Waiting for data extraction ...");
		page_audit.setAestheticAuditProgress(0.0);
		page_audit.setContentAuditMsg("Waiting for data extraction ...");
	   	page_audit.setContentAuditProgress(0.0);
	   	page_audit.setInfoArchMsg("Waiting for data extraction ...");
	   	page_audit.setInfoArchitectureAuditProgress(0.0);
	   	
	   	page_audit = (PageAuditRecord)audit_record_service.save(page_audit, 
	   														   	  message.getAccountId(), 
	   														   	  message.getDomainId());
	   	
	   	audit_record_service.addPageToAuditRecord(page_audit.getId(), page_state.getId());
	   	audit_record_service.addPageAuditToDomainAudit(message.getAuditRecordId(), 
	   													page_audit.getKey());
	   	
		PageAuditRecordMessage audit_record_msg = new PageAuditRecordMessage(
														page_audit.getId(), 
														message.getDomainId(), 
														message.getAccountId(), 
														message.getAuditRecordId(),
														page_state);
		
		ActorRef content_auditor = getContext().actorOf(SpringExtProvider.get(actor_system)
											.props("contentAuditor"), "contentAuditor"+UUID.randomUUID());
		log.warn("sending message to content auditor....");
		content_auditor.tell(audit_record_msg, getSelf());							

		ActorRef info_architecture_auditor = getContext().actorOf(SpringExtProvider.get(actor_system)
		   			.props("informationArchitectureAuditor"), "informationArchitectureAuditor"+UUID.randomUUID());
		info_architecture_auditor.tell(audit_record_msg, getSelf());

		ActorRef aesthetic_auditor = getContext().actorOf(SpringExtProvider.get(actor_system)
					.props("aestheticAuditor"), "aestheticAuditor"+UUID.randomUUID());		
		aesthetic_auditor.tell(audit_record_msg, getSelf());
	}
	
	private void stopAudit(Message message) {
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

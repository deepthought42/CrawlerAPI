package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.Account;
import com.looksee.models.PageState;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.CrawlAction;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.message.AuditError;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.ExceededSubscriptionMessage;
import com.looksee.models.message.Message;
import com.looksee.models.message.PageAuditRecordMessage;
import com.looksee.models.message.PageCrawlActionMessage;
import com.looksee.models.message.PageDataExtractionError;
import com.looksee.models.message.PageDataExtractionMessage;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.SendGridMailService;
import com.looksee.utils.AuditUtils;

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
						
						ActorRef page_state_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
					   			.props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
						page_state_builder.tell(message, getSelf());					
					}
					else if(message.getAction().equals(CrawlAction.STOP)){
						stopAudit(message);
					}
				})
				.match(PageDataExtractionError.class, message -> {
					log.warn("Error occurred while extracting page state for url "+message.getUrl()+";    error = "+message.getErrorMessage());
					/**
					 * NOTE: THIS STILL NEEDS TO BE DONE
					 * 
					 * USER SHOULD BE INFORMED OF PAGE DATA EXTRACTION ERROR AND PAGE AUDIT RECORD SHOULD BE UPDATED ACCORDINGLY
					 */
				})
				.match(PageDataExtractionMessage.class, message -> {
					initiatePageAudits(message.getPageState(), message);
				})
				.match(AuditProgressUpdate.class, message -> {
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
						
						if(AuditUtils.isPageAuditComplete(audit_record)) {
							audit_record = markDomainAuditComplete(audit_record, message);
							
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
					/**
					 * NOTE: THIS STILL NEEDS TO BE DONE
					 * 
					 * RESULT SHOULD BE TO UPDATE AUDIT RECORD IF IT EXISTS AND TO INFORM USER OF SUBSCTIPTION EXCEPTION
					 */
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
	private AuditRecord markDomainAuditComplete(AuditRecord audit_record, Message message) {
		audit_record.setContentAuditProgress(1.0);
		audit_record.setAestheticAuditProgress(1.0);
		audit_record.setDataExtractionProgress(1.0);
		audit_record.setInfoArchitectureAuditProgress(1.0);
		audit_record.setEndTime(LocalDateTime.now());
		audit_record.setStatus(ExecutionStatus.COMPLETE);
		return  audit_record_service.save(audit_record, 
										  message.getAccountId(), 
										  message.getDomainId());	
	}

	private void initiatePageAudits(PageState page_state, Message message) {
		AuditRecord page_audit = audit_record_service.findById(message.getAuditRecordId()).get();
		page_audit.setUrl(page_state.getUrl());
		page_audit.setDataExtractionProgress(1.0);
		page_audit.setDataExtractionMsg("Data Extraction complete for "+page_state.getUrl());
		page_audit.setAestheticMsg("Starting visual design audit ...");
		page_audit.setAestheticAuditProgress(0.01);
		page_audit.setContentAuditMsg("Starting content audit ...");
	   	page_audit.setContentAuditProgress(0.01);
	   	page_audit.setInfoArchMsg("Starting information architecture audit ...");
	   	page_audit.setInfoArchitectureAuditProgress(0.01);
	   	
	   	
	   	page_audit = (PageAuditRecord)audit_record_service.save(page_audit, 
	   														   	  message.getAccountId(), 
	   														   	  message.getDomainId());
	   	
	   	account_service.addAuditRecord(message.getAccountId(), page_audit.getId());
	   	audit_record_service.addPageToAuditRecord(page_audit.getId(), page_state.getId());
	   	
		PageAuditRecordMessage audit_record_msg = new PageAuditRecordMessage(
														page_audit.getId(), 
														message.getDomainId(), 
														message.getAccountId(), 
														message.getAuditRecordId(),
														page_state);
		
		ActorRef content_auditor = getContext().actorOf(SpringExtProvider.get(actor_system)
											.props("contentAuditor"), "contentAuditor"+UUID.randomUUID());
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

package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
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

import com.looksee.models.Account;
import com.looksee.models.Domain;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.audit.recommend.Recommendation;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.CrawlAction;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.enums.SubscriptionPlan;
import com.looksee.models.message.AuditError;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.message.ElementExtractionError;
import com.looksee.models.message.ElementProgressMessage;
import com.looksee.models.message.ElementsSaveError;
import com.looksee.models.message.ElementsSaved;
import com.looksee.models.message.PageAuditRecordMessage;
import com.looksee.models.message.PageCandidateFound;
import com.looksee.models.message.PageCrawlActionMessage;
import com.looksee.models.message.PageDataExtractionError;
import com.looksee.models.message.PageDataExtractionMessage;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.DomainService;
import com.looksee.services.PageStateService;
import com.looksee.services.RecommendationService;
import com.looksee.services.SendGridMailService;
import com.looksee.services.SubscriptionService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.AuditUtils;
import com.looksee.utils.BrowserUtils;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
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

	private Map<String, Long> total_dispatches;
	private Map<String, Long> total_dispatch_responses;
	private ActorRef web_crawler_actor;
	private Account account;
	
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private AccountService account_service;
	
	@Autowired
	private AuditService audit_service;

	@Autowired
	private SendGridMailService mail_service;
	
	@Autowired
	private SubscriptionService subscription_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	@Autowired
	private RecommendationService recommendation_service;
	
	private boolean is_domain_audit = false;
	private int total_pages = 0;
	private Map<String, Boolean> page_states_experienced;

	//subscribe to cluster changes
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
				MemberEvent.class, UnreachableMember.class);
		this.total_dispatch_responses = new HashMap<>();
		this.total_dispatches = new HashMap<>();
		this.page_states_experienced = new HashMap<>();
		this.total_pages = 0;
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
						if(message.isIndividual()) {
							this.is_domain_audit = false;
							PageCrawlActionMessage crawl_action_msg = new PageCrawlActionMessage(CrawlAction.START, 
																								 message.getAccountId(), 
																								 (PageAuditRecord)message.getAuditRecord(), 
																								 message.getUrl(), 
																								 message.getDomainId());
							
							ActorRef page_state_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
						   			.props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
							page_state_builder.tell(crawl_action_msg, getSelf());
						}
						else {
							log.warn("starting domain audit");
							this.is_domain_audit = true;
							//send message to page data extractor
							ActorRef web_crawl_actor = getContext().actorOf(SpringExtProvider.get(actor_system)
									.props("webCrawlerActor"), "webCrawlerActor"+UUID.randomUUID());
							
							web_crawl_actor.tell(message, getSelf());
						}
					}
					else if(message.getAction().equals(CrawlAction.STOP)){
						stopAudit(message);
					}
					
				})
				.match(PageCandidateFound.class, message -> {
					log.warn("Page candidate recieved");
					try {
						String url_without_protocol = BrowserUtils.getPageUrl(message.getUrl());
						if(!this.page_states_experienced.containsKey(url_without_protocol)) {
							this.page_states_experienced.put(url_without_protocol, Boolean.TRUE);
	
							this.total_pages++;
							//DomainAuditRecord domain_audit_record = (DomainAuditRecord)audit_record_service.findById(message.getAuditRecordId()).get();						
							
							Account account = account_service.findById(message.getAccountId()).get();
					    	SubscriptionPlan plan = SubscriptionPlan.create(account.getSubscriptionType());
					    	int page_audit_cnt = audit_record_service.getPageAuditCount(message.getAuditRecordId());
							
					    	if(subscription_service.hasExceededDomainPageAuditLimit(plan, page_audit_cnt)
							) {	
								getContext().getSender().tell(PoisonPill.getInstance(), getSelf());
								
								log.warn("Account "+message.getAccountId() +" has exceeded limit on number of pages available for the domain");
							}
							else {
								//Account is still within page limit. continue with mapping page 
		
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
							   	
							   	audit_record = (PageAuditRecord)audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());
							   	
							   	audit_record_service.addPageAuditToDomainAudit(message.getAuditRecordId(), audit_record.getKey());
								
								PageCrawlActionMessage crawl_action_msg = new PageCrawlActionMessage(CrawlAction.START,
																									 message.getDomainId(),
																									 message.getAccountId(), 
																									 audit_record, 
																									 message.getUrl());
								
								ActorRef page_state_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
							   			.props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
								page_state_builder.tell(crawl_action_msg, getSelf());
							}
						}
					}catch(Exception e) {
						e.printStackTrace();
					}
				})
				.match(PageDataExtractionError.class, message -> {
					log.warn("Error occurred while extracting page state for url "+message.getUrl()+";    error = "+message.getErrorMessage());
					AuditRecord audit_record = audit_record_service.findById(message.getAuditRecordId()).get();
					audit_record.setContentAuditProgress(1.0);
					audit_record.setInfoArchitectureAuditProgress(1.0);
					audit_record.setAestheticAuditProgress(1.0);
					audit_record.setDataExtractionProgress(1.0);
					audit_record.setStatus(ExecutionStatus.ERROR);
					audit_record.setStatusMessage(message.getErrorMessage());
					audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());
				})
				.match(ElementProgressMessage.class, message -> {
					ActorRef data_extraction_supervisor = getContext().actorOf(SpringExtProvider.get(actor_system)
							.props("dataExtractionSupervisor"), "dataExtractionSupervisor"+UUID.randomUUID());
					data_extraction_supervisor.tell(message, getSelf());
				})
				.match(ElementExtractionError.class, message -> {
					long response_count = 0L; 
					if(this.total_dispatch_responses.containsKey(message.getPageUrl())) {
						response_count = this.total_dispatch_responses.get(message.getPageUrl());
					}
					this.total_dispatch_responses.put(message.getPageUrl(), ++response_count);
					
					log.warn("an error occurred during element extraction   "+message.getPageUrl());
					try {
						//TODO : add ability to track progress of elements mapped within the xpaths and to tell when the 
						//       system is done extracting element data and the page is ready for auditing
						
						PageAuditRecord audit_record = (PageAuditRecord)audit_record_service.findById(message.getAuditRecordId()).get();
						if(response_count == this.total_dispatches.get(message.getPageUrl())) {
							audit_record.setStatus(ExecutionStatus.RUNNING_AUDITS);
							audit_record.setDataExtractionMsg("Done!");
							audit_record.setDataExtractionProgress(1.0);
							audit_record.setAestheticAuditProgress(1/100.0);
							audit_record.setAestheticMsg("Starting visual design audit");
	
							audit_record.setContentAuditProgress(1/100.0);
							audit_record.setContentAuditMsg("Starting content audit");
	
							audit_record.setInfoArchitectureAuditProgress(1/100.0);
							audit_record.setInfoArchMsg("Starting Information Architecture audit");
							audit_record.setElementsReviewed(this.total_dispatch_responses.get(message.getPageUrl()) );
							audit_record = (PageAuditRecord) audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());
						
							/*
						   	log.warn("requesting performance audit from performance auditor....");
						   	ActorRef performance_insight_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
						   			.props("performanceAuditor"), "performanceAuditor"+UUID.randomUUID());
						   	performance_insight_actor.tell(page_state, getSelf());
						   	*/
							PageAuditRecordMessage audit_record_msg = new PageAuditRecordMessage(
																				audit_record.getId(), 
																				message.getDomainId(), 
																				message.getAccountId(), 
																				message.getAuditRecordId());
							
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
						else {
							audit_record.setDataExtractionMsg("Error Extracting elements in batch "+this.total_dispatch_responses.get(message.getPageUrl()) + " / "+this.total_dispatches.get(message.getPageUrl()));
							audit_record.setDataExtractionProgress(this.total_dispatch_responses.get(message.getPageUrl())/ (double)this.total_dispatches.get(message.getPageUrl()));
							audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());
						}
					}catch(Exception e) {
						log.error("Exception occurred while page state builder processed ElementProgressMessage!!");
						e.printStackTrace();
					}
				})
				.match(PageDataExtractionMessage.class, message -> {
					this.total_dispatches.put(message.getUrl(), (long)message.getDispatchCount());

					AuditRecord audit_record = audit_record_service.findById(message.getAuditRecordId()).get();
					audit_record.setDataExtractionMsg("Extracting elements");
					audit_record.setDataExtractionProgress(0.1);
					audit_record.setStatus(ExecutionStatus.EXTRACTING_ELEMENTS);
					audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());
				})
				.match(ElementsSaved.class, message -> {
					
					long response_count = 0L; 
					if(this.total_dispatch_responses.containsKey(message.getPageUrl())) {
						response_count = this.total_dispatch_responses.get(message.getPageUrl());
					}
					this.total_dispatch_responses.put(message.getPageUrl(), ++response_count);

					page_state_service.addAllElements(message.getPageStateId(), message.getElements());

					try {
						//TODO : add ability to track progress of elements mapped within the xpaths and to tell when the 
						//       system is done extracting element data and the page is ready for auditing
						
						PageAuditRecord audit_record = (PageAuditRecord)audit_record_service.findById(message.getAuditRecordId()).get();
						if(response_count == this.total_dispatches.get(message.getPageUrl())) {
							audit_record.setStatus(ExecutionStatus.RUNNING_AUDITS);

							audit_record.setDataExtractionMsg("Done!");
							audit_record.setDataExtractionProgress(1.0);
							audit_record.setAestheticAuditProgress(1/100.0);
							audit_record.setAestheticMsg("Starting visual design audit");
	
							audit_record.setContentAuditProgress(1/100.0);
							audit_record.setContentAuditMsg("Starting content audit");
	
							audit_record.setInfoArchitectureAuditProgress(1/100.0);
							audit_record.setInfoArchMsg("Starting Information Architecture audit");
							audit_record.setElementsReviewed(this.total_dispatch_responses.get(message.getPageUrl()) );
							audit_record = (PageAuditRecord) audit_record_service.save(audit_record, 
																						message.getAccountId(), 
																						message.getDomainId());
						
							//send page audit record to design system extractor
							ActorRef design_system_extractor = getContext().actorOf(SpringExtProvider.get(actor_system)
						   			.props("designSystemExtractor"), "designSystemExtractor"+UUID.randomUUID());
							log.warn("sending message to design system extractor ....");
							PageAuditRecordMessage page_audit_msg = new PageAuditRecordMessage( audit_record.getId(), 
																								message.getDomainId(), 
																								message.getAccountId(), 
																								message.getAuditRecordId());
							design_system_extractor.tell(page_audit_msg, getSelf());
							
							/*
						   	log.warn("requesting performance audit from performance auditor....");
						   	ActorRef performance_insight_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
						   			.props("performanceAuditor"), "performanceAuditor"+UUID.randomUUID());
						   	performance_insight_actor.tell(page_state, getSelf());
						   	*/
							PageAuditRecordMessage audit_record_msg = new PageAuditRecordMessage(
																				audit_record.getId(), 
																				message.getDomainId(), 
																				message.getAccountId(), 
																				message.getAuditRecordId());
							
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
						else {
							audit_record.setDataExtractionMsg("Elements saved successfully - batch "+this.total_dispatch_responses.get(message.getPageUrl()) + " / "+this.total_dispatches.get(message.getPageUrl()));
							audit_record.setDataExtractionProgress(this.total_dispatch_responses.get(message.getPageUrl())/ (double)this.total_dispatches.get(message.getPageUrl()));
							audit_record.setElementsReviewed(this.total_dispatch_responses.get(message.getPageUrl()));
							audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());
						}
					}catch(Exception e) {
						log.error("Exception occurred while page state builder processed ElementProgressMessage!!");
						e.printStackTrace();
					}
				})
				.match(ElementsSaveError.class, message -> {
					log.warn("error saving elements");
					AuditRecord audit_record = audit_record_service.findById(message.getAuditRecordId()).get();
					audit_record.setDataExtractionMsg("Error Saving elements "+this.total_dispatch_responses.get(message.getPageUrl()) + " / "+this.total_dispatches.get(message.getPageUrl()));
					audit_record.setDataExtractionProgress(this.total_dispatch_responses.get(message.getPageUrl())/ (double)this.total_dispatches.get(message.getPageUrl()));
					audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());
				})
				.match(AuditProgressUpdate.class, message -> {
					try {
						AuditRecord audit_record = audit_record_service.findById(message.getAuditRecordId()).get();
						audit_record.setDataExtractionProgress(1.0);
						audit_record.setStatus(ExecutionStatus.RUNNING_AUDITS);

						if(AuditCategory.CONTENT.equals(message.getCategory())) {
							audit_record.setContentAuditProgress( message.getProgress() );
							audit_record.setContentAuditMsg( message.getMessage());
						}
						else if(AuditCategory.AESTHETICS.equals(message.getCategory())) {
							audit_record.setAestheticAuditProgress( message.getProgress() );
							audit_record.setAestheticMsg(message.getMessage());
						}
						else if(AuditCategory.INFORMATION_ARCHITECTURE.equals(message.getCategory())) {
							audit_record.setInfoArchitectureAuditProgress( message.getProgress() );
							audit_record.setInfoArchMsg(message.getMessage());
						}
						
						audit_record =  audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());	
	
						if(message.getAudit() != null) {
							for(UXIssueMessage issue: message.getAudit().getMessages()) {
								Set<Recommendation> recommendations = new HashSet<>();
								
								issue.getRecommendations().remove(null);
								for(Recommendation recommendation: issue.getRecommendations()) {
									recommendations.add(recommendation_service.save(recommendation));
								}
								issue.setRecommendations(recommendations);
								issue = issue_message_service.save(issue);
							}
							Audit audit = audit_service.save(message.getAudit());
							audit_record_service.addAudit( audit_record.getId(), audit.getId() );							
						}
						
						if(this.is_domain_audit) {
							//extract color from page audits for color palette
							
							try {
								if(audit_record instanceof PageAuditRecord) {
									audit_record = audit_record_service.getDomainAuditRecordForPageRecord(audit_record.getId()).get();
								}
								
								if( audit_record_service.isDomainAuditComplete(audit_record)) {
									audit_record.setEndTime(LocalDateTime.now());
									audit_record.setStatus(ExecutionStatus.COMPLETE);
									audit_record =  audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());	
									log.warn("Domain audit is complete(part 2) :: "+audit_record.getId());
									
									Account account = account_service.findById(message.getAccountId()).get();
									Domain domain = domain_service.findByAuditRecord(message.getAuditRecordId()); //findById(message.getDomainId()).get();  //findByAuditRecord(audit_record.getId());
									mail_service.sendDomainAuditCompleteEmail(account.getEmail(), domain.getUrl(), domain.getId());
								}
							}
							catch(Exception e) {
								e.printStackTrace();
							}
						}
						else if(audit_record instanceof PageAuditRecord){
							boolean is_page_audit_complete = AuditUtils.isPageAuditComplete(audit_record);						
							if(is_page_audit_complete) {
								log.warn("page audit is complete!");
								audit_record.setEndTime(LocalDateTime.now());
								audit_record.setStatus(ExecutionStatus.COMPLETE);
								audit_record =  audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());	
								
								log.warn("Page audit updated to reflect completion : "+audit_record.getUrl());

								PageState page = audit_record_service.getPageStateForAuditRecord(audit_record.getId());								
								
								log.warn("Retrieving account ... "+message.getAccountId());
								log.warn("using audit record id :: "+audit_record.getId());
								Optional<Account> account_opt = audit_record_service.getAccount(audit_record.getId());// account_service.findById(message.getAccountId());
								if(account_opt.isPresent()) {
									log.warn("account is present for audit record");
									Account account = account_opt.get();
									log.warn("sending email to account :: "+account.getEmail());
									mail_service.sendPageAuditCompleteEmail(account.getEmail(), page.getUrl(), audit_record.getId());
								}
							}
						}
					} catch(Exception e) {
						log.warn("failed to retrieve audit record with id : "+message.getAuditRecordId());
						e.printStackTrace();
					}
				})
				.match(AuditError.class, message -> {
					AuditRecord audit_record = audit_record_service.findById(message.getAuditRecordId()).get();
					audit_record.setStatus(ExecutionStatus.ERROR);

					if(AuditCategory.CONTENT.equals(message.getAuditCategory())) {
						audit_record.setContentAuditProgress( 1 );
						audit_record.setContentAuditMsg( message.getErrorMessage());
					}
					else if(AuditCategory.AESTHETICS.equals(message.getAuditCategory())) {
						audit_record.setAestheticAuditProgress( 1 );
						audit_record.setAestheticMsg(message.getErrorMessage());
					}
					else if(AuditCategory.INFORMATION_ARCHITECTURE.equals(message.getAuditCategory())) {
						audit_record.setInfoArchitectureAuditProgress( 1 );
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

package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.looksee.models.journeys.Journey;
import com.looksee.models.journeys.SimpleStep;
import com.looksee.models.journeys.Step;
import com.looksee.models.message.AuditError;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.ConfirmedJourneyMessage;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.message.ElementsSaved;
import com.looksee.models.message.JourneyCrawlActionMessage;
import com.looksee.models.message.JourneyExaminationProgressMessage;
import com.looksee.models.message.PageAuditRecordMessage;
import com.looksee.models.message.PageCandidateFound;
import com.looksee.models.message.PageCrawlActionMessage;
import com.looksee.models.message.PageDataExtractionError;
import com.looksee.models.message.PageDataExtractionMessage;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DomainService;
import com.looksee.services.JourneyService;
import com.looksee.services.PageStateService;
import com.looksee.services.SendGridMailService;
import com.looksee.services.StepService;
import com.looksee.services.SubscriptionService;
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

	private ActorRef web_crawler_actor;
	private Account account;
	
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private AccountService account_service;

	@Autowired
	private StepService step_service;
	
	@Autowired
	private JourneyService journey_service;
	
	@Autowired
	private SendGridMailService mail_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private SubscriptionService subscription_service;
	
	private boolean is_domain_audit = false;
	private long domain_audit_id = 0L;
	private int total_pages = 0;
	private int total_pages_audited = 0;
	private Map<String, Boolean> page_urls = new HashMap<>();
	private Map<String, Boolean> page_states_experienced;

	private List<PageState> analyzed_pages = new ArrayList<>();
	private double aesthetic_audits_completed;
	private double total_aesthetic_audits;

	//subscription tracking
	boolean hasUserExceededSubscriptionAllowance;

	
	//PROGRESS TRACKING VARIABLES
	double journey_mapping_progress = 0.0;
	
	//subscribe to cluster changes
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
				MemberEvent.class, UnreachableMember.class);
		this.page_states_experienced = new HashMap<>();
		this.total_pages = 0;
		this.aesthetic_audits_completed= 0;
		this.total_aesthetic_audits = 4;
		this.hasUserExceededSubscriptionAllowance=false;
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
					this.total_aesthetic_audits = 3;
					//HANDLE SINGLE PAGE AUDIT ACTION
					if(message.getAction().equals(CrawlAction.START)){
						this.is_domain_audit = false;
						log.warn("starting single page audit for  :: "+message.getUrl());
						PageAuditRecord page_audit_record = (PageAuditRecord)audit_record_service.findById(message.getAuditRecordId()).get();
						
						PageCrawlActionMessage crawl_action_msg = new PageCrawlActionMessage(CrawlAction.START, 
																							 message.getAccountId(), 
																							 page_audit_record, 
																							 message.getUrl(), 
																							 message.getDomainId());
						
						
						ActorRef page_state_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
					   			.props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
						page_state_builder.tell(crawl_action_msg, getSelf());					
					}
					else if(message.getAction().equals(CrawlAction.STOP)){
						stopAudit(message);
					}
				})
				.match(CrawlActionMessage.class, message-> {
					this.total_aesthetic_audits = 4;

					if(message.getAction().equals(CrawlAction.START)){
						log.warn("starting domain audit");
						this.is_domain_audit = true;
						this.domain_audit_id = message.getAuditRecordId();
						//send message to webCrawlerActor to get pages
						/*
						ActorRef web_crawl_actor = getContext().actorOf(SpringExtProvider.get(actor_system)
								.props("webCrawlerActor"), "webCrawlerActor"+UUID.randomUUID());
						
						web_crawl_actor.tell(message, getSelf());
						*/
						ActorRef crawler_actor = getContext().actorOf(SpringExtProvider.get(actor_system)
								  .props("crawlerActor"), "crawlerActor"+UUID.randomUUID());
						crawler_actor.tell(message, getSelf());

					}
					else if(message.getAction().equals(CrawlAction.STOP)){
						stopAudit();
					}
					
				})
				.match(JourneyCrawlActionMessage.class, message-> {
					this.total_aesthetic_audits = 4;

					if(message.getAction().equals(CrawlAction.START)){
						log.warn("starting domain audit");
						this.is_domain_audit = true;
						this.domain_audit_id = message.getAuditRecordId();
						//send message to webCrawlerActor to get pages
						
						ActorRef crawler_actor = getContext().actorOf(SpringExtProvider.get(actor_system)
								  .props("crawlerActor"), "crawlerActor"+UUID.randomUUID());
						crawler_actor.tell(message, getSelf());

					}
					else if(message.getAction().equals(CrawlAction.STOP)){
						stopAudit();
					}
					
				})
				.match(PageCandidateFound.class, message -> {
					if(this.hasUserExceededSubscriptionAllowance) {
						return;
					}
					log.warn("Page candidate found message recieved by AUDIT MANAGER");
					try {
						String url_without_protocol = BrowserUtils.getPageUrl(message.getUrl());
						if(!this.page_states_experienced.containsKey(url_without_protocol)) {
							this.page_states_experienced.put(url_without_protocol, Boolean.TRUE);
	
							this.total_pages++;
							DomainAuditRecord domain_audit_record = (DomainAuditRecord)audit_record_service.findById(message.getAuditRecordId()).get();						
							domain_audit_record.setTotalPages( this.page_states_experienced.keySet().size());
							audit_record_service.save(domain_audit_record, message.getAccountId(), message.getDomainId());
							
							if(this.account == null) {
								this.account = account_service.findById(message.getAccountId()).get();
							}
					    	//int page_audit_cnt = audit_record_service.getPageAuditCount(message.getAuditRecordId());
							
					    	/*
					    	if(subscription_service.hasExceededDomainPageAuditLimit(plan, page_audit_cnt)
							) {	
								getContext().getSender().tell(PoisonPill.getInstance(), getSelf());
								
								log.warn("Account "+message.getAccountId() +" has exceeded limit on number of pages available for the domain");
							}
							else
							*/
					    	SubscriptionPlan plan = SubscriptionPlan.create(account.getSubscriptionType());

							if(!subscription_service.hasExceededDomainPageAuditLimit(plan, page_urls.size())) {
								page_urls.put(url_without_protocol, Boolean.TRUE);
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
						}
					}catch(Exception e) {
						e.printStackTrace();
					}
				})
				.match(PageDataExtractionMessage.class, message -> {
					log.warn("audit manager received page data extracton message");
					AuditRecord audit_record = audit_record_service.findById(message.getAuditRecordId()).get();
					audit_record.setDataExtractionMsg("Extracting elements");
					audit_record.setDataExtractionProgress(0.1);
					audit_record.setStatus(ExecutionStatus.EXTRACTING_ELEMENTS);
					audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());
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
				.match(ElementsSaved.class, message -> {
					PageState page_state = page_state_service.findById(message.getPageStateId()).get();

					PageAuditRecordMessage audit_record_msg = new PageAuditRecordMessage(
																	message.getAuditRecordId(), 
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
				})
				.match(ConfirmedJourneyMessage.class, message -> {
					if(hasUserExceededSubscriptionAllowance) {
						return;
					}
					
					log.warn("Handling confirmed journey message with steps :: "+message.getSteps());
					//save journey steps
					List<Step> saved_steps = new ArrayList<>();
					for(Step step : message.getSteps()) {
						saved_steps.add(step_service.save(step));
					}
					message.setSteps(saved_steps);
					//build create and save journey
					Journey journey = new Journey(message.getSteps());
					journey = journey_service.save(journey);
					
					//add journey to domain audit
					audit_record_service.addJourney(this.domain_audit_id, journey.getId());
					
					//retrieve all unique page states for journey steps
					List<PageState> page_states = getAllUniquePageStates(message.getSteps());
					Domain domain = domain_service.findById(message.getDomainId()).get();
					//remove all unique pageStates that have already been analyzed
					page_states = page_states.stream().filter(page -> !page_urls.containsKey(page.getUrl()) && !BrowserUtils.isExternalLink(domain.getUrl(), page.getUrl())).collect(Collectors.toList());
					
					//create PageAuditRecord for each page that hasn't been analyzed. 
					Account account = account_service.findById(message.getAccountId()).get();
			    	SubscriptionPlan plan = SubscriptionPlan.create(account.getSubscriptionType());
					
			    	//For each page audit record perform audits
					for(PageState page_state : page_states) {
						/*
						log.warn("Auditing page state :: "+page_state.getKey());
						PageAuditRecord page_audit_record = new PageAuditRecord(ExecutionStatus.BUILDING_PAGE, 
																				new HashSet<>(), 
																				null, 
																				true);
						page_audit_record = (PageAuditRecord)audit_record_service.save(page_audit_record);
					   	log.warn("adding page audit " + page_audit_record.getId() + " to domain audit :: "+message.getAuditRecordId());
						audit_record_service.addPageAuditToDomainAudit(message.getAuditRecordId(), 
																	   page_audit_record.getId());
						*/
						String url_without_protocol = page_state.getUrl();

						if(!subscription_service.hasExceededDomainPageAuditLimit(plan, page_urls.size())) {
							//total_pages_audited++;
							page_urls.put(url_without_protocol, Boolean.TRUE);

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
						   	
						   	audit_record = (PageAuditRecord)audit_record_service.save(audit_record, 
						   														   	  message.getAccountId(), 
						   														   	  message.getDomainId());
						   	audit_record_service.addPageToAuditRecord(audit_record.getId(), page_state.getId());
						   	
						   	audit_record_service.addPageAuditToDomainAudit(message.getAuditRecordId(), 
						   												   audit_record.getKey());
							
							PageAuditRecordMessage audit_record_msg = new PageAuditRecordMessage(
																				audit_record.getId(), 
																				message.getDomainId(), 
																				message.getAccountId(), 
																				audit_record.getId(),
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
						else {
							log.warn("+++++++++++++++++++++++++++++++++++++++");
							log.warn("User has exceeded domain page audit limit");
							log.warn("+++++++++++++++++++++++++++++++++++++++");
							hasUserExceededSubscriptionAllowance = true;
							getSender().tell(PoisonPill.class, getSelf());
							break;
						}
					}
					
				})
				.match(JourneyExaminationProgressMessage.class, message -> {
					journey_mapping_progress = message.getExaminedJourneys()/(double)message.getGeneratedJourneys();
					Account account = account_service.findById(message.getAccountId()).get();
					SubscriptionPlan plan = SubscriptionPlan.create(account.getSubscriptionType());

					log.warn("current journey mapping progress :: "+journey_mapping_progress);
					AuditRecord audit_record = audit_record_service.findById(message.getAuditRecordId()).get();
					log.warn("updating audit record data extraction progress :: "+audit_record.getKey());
					if(!subscription_service.hasExceededDomainPageAuditLimit(plan, page_urls.size())) {
						//set progress for audit record journey mapping
						audit_record.setDataExtractionProgress(journey_mapping_progress);
					}
					else {
						audit_record.setDataExtractionProgress(1.0);
					}
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
							this.aesthetic_audits_completed++;
							audit_record.setAestheticAuditProgress( message.getProgress());
							audit_record.setAestheticMsg(message.getMessage());
						}
						else if(AuditCategory.INFORMATION_ARCHITECTURE.equals(message.getCategory())) {
							audit_record.setInfoArchitectureAuditProgress( message.getProgress() );
							audit_record.setInfoArchMsg(message.getMessage());
						}
						
						audit_record =  audit_record_service.save(audit_record, 
																  message.getAccountId(), 
																  message.getDomainId());	
	
						if(message.getAudit() != null) {
							/*
							for(UXIssueMessage issue: message.getAudit().getMessages()) {
								Set<Recommendation> recommendations = new HashSet<>();
								
								issue.getRecommendations().remove(null);
								for(Recommendation recommendation: issue.getRecommendations()) {
									recommendations.add(recommendation_service.save(recommendation));
								}
								
								recommendations = recommendations.parallelStream().filter(rec -> rec != null).collect(Collectors.toSet());
								issue.setRecommendations(recommendations);
								ElementState element = null;
								if(issue instanceof ElementStateIssueMessage) {
									element = ((ElementStateIssueMessage) issue).getElement();
									element.print();
									((ElementStateIssueMessage) issue).setElement(null);
								}
								issue = issue_message_service.save(issue);
								if(element != null) {
									issue_message_service.addElement(issue.getId(), element.getId());
								}
							}
							Audit audit = audit_service.save(message.getAudit());
							 */
							audit_record_service.addAudit( audit_record.getId(), message.getAudit().getId() );							
						}
						
						if(this.is_domain_audit) {
							//extract color from page audits for color palette
							
							try {
								if(audit_record instanceof PageAuditRecord) {
									audit_record = audit_record_service.getDomainAuditRecordForPageRecord(audit_record.getId()).get();
									//audit_record = audit_record_service.findById(audit_record.getId()).get();
								}
								Account account = account_service.findById(message.getAccountId()).get();
								SubscriptionPlan plan = SubscriptionPlan.create(account.getSubscriptionType());

								if(subscription_service.hasExceededDomainPageAuditLimit(plan, page_urls.size())) {
								//if( audit_record_service.isDomainAuditComplete( audit_record, total_pages, total_pages_audited)) {
									
									audit_record.setEndTime(LocalDateTime.now());
									audit_record.setStatus(ExecutionStatus.COMPLETE);
									audit_record =  audit_record_service.save(audit_record, 
																			  message.getAccountId(), 
																			  message.getDomainId());	
									log.warn("Domain audit is complete(part 2) :: "+audit_record.getId());
									log.warn("Domain id :: "+message.getDomainId());
									Domain domain = domain_service.findById(message.getDomainId()).get(); //findById(message.getDomainId()).get();  //findByAuditRecord(audit_record.getId());
									log.warn("Domain email(part 2) :: "+domain.getId());
									log.warn("Account (part 2) :: "+account.getId());

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
								audit_record = audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());	
							
								PageState page = audit_record_service.getPageStateForAuditRecord(audit_record.getId());								
								Optional<Account> account_opt = audit_record_service.getAccount(audit_record.getId());// account_service.findById(message.getAccountId());
								if(account_opt.isPresent()) {
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
	
	private List<PageState> getAllUniquePageStates(List<Step> steps) {
		List<PageState> page_states = new ArrayList<>();
		Map<String, Boolean> key_map = new HashMap<>();
		
		for(Step step : steps) {
			if(!key_map.containsKey( step.getStartPage().getUrl() )){
				key_map.put(step.getStartPage().getUrl(), Boolean.TRUE);				
				page_states.add(((SimpleStep)step).getStartPage());
			}
			
			if(!key_map.containsKey( step.getEndPage().getUrl() )){
				key_map.put(step.getEndPage().getUrl(), Boolean.TRUE);				
				page_states.add(step.getEndPage());
			}
		}
		
		return page_states.stream().distinct().collect(Collectors.toList());
	}

	private void stopAudit() {		
		//stop all discovery processes
		if(web_crawler_actor != null){
			//actor_system.stop(web_crawler_actor);
			web_crawler_actor = null;
		}
	}
	
	private void stopAudit(PageCrawlActionMessage message) {
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

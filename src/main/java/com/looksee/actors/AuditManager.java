package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

import com.looksee.api.MessageBroadcaster;
import com.looksee.models.Account;
import com.looksee.models.Domain;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditStage;
import com.looksee.models.enums.CrawlAction;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.message.DomainAuditMessage;
import com.looksee.models.message.ElementProgressMessage;
import com.looksee.models.message.ElementsSaved;
import com.looksee.models.message.PageAuditRecordMessage;
import com.looksee.models.message.PageCandidateFound;
import com.looksee.models.message.PageCrawlActionMessage;
import com.looksee.models.message.PageStateAuditComplete;
import com.looksee.models.message.StartSinglePageAudit;
import com.looksee.models.message.UpdatePageElementDispatches;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.DomainService;
import com.looksee.services.SendGridMailService;
import com.looksee.services.UXIssueMessageService;
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
	private AuditRecordService audit_record_service;
	
	@Autowired
	private AccountService account_service;
	
	@Autowired
	private AuditService audit_service;

	@Autowired
	private SendGridMailService mail_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	
	
	Map<String, PageState> page_states_experienced = new HashMap<>();
	Map<String, PageState> page_states_audited = new HashMap<>();

	//subscribe to cluster changes
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
				MemberEvent.class, UnreachableMember.class);
		this.total_dispatch_responses = new HashMap<>();
		this.total_dispatches = new HashMap<>();
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
						ActorRef web_crawl_actor = getContext().actorOf(SpringExtProvider.get(actor_system)
								.props("webCrawlerActor"), "webCrawlerActor"+UUID.randomUUID());
						web_crawl_actor.tell(message, getSelf());
					}
					else if(message.getAction().equals(CrawlAction.STOP)){
						stopAudit(message);
					}
					
				})
				.match(PageCandidateFound.class, message -> {					
					PageAuditRecord audit_record = new PageAuditRecord(ExecutionStatus.IN_PROGRESS, new HashSet<>(), null, false);
				   	audit_record.setAestheticMsg("Waiting for data extraction ...");
				   	audit_record.setContentAuditMsg("Waiting for data extraction ...");
				   	audit_record.setInfoArchMsg("Waiting for data extraction ...");
				   	audit_record = (PageAuditRecord)audit_record_service.save(audit_record);
				   	
				   	audit_record_service.addPageAuditToDomainAudit(message.getAuditRecordId(), audit_record.getKey());
					
					PageCrawlActionMessage crawl_action_msg = new PageCrawlActionMessage(CrawlAction.START, -1, audit_record, message.getUrl());
					ActorRef page_state_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
				   			.props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
					page_state_builder.tell(crawl_action_msg, getSelf());
					
				})
				.match(StartSinglePageAudit.class, message -> {		
					PageCrawlActionMessage crawl_action_msg = new PageCrawlActionMessage(CrawlAction.START, -1, message.getAuditRecord(), message.getUrl());
					ActorRef page_state_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
				   			.props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
					page_state_builder.tell(crawl_action_msg, getSelf());
					
				})
				.match(PageStateAuditComplete.class, audit_complete -> {
					Domain domain = domain_service.findByPageState(audit_complete.getPageState().getKey());
					page_states_audited.put(audit_complete.getPageState().getKey(), audit_complete.getPageState());

					Set<PageState> pages = domain_service.getPages(domain.getUrl());
					Set<PageState> page_states = domain_service.getPageStates(domain.getId());
					
					mail_service.sendPageAuditCompleteEmail(account.getEmail(), audit_complete.getPageState().getUrl(), audit_complete.getAuditRecordId());
					
					//find user account
					if( pages.size() == page_states.size()) {
						Account account = account_service.findById(audit_complete.getAccountId()).get();
						//send domain audit complete
						mail_service.sendDomainAuditCompleteEmail(account.getEmail(), domain.getUrl(), domain.getId());
						

						
						DomainAuditMessage domain_audit_msg = new DomainAuditMessage( domain, AuditStage.RENDERED);
						//AuditSet audit_record_set = new AuditSet(audits);
						ActorRef auditor = getContext().actorOf(SpringExtProvider.get(actor_system)
								.props("auditor"), "auditor"+UUID.randomUUID());
						auditor.tell(domain_audit_msg, getSelf());
					}
				})
				.match(PageAuditRecordMessage.class, audit_record -> {
					PageState page_state = audit_record_service.getPageStateForAuditRecord(audit_record.getPageAuditRecord().getId());
					String url_str = BrowserUtils.sanitizeUserUrl(page_state.getUrl());
					
					URL url = new URL(url_str);
					String host = url.getHost();
					
					log.warn("(AUDIT MANAGER) looking up audit record using host  :: "+host);
					
					//NOTE: Audit record can be null, need to handle that scenario
					//Optional<DomainAuditRecord> audit_record_opt = domain_service.getMostRecentAuditRecord(audit_record.getDomainId());					
					//audit_record_service.save(audit_record.getPageAuditRecord());
					log.warn("adding page audit with key = "+audit_record.getPageAuditRecord().getKey() + " to domain audit record with id= "+audit_record.getAuditRecordId());
					audit_record_service.addPageAuditToDomainAudit(audit_record.getAuditRecordId(), audit_record.getPageAuditRecord().getKey());

					
					log.warn("Audit record :: " + audit_record);
					
					List<Audit> audit_list = new ArrayList<>(audit_record.getPageAuditRecord().getAudits());
					//save all audits in audit list to database and add them to the audit record
					for(Audit audit : audit_list){
						Audit saved_audit = audit_service.save(audit);
						audit_record_service.addAudit( audit_record.getPageAuditRecord().getKey(), saved_audit.getKey() );
						
						//send pusher message to clients currently subscribed to domain audit channel
						MessageBroadcaster.broadcastAudit(host, audit);
					}	
				})
				.match(ElementProgressMessage.class, message -> {
					ActorRef data_extraction_supervisor = getContext().actorOf(SpringExtProvider.get(actor_system)
							.props("dataExtractionSupervisor"), "dataExtractionSupervisor"+UUID.randomUUID());
					data_extraction_supervisor.tell(message, getSelf());
					this.total_dispatches.put(message.getPageUrl(), message.getTotalDispatches());
					
				})
				.match(ElementsSaved.class, message -> {
					long response_count = 0L;

					if(this.total_dispatch_responses.containsKey(message.getPageUrl())) {
						response_count = this.total_dispatch_responses.get(message.getPageUrl());
					}
										
					this.total_dispatch_responses.put(message.getPageUrl(), ++response_count);
					try {
						//TODO : add ability to track progress of elements mapped within the xpaths and to tell when the 
						//       system is done extracting element data and the page is ready for auditing
						
						PageAuditRecord audit_record = (PageAuditRecord)audit_record_service.findById(message.getAuditRecordId()).get();
						if(this.total_dispatch_responses.get(message.getPageUrl()) == this.total_dispatches.get(message.getPageUrl())) {
							//log.warn("ALL PAGE ELEMENT STATES HAVE BEEN MAPPED SUCCESSFULLY!!!!!");
							audit_record.setDataExtractionMsg("Done!");
							audit_record.setDataExtractionProgress(1.0);
							audit_record.setAestheticAuditProgress(1/20.0);
							audit_record.setAestheticMsg("Starting visual design audit");
	
							audit_record.setContentAuditProgress(1/20.0);
							audit_record.setContentAuditMsg("Starting content audit");
	
							audit_record.setInfoArchAuditProgress(1/20.0);
							audit_record.setInfoArchMsg("Starting Information Architecture audit");
							audit_record.setElementsReviewed(this.total_dispatch_responses.get(message.getPageUrl()) );
							audit_record = (PageAuditRecord) audit_record_service.save(audit_record);
						
							/*
						   	log.warn("requesting performance audit from performance auditor....");
						   	ActorRef performance_insight_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
						   			.props("performanceAuditor"), "performanceAuditor"+UUID.randomUUID());
						   	performance_insight_actor.tell(page_state, getSelf());
						   	*/
							ActorRef content_auditor = getContext().actorOf(SpringExtProvider.get(actor_system)
						   			.props("contentAuditor"), "contentAuditor"+UUID.randomUUID());
						
							content_auditor.tell(audit_record, getSelf());							

							ActorRef info_architecture_auditor = getContext().actorOf(SpringExtProvider.get(actor_system)
							   			.props("informationArchitectureAuditor"), "informationArchitectureAuditor"+UUID.randomUUID());
							info_architecture_auditor.tell(audit_record, getSelf());

							ActorRef aesthetic_auditor = getContext().actorOf(SpringExtProvider.get(actor_system)
										.props("aestheticAuditor"), "aestheticAuditor"+UUID.randomUUID());		
							aesthetic_auditor.tell(audit_record, getSelf());
						}
						else {
							audit_record.setElementsReviewed(this.total_dispatch_responses.get(message.getPageUrl()));
							audit_record_service.save(audit_record);
						}
					}catch(Exception e) {
						log.error("Exception occurred while page state builder processed ElementProgressMessage!!");
						e.printStackTrace();
					}
				})
				.match(UpdatePageElementDispatches.class, message -> {
					//log.warn("initializing dispatches for "+message.getUrl());
					this.total_dispatches.put(message.getUrl(), message.getTotalDispatches());
					this.total_dispatch_responses.put(message.getUrl(), 0L);
				})
				.match(AuditProgressUpdate.class, message -> {
					try {
						AuditRecord page_audit_record = audit_record_service.findById(message.getAuditRecordId()).get();
						
						if(AuditCategory.CONTENT.equals(message.getCategory())) {
							page_audit_record.setContentAuditProgress( message.getProgress() );
							page_audit_record.setContentAuditMsg( message.getMessage());
						}
						else if(AuditCategory.AESTHETICS.equals(message.getCategory())) {
							page_audit_record.setAestheticAuditProgress( message.getProgress() );
							page_audit_record.setAestheticMsg(message.getMessage());
						}
						else if(AuditCategory.INFORMATION_ARCHITECTURE.equals(message.getCategory())) {
							page_audit_record.setInfoArchAuditProgress( message.getProgress() );
							page_audit_record.setInfoArchMsg(message.getMessage());
						}
						page_audit_record =  audit_record_service.save(page_audit_record);	
	
						if(message.getAudit() != null) {
							
							Set<UXIssueMessage> issue_set = new HashSet<UXIssueMessage>();
							//List<Long> issue_ids = new ArrayList<>();
							for(UXIssueMessage issue: message.getAudit().getMessages()) {
								UXIssueMessage issue_record = issue_message_service.save(issue);
								issue_set.add(issue_record);
								//issue_ids.add(issue_record.getId());
							}
							
							message.getAudit().getMessages().addAll(issue_set);
							Audit audit = audit_service.save(message.getAudit());
							audit_record_service.addAudit( page_audit_record.getId(), audit.getId() );
							//audit_service.addAllIssues(audit.getId(), issue_ids);
							/*
							List<UXIssueMessage> issue_messages = new ArrayList<>();
							issue_messages.addAll(message.getAudit().getMessages());
							Iterable<UXIssueMessage> issues = issue_message_service.saveAll(issue_messages);
							Set<UXIssueMessage> issue_set = StreamSupport
																  .stream(issues.spliterator(), true)
																  .collect(Collectors.toSet());
							*/
							
						}
						
						boolean is_audit_complete = AuditUtils.isPageAuditComplete(page_audit_record);
						if(is_audit_complete) {
							PageState page = audit_record_service.getPageStateForAuditRecord(page_audit_record.getId());
							
							Set<Account> accounts = account_service.findForAuditRecord(page_audit_record.getId());
							for(Account account: accounts) {
								mail_service.sendPageAuditCompleteEmail(account.getEmail(), page.getUrl(), page_audit_record.getId());
							}
						}
					} catch(Exception e) {
						e.printStackTrace();
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
	
	private boolean isAestheticsAuditComplete(Set<Audit> audits) {
		return audits.size() == 3;
	}

	private boolean isContentAuditComplete(Set<Audit> allContentAudits) {
		return allContentAudits.size() == 3;
	}
	
	private boolean isInformationArchitectureAuditComplete(Set<Audit> audits) {
		return audits.size() == 3;
	}

	private long getAuditCount(AuditCategory content, Set<PageAuditRecord> audit_records) {
		for(AuditRecord audit_record : audit_records) {
		}
		
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

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
import com.looksee.models.Domain;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.AuditStats;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditStage;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.CrawlAction;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.message.DomainAuditMessage;
import com.looksee.models.message.PageAuditRecordMessage;
import com.looksee.models.message.PageStateAuditComplete;
import com.looksee.models.message.PageStateMessage;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.DomainService;
import com.looksee.services.SendGridMailService;
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
					if(!page_states_experienced.containsKey(page_state_msg.getPageState().getKey()) 
							&& page_state_msg.getPageState().getHttpStatus() != 404) {
						Optional<AuditRecord> record = audit_record_service.findById(page_state_msg.getAuditRecordId());
				 		long content_audits_complete = 0;
			    		long info_arch_audits_complete = 0;
			    		long aesthetic_audits_complete = 0;
			    		
			    		double content_score = 0.0;
			    		double written_content_score = 0.0;
			    		double imagery_score = 0.0;
			    		double videos_score = 0.0;
			    		double audio_score = 0.0;
			    		
			    		double info_arch_score = 0.0;
			    		double seo_score = 0.0;
			    		double menu_analysis_score = 0.0;
			    		double performance_score = 0.0;
			    		
			    		double aesthetic_score = 0.0;
			    		double color_score = 0.0;
			    		double typography_score = 0.0;
			    		double whitespace_score = 0.0;
			    		double branding_score = 0.0;
			    		
						if( record.isPresent() ) {
							if(record.get() instanceof DomainAuditRecord) {								
								DomainAuditRecord audit_record = (DomainAuditRecord)record.get();
								
								Set<PageAuditRecord> audit_records = audit_record_service.getPageAuditRecords(page_state_msg.getAuditRecordId());
								//get Page Count
								long page_count = audit_records.size();
								
								for(PageAuditRecord page_audit : audit_records) {
									Set<Audit> content_audits = audit_record_service.getAllContentAudits(page_audit.getId());
									content_score = AuditUtils.calculateScore(content_audits);
									written_content_score = AuditUtils.calculateSubcategoryScore(content_audits, AuditSubcategory.WRITTEN_CONTENT);
									imagery_score = AuditUtils.calculateSubcategoryScore(content_audits, AuditSubcategory.IMAGERY);
									videos_score = AuditUtils.calculateSubcategoryScore(content_audits, AuditSubcategory.VIDEOS);
									audio_score = AuditUtils.calculateSubcategoryScore(content_audits, AuditSubcategory.AUDIO);
									//get total content audit pages
									boolean is_content_audit_complete = isContentAuditComplete(content_audits); // getContentAudit(audit_record.getId(), page_state_msg.getAuditRecordId()).size();//getAuditCount(AuditCategory.CONTENT, audit_records);
									if(is_content_audit_complete) {
										content_audits_complete++;
									}
									
									Set<Audit> info_architecture_audits = audit_record_service.getAllContentAudits(page_audit.getId());
									info_arch_score = AuditUtils.calculateScore(info_architecture_audits);
									seo_score = AuditUtils.calculateSubcategoryScore(info_architecture_audits, AuditSubcategory.SEO);
									menu_analysis_score = AuditUtils.calculateSubcategoryScore(info_architecture_audits, AuditSubcategory.MENU_ANALYSIS);
									performance_score = AuditUtils.calculateSubcategoryScore(info_architecture_audits, AuditSubcategory.PERFORMANCE);
									//get total information architecture audit pages
									boolean is_info_arch_audit_complete = isInformationArchitectureAuditComplete(info_architecture_audits);
									if(is_info_arch_audit_complete) {
										info_arch_audits_complete++;
									}
									
									Set<Audit> aesthetics_audits = audit_record_service.getAllContentAudits(page_audit.getId());
									aesthetic_score = AuditUtils.calculateScore(aesthetics_audits);
									color_score = AuditUtils.calculateSubcategoryScore(aesthetics_audits, AuditSubcategory.COLOR_MANAGEMENT);
									typography_score = AuditUtils.calculateSubcategoryScore(aesthetics_audits, AuditSubcategory.TYPOGRAPHY);
									whitespace_score = AuditUtils.calculateSubcategoryScore(aesthetics_audits, AuditSubcategory.WHITESPACE);
									branding_score = AuditUtils.calculateSubcategoryScore(aesthetics_audits, AuditSubcategory.BRANDING);

									//get total aesthetic audit pages
									boolean is_aesthetic_audit_complete = isAestheticsAuditComplete(aesthetics_audits);
									if(is_aesthetic_audit_complete) {
										aesthetic_audits_complete++;
									}
								}
								
								//build stats object
								AuditStats audit_stats = new AuditStats(audit_record.getId(), 
																		audit_record.getStartTime(), 
																		audit_record.getEndTime(), 
																		page_count, 
																		content_audits_complete,
																		audit_record.getContentAuditProgress(),
																		content_score,
																		audit_record.getContentAuditMsg(), 
																		written_content_score,
																		imagery_score,
																		videos_score,
																		audio_score,
																		info_arch_audits_complete, 
																		audit_record.getInfoArchAuditProgress(), 
																		info_arch_score, 
																		audit_record.getInfoArchMsg(), 
																		seo_score,
																		menu_analysis_score,
																		performance_score,
																		aesthetic_audits_complete,
																		audit_record.getAestheticAuditProgress(),
																		aesthetic_score,
																		audit_record.getAestheticMsg(),
																		color_score,
																		typography_score,
																		whitespace_score,
																		branding_score);
								
								MessageBroadcaster.sendAuditStatUpdate(page_state_msg.getAccountId(), audit_stats);
							}		
						}
						
						page_states_experienced.put(page_state_msg.getPageState().getKey(), page_state_msg.getPageState());
						/*
						ActorRef journeyMapper = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("journeyMappingManager"), "journeyMappingManager"+UUID.randomUUID());
						journeyMapper.tell(new URL(page_state.getUrl()), getSelf());
						*/
						
						/**
						 * NOTE: Performance is disabled because of issue with pages that are not found
						 * 
				   		ActorRef insight_auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								.props("performanceAuditor"), "performanceAuditor"+UUID.randomUUID());
						
						//rendered_page_state_count++;
				   		insight_auditor.tell(page_state_msg.getPageState(), getSelf());
						
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
					
					//find user account
					if( pages.size() == page_states.size()) {
						Account account = account_service.findById(audit_complete.getAccountId()).get();
						//send domain audit complete
						mail_service.sendDomainAuditCompleteEmail(account.getEmail(), domain.getUrl(), domain.getId());
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

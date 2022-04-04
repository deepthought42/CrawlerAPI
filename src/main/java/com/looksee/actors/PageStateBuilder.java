package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.ColorData;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.BrowserCrawlActionMessage;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.message.ElementExtractionError;
import com.looksee.models.message.ElementExtractionMessage;
import com.looksee.models.message.ElementProgressMessage;
import com.looksee.models.message.ElementsSaveError;
import com.looksee.models.message.ElementsSaved;
import com.looksee.models.message.PageAuditRecordMessage;
import com.looksee.models.message.PageCrawlActionMessage;
import com.looksee.models.message.PageDataExtractionError;
import com.looksee.models.message.PageDataExtractionMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.PageStateService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ImageUtils;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberExited;
import akka.cluster.ClusterEvent.MemberLeft;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

@Component
@Scope("prototype")
public class PageStateBuilder extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(PageStateBuilder.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	private long total_dispatches = 0;
	private long total_dispatch_responses = 0;
	private long total_save_dispatches = 0;

	
	private List<String> xpaths;
	private PageState page_state;
	//private Map<String, Integer> total_dispatches;
	
	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ActorSystem actor_system;
	
	
	//subscribe to cluster changes
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
				MemberEvent.class, UnreachableMember.class);
		this.xpaths = new ArrayList<>();
		this.total_dispatches = 0L;
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
	 * 
	 * PageCrawlActionMessage - Extracts data for individual web page
	 * 		Emits Events -
	 * 				PageDataExtractionMessage - used for tracking completion of page data extraction
	 * 
	 * @throws NullPointerException
	 * @throws IOException
	 * @throws NoSuchElementException
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(PageCrawlActionMessage.class, crawl_action-> {
					try {						
						int http_status = BrowserUtils.getHttpStatus(crawl_action.getUrl());
	
						//usually code 301 is returned which is a redirect, which is usually transferring to https
						if(http_status == 404 || http_status == 408) {
							log.warn("Recieved 404 status for link :: "+crawl_action.getUrl());
							//send message to audit manager letting it know that an error occurred
							PageDataExtractionError extraction_tracker = new PageDataExtractionError(crawl_action.getDomainId(), 
													 crawl_action.getAccountId(), 
													 crawl_action.getAuditRecordId(), 
													 crawl_action.getUrl().toString(), 
													 "Received "+http_status+" status while building page state "+crawl_action.getUrl());

							getContext().getParent().tell(extraction_tracker, getSelf());
							return;
						}
						
						//update audit record with progress
						PageState page_state = browser_service.buildPageState(crawl_action.getUrl()); 
						
						final PageState page_state_record = page_state_service.save(page_state);
						List<String> xpaths = browser_service.extractAllUniqueElementXpaths(page_state_record.getSrc());

						int XPATH_PARTITIONS = 3; // this is meant to replace XPATH_CHUNK_SIZE
						int XPATH_CHUNK_SIZE = (int)Math.ceil( xpaths.size() / (double)XPATH_PARTITIONS );
						this.total_dispatches = 0L;
						this.xpaths.addAll(xpaths);
						
						audit_record_service.addPageToAuditRecord(crawl_action.getAuditRecord().getId(), page_state_record.getId());
						//crawl_action.getAuditRecord().setPageState(page_state_record);
						
					   	int start_xpath_index = 0;
					   	int last_xpath_index = 0;
						List<List<String>> xpath_lists = new ArrayList<>();

						while(start_xpath_index < (xpaths.size()-1)) {
					   		last_xpath_index = (start_xpath_index + XPATH_CHUNK_SIZE);
					   		if(last_xpath_index >= xpaths.size()) {
					   			last_xpath_index = xpaths.size()-1;
					   		}
					   		List<String> xpath_subset = xpaths.subList(start_xpath_index, last_xpath_index);
					   		xpath_lists.add(xpath_subset);
						   
					   		ElementExtractionMessage element_extraction_msg = 
						   								new ElementExtractionMessage(crawl_action.getAccountId(), 
						   															 page_state_record, 
						   															 crawl_action.getAuditRecordId(), 
						   															 xpath_subset, 
						   															 crawl_action.getDomainId());
							ActorRef element_extractor = getContext().actorOf(SpringExtProvider.get(actor_system)
						   			.props("elementStateExtractor"), "elementStateExtractor"+UUID.randomUUID());
		
							element_extractor.tell(element_extraction_msg, getSelf());					
						
							//log.warn("Element state list length   =   "+elements.size());
							//page_state_record.addElements(elements);
							start_xpath_index = last_xpath_index;
					   	}
						
					}catch(Exception e) {
						PageDataExtractionError extraction_tracker = new PageDataExtractionError(crawl_action.getDomainId(), 
																								 crawl_action.getAccountId(), 
																								 crawl_action.getAuditRecordId(), 
																								 crawl_action.getUrl().toString(), 
																								 "An exception occurred while building page state "+crawl_action.getUrl()+".\n"+e.getMessage());

						getContext().getParent().tell(extraction_tracker, getSelf());

						log.error("An exception occurred that bubbled up to the page state builder");
						e.printStackTrace();
					}
				})
				.match(BrowserCrawlActionMessage.class, crawl_action-> {
					log.warn("Page state recieved CrawlActionMessage");
					try {						
						int http_status = BrowserUtils.getHttpStatus(crawl_action.getUrl());
						boolean requires_authentication = false;
						//usually code 301 is returned which is a redirect, which is usually transferring to https
						if(http_status == 404 || http_status == 408) {
							log.warn("Recieved 404 status for link :: "+crawl_action.getUrl());
							//send message to audit manager letting it know that an error occurred
							PageDataExtractionError extraction_tracker = new PageDataExtractionError(crawl_action.getDomainId(), 
													 crawl_action.getAccountId(), 
													 crawl_action.getAuditRecordId(), 
													 crawl_action.getUrl().toString(), 
													 "Received "+http_status+" status while building page state "+crawl_action.getUrl());

							getContext().getParent().tell(extraction_tracker, getSelf());
							return;
						}
						else if(http_status == 401) {
							requires_authentication = true;
						}
						else if(http_status == 403) {
							
						}
						
						//update audit record with progress
						//this.page_state = browser_service.buildPageState(crawl_action.getUrl()); 
						this.page_state = browser_service.performBuildPageProcess(crawl_action.getUrl(), crawl_action.getBrowser()); 

						this.page_state = page_state_service.save(this.page_state);
						final PageState page_state_record = this.page_state;
						List<String> xpaths = browser_service.extractAllUniqueElementXpaths(page_state_record.getSrc());

						int XPATH_PARTITIONS = 3; // this is meant to replace XPATH_CHUNK_SIZE
						int XPATH_CHUNK_SIZE = (int)Math.ceil( xpaths.size() / (double)XPATH_PARTITIONS );
						this.total_dispatches = 0L;
						this.xpaths.addAll(xpaths);
						
						audit_record_service.addPageToAuditRecord(crawl_action.getAuditRecordId(), page_state_record.getId());
						//crawl_action.getAuditRecord().setPageState(page_state_record);
						
						List<ElementState> element_states = browser_service.buildPageElementsWithoutNavigation( page_state, 
																												xpaths,
																												crawl_action.getAuditRecordId(),
																												page_state.getFullPageHeight(),
																												crawl_action.getBrowser());


						//ENRICHMENT : BACKGROUND COLORS
						element_states = element_states.parallelStream()
														.filter(element -> element != null)
														.map(element -> {
								try {
									ColorData font_color = new ColorData(element.getRenderedCssValues().get("color"));				
									//extract opacity color
									ColorData bkg_color = null;
									if(element.getScreenshotUrl().trim().isEmpty()) {
									bkg_color = new ColorData(element.getRenderedCssValues().get("background-color"));
									}
									else {
									//log.warn("extracting background color");
									bkg_color = ImageUtils.extractBackgroundColor( new URL(element.getScreenshotUrl()),
																   font_color);
									
									//log.warn("done extracting background color");
									}
									String bg_color = bkg_color.rgb();	
									
									//Identify background color by getting largest color used in picture
									//ColorData background_color_data = ImageUtils.extractBackgroundColor(new URL(element.getScreenshotUrl()));
									ColorData background_color = new ColorData(bg_color);
									element.setBackgroundColor(background_color.rgb());
									element.setForegroundColor(font_color.rgb());
									
									double contrast = ColorData.computeContrast(background_color, font_color);
									element.setTextContrast(contrast);
									return element;
								}
								catch (Exception e) {
									log.warn("element screenshot url  :: "+element.getScreenshotUrl());
									e.printStackTrace();
								}
							return element;
						})
						.collect(Collectors.toList());
						
						
						/*
					   	int start_xpath_index = 0;
					   	int last_xpath_index = 0;
						List<List<String>> xpath_lists = new ArrayList<>();

						while(start_xpath_index < (xpaths.size()-1)) {
					   		last_xpath_index = (start_xpath_index + XPATH_CHUNK_SIZE);
					   		if(last_xpath_index >= xpaths.size()) {
					   			last_xpath_index = xpaths.size()-1;
					   		}
					   		List<String> xpath_subset = xpaths.subList(start_xpath_index, last_xpath_index);
					   		xpath_lists.add(xpath_subset);
						   
					   		ElementExtractionMessage element_extraction_msg = 
						   								new ElementExtractionMessage(crawl_action.getAccountId(), 
						   															 page_state_record, 
						   															 crawl_action.getAuditRecordId(), 
						   															 xpath_subset, 
						   															 crawl_action.getDomainId());
							ActorRef element_extractor = getContext().actorOf(SpringExtProvider.get(actor_system)
						   			.props("elementStateExtractor"), "elementStateExtractor"+UUID.randomUUID());
		
							element_extractor.tell(element_extraction_msg, getSelf());					
						
							this.total_dispatches++;
							//log.warn("Element state list length   =   "+elements.size());
							//page_state_record.addElements(elements);
							start_xpath_index = last_xpath_index;
					   	}
						*/
						/*
						PageDataExtractionMessage extraction_tracker = new PageDataExtractionMessage(crawl_action.getDomainId(), 
																									 crawl_action.getAccountId(), 
																									 crawl_action.getAuditRecordId(), 
																									 page_state.getUrl(), 
																									 xpath_lists.size());
						
						getContext().getParent().tell(extraction_tracker, getSelf());
						*/
					}catch(Exception e) {
						PageDataExtractionError extraction_tracker = new PageDataExtractionError(crawl_action.getDomainId(), 
																								 crawl_action.getAccountId(), 
																								 crawl_action.getAuditRecordId(), 
																								 crawl_action.getUrl().toString(), 
																								 "An exception occurred while building page state "+crawl_action.getUrl()+".\n"+e.getMessage());

						getContext().getParent().tell(extraction_tracker, getSelf());

						log.error("An exception occurred that bubbled up to the page state builder");
						e.printStackTrace();
					}
				})
				.match(ElementProgressMessage.class, message -> {
					log.warn("sending elements to be saved");
					ActorRef data_extraction_supervisor = getContext().actorOf(SpringExtProvider.get(actor_system)
							.props("dataExtractionSupervisor"), "dataExtractionSupervisor"+UUID.randomUUID());
					data_extraction_supervisor.tell(message, getSelf());
				})
				.match(ElementExtractionError.class, message -> {
					log.warn("error extracting elements");
					long response_count = this.total_dispatch_responses++;
										
					log.warn("an error occurred during element extraction   "+message.getPageUrl());
					try {
						//TODO : add ability to track progress of elements mapped within the xpaths and to tell when the 
						//       system is done extracting element data and the page is ready for auditing
						
						PageAuditRecord audit_record = (PageAuditRecord)audit_record_service.findById(message.getAuditRecordId()).get();
						if(response_count == this.total_dispatches) {
							audit_record.setStatus(ExecutionStatus.RUNNING_AUDITS);
							audit_record.setDataExtractionMsg("Done!");
							audit_record.setDataExtractionProgress(1.0);
							audit_record.setAestheticAuditProgress(1/100.0);
							audit_record.setAestheticMsg("Starting visual design audit");
	
							audit_record.setContentAuditProgress(1/100.0);
							audit_record.setContentAuditMsg("Starting content audit");
	
							audit_record.setInfoArchitectureAuditProgress(1/100.0);
							audit_record.setInfoArchMsg("Starting Information Architecture audit");
							audit_record.setElementsReviewed(this.total_dispatch_responses );
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
							audit_record.setDataExtractionMsg("Error Extracting elements in batch "+this.total_dispatch_responses + " / "+this.total_dispatches);
							audit_record.setDataExtractionProgress(this.total_dispatch_responses / (double)this.total_dispatches);
							audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());
						}
					}catch(Exception e) {
						log.error("Exception occurred while page state builder processed ElementProgressMessage!!");
						e.printStackTrace();
					}
				})
				.match(ElementsSaved.class, message -> {
					this.total_save_dispatches++;
					log.warn("Elements saved successfully :: batch "+this.total_save_dispatches + " of "+this.total_dispatches);
					
					page_state_service.addAllElements(message.getPageStateId(), message.getElements());
					this.page_state.setElements(page_state_service.getElementStates(this.page_state.getId()));
					try {
						//TODO : add ability to track progress of elements mapped within the xpaths and to tell when the 
						//       system is done extracting element data and the page is ready for auditing
						
						
						//PageAuditRecord audit_record = (PageAuditRecord)audit_record_service.findById(message.getAuditRecordId()).get();
						if(this.total_save_dispatches == this.total_dispatches) {
							log.warn("all dispatches have been processed!!!!");
							/*NOTE: THIS LOGIC SHOULD BE HANDLED IN THE AUDIT MANAGER. MOVE IT THERE ONCE IT'S CLEAR WHERE IT BELONGS */
							/*
							audit_record.setStatus(ExecutionStatus.RUNNING_AUDITS);

							audit_record.setDataExtractionMsg("Done!");
							audit_record.setDataExtractionProgress(1.0);
							audit_record.setAestheticAuditProgress(1/100.0);
							audit_record.setAestheticMsg("Starting visual design audit");
	
							audit_record.setContentAuditProgress(1/100.0);
							audit_record.setContentAuditMsg("Starting content audit");
	
							audit_record.setInfoArchitectureAuditProgress(1/100.0);
							audit_record.setInfoArchMsg("Starting Information Architecture audit");
							audit_record.setElementsReviewed(this.total_dispatch_responses );
							audit_record = (PageAuditRecord) audit_record_service.save(audit_record, 
																						message.getAccountId(), 
																						message.getDomainId());
							*/
							//send page audit record to design system extractor
							/*
							if(message.getDomainId() >= 0) {
								ActorRef design_system_extractor = getContext().actorOf(SpringExtProvider.get(actor_system)
							   			.props("designSystemExtractor"), "designSystemExtractor"+UUID.randomUUID());
								log.warn("sending message to design system extractor ....");
								PageAuditRecordMessage page_audit_msg = new PageAuditRecordMessage( audit_record.getId(), 
																									message.getDomainId(), 
																									message.getAccountId(), 
																									message.getAuditRecordId());
								design_system_extractor.tell(page_audit_msg, getSelf());
							}
							*/
							/*
						   	log.warn("requesting performance audit from performance auditor....");
						   	ActorRef performance_insight_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
						   			.props("performanceAuditor"), "performanceAuditor"+UUID.randomUUID());
						   	performance_insight_actor.tell(page_state, getSelf());
						   	*/
							log.warn("sending PageDataExtraction to parent actor :: "+getContext().getParent().getClass());
							PageDataExtractionMessage extraction_tracker = new PageDataExtractionMessage(message.getDomainId(), 
																										 message.getAccountId(), 
																										 message.getAuditRecordId(), 
																										 message.getPageUrl(), 
																										 this.xpaths.size(),
																										 this.page_state);
							
							getContext().getParent().tell(extraction_tracker, getSelf());
							
						}
						else {
							log.warn("total save dispatches does not match total dispatch responses");
							/*NOTE: THIS LOGIC SHOULD BE HANDLED IN THE AUDIT MANAGER. MOVE IT THERE ONCE IT'S CLEAR WHERE IT BELONGS */
							/*
							audit_record.setDataExtractionMsg("Elements saved successfully - batch "+this.total_dispatch_responses + " / "+this.total_dispatches);
							audit_record.setDataExtractionProgress(this.total_dispatch_responses / (double)this.total_dispatches);
							audit_record.setElementsReviewed(this.total_dispatch_responses);
							audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());
							*/
						}
					}catch(Exception e) {
						log.error("Exception occurred while page state builder processed ElementProgressMessage!!");
						e.printStackTrace();
					}
				})
				.match(ElementsSaveError.class, message -> {
					this.total_save_dispatches++;
					log.warn("error saving elements");
					AuditRecord audit_record = audit_record_service.findById(message.getAuditRecordId()).get();
					audit_record.setDataExtractionMsg("Error Saving elements "+this.total_dispatch_responses + " / "+this.total_dispatches);
					
					double responses = (double)this.total_dispatch_responses;
					double dispatches = (double)this.total_dispatches;
					audit_record.setDataExtractionProgress( responses / dispatches);
					audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());
				})
				.match(AuditProgressUpdate.class, message -> {
					getContext().parent().forward(message, getContext());
				})
				.match(ElementExtractionError.class, message -> {
					getContext().parent().forward(message, getContext());
				})
				.match(MemberUp.class, mUp -> {
					log.info("Member is Up: {}", mUp.member());
				})
				.match(UnreachableMember.class, mUnreachable -> {
					log.info("Member detected as unreachable: {}", mUnreachable.member());
				})
				.match(MemberRemoved.class, mRemoved -> {
					log.info("Member is Removed: {}", mRemoved.member());
				})
				.match(MemberLeft.class, mRemoved -> {
					log.info("Member Left cluster: {}", mRemoved.member());
				})
				.match(MemberExited.class, mRemoved -> {
					log.info("Member exited: {}", mRemoved.member());
				})
				.matchAny(o -> {
					log.info("received unknown message of type :: "+o.getClass().getName());
				})
				.build();
	}
}

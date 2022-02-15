package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.PageState;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.ElementExtractionError;
import com.looksee.models.message.ElementExtractionMessage;
import com.looksee.models.message.ElementProgressMessage;
import com.looksee.models.message.PageCrawlActionMessage;
import com.looksee.models.message.PageDataExtractionError;
import com.looksee.models.message.PageDataExtractionMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.PageStateService;
import com.looksee.utils.BrowserUtils;

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
	
	private Map<String, Integer> total_xpaths;
	private Map<String, Integer> total_dispatches;
	
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
		this.total_xpaths = new HashMap<>();
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
						this.total_dispatches.put(page_state.getUrl(), 0);
						this.total_xpaths.put(page_state.getUrl(), xpaths.size());
						
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
						
						PageDataExtractionMessage extraction_tracker = new PageDataExtractionMessage(crawl_action.getDomainId(), 
																									 crawl_action.getAccountId(), 
																									 crawl_action.getAuditRecordId(), 
																									 page_state.getUrl(), 
																									 xpath_lists.size());
						
						getContext().getParent().tell(extraction_tracker, getSelf());
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
					message.setTotalXpaths(this.total_xpaths.get(message.getPageUrl()));
					message.setTotalDispatches(this.total_dispatches.get(message.getPageUrl()));
					getContext().parent().forward(message, getContext());
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

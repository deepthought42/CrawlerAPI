package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.PageState;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.message.ElementExtractionMessage;
import com.looksee.models.message.ElementProgressMessage;
import com.looksee.models.message.PageCrawlActionMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.PageStateService;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

@Component
@Scope("prototype")
public class PageStateBuilder extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(PageStateBuilder.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());

	private long total_dispatches;
	private long total_dispatch_responses;
	
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
				.match(PageCrawlActionMessage.class, crawl_action-> {
					log.warn("building page state in page state builder actor ....");
					

					//update audit record with progress
					
					PageState page_state = browser_service.buildPageState(crawl_action.getUrl(), crawl_action.getAuditRecord());
					final PageState page_state_record = page_state_service.save(page_state);

					AuditRecord audit_record = audit_record_service.findById(crawl_action.getAuditRecordId()).get();
					audit_record.setDataExtractionProgress(1.0/3.0);
					audit_record = audit_record_service.save(audit_record);
					audit_record_service.addPageToAuditRecord(crawl_action.getAuditRecord().getId(), page_state_record.getId());
					//crawl_action.getAuditRecord().setPageState(page_state_record);
				   	
				   	List<String> xpaths = browser_service.extractAllUniqueElementXpaths(page_state_record.getSrc());
				   	int start_xpath_index = 0;
				   	int last_xpath_index = 0;
					List<List<String>> xpath_lists = new ArrayList<>();

				   //	List<CompletableFuture<Void>> futures_list = new ArrayList<>();
					total_dispatches = 0;
					total_dispatch_responses = 0;
					
					int XPATH_CHUNK_SIZE = 100;
				   	while(start_xpath_index < (xpaths.size()-1)) {
				   		last_xpath_index = (start_xpath_index + XPATH_CHUNK_SIZE);
				   		if(last_xpath_index >= xpaths.size()) {
				   			last_xpath_index = xpaths.size()-1;
				   		}
				   		List<String> xpath_subset = xpaths.subList(start_xpath_index, last_xpath_index);
				   		xpath_lists.add(xpath_subset);
					   
				   		ElementExtractionMessage element_extraction_msg = 
					   								new ElementExtractionMessage(page_state_record, 
					   															 crawl_action.getAuditRecord(), 
					   															 xpath_subset);
						log.warn("Sending page for element extraction.....");
						ActorRef element_extractor = actor_system.actorOf(SpringExtProvider.get(actor_system)
					   			.props("elementStateExtractor"), "elementStateExtractor"+UUID.randomUUID());
	
						element_extractor.tell(element_extraction_msg, getSelf());					
					
						//log.warn("Element state list length   =   "+elements.size());
						//page_state_record.addElements(elements);
						total_dispatches++;
						start_xpath_index = last_xpath_index;
				   	}
				})
				.match(ElementProgressMessage.class, message -> {
					log.warn("recieved element progress message update");
					this.total_dispatch_responses++;
					log.warn("dispatch responses ....   "+this.total_dispatch_responses);
					log.warn("total dispatched... "+ this.total_dispatches);
					
					AuditRecord audit_record = audit_record_service.findById(message.getAuditRecordId()).get();
					audit_record.setDataExtractionProgress((double)total_dispatch_responses/(double)total_dispatches);
					audit_record = audit_record_service.save(audit_record);
					//TODO : add ability to track progress of elements mapped within the xpaths and to tell when the 
					//       system is done extracting element data and the page is ready for auditing
					
					if(this.total_dispatch_responses == this.total_dispatches) {
						log.warn("ALL PAGE ELEMENT STATES HAVE BEEN MAPPED SUCCESSFULLY!!!!!");
						audit_record = audit_record_service.findById(message.getAuditRecordId()).get();
						audit_record.setDataExtractionProgress(1.0);
						audit_record = audit_record_service.save(audit_record);
					/*
				   	log.warn("requesting performance audit from performance auditor....");
				   	ActorRef performance_insight_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
				   			.props("performanceAuditor"), "performanceAuditor"+UUID.randomUUID());
				   	performance_insight_actor.tell(page_state, ActorRef.noSender());
				   	*/
				   						
					   	log.warn("Running information architecture audit via actor");
						ActorRef content_auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
					   			.props("contentAuditor"), "contentAuditor"+UUID.randomUUID());
						content_auditor.tell(audit_record, getSelf());
					   	
					   	log.warn("Running information architecture audit via actor");
						ActorRef info_architecture_auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
					   			.props("informationArchitectureAuditor"), "informationArchitectureAuditor"+UUID.randomUUID());
						info_architecture_auditor.tell(audit_record, getSelf());
						
						log.warn("Running aesthetic audit via actor");
						ActorRef aesthetic_auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
					   			.props("aestheticAuditor"), "aestheticAuditor"+UUID.randomUUID());
						aesthetic_auditor.tell(audit_record, getSelf());
						
						postStop();
					}
					
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
				.matchAny(o -> {
					log.info("received unknown message of type :: "+o.getClass().getName());
				})
				.build();
	}
}

package com.looksee.actors;


import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.Domain;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.message.ElementExtractionMessage;
import com.looksee.models.message.PageCrawlActionMessage;
import com.looksee.models.message.PageStateMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainService;
import com.looksee.services.PageStateService;
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
 * Web Crawler actor that can handle crawling a website in various ways. 
 * 
 *  1. Links only crawling ( captures links and finds pages) - CrawlAction
 *  2. Interaction crawl (crawl a specific page and look for element interactions)
 *  3. Journey crawl (explore a domain in browser as a "user" and record journey paths)
 *  
 */
@Component
@Scope("prototype")
public class WebCrawlerActor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(WebCrawlerActor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
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
				.match(CrawlActionMessage.class, crawl_action-> {
					/* perform site wide crawl */
					Domain domain = domain_service.findById(crawl_action.getDomainId()).get();
					String initial_url = domain.getUrl();

					Map<String, PageState> pages = new HashMap<>();
					Map<String, Boolean> frontier = new HashMap<>();
					Map<String, PageState> visited = new HashMap<>();
											
					//add link to frontier
					frontier.put(initial_url, Boolean.TRUE);
					
					while(!frontier.isEmpty()) {
						Map<String, Boolean> external_links = new HashMap<>();
						//remove link from beginning of frontier
						String raw_url = frontier.keySet().iterator().next();
						URL sanitized_url = new URL(BrowserUtils.sanitizeUrl(raw_url));
						String page_url_str = BrowserUtils.getPageUrl(sanitized_url);
						
						frontier.remove(raw_url);
						if(visited.containsKey(page_url_str)) {
							continue;
						}
						
						if( BrowserUtils.isImageUrl(page_url_str) 
								|| page_url_str.endsWith(".pdf")
								|| !page_url_str.contains(domain.getUrl())){
							log.warn("is url and image url ??  "+ BrowserUtils.isImageUrl(page_url_str));
							log.warn("does url end with .pdf??   ::  "+ page_url_str.endsWith(".pdf"));
							log.warn("is url in visited array???    "+visited.containsKey(page_url_str));
							log.warn("contains domain url?  :: "+ page_url_str.contains(domain.getUrl()));
							log.warn("WebCrawler skipping url :: "+page_url_str);
							visited.compute(page_url_str, null);
							continue;
						}

						URL page_url_obj = new URL(BrowserUtils.sanitizeUrl(page_url_str));
						//construct page and add page to list of page states
						//retrieve html source for page
						try {
							PageState page_state = browser_service.buildPageState(page_url_obj, crawl_action.getAuditRecord());
							page_state = page_state_service.save(page_state);
							
							AuditRecord audit_record = audit_record_service.findById(crawl_action.getAuditRecordId()).get();
							audit_record.setDataExtractionProgress(1.0/3.0);
							audit_record = audit_record_service.save(audit_record);
							
						   	List<String> xpaths = browser_service.extractAllUniqueElementXpaths(page_state.getSrc());
						   	audit_record = audit_record_service.findById(crawl_action.getAuditRecordId()).get();
							audit_record.setDataExtractionProgress(2.0/3.0);
							audit_record = audit_record_service.save(audit_record);
							
							List<ElementState> elements = browser_service.buildPageElements(page_state, 
																							xpaths,
																							audit_record.getId());
							page_state.addElements(elements);
							crawl_action.getAuditRecord().setDataExtractionProgress(3.0/3.0);
							audit_record_service.save(crawl_action.getAuditRecord());
							
							log.warn("http status =  "+page_state.getHttpStatus());
							pages.put(page_state.getKey(), page_state);
							domain_service.addPage(domain.getId(), page_state.getKey());
							
							if(page_state.getHttpStatus() == 404) {
								continue;
							}
							
							//send message to page data extractor
							
							PageStateMessage page_msg = new PageStateMessage(page_state, crawl_action.getDomainId(), crawl_action.getAccountId(), crawl_action.getAuditRecordId());
							//Send PageVerstion to audit manager
							getSender().tell(page_msg, getSelf());
							
							Document doc = Jsoup.parse(page_state.getSrc());
							Elements links = doc.select("a");
							
							//iterate over links and exclude external links from frontier
							for (Element link : links) {
								String href_str = link.attr("href");
								href_str = href_str.replaceAll(";", "");

								if(!link.attr("href").isEmpty() && href_str.isEmpty()) {
									href_str = domain.getUrl() + link.attr("href");
								}
								if(href_str == null || href_str.isEmpty()) {
									continue;
								}

								try {
									URL href_url = new URL( BrowserUtils.sanitizeUrl(href_str));
									String href = BrowserUtils.getPageUrl(href_url);
									boolean isExternalLink = BrowserUtils.isExternalLink(domain.getUrl().replace("www.", ""), href);
									//check if external link
									if( isExternalLink || href.startsWith("mailto:")) {
										log.warn("adding to external links :: "+href);
					   					external_links.put(href, Boolean.TRUE);
									}
									else if( !visited.containsKey(href) 
											&& !frontier.containsKey(href) 
											&& !isExternalLink
									){
										log.warn("adding to internal links :: "+href);
	
										//add link to frontier
										frontier.put(href, Boolean.TRUE);
									}
								}
								catch(MalformedURLException e) {
									log.warn("malformed href value ....  "+href_str);
									e.printStackTrace();
								}
							}
							visited.put(page_url_str, page_state);
							
						}catch(SocketTimeoutException e) {
							log.warn("Error occurred while navigating to :: "+page_url_str);
						}
						catch(HttpStatusException e) {
							log.warn("HTTP Status Exception occurred while navigating to :: "+page_url_str);
							e.printStackTrace();
							visited.put(page_url_str, null);
						}
						catch(IllegalArgumentException e) {
							log.warn("illegal argument exception occurred when connecting to ::  " + page_url_str);
							e.printStackTrace();
						}
						catch(UnsupportedMimeTypeException e) {
							log.warn(e.getMessage() + " : " +e.getUrl());
						}
					}
				})
				.match(PageCrawlActionMessage.class, crawl_action-> {
					
					ActorRef page_state_builder = actor_system.actorOf(SpringExtProvider.get(actor_system)
				   			.props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());

					page_state_builder.tell(crawl_action, getSelf());					
				
					
				   	/*
				   	
					List<List<? extends Object>> element_lists = xpath_lists.parallelStream()
												   .map(xpath_list -> {
												   		try {
															return browser_service.buildPageElements(page_state, xpath_list);
															//page_state.addElements(elements);
														} catch (MalformedURLException e) {
															// TODO Auto-generated catch block
															e.printStackTrace();
														}
												   		return new ArrayList<>();
												   	})
												   	.collect(Collectors.toList());
					
					//unroll element lists
					List<ElementState> elements = new ArrayList<>();
					for(List<? extends Object> element_list: element_lists) {
						for(Object element_obj : element_list) {
							elements.add((ElementState)element_obj);
							page_state_service.addElement(page_state.getId(), ((ElementState)element_obj).getKey());
						}
					}
					
					log.warn("element list size : "+elements.size());
					page_state.addElements(elements);
					*/
			   		//List<ElementState> elements = browser_service.buildPageElements(page_state, crawl_action.getAuditRecord(), xpaths);
					//update audit record with progress
					/*
					AuditRecord audit_record = audit_record_service.findById(crawl_action.getAuditRecordId()).get();
					audit_record.setDataExtractionProgress(3.0/3.0);
					audit_record_service.save(audit_record);
					*/
					//domain.addPage(page);
					//domain_service.addPage(domain.getId(), page_state.getKey());
					
				   	//check if page state already
				   	//perform audit and return audit result
				   	log.warn("?????????????????????????????????????????????????????????????????????");
				   	log.warn("?????????????????????????????????????????????????????????????????????");
				   	log.warn("?????????????????????????????????????????????????????????????????????");
				   	
				   	/*
				   	log.warn("requesting performance audit from performance auditor....");
				   	ActorRef performance_insight_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
				   			.props("performanceAuditor"), "performanceAuditor"+UUID.randomUUID());
				   	performance_insight_actor.tell(page_state, ActorRef.noSender());
				   	*/
				   	/*
				   	log.warn("Running information architecture audit via actor");
					ActorRef content_auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
				   			.props("contentAuditor"), "contentAuditor"+UUID.randomUUID());
					content_auditor.tell(crawl_action.getAuditRecord(), getSelf());
				   	
				   	log.warn("Running information architecture audit via actor");
					ActorRef info_architecture_auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
				   			.props("informationArchitectureAuditor"), "informationArchitectureAuditor"+UUID.randomUUID());
					info_architecture_auditor.tell(crawl_action.getAuditRecord(), getSelf());
					
					log.warn("Running aesthetic audit via actor");
					ActorRef aesthetic_auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
				   			.props("aestheticAuditor"), "aestheticAuditor"+UUID.randomUUID());
					aesthetic_auditor.tell(crawl_action.getAuditRecord(), getSelf());
					*/
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

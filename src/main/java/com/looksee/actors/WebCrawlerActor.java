package com.looksee.actors;


import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

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
import com.looksee.models.PageState;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.message.PageStateMessage;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainService;
import com.looksee.services.PageStateService;
import com.looksee.utils.BrowserUtils;

import akka.actor.AbstractActor;
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
	private DomainService domain_service;
	
	@Autowired
	private PageStateService page_state_service;
	
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
					Domain domain = domain_service.findById(crawl_action.getDomainId()).get();
					String initial_url = domain.getUrl();
					
					if(crawl_action.isIndividual()) {
						PageState page_state = browser_service.buildPageState(new URL(initial_url));
						page_state = page_state_service.save(page_state);
						//domain.addPage(page);
						domain_service.addPage(domain.getId(), page_state.getKey());
						
						getSender().tell(page_state, getSelf());
					}
					else {
						Map<String, PageState> pages = new HashMap<>();
						Map<String, Boolean> frontier = new HashMap<>();
						Map<String, PageState> visited = new HashMap<>();
												
						//add link to frontier
						frontier.put(initial_url, Boolean.TRUE);
						
						while(!frontier.isEmpty()) {
							
							Map<String, Boolean> external_links = new HashMap<>();
							//remove link from beginning of frontier
							String page_url_str = frontier.keySet().iterator().next();
							
							frontier.remove(page_url_str);
							if( BrowserUtils.isImageUrl(page_url_str) 
									|| page_url_str.endsWith(".pdf")
									|| !page_url_str.contains(domain.getUrl())){
								continue;
							}

							URL page_url_obj = new URL(BrowserUtils.sanitizeUrl(page_url_str));
							page_url_str = BrowserUtils.getPageUrl(page_url_obj);
							//construct page and add page to list of page states
							//retrieve html source for page
							try {
								PageState page_state = browser_service.buildPageState(new URL(BrowserUtils.sanitizeUrl(page_url_str)));
								page_state = page_state_service.save(page_state);

								pages.put(page_state.getKey(), page_state);
								domain_service.addPage(domain.getId(), page_state.getKey());
								
								visited.put(page_url_str, page_state);
								//send message to page data extractor
								
								PageStateMessage page_msg = new PageStateMessage(page_state, crawl_action.getDomainId(), crawl_action.getAccountId(), crawl_action.getAuditRecordId());
								//Send PageVerstion to audit manager
								getSender().tell(page_msg, getSelf());
								
								Document doc = Jsoup.connect(page_url_obj.toString()).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").get();
								Elements links = doc.select("a");
								
								//iterate over links and exclude external links from frontier
								for (Element link : links) {
									String href_str = link.absUrl("href");
									if(href_str == null || href_str.isEmpty()) {
										continue;
									}

									String href = BrowserUtils.sanitizeUrl(href_str);
									URL href_url = new URL(href);
									href = BrowserUtils.getPageUrl(href_url);
									
									//check if external link
									if( BrowserUtils.isExternalLink(domain.getUrl().replace("www.", ""), href) || href.startsWith("mailto:")) {
										log.warn("adding to external links :: "+href);
					   					external_links.put(href, Boolean.TRUE);
									}
									else if( !visited.containsKey(href) 
											&& !frontier.containsKey(href) 
											&& !BrowserUtils.isExternalLink(domain.getUrl().replace("www.", ""), href)
									){
										//add link to frontier
										frontier.put(href, Boolean.TRUE);
									}
								}
							}catch(SocketTimeoutException e) {
								log.warn("Error occurred while navigating to :: "+page_url_str);
							}
							catch(HttpStatusException e) {
								log.warn("HTTP Status Exception occurred while navigating to :: "+page_url_str);
								e.printStackTrace();
							}
							catch(IllegalArgumentException e) {
								log.warn("illegal argument exception occurred when connecting to ::  " + page_url_str);
								e.printStackTrace();
							}
							catch(UnsupportedMimeTypeException e) {
								log.warn(e.getMessage() + " : " +e.getUrl());
							}
						}
						
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

package com.minion.actors;


import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

import com.minion.browsing.Browser;
import com.qanairy.helpers.BrowserConnectionHelper;
import com.qanairy.models.CrawlStat;
import com.qanairy.models.Domain;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageVersion;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.message.CrawlActionMessage;
import com.qanairy.services.BrowserService;
import com.qanairy.services.DomainService;
import com.qanairy.services.PageVersionService;
import com.qanairy.services.PageStateService;
import com.qanairy.utils.BrowserUtils;
import com.qanairy.utils.TimingUtils;

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
	private PageVersionService page_service;
	
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
					Map<String, Boolean> frontier = new HashMap<>();
					Map<String, PageVersion> visited = new HashMap<>();
					Domain domain = crawl_action.getDomain();
					
					String initial_url = "http://"+domain.getHost()+domain.getEntryPath();
					LocalDateTime start_time = LocalDateTime.now();
					Map<String, PageVersion> pages = new HashMap<>();
					
					//add link to frontier
					frontier.put(initial_url, Boolean.TRUE);
					
					while(!frontier.isEmpty()) {
						
						Map<String, Boolean> external_links = new HashMap<>();
						//remove link from beginning of frontier
						String page_url_str = frontier.keySet().iterator().next();
						log.warn("page url string :: "+page_url_str);
						//page_url_str = BrowserUtils.sanitizeUserUrl(page_url_str);
						//log.warn("page url string after sanitize  ::  "+page_url_str);
						frontier.remove(page_url_str);
						if( BrowserUtils.isImageUrl(page_url_str) || page_url_str.endsWith(".pdf")){
							continue;
						}
						
						URL page_url = new URL(page_url_str);

						//construct page and add page to list of page states
						//retrieve html source for page
						try {
							Document doc = Jsoup.connect(page_url_str).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").get();
							PageVersion page = browser_service.buildPage(doc.outerHtml(), page_url_str, doc.title());
							page = page_service.save( page );
							pages.put(page.getKey(), page);
							domain.addPage(page);
							domain = domain_service.save(domain);
							
							visited.put(page_url_str, page);
							//send message to page data extractor
							log.debug("sending page to an audit manager...");
							getSender().tell(page, getSelf());
							log.warn("page url :: "+page_url);
							log.warn("page host :: "+page_url.getHost());
							log.warn("page path :: "+page_url.getPath());
							log.warn("----------------------------------------------------------------");
							log.warn("----------------------------------------------------------------");
							
							Elements links = doc.select("a");
							for (Element link : links) {
								
								String href_str = link.absUrl("href");
								if(href_str == null || href_str.isEmpty()) {
									continue;
								}
								
								String href = BrowserUtils.sanitizeUrl(href_str);
								//check if external link
								if( BrowserUtils.isExternalLink(domain.getHost().replace("www.", ""), href_str) || href_str.startsWith("mailto:")) {
									log.debug("adding to external links :: "+href_str);
				   					external_links.put(href_str, Boolean.TRUE);
								}
								else if( !visited.containsKey(href) && !frontier.containsKey(href)){
									log.warn("href after sanitize :: " + href_str);
									log.warn("adding link to frontier :: " + href);
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
					LocalDateTime end_time = LocalDateTime.now();
					long run_time = start_time.until(end_time, ChronoUnit.MILLIS);
					CrawlStat crawl_stats = new CrawlStat( domain.getHost(),
															start_time,
														    end_time,
													    	pages.size(), 
													    	run_time/pages.size());
					getSender().tell(crawl_stats, getSelf());
					System.out.println("total links visited :::  "+visited.keySet().size());
				})
				.match(PageVersion.class, page -> {
					log.warn("Web crawler received page");
					
					boolean rendering_incomplete = true;
					boolean xpath_extraction_incomplete = true;

					int cnt = 0;
					PageState page_state = null;
					Browser browser = null;
					Map<String, ElementState> elements_mapped = new HashMap<>();
					List<String> xpaths = new ArrayList<>();
					do {
						try {
							browser = BrowserConnectionHelper.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
							log.warn("navigating to page state url ::   "+page.getUrl());
							browser.navigateTo(page.getUrl());
							if(page_state == null) {
								log.warn("getting browser for rendered page state extraction...");
								//navigate to page url
								page_state = browser_service.buildPageState(page, browser);
								//send RenderedPageState to sender
							}
							//extract all element xpaths
							if(xpath_extraction_incomplete) {
								log.warn("extracting elements from body tag for page_state  ::    "+page_state.getUrl());
								xpaths.addAll(browser_service.extractAllUniqueElementXpaths(page_state.getSrc()));
								xpath_extraction_incomplete=false;
							}
							
							//for each xpath then extract element state
							log.warn("getting element states for page state :: "+page_state.getUrl());
							List<ElementState> elements = browser_service.extractElementStates(page_state, xpaths, browser, elements_mapped);
							page_state.addElements(elements);

							rendering_incomplete = false;
							cnt=100;
							browser.close();
							break;
						}
						catch(Exception e) {
							if(browser != null) {
								browser.close();
							}
							log.warn("Webdriver exception thrown..."+e.getMessage());
							e.printStackTrace();
						}
						TimingUtils.pauseThread(15000L);
					}while(rendering_incomplete && cnt < 50);
					
					page_state = page_state_service.save(page_state);
					page.addPageState(page_state);
					page = page_service.save(page);
					log.warn("telling sender of Rendered Page State outcomes ....");
					getSender().tell( page_state, getSelf());
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

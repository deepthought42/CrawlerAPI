package com.looksee.actors;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jsoup.Jsoup;
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
import com.looksee.models.enums.BrowserEnvironment;
import com.looksee.models.enums.BrowserType;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.message.PageCandidateFound;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainService;
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
	private DomainService domain_service;
	
	@Autowired
	private BrowserService browser_service;
	
	
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

					//Map<String, PageState> pages = new HashMap<>();
					Map<String, Boolean> frontier = new HashMap<>();
					Map<String, PageState> visited = new HashMap<>();
											
					//add link to frontier
					frontier.put(initial_url, Boolean.TRUE);
					Map<String, Boolean> external_links = new HashMap<>();

					
					
					while(!frontier.isEmpty()) {
						//remove link from beginning of frontier
						String raw_url = frontier.keySet().iterator().next();
						frontier.remove(raw_url);

						if(raw_url.trim().isEmpty()) {
							continue;
						}
						
						URL sanitized_url = new URL(BrowserUtils.sanitizeUrl(raw_url));
						String page_url_str = BrowserUtils.getPageUrl(sanitized_url);
						
						if(visited.containsKey(page_url_str)) {
							continue;
						}
						
						if( BrowserUtils.isFile(page_url_str)
								|| BrowserUtils.isJavascript(page_url_str)
								|| page_url_str.startsWith("itms-apps:")
								|| page_url_str.startsWith("snap:")
								|| page_url_str.startsWith("tel:")
								|| page_url_str.startsWith("mailto:")
								|| BrowserUtils.isExternalLink(domain.getUrl(), page_url_str)){
							log.warn("is url and image url ??  "+ BrowserUtils.isImageUrl(page_url_str));
							log.warn("isFile??   ::  "+ BrowserUtils.isFile(page_url_str));
							log.warn("is url in visited array???    "+visited.containsKey(page_url_str));
							log.warn("contains domain url?  :: "+ page_url_str.contains(domain.getUrl()));
							log.warn("WebCrawler skipping url :: "+page_url_str);

							visited.put(page_url_str, null);
							continue;
						}

						//URL page_url_obj = new URL(BrowserUtils.sanitizeUrl(page_url_str));
						//construct page and add page to list of page states
						//retrieve html source for page
						PageCandidateFound candidate = new PageCandidateFound(crawl_action.getAuditRecordId(), sanitized_url);
						getSender().tell(candidate, getSelf());
						
						visited.put(page_url_str, null);

						int http_status = BrowserUtils.getHttpStatus(sanitized_url);

						//usually code 301 is returned which is a redirect, which is usually transferring to https
						if(http_status == 404) {
							log.warn("Recieved 404 status for link :: "+sanitized_url);
							continue;
						}
						
						String page_source = "";
						
						page_source = browser_service.getPageSource(BrowserType.CHROME, BrowserEnvironment.TEST, sanitized_url);
						
						try {
							//Document doc = Jsoup.connect(sanitized_url.toString()).get();
							Document doc = Jsoup.parse(page_source);
							Elements links = doc.select("a");
							String domain_host = domain.getUrl().replace("www.", "");
							
							//iterate over links and exclude external links from frontier
							for (Element link : links) {
								String href_str = link.attr("href");
								href_str = href_str.replaceAll(";", "").trim();
								if(href_str == null 
										|| href_str.isEmpty() 
										|| BrowserUtils.isJavascript(href_str)
										|| href_str.startsWith("itms-apps:")
										|| href_str.startsWith("snap:")
										|| href_str.startsWith("tel:")
										|| href_str.startsWith("mailto:")
										|| BrowserUtils.isFile(href_str)
								) {
									continue;
								}
								
								try {
									URL href_url = new URL( BrowserUtils.sanitizeUrl(BrowserUtils.formatUrl("http", domain.getUrl(), href_str)));
									String href = BrowserUtils.getPageUrl(href_url);
									//check if external link
									
									if(BrowserUtils.isRelativeLink(domain_host, href)) {
										href = sanitized_url.getHost() + href;
									}
									href = href.replace("http://","").replace("https://", "");
									URL sanitized_href = new URL(BrowserUtils.sanitizeUrl(href));
									page_url_str = BrowserUtils.getPageUrl(sanitized_href);
									boolean is_external_link = BrowserUtils.isExternalLink(domain_host, page_url_str);
									boolean is_subdomain = BrowserUtils.isSubdomain(domain_host, sanitized_href.getHost());
									if( is_external_link || is_subdomain) {
										
										external_links.put(page_url_str, Boolean.TRUE);
									}
									else if(!visited.containsKey(page_url_str)) {											
										//add link to frontier
										frontier.put(page_url_str, Boolean.TRUE);
									}
								}
								catch(MalformedURLException e) {
									log.error("malformed href value ....  "+href_str);
									//e.printStackTrace();
								}
							}
						} 
						catch(IllegalArgumentException e) {
							log.warn("illegal argument exception occurred when connecting to ::  " + page_url_str);
							e.printStackTrace();
						} 
						catch(Exception e) {
							log.error("Something went wrong while crawling page "+sanitized_url);
							e.printStackTrace();
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

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
import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.browsing.Browser;
import com.looksee.models.Account;
import com.looksee.models.Domain;
import com.looksee.models.PageState;
import com.looksee.models.enums.BrowserEnvironment;
import com.looksee.models.enums.BrowserType;
import com.looksee.models.enums.SubscriptionPlan;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.message.PageCandidateFound;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainService;
import com.looksee.services.SubscriptionService;
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

	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private AccountService account_service;
	
	@Autowired
	private SubscriptionService subscription_service;
	
	Map<String, Boolean> frontier = new HashMap<>();
	Map<String, PageState> visited = new HashMap<>();
	Map<String, Boolean> external_links = new HashMap<>();
	Map<String, Boolean> subdomains = new HashMap<>();

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
											
					//add link to frontier
					frontier.put(initial_url, Boolean.TRUE);
					Account account = account_service.findById(crawl_action.getAccountId()).get();
					SubscriptionPlan plan = SubscriptionPlan.create(account.getSubscriptionType());

					while(!frontier.isEmpty()) {
						//remove link from beginning of frontier
						String raw_url = frontier.keySet().iterator().next();
						frontier.remove(raw_url);

						if(raw_url.trim().isEmpty()) {
							continue;
						}
						URL sanitized_url = new URL(BrowserUtils.sanitizeUrl(BrowserUtils.formatUrl("http", domain.getUrl(), raw_url, false), false));
						String page_url = BrowserUtils.getPageUrl(sanitized_url);
						
						if(visited.containsKey(page_url.toString())) {
							continue;
						}
						
						int page_audit_cnt = audit_record_service.getPageAuditCount(crawl_action.getAuditRecord().getId());
						log.warn("checking if user has exceeded account restriction : "+page_audit_cnt + " : domain id = "+ crawl_action.getDomainId());
						//quick check to make sure we haven't exceeded user plan
						if(subscription_service.hasExceededDomainPageAuditLimit(plan, page_audit_cnt)) {
							log.warn("Stopping webcrawler actor because user has exceeded limit of number of pages they can perform per audit");
							this.getContext().stop(getSelf());
							break;
						}
						
						if( BrowserUtils.isFile(sanitized_url.toString())
								|| BrowserUtils.isJavascript(sanitized_url.toString())
								|| sanitized_url.toString().startsWith("itms-apps:")
								|| sanitized_url.toString().startsWith("snap:")
								|| sanitized_url.toString().startsWith("tel:")
								|| sanitized_url.toString().startsWith("applenews:")
								|| sanitized_url.toString().startsWith("applenewss:")
								|| sanitized_url.toString().startsWith("mailto:")
								|| BrowserUtils.isExternalLink(domain.getUrl(), sanitized_url.toString())){
							visited.put(page_url, null);
							continue;
						}
						
						//Check http status to ensure page exists before trying to extract info from page
						int http_status = BrowserUtils.getHttpStatus(sanitized_url);

						//usually code 301 is returned which is a redirect, which is usually transferring to https
						if(http_status == 404 || http_status == 408) {
							log.warn("Recieved 404 status for link :: "+sanitized_url);
							visited.put(page_url, null);
							continue;
						}
						
						int attempt_cnt = 0;
						String page_src = "";
						do {
							Browser browser = null;
							try {
								browser = browser_service.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
								browser.navigateTo(sanitized_url.toString());
								
								sanitized_url = new URL(browser.getDriver().getCurrentUrl());
								page_src = browser_service.getPageSource( browser, sanitized_url);
								attempt_cnt = 10000000;
								break;
							}
							catch(MalformedURLException e) {
								log.warn("Malformed URL exception occurred for  "+sanitized_url);
								break;
							}
							catch(WebDriverException | GridException e) {								
								log.warn("failed to obtain page source during crawl of :: "+sanitized_url);
							}
							finally {
								if(browser != null) {
									browser.close();
								}
							}
						}while (page_src.trim().isEmpty() && attempt_cnt < 100000);
						
						//URL page_url_obj = new URL(BrowserUtils.sanitizeUrl(page_url_str));
						//construct page and add page to list of page states
						//retrieve html source for page
						log.warn("sending page candidate to AuditManager....");
						PageCandidateFound candidate = new PageCandidateFound(crawl_action.getAccountId(), 
																			  crawl_action.getAuditRecordId(), 
																			  crawl_action.getDomainId(),
																			  sanitized_url);
						getSender().tell(candidate, getSelf());
						
						visited.put(page_url, null);

						try {
							Document doc = Jsoup.parse(page_src);
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
										|| href_str.startsWith("applenews:") //both apple news spellings are here because its' not clear which is the proper protocol
										|| href_str.startsWith("applenewss:")//both apple news spellings are here because its' not clear which is the proper protocol
										|| BrowserUtils.isFile(href_str)
								) {
									continue;
								}
								
								try {
									URL href_url = new URL( BrowserUtils.sanitizeUrl(BrowserUtils.formatUrl("http", domain.getUrl(), href_str, false), false));
									String link_page_url = BrowserUtils.getPageUrl(href_url);
									
									if( BrowserUtils.isExternalLink(domain_host, href_url.toString())) {
										external_links.put(href_url.toString(), Boolean.TRUE);
									}
									else if(BrowserUtils.isSubdomain(domain_host, href_url.getHost())) {
										subdomains.put(href_url.toString(), Boolean.TRUE);
									}
									else if(!visited.containsKey(link_page_url)){
										//add link to frontier
										frontier.put(href_url.toString(), Boolean.TRUE);
									}
								}
								catch(MalformedURLException e) {
									log.error("malformed href value ....  "+href_str);
									//e.printStackTrace();
								}
							}

						} 
						catch(IllegalArgumentException e) {
							log.warn("illegal argument exception occurred when connecting to ::  " + sanitized_url.toString());
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

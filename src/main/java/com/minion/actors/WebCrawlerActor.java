package com.minion.actors;


import java.io.IOException;
import java.net.SocketTimeoutException;
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

import com.qanairy.models.Domain;
import com.qanairy.models.Page;
import com.qanairy.models.message.CrawlActionMessage;
import com.qanairy.services.DomainService;
import com.qanairy.services.PageService;
import com.qanairy.utils.BrowserUtils;

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
	private PageService page_service;

	
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
					Map<String, Page> visited = new HashMap<>();
					Domain domain = crawl_action.getDomain();
					//add link to frontier
					frontier.put(domain.getUrl(), Boolean.TRUE);
					
					
					while(!frontier.isEmpty()) {
						Map<String, Boolean> external_links = new HashMap<>();
						Page page = null;
						//remove link from beginning of frontier
						String page_url = frontier.keySet().iterator().next();
						frontier.remove(page_url);
						if(page_url.isEmpty() || page_url.contains("mailto") || page_url.contains(".jpg") || page_url.contains(".png")) {
							continue;
						}

						//construct page and add page to list of page states
						page = new Page(page_url);
						page = page_service.save( page );

						log.debug("page created with url..."+page_url);			
						domain.addPage(page);
						domain = domain_service.save(domain);
						
						visited.put(page_url, page);
						//send message to page data extractor
						getSender().tell(page, getSelf());

						//retrieve html source for page
						try {
							Document doc = Jsoup.connect(page_url).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").get();
							log.debug("Document title :: " + doc.title());
							Elements links = doc.select("a");
							for (Element link : links) {
								String href = BrowserUtils.sanitizeUrl(link.absUrl("href"));
								
								//check if external link
								if( !href.isEmpty() && BrowserUtils.isExternalLink(domain.getHost().replace("www.", ""), href)) {
									log.debug("adding to external links :: "+href);
				   					external_links.put(href, Boolean.TRUE);
								}
								else if( !href.isEmpty() && !visited.containsKey(href) && !frontier.containsKey(href) && !BrowserUtils.isImageUrl(href)){
									log.warn("adding link to frontier :: "+href);
									//add link to frontier
									frontier.put(href, Boolean.TRUE);
								}
							}
						}catch(SocketTimeoutException e) {
							log.warn("Error occurred while navigating to :: "+page_url);
						}
						catch(HttpStatusException e) {
							log.warn("HTTP Status Exception occurred while navigating to :: "+page_url);
							e.printStackTrace();
						}
						catch(IllegalArgumentException e) {
							log.warn("illegal argument exception occurred when connecting to ::  " + page_url);
							e.printStackTrace();
						}
						catch(UnsupportedMimeTypeException e) {
							log.warn(e.getMessage() + " : " +e.getUrl());
						}
					}
					System.out.println("total links visited :::  "+visited.keySet().size());
					postStop();
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

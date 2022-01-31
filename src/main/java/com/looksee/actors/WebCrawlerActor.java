package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.Domain;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.message.SourceMessage;
import com.looksee.models.message.Message;
import com.looksee.models.message.PageCandidateFound;
import com.looksee.models.message.DomainMessage;
import com.looksee.services.DomainService;
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
	private DomainService domain_service;
	
	@Autowired
	private ActorSystem actor_system;

	private ActorRef audit_manager;
	private ActorRef source_extractor;
	private ActorRef link_extractor;

	
	private Map<String, Boolean> frontier = new HashMap<>();
	private Map<String, Boolean> visited = new HashMap<>();
	private Map<String, Boolean> external_links = new HashMap<>();
	private Map<String, Boolean> subdomains = new HashMap<>();

	//subscribe to cluster changes
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
				MemberEvent.class, UnreachableMember.class);
		
		source_extractor = getContext().actorOf(SpringExtProvider.get(actor_system)
											   .props("sourceExtractionActor"), "sourceExtractionActor" + UUID.randomUUID());
		link_extractor = getContext().actorOf(SpringExtProvider.get(actor_system)
											   .props("linkExtractionActor"), "linkExtractionActor" + UUID.randomUUID());
		audit_manager = null;
		
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
				//From AuditManager
				.match(CrawlActionMessage.class, crawl_action -> {
					this.audit_manager = getContext().getSender();

					processFrontier(crawl_action);
				})
				//From LinkExtraction to SourceExtraction
				.match(DomainMessage.class, crawl_action -> {
					URL sanitized_url = new URL(crawl_action.getRawUrl());
					String page_url = BrowserUtils.getPageUrl(sanitized_url);
          
					if(!visited.containsKey(page_url)){
						frontier.put(page_url, Boolean.TRUE);
					}

					processFrontier(crawl_action);
				})
				//From SourceExtraction to LinkExtraction
				.match(SourceMessage.class, page_src_msg -> {
					this.link_extractor.tell(page_src_msg, getSelf());

					//URL page_url_obj = new URL(BrowserUtils.sanitizeUrl(page_url_str));
					//construct page and add page to list of page states
					//retrieve html source for page
					log.warn("sending page candidate to AuditManager....");
					PageCandidateFound candidate = new PageCandidateFound(page_src_msg.getAccountId(), 
																		  page_src_msg.getAuditRecordId(), 
																		  page_src_msg.getDomainId(),
																		  page_src_msg.getSanitizedUrl());

					this.audit_manager.tell(candidate, getSelf());
				})
				.match(AbstractMap.SimpleEntry.class, mPair -> {
					//Insert into the correct map
					switch(mPair.getKey().toString()){
						case "visited":
							this.visited.put(mPair.getKey().toString(), Boolean.TRUE);
						break;
						case "frontier":
							this.frontier.put(mPair.getKey().toString(), Boolean.TRUE);
						break;
						case "subdomain":
							this.subdomains.put(mPair.getKey().toString(), Boolean.TRUE);
						break;
						case "external_link":
							this.external_links.put(mPair.getKey().toString(), Boolean.TRUE);
						break;
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
					log.info("received unknown message of type :: " + o.getClass().getName());
				})
				.build();
	}

	private void processFrontier(Message crawl_action) throws MalformedURLException {
		/* perform site wide crawl */
		Domain domain = domain_service.findById(crawl_action.getDomainId()).get();
		String initial_url = domain.getUrl();
								
		//add link to frontier
		frontier.put(initial_url, Boolean.TRUE);
	
		if(!frontier.isEmpty()) {
			//remove link from beginning of frontier
			String raw_url = frontier.keySet().iterator().next();
			frontier.remove(raw_url);
			
			URL sanitized_url = new URL(BrowserUtils.sanitizeUrl(BrowserUtils.formatUrl("http", domain.getUrl(), raw_url, false), false));
			String page_url = BrowserUtils.getPageUrl(sanitized_url);
					
			if(!visited.containsKey(page_url.toString())) {
				DomainMessage domain_msg = new DomainMessage(crawl_action, domain, raw_url);
				
				this.source_extractor.tell(domain_msg, getSelf());
			}
		}
	}
}
	

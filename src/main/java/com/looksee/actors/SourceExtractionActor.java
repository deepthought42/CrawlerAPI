package com.looksee.actors;

import java.net.URL;
import java.util.AbstractMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.message.DomainMessage;
import com.looksee.models.message.SourceMessage;
import com.looksee.models.Domain;
import com.looksee.services.BrowserService;
import com.looksee.utils.BrowserUtils;

import akka.actor.AbstractActor;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

/**
 * Source Extraction actor that performs the following:
 * 
 * 1. Checks the validity of the URL by checking its protocol.
 * 2. Connect to the URL to check the connection to prepare it for extraction.
 * 3. If valid, send the domain and page source to the LinkExtractionActor for extraction.
 * 
 */
@Component
@Scope("prototype")
public class SourceExtractionActor extends AbstractActor {
	private static Logger log = LoggerFactory.getLogger(SourceExtractionActor.class);
	
	@Autowired
	private BrowserService browser_service;
	
	public Receive createReceive(){
		return receiveBuilder()
			.match(DomainMessage.class, domain_msg -> {
				Domain domain = domain_msg.getDomain();
				URL sanitized_url = new URL(BrowserUtils.sanitizeUrl(domain_msg.getRawUrl(), false));
				String page_url = BrowserUtils.getPageUrl(sanitized_url);
				
				getSender().tell(new AbstractMap.SimpleEntry<String, String>("visited", page_url), getSelf());
				if(BrowserUtils.isValidUrl(sanitized_url.toString(), domain.getUrl())){
				
					if(BrowserUtils.hasValidHttpStatus(sanitized_url)) {
						String page_src = BrowserUtils.extractPageSrc(sanitized_url, browser_service);
						SourceMessage source = new SourceMessage(domain_msg, sanitized_url, page_src);
		
						getSender().tell(source, getSelf());
					}
					else {
						log.warn("Recieved 404 status for link :: " + sanitized_url);

						this.getContext().stop(getSelf());
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
				log.info("received unknown message of type :: " + o.getClass().getName());
			})
			.build();
	}
}

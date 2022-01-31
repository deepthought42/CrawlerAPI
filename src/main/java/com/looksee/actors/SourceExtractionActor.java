package com.looksee.actors;

import java.net.URL;
import java.util.AbstractMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.looksee.models.message.DomainMessage;
import com.looksee.models.message.SourceMessage;
import com.looksee.models.Domain;
import com.looksee.services.BrowserService;
import com.looksee.utils.BrowserUtils;

import akka.actor.AbstractActor;

public class SourceExtractionActor extends AbstractActor {
	private static Logger log = LoggerFactory.getLogger(SourceExtractionActor.class);
	
	@Autowired
	private BrowserService browser_service;
	
	public Receive createReceive(){
		return receiveBuilder()
			.match(DomainMessage.class, domain_msg -> {
				Domain domain = domain_msg.getDomain();
				URL sanitized_url = new URL(domain_msg.getRawUrl());
				String page_url = BrowserUtils.getPageUrl(sanitized_url);
				
				if(BrowserUtils.isValidUrl(sanitized_url.toString(), domain.getUrl())){
					getSender().tell(new AbstractMap.SimpleEntry<String, String>("visited", page_url), getSelf());
				
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
			.build();
	}
}

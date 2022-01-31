package com.looksee.actors;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.looksee.models.Domain;
import com.looksee.models.message.DomainMessage;
import com.looksee.models.message.SourceMessage;
import com.looksee.services.DomainService;
import com.looksee.utils.BrowserUtils;

import akka.actor.AbstractActor;

public class LinkExtractionActor extends AbstractActor {
	private static Logger log = LoggerFactory.getLogger(LinkExtractionActor.class);

	@Autowired
	private DomainService domain_service;

	public Receive createReceive(){
		return receiveBuilder()
			.match(SourceMessage.class, page_src_msg -> {
				Domain domain = domain_service.findById(page_src_msg.getDomainId()).get();
				String page_src = page_src_msg.getPageSrc();

				URL sanitized_url = page_src_msg.getSanitizedUrl();

				try {
					Document doc = Jsoup.parse(page_src);
					Elements links = doc.select("a");
					String domain_host = domain.getUrl().replace("www.", "");
					
					//iterate over links and exclude external links from frontier
					for (Element link : links) {
						String href_str = link.attr("href");
						href_str = href_str.replaceAll(";", "").trim();
						
						if(!BrowserUtils.isValidLink(href_str)) {
							continue;
						}
						
						try {
							URL href_url = new URL(BrowserUtils.sanitizeUrl(BrowserUtils.formatUrl("http", domain.getUrl(), href_str, false), false));
							
							if(BrowserUtils.isExternalLink(domain_host, href_url.toString())) {
								getSender().tell(new AbstractMap.SimpleEntry<String, String>("external_link", href_url.toString()), getSelf());
							}
							else if(BrowserUtils.isSubdomain(domain_host, href_url.getHost())) {
								getSender().tell(new AbstractMap.SimpleEntry<String, String>("subdomain", href_url.toString()), getSelf());
							}
						}
						catch(MalformedURLException e) {
							log.error("malformed href value ....  " + href_str);
						}
					}
				} 
				catch(IllegalArgumentException e) {
					log.warn("illegal argument exception occurred when connecting to ::  " + sanitized_url.toString());
					e.printStackTrace();
				} 
				catch(Exception e) {
					log.error("Something went wrong while crawling page " + sanitized_url);
					e.printStackTrace();
				}
				
				DomainMessage domain_msg = new DomainMessage(page_src_msg, domain, sanitized_url.toString());

				getSender().tell(domain_msg, getSelf());
			})
			.build();
	}

 
}

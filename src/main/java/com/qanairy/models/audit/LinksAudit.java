package com.qanairy.models.audit;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.services.ObservationService;
import com.qanairy.services.PageStateService;
import com.qanairy.utils.BrowserUtils;

/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class LinksAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(LinksAudit.class);
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ObservationService observation_service;
	
	private List<ElementState> links_without_href_attribute =  new ArrayList<>();
	private List<ElementState> links_without_href_value =  new ArrayList<>();
	private List<ElementState> invalid_links = new ArrayList<>();
	private List<ElementState> dead_links = new ArrayList<>();
	private List<ElementState> non_labeled_links = new ArrayList<>();

	public LinksAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
	}
	
	private static String getAuditDescription() {
		return "A hyperlink that takes you to a new location should be reactive and result in the user navigating to an existing webpage";
	}

	private static List<String> buildBestPractices() {
		List<String> best_practices = new ArrayList<>();
		best_practices.add("Label should describe what the link is. \"Click here\" should not be used.");
		
		return best_practices;
	}
	
	private static String getAdaDescription() {
		return "\r\n" + 
				"2.4.4 - Descriptive Links\r\n" + 
				"The purpose of each link can be determined from the link text alone or from the link text together with its programmatically determined link context.\r\n" + 
				"\r\n" + 
				"2.4.7 - Visible Focus\r\n" + 
				"When an interactive element (link, button, form field, selectable element, etc.) receives focus, a visual indicator shows so a user can see what element they are currently on.";
	}
	
	
	/**
	 * {@inheritDoc}
	 * 
	 * Scores links on a page based on if the link has an href value present, the url format is valid and the 
	 *   url goes to a location that doesn't produce a 4xx error 
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page_state) {
		assert page_state != null;
		log.warn("---------------------------------------------------------------------------");
		log.warn("EXECUTING LINKS AUDIT !!!");
		//check if page state already has a Link audit
		Audit audit = page_state_service.findAuditBySubCategory(AuditSubcategory.LINKS, page_state.getKey());
		if(audit != null) {
			return audit;
		}
		
		//List<ElementState> link_elements = page_state_service.getLinkElementStates(user_id, page_state.getKey());
		List<ElementState> link_elements = new ArrayList<>();
		for(ElementState element : page_state.getElements()) {
			if(element.getName().equalsIgnoreCase("a")) {
				link_elements.add(element);
			}
		}
		
		List<Observation> observations = new ArrayList<>();
		//score each link element
		int score = 0;
		for(ElementState link : link_elements) {
	
			Document jsoup_doc = Jsoup.parseBodyFragment(link.getOuterHtml(), page_state.getUrl());
			Element element = jsoup_doc.getElementsByTag("a").first();
			
			if(element.hasAttr("href")) {
				score++;
			}
			else {
				links_without_href_attribute.add(link);
				continue;
			}
			String href = element.absUrl("href");

			//if href is a mailto link then give score full remaining value and continue
			if(href.startsWith("mailto:")) {
				score += 5;
				continue;
			}
			
			//does element have an href value?
			if(href != null && !href.isEmpty()) {
				
				score++;
				try {
					URI uri = new URI(href);
					if(!uri.isAbsolute()) {
						href = page_state.getUrl() + href;
					}
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}
			else {
				links_without_href_value.add(link);
				continue;
			}
			
			//is element link a valid url?
			URL url_href = null;
			try {
				url_href = new URL(href);
				score++;
			} catch (MalformedURLException e) {
				invalid_links.add(link);
				e.printStackTrace();
			}
			
			//Does link have a valid URL? yes(1) / No(0)
			try {
				if(BrowserUtils.doesUrlExist(url_href)) {
					score++;
				}
				else {
					dead_links.add(link);
				}
			} catch (IOException e) {
				dead_links.add(link);
				e.printStackTrace();
			}
			
			//Does link contain a text label inside it
			 if(element.hasText()) {
				score++;
			 }
			 else if(!element.hasText() && element.getElementsByTag("img").isEmpty()) {
				//does element use image as links?
				non_labeled_links.add(link);
			}
			 
			//TODO : Does link have a hover styling? yes(1) / No(0)
			
			//TODO : Is link label relevant to destination url or content? yes(1) / No(0)
				//TODO :does link text exist in url?
				if(href.contains(element.ownText())) {
					score++;
				}
				
				//TODO :does target content relate to link?
			
		}
		
		if(!links_without_href_attribute.isEmpty()) {
			ElementObservation observation = new ElementObservation(links_without_href_attribute, "Links without an 'href' attribute present");
			observations.add(observation_service.save(observation));
		}
		
		if(!links_without_href_value.isEmpty()) {
			ElementObservation observation = new ElementObservation(links_without_href_value, "Links without emptry 'href' values");
			observations.add(observation_service.save(observation));
		}
		
		if(!invalid_links.isEmpty()) {
			ElementObservation observation = new ElementObservation(invalid_links, "Links with invalid addresses");
			observations.add(observation_service.save(observation));
		}
		
		if(!dead_links.isEmpty()) {
			ElementObservation observation = new ElementObservation(dead_links, "Dead links");
			observations.add(observation_service.save(observation));
		}
		
		if(!non_labeled_links.isEmpty()) {
			ElementObservation observation = new ElementObservation(non_labeled_links, "Links without text");
			observations.add(observation_service.save(observation));
		}
		
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE, AuditSubcategory.LINKS, score, observations, AuditLevel.PAGE,link_elements.size()*6); //the contstant 6 in this equation is the exact number of boolean checks for this audit
	}
}

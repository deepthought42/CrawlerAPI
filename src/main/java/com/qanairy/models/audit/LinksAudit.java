package com.qanairy.models.audit;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.gcp.CloudVisionUtils;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditName;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.models.enums.Priority;
import com.qanairy.services.BrowserService;
import com.qanairy.services.ElementStateService;
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
	private ObservationService observation_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ElementStateService element_state_service;
	
	private List<ElementState> links_without_href_attribute =  new ArrayList<>();
	private List<ElementState> links_without_href_value =  new ArrayList<>();
	private List<ElementState> invalid_links = new ArrayList<>();
	private List<ElementState> dead_links = new ArrayList<>();
	private List<ElementState> non_labeled_links = new ArrayList<>();

	public LinksAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
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
	
		//List<ElementState> link_elements = page_state_service.getLinkElementStates(user_id, page_state.getKey());
		List<ElementState> link_elements = new ArrayList<>();
		for(ElementState element : page_state_service.getElementStates(page_state.getKey())) {
			if(element.getName().equalsIgnoreCase("a")) {
				link_elements.add(element);
			}
		}
		
		log.warn("------------------------------------------------------------");
		log.warn("Link elements found ... "+link_elements.size());
		List<Observation> observations = new ArrayList<>();
		//score each link element
		int score = 0;
		for(ElementState link : link_elements) {
			
			Document jsoup_doc = Jsoup.parseBodyFragment(link.getOuterHtml(), page_state.getUrl());
			Element element = jsoup_doc.getElementsByTag("a").first();
		
			//String href = link.getAttribute("href");
			if( element.hasAttr("href") ) {
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
			
			log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			
			//Does link contain a text label inside it
			 if(!link.getAllText().isEmpty()) {
				 log.warn("link has text.................");
				 score++;
			 }
			 else {
				 boolean element_includes_text = false;
				 log.warn("link xpath :: " + link.getXpath());
				 List<ElementState> element_states = element_state_service.getChildElements( page_state.getKey(), link.getXpath() );
				 log.warn("# of child elements :: " + element_states.size());
				 log.warn("link outerhtml  ::   " + link.getOuterHtml());
				 //check each child element. if element is an image and does not include text then add link to non labeled links
				 //for(ElementState child_element : element_states) {
				//	 if("img".contentEquals(child_element.getName())) {
						//send img src to google for text extraction
						try {
							URL url = new URL( link.getScreenshotUrl() );
							log.warn("image src :  "+url.toString());
							BufferedImage img_src = ImageIO.read( url );
							List<String> image_text_list = CloudVisionUtils.extractImageText(img_src);
							log.warn("image text list :: "+image_text_list.size());
							
							for(String text : image_text_list) {
								log.warn("text value :: "+text);
								if(text != null && !text.isEmpty()) {
									element_includes_text = true;
								}
							}
							
						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				
				 
				 if(!element_includes_text) {
					 log.warn("link doesn't have a text label");
					 //does element use image as links?
					 non_labeled_links.add(link);
				 }
				 else {
					 score++;
				 }
			}
			 log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			 log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			 
			//TODO : Does link have a hover styling? yes(1) / No(0)
			
			//TODO : Is link label relevant to destination url or content? yes(1) / No(0)
				//TODO :does link text exist in url?
			//	if(href.contains(element.ownText())) {
				//	score++;
				//}
				
				//TODO :does target content relate to link?
			
		}
		
		
		
		String why_it_matters = "Dead links are links whose source can't be found. When users encounter dead links"
				+ " they perceive the validity of what you have to say as less valuable. Often, after experiencing a"
				+ " dead link, users bounce in search of a mord()e reputable source.";
		
		String ada_compliance = "There is no ADA guideline for dead links";
		
		if(!links_without_href_attribute.isEmpty()) {
			Set<String> recommendations = new HashSet<>();
			recommendations.add("Make sure links have a url set for the href value.");
			
			ElementStateObservation observation = new ElementStateObservation(
															links_without_href_attribute, 
															"Links without an 'href' attribute present", 
															why_it_matters, 
															ada_compliance, 
															Priority.HIGH,
															recommendations);
			observations.add(observation_service.save(observation));
		}
		
		if(!links_without_href_value.isEmpty()) {
			Set<String> recommendations = new HashSet<>();
			recommendations.add("Make sure links have a url set for the href value.");
			
			ElementStateObservation observation = new ElementStateObservation(
															links_without_href_value, 
															"Links without empty 'href' values", 
															why_it_matters, 
															ada_compliance, 
															Priority.HIGH,
															recommendations);
			observations.add(observation_service.save(observation));
		}
		
		if(!invalid_links.isEmpty()) {
			Set<String> recommendations = new HashSet<>();
			recommendations.add("Make sure links point to a valid url.");
			
			ElementStateObservation observation = new ElementStateObservation(
															invalid_links, 
															"Links with invalid addresses", 
															why_it_matters, 
															ada_compliance, 
															Priority.HIGH,
															recommendations);
			observations.add(observation_service.save(observation));
		}
		
		if(!dead_links.isEmpty()) {
			Set<String> recommendations = new HashSet<>();
			recommendations.add("Make sure links point to a valid url.");
			
			ElementStateObservation observation = new ElementStateObservation(
														dead_links, 
														"Dead links", 
														why_it_matters, 
														ada_compliance, 
														Priority.HIGH,
														recommendations);
			observations.add(observation_service.save(observation));
		}
		
		if(!non_labeled_links.isEmpty()) {
			Set<String> recommendations = new HashSet<>();
			recommendations.add("For best usability make sure links include text.");
			
			ElementStateObservation observation = new ElementStateObservation(
															non_labeled_links, 
															"Links without text", 
															why_it_matters, 
															ada_compliance, 
															Priority.HIGH,
															recommendations);
			observations.add(observation_service.save(observation));
		}
		
		log.warn("LINKS AUDIT SCORE ::  "+score + " / " + (link_elements.size()*5));
		
		
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
						 AuditSubcategory.PERFORMANCE,
						 AuditName.LINKS,
						 score,
						 observations,
						 AuditLevel.PAGE,
						 link_elements.size()*5,
						 page_state.getUrl()); 
		//the contstant 6 in this equation is the exact number of boolean checks for this audit
	}
}

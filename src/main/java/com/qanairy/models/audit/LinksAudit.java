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
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.enums.Priority;
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
	
	private static final int MAX_POINTS_EACH = 5;
	
	@Autowired
	private ObservationService observation_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	

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
		
		Set<UXIssueMessage> links_without_href_attribute =  new HashSet<>();
		Set<UXIssueMessage> links_without_href_value =  new HashSet<>();
		Set<UXIssueMessage> invalid_links = new HashSet<>();
		Set<UXIssueMessage> dead_links = new HashSet<>();
		Set<UXIssueMessage> non_labeled_links = new HashSet<>();
	
		//List<ElementState> link_elements = page_state_service.getLinkElementStates(user_id, page_state.getKey());
		List<ElementState> link_elements = new ArrayList<>();
		List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
		
		for(ElementState element : elements) {
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

			if( element.hasAttr("href") ) {
				score++;
			}
			else {
				String recommendation = "Make sure links have a url set for the href value.";
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH,
																recommendation, 
																link);
				links_without_href_attribute.add(issue_message);
				continue;
			}
			String href = element.attr("href");
			
			log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			log.warn("actual href value :: "+element.attr("href"));
			log.warn("href as absolute url :: "+href);
			//if href is a mailto link then give score full remaining value and continue
			if(href.startsWith("mailto:")) {
				score += 4;
				continue;
			}
			
			//does element have an href value?
			if(href != null && !href.isEmpty()) {
				score++;
			}
			else {
				String recommendation = "Make sure links have a url set for the href value.";
				ElementStateIssueMessage issue_Message = new ElementStateIssueMessage(
																Priority.HIGH, 
																recommendation, 
																link);
				links_without_href_value.add(issue_Message);
				continue;
			}
			
			//is element link a valid url?
			try {
				log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				log.warn("href before check :: "+href);
				
				log.warn("does href start with relative indicator :: " + (href.startsWith("/") || href.startsWith("../")));
				
				URI uri = new URI(href);
				log.warn("is href absolute? :: "+uri.isAbsolute());
				if(!uri.isAbsolute()) {
					URL page_url = new URL(BrowserUtils.sanitizeUrl(page_state.getUrl()));
					log.warn("URI is not absolute :: "+href);
					href.replaceAll("../", "");
					if(href.startsWith("/") && href.length() > 1) {
						log.warn("href starts with a '/'  :: "+href);
						href = href.substring(1);
					}
					else if(href.strip().contentEquals("/")) {
						href = "";
					}
					href = page_url.getHost() + "/" + href;
					log.warn("new href 1 :: "+href);

				}
			
				log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				//if starts with / then append host
			
				score++;
			} catch (MalformedURLException e) {
				String recommendation = "Make sure links point to a valid url.";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH, 
																recommendation, 
																link);
				invalid_links.add(issue_message);
				e.printStackTrace();
			} catch (URISyntaxException e) {
				String recommendation = "Make sure links point to a valid url.";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH, 
																recommendation, 
																link);
				invalid_links.add(issue_message);
				e.printStackTrace();
			}
			
			//Does link have a valid URL? yes(1) / No(0)
			try {
				log.warn("href before building URL object :: "+href);

				URL url_href = new URL(BrowserUtils.sanitizeUrl(href));

				log.warn("url href object :: "+url_href.toString());

				log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				
				if(BrowserUtils.doesUrlExist(url_href)) {
					score++;
				}
				else {
					String recommendaiotn = "Make sure links point to a valid url.";
					
					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.HIGH,
																	recommendaiotn,
																	link);
					dead_links.add(issue_message);
				}
			} catch (IOException e) {
				String recommendaiotn = "Make sure links point to a valid url.";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH,
																recommendaiotn,
																link);
				dead_links.add(issue_message);
				e.printStackTrace();
			}
			
			//Does link contain a text label inside it
			 if(!link.getAllText().isEmpty()) {
				 score++;
			 }
			 else {
				 boolean element_includes_text = false;
				// List<ElementState> element_states = element_state_service.getChildElements( page_state.getKey(), link.getXpath() );
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
					String recommendation = "For best usability make sure links include text.";
					
					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.HIGH,
																	recommendation, 
																	link);
					 //does element use image as links?
					 non_labeled_links.add(issue_message);
				 }
				 else {
					 score++;
				 }
			}
			 
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
		
		Set<String> labels = new HashSet<>();
		labels.add("accessibility");
		labels.add("information architecture");
		labels.add("navigation");
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.INFORMATION_ARCHITECTURE.getShortName());
		
		if(!links_without_href_attribute.isEmpty()) {
			
			Observation observation = new Observation(
												"Links without an 'href' attribute present",
												why_it_matters,
												ada_compliance,
												ObservationType.ELEMENT,
												labels,
												categories,
												links_without_href_attribute);
			observations.add(observation_service.save(observation));
		}
		
		if(!links_without_href_value.isEmpty()) {
			
			Observation observation = new Observation(
												"Links with empty 'href' values", 
												why_it_matters, 
												ada_compliance,
												ObservationType.ELEMENT,
												labels,
												categories,
												links_without_href_value);
			observations.add(observation_service.save(observation));
		}
		
		if(!invalid_links.isEmpty()) {
			
			Observation observation = new Observation(
												"Links with invalid addresses", 
												why_it_matters, 
												ada_compliance,
												ObservationType.ELEMENT,
												labels,
												categories,
												invalid_links);
			observations.add(observation_service.save(observation));
		}
		
		if(!dead_links.isEmpty()) {
			Observation observation = new Observation(
												"Dead links", 
												why_it_matters, 
												ada_compliance, 
												ObservationType.ELEMENT,
												labels,
												categories,
												dead_links);
			
			observations.add(observation_service.save(observation));
		}
		
		if(!non_labeled_links.isEmpty()) {
			Observation observation = new Observation(
												"Links without text",
												why_it_matters,
												ada_compliance,
												ObservationType.ELEMENT,
												labels,
												categories,
												non_labeled_links);
			
			observations.add(observation_service.save(observation));
		}
		
		log.warn("LINKS AUDIT SCORE ::  "+score + " / " + (link_elements.size()*5));
		
		
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
						 AuditSubcategory.PERFORMANCE,
						 AuditName.LINKS,
						 score,
						 observations,
						 AuditLevel.PAGE,
						 link_elements.size() * MAX_POINTS_EACH,
						 page_state.getUrl()); 
		//the contstant 6 in this equation is the exact number of boolean checks for this audit
	}
}

package com.looksee.models.audit.informationarchitecture;

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

import com.looksee.api.MessageBroadcaster;
import com.looksee.gcp.CloudVisionUtils;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.ElementStateIssueMessage;
import com.looksee.models.audit.IExecutablePageStateAudit;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.Priority;
import com.looksee.services.PageStateService;
import com.looksee.utils.BrowserUtils;

/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class LinksAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(LinksAudit.class);
	
	private static final int MAX_POINTS_EACH = 5;
	
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
	 *   
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record) {
		assert page_state != null;
		
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		List<ElementState> link_elements = new ArrayList<>();
		List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
		
		String ada_compliance = "There is no ADA guideline for dead links";
		
		for(ElementState element : elements) {
			if(element.getName().equalsIgnoreCase("a")) {
				link_elements.add(element);
			}
		}
		
		Set<String> labels = new HashSet<>();
		labels.add("information architecture");
		labels.add("accessibility");
		labels.add("navigation");
		labels.add("links");

		//score each link element
		int score = 0;
		for(ElementState link : link_elements) {			
			Document jsoup_doc = Jsoup.parseBodyFragment(link.getOuterHtml(), page_state.getUrl());
			Element element = jsoup_doc.getElementsByTag("a").first();

			if( element.hasAttr("href") ) {
				score++;
				String recommendation = "Make sure links have a url set for the href value.";
				String description = "Link missing href attribute";
				String title = "Link missing href attribute";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.NONE,
																description,
																recommendation, 
																link,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																1,
																1);
				issue_messages.add(issue_message);
			}
			else {
				String recommendation = "Make sure links have a url set for the href value.";
				String description = "Link missing href attribute";
				String title = "Link missing href attribute";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH,
																description,
																recommendation, 
																link,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																0,
																1);
				issue_messages.add(issue_message);
				continue;
			}
			String href = element.attr("href");
			
			//if href is a mailto link then give score full remaining value and continue
			if(href.startsWith("mailto:")) {
				score += 4;
				String recommendation = "";
				String description = "Link uses mailto: protocol to allow users to send email";
				String title = "Link uses mailto: protocol";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.NONE,
																description,
																recommendation, 
																link,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																1,
																1);
				issue_messages.add(issue_message);
				continue;
			}
			//if href is a telephone link then give score full remaining value and continue
			else if(href.startsWith("tel:")) {
				score += 4;
				String recommendation = "";
				String description = "Link uses tel: protocol to allow users to call";
				String title = "Link uses mailto: protocol";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.NONE,
																description,
																recommendation, 
																link,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																1,
																1);
				issue_messages.add(issue_message);
				continue;
			}
			else if(element.hasAttr("role") 
					&& ("presentation".contentEquals(element.attr("role")) 
							|| "none".contentEquals(element.attr("role")))){
				//Skip this element because the prensentation/none role removes all semantic meaning from element
				continue;
			}
			
			//does element have an href value?
			if(href != null && !href.isEmpty()) {
				score++;
				String recommendation = "";
				String description = "Links have a url set for the href value";
				String title = "Link has url set for href value";

				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.NONE, 
																description, 
																recommendation, 
																link,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																1,
																1);
				issue_messages.add(issue_message);
				MessageBroadcaster.sendIssueMessage(page_state.getId(), issue_message);
			}
			else {
				String recommendation = "Make sure links have a url set for the href value";
				String description = "Make sure links have a url set for the href value";
				String title = "Link url is missing";

				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH, 
																description, 
																recommendation, 
																link,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																0,
																1);
				issue_messages.add(issue_message);
				MessageBroadcaster.sendIssueMessage(page_state.getId(), issue_message);
				continue;
			}
			
			//is element link a valid url?
			try {
				
				URI uri = new URI(href);
				if(!uri.isAbsolute()) {
					log.warn("URI is relative");
					href.replaceAll("../", "").strip();
					if(href.startsWith("//")) {
						href = href.substring(1);
					}
					
					if(href.contentEquals("/") || href.isEmpty()) {
						href = "";
					}
					else if(!href.startsWith("/") && href.length() > 1){
						href = "/"+href;
					}
					
					href = new URL(BrowserUtils.sanitizeUrl(page_state.getUrl())).getHost() + href;
					href = BrowserUtils.getPageUrl(new URL(BrowserUtils.sanitizeUrl(href)));
				}

				//if starts with / then append host
			
				score++;
				
				String recommendation = "";
				String description = "Link URL is properly formatted : "+href;
				String title = "Link URL is properly formatted";

				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.NONE, 
																description, 
																recommendation, 
																link,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																1,
																1);
				issue_messages.add(issue_message);
			} catch (MalformedURLException e) {
				String recommendation = "Make sure link url format is valid. For example \"https://www.google.com\"";
				String description = "link url is not a valid format "+href;
				String title = "Invalid link url format";

				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH, 
																description, 
																recommendation, 
																link,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																0,
																1);
				issue_messages.add(issue_message);
				e.printStackTrace();
			} catch (URISyntaxException e) {
				String recommendation = "Make sure link url format is valid. For example \"https://www.google.com\"";
				String description = "Invalid link uri syntax - "+href;
				String title = "Invalid link url";

				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH, 
																description, 
																recommendation, 
																link,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title,
																0,
																1);
				issue_messages.add(issue_message);
				e.printStackTrace();
			}
			
			//Does link have a valid URL? yes(1) / No(0)
			try {						
				if(BrowserUtils.isJavascript(href)) {
					log.warn("href value (before sanitizing) :: "+href);
					score++;
					String recommendation = "Links should have a valid URL in them. We suggest avoiding the use of the javascript protocol, expecially if you are going to use it to crete a non working link";
					String description = "This link has the href value set to 'javascript:void(0)', which causes the link to appear to users as if it doesn't work.";
					String title = "Invalid link url";

					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.HIGH,
																	description,
																	recommendation, 
																	link,
																	AuditCategory.INFORMATION_ARCHITECTURE,
																	labels,
																	ada_compliance,
																	title,
																	0,
																	1);
					issue_messages.add(issue_message);
				}
				else {
					
					URL url_href = new URL(BrowserUtils.sanitizeUrl(href));
					
					if(BrowserUtils.doesUrlExist(url_href)) {
						score++;
						String recommendation = "";
						String description = "Link points to valid location - "+href;
						String title = "Link points to valid location";
	
						ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																		Priority.NONE,
																		description,
																		recommendation, 
																		link,
																		AuditCategory.INFORMATION_ARCHITECTURE,
																		labels,
																		ada_compliance,
																		title,
																		1,
																		1);
						issue_messages.add(issue_message);
					}
					else {
						String recommendation = "Make sure links point to valid locations";
						String description = "Link destination could not be found - "+href;
						String title = "Invalid link url";
	
						ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																		Priority.HIGH,
																		description,
																		recommendation, 
																		link,
																		AuditCategory.INFORMATION_ARCHITECTURE,
																		labels,
																		ada_compliance,
																		title,
																		0,
																		1);
						issue_messages.add(issue_message);
					}
				}
			} catch (IOException e) {
				String recommendation = "Make sure links point to a valid url";
				String description = "Invalid link url (IOException) - "+href;
				String title = "Invalid link url";

				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH,
																description,
																recommendation, 
																link, 
																AuditCategory.INFORMATION_ARCHITECTURE, 
																labels,
																ada_compliance,
																title, 
																3,
																4);
				issue_messages.add(issue_message);
				log.warn("IO error occurred while auditing links ...."+e.getMessage());
				log.warn("href value :: "+href);
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//Does link contain a text label inside it
			 if(!link.getAllText().isEmpty()) {
				 score++;
				 
				String recommendation = "";
				String description = "Link contains text and is setup correctly. Well done!";
				String title = "Link is setup correctly and considered accessible";

				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH,
																description, 
																recommendation, 
																link,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title, 
																4,
																4);
			 }
			 else {
				 boolean element_includes_text = false;

				//send img src to google for text extraction
				try {
					URL url = new URL( link.getScreenshotUrl() );
					BufferedImage img_src = ImageIO.read( url );
					List<String> image_text_list = CloudVisionUtils.extractImageText(img_src);
					
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
					String recommendation = "For best usability make sure links include text. You can assign text to a link by entering text within the link tag or by using an image with text";
					String description = "Link doesn't contain any text";
					String title = "Link is missing text";

					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.HIGH,
																	description, 
																	recommendation, 
																	link,
																	AuditCategory.INFORMATION_ARCHITECTURE,
																	labels,
																	ada_compliance,
																	title, 
																	3,
																	4);
					 //does element use image as links?
					 issue_messages.add(issue_message);
				 }
				 else {
					 score++;
					 String recommendation = "";
					 String description = "Link contains text and is setup correctly. Well done!";
					 String title = "Link is setup correctly and considered accessible";

					 ElementStateIssueMessage issue_message = new ElementStateIssueMessage(Priority.HIGH,
																							description, 
																							recommendation, 
																							link,
																							AuditCategory.INFORMATION_ARCHITECTURE,
																							labels,
																							ada_compliance,
																							title, 
																							4,
																							4);
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
		
		
		/*
		String why_it_matters = "Dead links are links whose source can't be found. When users encounter dead links"
				+ " they perceive the validity of what you have to say as less valuable. Often, after experiencing a"
				+ " dead link, users bounce in search of a more reputable source.";
		*/
		String why_it_matters = "Links without text are less accessible as well as generally impacting usability. "
				+ "When links don't have text, users that rely on screen readers are unable to understand what links without text are meant to accomplish."
				+ "Links without text also affect how usable your site seems, because users may not be familiar with any images or icons used as links.";		
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.INFORMATION_ARCHITECTURE.getShortName());
		
		log.warn("LINKS AUDIT SCORE ::  "+score + " / " + (link_elements.size()*5));
		
		String description = "Making sure your links are setup correctly is incredibly important";
		
		int points_earned = 0;
		int max_points = 0;
		for(UXIssueMessage issue_msg : issue_messages) {
			points_earned += issue_msg.getPoints();
			max_points += issue_msg.getMaxPoints();
		}
		
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
						 AuditSubcategory.PERFORMANCE,
						 AuditName.LINKS,
						 points_earned,
						 issue_messages,
						 AuditLevel.PAGE,
						 max_points,
						 page_state.getUrl(),
						 why_it_matters, 
						 description,
						 page_state,
						 true); 
		//the contstant 6 in this equation is the exact number of boolean checks for this audit
	}
}

package com.looksee.models.audit;

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
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
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
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page_state) {
		assert page_state != null;
		
		Set<UXIssueMessage> issue_messages = new HashSet<>();
	
		//List<ElementState> link_elements = page_state_service.getLinkElementStates(user_id, page_state.getKey());
		List<ElementState> link_elements = new ArrayList<>();
		List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
		
		String ada_compliance = "There is no ADA guideline for dead links";
		
		for(ElementState element : elements) {
			if(element.getName().equalsIgnoreCase("a")) {
				link_elements.add(element);
			}
		}
		
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
				String description = "Link missing href attribute";
				String title = "Link missing href attribute";

				Set<String> labels = new HashSet<>();
				labels.add("information architecture");
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH,
																description,
																recommendation, 
																link,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title);
				issue_messages.add(issue_message);
				continue;
			}
			String href = element.attr("href");
			
			//if href is a mailto link then give score full remaining value and continue
			if(href.startsWith("mailto:")) {
				score += 4;
				continue;
			}
			Set<String> labels = new HashSet<>();
			labels.add("information architecture");
			
			//does element have an href value?
			if(href != null && !href.isEmpty()) {
				score++;
			}
			else {
				String recommendation = "Make sure links have a url set for the href value";
				String description = "Make sure links have a url set for the href value";
				String title = "Link url is missing";

				ElementStateIssueMessage issue_Message = new ElementStateIssueMessage(
																Priority.HIGH, 
																description, 
																recommendation, 
																link,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance,
																title);
				issue_messages.add(issue_Message);
				continue;
			}
			
			//is element link a valid url?
			try {
				
				URI uri = new URI(href);
				if(!uri.isAbsolute()) {
					log.warn("URI is relative");
					URL page_url = new URL(BrowserUtils.sanitizeUrl(page_state.getUrl()));
					href.replaceAll("../", "");
					if(href.startsWith("/") && href.length() > 1) {
						href = href.substring(1);
					}
					else if(href.strip().contentEquals("/")) {
						href = "";
					}
					href = BrowserUtils.getPageUrl(page_url);
				}

				//if starts with / then append host
			
				score++;
			} catch (MalformedURLException e) {
				String recommendation = "Make sure links point to a valid url";
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
																title);
				issue_messages.add(issue_message);
				e.printStackTrace();
			} catch (URISyntaxException e) {
				String recommendation = "Make sure links point to a valid url";
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
																title);
				issue_messages.add(issue_message);
				e.printStackTrace();
			}
			
			//Does link have a valid URL? yes(1) / No(0)
			try {
				URL url_href = new URL(BrowserUtils.sanitizeUrl(href));
				
				if(BrowserUtils.doesUrlExist(url_href)) {
					score++;
				}
				else {
					String recommendation = "Make sure links point to valid locations";
					String description = "Link destination could not be found - "+href;
					String title = "Invalid link url";

					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.HIGH,
																	description,
																	recommendation, link,
																	AuditCategory.INFORMATION_ARCHITECTURE,
																	labels,
																	ada_compliance,
																	title);
					issue_messages.add(issue_message);
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
																title);
				issue_messages.add(issue_message);
				log.warn("IO error occurred while auditing links ...."+e.getMessage());
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//Does link contain a text label inside it
			 if(!link.getAllText().isEmpty()) {
				 score++;
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
																	title);
					 //does element use image as links?
					issue_messages.add(issue_message);
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
		
		
		Set<String> labels = new HashSet<>();
		labels.add("accessibility");
		labels.add("information architecture");
		labels.add("navigation");
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.INFORMATION_ARCHITECTURE.getShortName());
		
		log.warn("LINKS AUDIT SCORE ::  "+score + " / " + (link_elements.size()*5));
		
		String description = "Making sure your links are setup correctly is incredibly important";
		
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
						 AuditSubcategory.PERFORMANCE,
						 AuditName.LINKS,
						 score,
						 issue_messages,
						 AuditLevel.PAGE,
						 link_elements.size() * MAX_POINTS_EACH,
						 page_state.getUrl(),
						 why_it_matters, 
						 description,
						 page_state); 
		//the contstant 6 in this equation is the exact number of boolean checks for this audit
	}
}

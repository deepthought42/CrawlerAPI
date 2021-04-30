package com.looksee.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.Priority;

/**
 * Responsible for executing an audit on the images on a page to determine adherence to alternate text best practices 
 *  for the visual audit category
 */
@Component
public class ImageAltTextAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ImageAltTextAudit.class);
	
	public ImageAltTextAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
	}

	
	/**
	 * {@inheritDoc}
	 * 
	 * Scores images on a page based on if the image has an "alt" value present, format is valid and the 
	 *   url goes to a location that doesn't produce a 4xx error 
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page_state) {
		assert page_state != null;
		
		Set<UXIssueMessage> issue_messages =  new HashSet<>();

		Set<String> labels = new HashSet<>();
		labels.add("accessibility");
		labels.add("images");
		
		String tag_name = "img";
		//List<ElementState> link_elements = page_state_service.getLinkElementStates(user_id, page_state.getKey());
		List<ElementState> image_elements = new ArrayList<>();
		for(ElementState element : page_state.getElements()) {
			if(element.getName().equalsIgnoreCase(tag_name)) {
				image_elements.add(element);
			}
		}
		
		String why_it_matters = "Alt-text helps with both SEO and accessibility. Search engines use alt-text"
				+ " to help determine how usable and your site is as a way of ranking your site.";
		
		String ada_compliance = "Your website does not meet the level A ADA compliance requirement for" + 
				" ‘Alt’ text for images present on the website.";
	
		
		//score each link element
		int score = 0;
		for(ElementState image_element : image_elements) {
	
			Document jsoup_doc = Jsoup.parseBodyFragment(image_element.getOuterHtml(), page_state.getUrl());
			Element element = jsoup_doc.getElementsByTag(tag_name).first();
			
			//Check if element has "alt" attribute present
			if(element.hasAttr("alt")) {
				score++;
			}
			else {
				String description = "Images without alternative text attribute";
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH, 
																description, 
																"Images without alternative text attribute", 
																image_element,
																AuditCategory.INFORMATION_ARCHITECTURE, 
																labels,
																ada_compliance);
				issue_messages.add(issue_message);
			}
			
			if(element.attr("alt").isEmpty()) {
				String description = "Images without alternative text defined as a non empty string value";
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH, 
																description, 
																"Images without alternative text defined as a non empty string value", 
																image_element,
																AuditCategory.INFORMATION_ARCHITECTURE,
																labels,
																ada_compliance);
				issue_messages.add(issue_message);
			}
			else {
				score++;
			}
		}
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.AESTHETICS.toString());
		
		/*
		if(!images_with_alt_text_defined.isEmpty()) {
			ElementStateObservation observation = new ElementStateObservation(
					images_with_alt_text_defined, 
					"Images that have alternative text defined as a non empty string value");
			observations.add(observation_service.save(observation));
		}
		*/
		
		log.warn("LINKS AUDIT SCORE ::  "+score + " / " + (image_elements.size()*2));
		String description = "Images without alternative text defined as a non empty string value";
		
		return new Audit(AuditCategory.CONTENT,
						 AuditSubcategory.IMAGERY,
						 AuditName.ALT_TEXT,
						 score,
						 issue_messages,
						 AuditLevel.PAGE,
						 image_elements.size()*2,
						 page_state.getUrl(), 
						 why_it_matters, 
						 description);
		
		//the contstant 2 in this equation is the exact number of boolean checks for this audit
	}
}

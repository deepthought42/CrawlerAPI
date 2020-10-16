package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
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

/**
 * Responsible for executing an audit on the images on a page to determine adherence to alternate text best practices 
 *  for the visual audit category
 */
@Component
public class ImageAltTextAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ImageAltTextAudit.class);
	
	@Autowired
	private ObservationService observation_service;
	
	private List<ElementState> images_without_alt_text =  new ArrayList<>();
	private List<ElementState> images_with_alt_text =  new ArrayList<>();
	private List<ElementState> images_without_alt_text_defined =  new ArrayList<>();
	private List<ElementState> images_with_alt_text_defined =  new ArrayList<>();

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
		
		String tag_name = "img";
		//List<ElementState> link_elements = page_state_service.getLinkElementStates(user_id, page_state.getKey());
		List<ElementState> image_elements = new ArrayList<>();
		for(ElementState element : page_state.getElements()) {
			if(element.getName().equalsIgnoreCase(tag_name)) {
				image_elements.add(element);
			}
		}
		
		List<Observation> observations = new ArrayList<>();
		//score each link element
		int score = 0;
		for(ElementState image_element : image_elements) {
	
			Document jsoup_doc = Jsoup.parseBodyFragment(image_element.getOuterHtml(), page_state.getUrl());
			Element element = jsoup_doc.getElementsByTag(tag_name).first();
			
			//Check if element has "alt" attribute present
			if(element.hasAttr("alt")) {
				score++;
				images_with_alt_text.add(image_element);
			}
			else {
				images_without_alt_text.add(image_element);
			}
			
			if(element.attr("alt").isEmpty()) {
				images_without_alt_text_defined.add(image_element);
			}
			else {
				score++;
				images_with_alt_text_defined.add(image_element);
			}
		}
		
		if(!images_without_alt_text.isEmpty()) {
			ElementStateObservation observation = new ElementStateObservation(
					images_without_alt_text, 
					"Images without alternative text attribute");
			observations.add(observation_service.save(observation));
		}
		
		if(!images_with_alt_text.isEmpty()) {
			ElementStateObservation observation = new ElementStateObservation(
					images_with_alt_text, 
					"Images that have alternative text attribute. These elements are more accessibly");
			observations.add(observation_service.save(observation));
		}
		
		if(!images_without_alt_text_defined.isEmpty()) {
			ElementStateObservation observation = new ElementStateObservation(
					images_without_alt_text_defined, 
					"Images without alternative text defined as a non empty string value");
			observations.add(observation_service.save(observation));
		}
		
		if(!images_with_alt_text_defined.isEmpty()) {
			ElementStateObservation observation = new ElementStateObservation(
					images_with_alt_text_defined, 
					"Images that have alternative text defined as a non empty string value");
			observations.add(observation_service.save(observation));
		}
		
		log.warn("LINKS AUDIT SCORE ::  "+score + " / " + (image_elements.size()*2));
		
		return new Audit(AuditCategory.VISUALS, 
						 AuditSubcategory.ALT_TEXT,
						 score, 
						 observations, 
						 AuditLevel.PAGE, 
						 image_elements.size()*2, page_state.getUrl()); 
		//the contstant 2 in this equation is the exact number of boolean checks for this audit
	}
}

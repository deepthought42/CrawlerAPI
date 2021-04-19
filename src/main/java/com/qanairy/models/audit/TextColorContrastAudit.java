package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditName;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.models.enums.Priority;
import com.qanairy.services.ElementStateService;
import com.qanairy.services.PageStateService;
import com.qanairy.services.UXIssueMessageService;
import com.qanairy.utils.BrowserUtils;
import com.qanairy.utils.ElementStateUtils;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class TextColorContrastAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TextColorContrastAudit.class);
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired 
	private ElementStateService element_state_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	public TextColorContrastAudit() {}

	/**
	 * {@inheritDoc}
	 * 
	 * Identifies colors used on page, the color scheme type used, and the ultimately the score for how the colors used conform to scheme
	 *  
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page_state) {
		assert page_state != null;
		
		int total_headlines = 0;
		int total_text_elems = 0;
		int headline_score = 0;
		int text_score = 0;

		List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
		//filter elements that aren't text elements
		List<ElementState> element_list = BrowserUtils.getTextElements(elements);
		
		String why_it_matters = "Color, just like the overall design, goes beyond aesthetics. It impacts the" + 
				" usability and functionality of your website, deciding what information" + 
				" stands out to the user." + 
				" A good contrast ratio makes your content easy to read and navigate" + 
				" through, creating a comfortable and engaging experience for your user. ";
		
		String ada_compliance = "Most items meet the minimum required contrast ratio. However, the" + 
				" small text items in grey do not meet the minimum contrast ratio of 4.5:1.";

		Set<UXIssueMessage> issue_messages = new HashSet<>();
		String recommendation = "Use colors for text and images of text with background colors that have a contrast of at least 4.5:1 for ADA compliance";
		
		Set<String> labels = new HashSet<>();
		labels.add("accessibility");
		labels.add("color contrast");
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.AESTHETICS.toString());
		
		//analyze screenshots of all text images for contrast
		for(ElementState element : element_list) {			
			try {
				String color = element.getRenderedCssValues().get("color");
				ColorData text_color = new ColorData(color);
				
				//Identify background color by getting largest color used in picture
				//ColorData background_color_data = ImageUtils.extractBackgroundColor(new URL(element.getScreenshotUrl()));
				ColorData background_color = new ColorData(element.getBackgroundColor());
				double contrast = ColorData.computeContrast(background_color, text_color);
				element.setTextContrast(contrast);
				element_state_service.save(element);
				if(ElementStateUtils.isHeader(element.getName())) {
					//score header element
					//calculate contrast between text color and background-color
					total_headlines++;
					/*
					headlines < 3; value = 1
					headlines > 3 and headlines < 4.5; value = 2
					headlines >= 4.5; value = 3
					 */
					if(contrast < 3) {
						//No points are rewarded for low contrast headers
						//low_header_contrast.add(element);
						String description = "Headline text has low contrast against the background";
						ColorContrastIssueMessage low_header_contrast_observation = new ColorContrastIssueMessage(
								Priority.HIGH,
								description,
								recommendation,
								contrast,
								text_color.rgb(),
								background_color.rgb(), 
								element,
								AuditCategory.AESTHETICS,
								labels);
						issue_messages.add(issue_message_service.save(low_header_contrast_observation));
					}
					else if(contrast >= 3 && contrast < 4.5) {
						headline_score += 1;
						String description = "Headline text has medium contrast against the background";
						ColorContrastIssueMessage mid_header_contrast_observation = new ColorContrastIssueMessage(
								Priority.HIGH,
								description,
								recommendation,
								contrast,
								text_color.rgb(),
								background_color.rgb(), 
								element,
								AuditCategory.AESTHETICS, 
								labels);

						issue_messages.add(issue_message_service.save(mid_header_contrast_observation));
					}
					else if(contrast >= 4.5) {
						headline_score += 2;
					}
				}
				else {
					total_text_elems++;
					/*
						text < 4.5; value = 1
						text >= 4.5 and text < 7; value = 2
						text >=7; value = 3
					 */
					if(contrast < 4.5) {
						String description = "Text has low contrast against the background";

						ColorContrastIssueMessage low_text_observation = new ColorContrastIssueMessage(
								Priority.HIGH,
								description,
								recommendation,
								contrast,
								text_color.rgb(),
								background_color.rgb(), 
								element,
								AuditCategory.AESTHETICS, 
								labels);
						//observations.add(observation_service.save(low_text_observation));

						//No points are rewarded for low contrast text
						issue_messages.add(issue_message_service.save(low_text_observation));
					}
					else if(contrast >= 4.5 && contrast < 7) {
						String description = "Text has medium contrast against the background";

						text_score += 1;
						ColorContrastIssueMessage mid_text_observation = new ColorContrastIssueMessage(
								Priority.HIGH,
								description,
								recommendation,
								contrast,
								text_color.rgb(),
								background_color.rgb(), 
								element,
								AuditCategory.AESTHETICS,
								labels);
						//observations.add(observation_service.save(mid_text_observation));

						issue_messages.add(issue_message_service.save(mid_text_observation));
					}
					else if(contrast >= 7) {
						text_score += 2;
					}
				}
			} catch (Exception e) {
				log.warn("element screenshot url  :: "+element.getScreenshotUrl());
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//create observation with issue messages inside
		
		
		/*
		if(!high_header_contrast.isEmpty()) {
			ElementStateObservation high_header_contrast_observation = new ElementStateObservation(high_header_contrast, "Headers with contrast above 4.5");
			issues.add(observation_service.save(high_header_contrast_observation));
		}
		*/
		
		int total_possible_points = ((total_headlines*2) + (total_text_elems*2));
		log.warn("TEXT COLOR CONTRAST AUDIT SCORE   ::   " + (headline_score+text_score) + " : " + total_possible_points);
	
		return new Audit(AuditCategory.AESTHETICS,
						 AuditSubcategory.COLOR_MANAGEMENT,
					     AuditName.TEXT_BACKGROUND_CONTRAST,
					     (headline_score + text_score),
					     issue_messages, 
					     AuditLevel.PAGE,
					     total_possible_points,
					     page_state.getUrl(),
					     why_it_matters,
					     ada_compliance,
					     "Text with contrast below 4.5");
	}
}
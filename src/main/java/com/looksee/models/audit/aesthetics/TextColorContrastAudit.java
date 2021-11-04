package com.looksee.models.audit.aesthetics;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.api.MessageBroadcaster;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.ColorContrastIssueMessage;
import com.looksee.models.audit.ColorData;
import com.looksee.models.audit.IExecutablePageStateAudit;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.Priority;
import com.looksee.services.ElementStateService;
import com.looksee.services.PageStateService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ImageUtils;


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
	
	public TextColorContrastAudit() {}

	/**
	 * {@inheritDoc}
	 * 
	 * Identifies colors used on page, the color scheme type used, and the ultimately the score for how the colors used conform to scheme
	 * 
	 * WCAG Success Criteria Source - https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html
	 * 
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record) {
		assert page_state != null;
		
		List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
		//filter elements that aren't text elements
		List<ElementState> element_list = BrowserUtils.getTextElements(elements);
		
		String why_it_matters = "Color, just like the overall design, goes beyond aesthetics. It impacts the" + 
				" usability and functionality of your website, deciding what information" + 
				" stands out to the user." + 
				" A good contrast ratio makes your content easy to read and navigate" + 
				" through, creating a comfortable and engaging experience for your user. ";

		Set<UXIssueMessage> issue_messages = new HashSet<>();
		String recommendation = "Use colors for text and images of text with background colors that have a contrast of at least 4.5:1 for ADA compliance";
		
		//analyze screenshots of all text images for contrast
		for(ElementState element : element_list) {
			Set<String> labels = new HashSet<>();
			labels.add(AuditCategory.AESTHETICS.toString().toLowerCase());
			labels.add("accessibility");
			labels.add("color contrast");
			
			ColorData font_color = new ColorData(element.getRenderedCssValues().get("color"));

			try {	
				//extract opacity color
				ColorData bkg_color = null;
				if(element.getScreenshotUrl().trim().isEmpty()) {
					bkg_color = new ColorData(element.getRenderedCssValues().get("background-color"));
				}
				else {
					bkg_color = ImageUtils.extractBackgroundColor( new URL(element.getScreenshotUrl()),
																   font_color); 
				}
				String bg_color = bkg_color.rgb();	
				
				//Identify background color by getting largest color used in picture
				//ColorData background_color_data = ImageUtils.extractBackgroundColor(new URL(element.getScreenshotUrl()));
				ColorData background_color = new ColorData(bg_color);
				element.setBackgroundColor(background_color.rgb());
				element.setForegroundColor(font_color.rgb());
				
				double contrast = ColorData.computeContrast(background_color, font_color);
				element.setTextContrast(contrast);
				element = element_state_service.save(element);
				
				if(!element.getOwnedText().isEmpty()){
					String og_font_size_str = element.getRenderedCssValues().get("font-size");
					String font_weight = element.getRenderedCssValues().get("font-weight");

					String font_size_str = og_font_size_str.replace("px", "");
					
					double font_size = BrowserUtils.convertPxToPt(Double.parseDouble(font_size_str.strip()));
					//if font size is greater than 18 point(24px) or if greater than 14 point(18.5px) and bold then check if contrast > 3 (A Compliance)
					//NOTE: The following measures of font size are in pixels not font points
					if(font_size >= 18 || (font_size >= 14 && BrowserUtils.isTextBold(font_weight))) {
						if( contrast < 3 ) {
							//low contrast header issue
							String title = "Large text has low contrast";
							String ada_compliance = "Text that is larger than 18 point or larger than 14 point and bold should meet the minimum contrast ratio of 3:1.";
							String description = "Headline text has low contrast against the background";
							recommendation = "Increase the contrast by either making the text darker or the background lighter";

							ColorContrastIssueMessage low_header_contrast_observation = new ColorContrastIssueMessage(
																									Priority.HIGH,
																									description,
																									recommendation,
																									contrast,
																									font_color.rgb(),
																									background_color.rgb(), 
																									element,
																									AuditCategory.AESTHETICS,
																									labels, 
																									ada_compliance,
																									title, 
																									font_size+"", 
																									0, 
																									2);

							issue_messages.add(low_header_contrast_observation);
							MessageBroadcaster.sendIssueMessage(page_state.getId(), low_header_contrast_observation);

						}
						else if(contrast >= 3 && contrast < 4.5) {
							//100% score
							//AA WCAG 2.1
							String title = "Large text meets minimum contrast for WCAG 2.1 compliance";
							String ada_compliance = "Text that is larger than 18pt font or larger than 14pt and bolded should meets minimum contrast of 3:1 for WCAG 2.1 AA standard.";
							//String description = "Headline text has recommended contrast against the background for <a href='https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html'>WCAG 2.1 AA</a> standard";
							String description = "Headline text has recommended contrast against the background for WCAG 2.1 AA standard";

							recommendation = "To reach AAA standards for WCAG 2.1 increase contrast to 4.5:1";
							labels.add("WCAG 2.1 AA");
							
							ColorContrastIssueMessage low_header_contrast_observation = new ColorContrastIssueMessage(
																									Priority.MEDIUM,
																									description,
																									recommendation,
																									contrast,
																									font_color.rgb(),
																									background_color.rgb(), 
																									element,
																									AuditCategory.AESTHETICS,
																									labels, 
																									ada_compliance,
																									title, 
																									font_size+"",
																									1,
																									2);
							
							issue_messages.add(low_header_contrast_observation);
							MessageBroadcaster.sendIssueMessage(page_state.getId(), low_header_contrast_observation);
						}
						else if(contrast >= 4.5) {
							//100% score
							//low contrast header issue
							String title = "Large text complies with WCAG 2.1 AAA standard";
							String ada_compliance = "Text that is larger than 18pt font or larger than 14pt and bolded should meets minimum contrast of 4.5:1 to meet WCAG 2.1 AAA standards.";
							//String description = "Headline text has recommended contrast for <a href='https://www.w3.org/WAI/WCAG21/Understanding/contrast-enhanced.html'>WCAG 2.1 AAA</a> standards against the background";
							String description = "Headline text has recommended contrast for WCAG 2.1 AAA standards against the background";
							recommendation = "";
							labels.add("WCAG 2.1 AAA");
							
							ColorContrastIssueMessage low_header_contrast_observation = new ColorContrastIssueMessage(
																									Priority.NONE,
																									description,
																									recommendation,
																									contrast,
																									font_color.rgb(),
																									background_color.rgb(), 
																									element,
																									AuditCategory.AESTHETICS,
																									labels, 
																									ada_compliance,
																									title, 
																									font_size+"",
																									2,
																									2);
							
							issue_messages.add(low_header_contrast_observation);
							MessageBroadcaster.sendIssueMessage(page_state.getId(), low_header_contrast_observation);
						}
					}
					else if((font_size < 18 && font_size >= 14 && !BrowserUtils.isTextBold(font_weight)) || font_size < 14 ) {
						if( contrast < 4.50 ) {
							//fail
							String title = "Text has low contrast";
							String description = "Text has low contrast against the background";
							String ada_compliance = "Text that is smaller than 18 point and larger than 14 point but not bold or just smaller than 14 point fonts should meet the minimum contrast ratio of 4.5:1.";
							recommendation = "Increase the contrast by either making the text darker or the background lighter";

							ColorContrastIssueMessage low_text_observation = new ColorContrastIssueMessage(
																						Priority.HIGH,
																						description,
																						recommendation,
																						contrast,
																						font_color.rgb(),
																						background_color.rgb(), 
																						element,
																						AuditCategory.AESTHETICS, 
																						labels, 
																						ada_compliance,
																						title,
																						font_size+"",
																						0, 
																						2);
							//observations.add(observation_service.save(low_text_observation));

							//No points are rewarded for low contrast text
							issue_messages.add(low_text_observation);
							MessageBroadcaster.sendIssueMessage(page_state.getId(), low_text_observation);

						}
						else if(contrast >= 4.50 && contrast < 7.0) {
							//100% score
							String title = "Text has minimum contrast for WCAG 2.1 AA standards";
							String description = "Text has minimum contrast against the background";
							String ada_compliance = "Text contrast meets WCAG 2.1 AA standards.";
							recommendation = "To reach AAA standards for WCAG 2.1 increase contrast to 7:1";

							labels.add("WCAG 2.1 AA");
							
							ColorContrastIssueMessage med_contrast_text_observation = new ColorContrastIssueMessage(
																						Priority.MEDIUM,
																						description,
																						recommendation,
																						contrast,
																						font_color.rgb(),
																						background_color.rgb(), 
																						element,
																						AuditCategory.AESTHETICS, 
																						labels, 
																						ada_compliance,
																						title,
																						font_size+"",
																						1, 
																						2);
							issue_messages.add(med_contrast_text_observation);
						}
						else if(contrast >= 7.0) {
							//100% score
							String title = "Text has appropriate contrast";
							String description = "Text has recommended contrast against the background";
							String ada_compliance = "Text contrast meets WCAG 2.1 enhanced(AAA) standards.";
							recommendation = "";
							labels.add("WCAG 2.1 AAA");
							
							ColorContrastIssueMessage high_contrast_text_observation = new ColorContrastIssueMessage(
																						Priority.NONE,
																						description,
																						recommendation,
																						contrast,
																						font_color.rgb(),
																						background_color.rgb(), 
																						element,
																						AuditCategory.AESTHETICS, 
																						labels, 
																						ada_compliance,
																						title,
																						font_size+"",
																						2, 
																						2);
							
							issue_messages.add(high_contrast_text_observation);
						}
					}
				}
			} catch(NullPointerException e) {
				log.warn("NPE thrown during text color contrast audit");
				e.printStackTrace();
			} catch (Exception e) {
				log.warn("element screenshot url  :: "+element.getScreenshotUrl());
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
		int points_earned = 0;
		int max_points = 0;
		for(UXIssueMessage issue_msg : issue_messages) {
			double multiplier = 1.0;
			if(issue_msg.getPriority().equals(Priority.HIGH)) {
				multiplier = 2.0;
			}
			
			points_earned += (issue_msg.getPoints() / multiplier);
			max_points += issue_msg.getMaxPoints();
		}
		
		//log.warn("TEXT COLOR CONTRAST AUDIT SCORE   ::   " + points_earned + " : " + max_points);	

		return new Audit(AuditCategory.AESTHETICS,
						 AuditSubcategory.COLOR_MANAGEMENT,
					     AuditName.TEXT_BACKGROUND_CONTRAST,
					     points_earned,
					     issue_messages, 
					     AuditLevel.PAGE,
					     max_points,
					     page_state.getUrl(),
					     why_it_matters,
					     "Text with contrast below 4.5", 
						 true);
	}
}
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
import com.looksee.services.UXIssueMessageService;
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
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
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
		
		int total_possible_points = 0;
		int text_score = 0;

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
		
		Set<String> labels = new HashSet<>();
		labels.add("accessibility");
		labels.add("color contrast");
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.AESTHETICS.toString());
		
		//analyze screenshots of all text images for contrast
		for(ElementState element : element_list) {
			ColorData font_color = new ColorData(element.getRenderedCssValues().get("color"));

			try {	
				//extract opacity color
				ColorData bkg_color = null;
				if(element.getScreenshotUrl().trim().isEmpty()) {
					bkg_color = new ColorData(element.getRenderedCssValues().get("background-color"));
				}
				else {
					bkg_color = ImageUtils.extractBackgroundColor( 
													new URL(element.getScreenshotUrl()),
													font_color); 
				}
				String bg_color = bkg_color.rgb();	
				ColorData text_color = new ColorData(element.getRenderedCssValues().get("color"));
				
				//Identify background color by getting largest color used in picture
				//ColorData background_color_data = ImageUtils.extractBackgroundColor(new URL(element.getScreenshotUrl()));
				ColorData background_color = new ColorData(bg_color);
				element.setBackgroundColor(background_color.rgb());
				element.setForegroundColor(text_color.rgb());
				
				double contrast = ColorData.computeContrast(background_color, text_color);
				element.setTextContrast(contrast);
				element = element_state_service.save(element);
				
				if(!element.getOwnedText().isEmpty()){
					String og_font_size_str = element.getRenderedCssValues().get("font-size");
					String font_weight = element.getRenderedCssValues().get("font-weight");

					String font_size_str = og_font_size_str.replace("px", "");
					
					double font_size = BrowserUtils.convertPxToPt(Double.parseDouble(font_size_str.strip()));
					total_possible_points += 1;
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
																									text_color.rgb(),
																									background_color.rgb(), 
																									element,
																									AuditCategory.AESTHETICS,
																									labels, 
																									ada_compliance,
																									title, 
																									font_size+"");
							
							issue_messages.add(issue_message_service.save(low_header_contrast_observation));
							MessageBroadcaster.sendIssueMessage(page_state.getId(), low_header_contrast_observation);

						}
						else if(contrast >= 3) {
							text_score += 1;
							//100% score
						}
					}
					else if((font_size < 18 && font_size >= 14 && !BrowserUtils.isTextBold(font_weight)) || font_size < 14 ) {
						if( contrast < 4.5 ) {
							//fail
							String title = "Text has low contrast";
							String description = "Text has low contrast against the background";
							String ada_compliance = "Text that is smaller than 18 point and larger than 14 point but not bold or just smaller than 14 point fonts should meet the minimum contrast ratio of 4.5:1.";
							ColorContrastIssueMessage low_text_observation = new ColorContrastIssueMessage(
																						Priority.HIGH,
																						description,
																						recommendation,
																						contrast,
																						text_color.rgb(),
																						background_color.rgb(), 
																						element,
																						AuditCategory.AESTHETICS, 
																						labels, 
																						ada_compliance,
																						title,
																						font_size+"");
							//observations.add(observation_service.save(low_text_observation));

							//No points are rewarded for low contrast text
							issue_messages.add(issue_message_service.save(low_text_observation));
							MessageBroadcaster.sendIssueMessage(page_state.getId(), low_text_observation);

						}
						else if(contrast >= 4.5) {
							text_score += 1;
							//100% score
						}
					}
				}
				
				/*
				if(ElementStateUtils.isHeader(element.getName())) {
					//score header element
					//calculate contrast between text color and background-color
					total_headlines++;
				
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
				*/
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
		
		log.warn("TEXT COLOR CONTRAST AUDIT SCORE   ::   " + text_score + " : " + total_possible_points);
	
		return new Audit(AuditCategory.AESTHETICS,
						 AuditSubcategory.COLOR_MANAGEMENT,
					     AuditName.TEXT_BACKGROUND_CONTRAST,
					     text_score,
					     issue_messages, 
					     AuditLevel.PAGE,
					     total_possible_points,
					     page_state.getUrl(),
					     why_it_matters,
					     "Text with contrast below 4.5", 
						 page_state,
						 true);
	}
}
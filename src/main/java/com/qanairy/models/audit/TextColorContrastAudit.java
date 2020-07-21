package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.services.DomainService;
import com.qanairy.services.ElementStateService;
import com.qanairy.utils.ElementStateUtils;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class TextColorContrastAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TextColorContrastAudit.class);
	
	@Autowired
	private ElementStateService element_state_service;
	
	public TextColorContrastAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.TEXT_BACKGROUND_CONTRAST);
	}
	
	private static String getAuditDescription() {
		return "Color contrast between background and text.";
	}

	private static List<String> buildBestPractices() {
		List<String> best_practices = new ArrayList<>();
		best_practices.add("According to the WCAG, \r\n" + 
				"Text: Contrast of 4.5 - 7 with the background. \r\n" + 
				"Large text/ Headlines: Contrast of 3 - 4.5 with the background. \r\n" + 
				"Black on white or vice versa is not recommended.");
		
		return best_practices;
	}
	
	private static String getAdaDescription() {
		return "1.4.1 - Use of Color \r\n" + 
				"Color is not used as the only visual means of conveying information, indicating an action, prompting a response, or distinguishing a visual element.\r\n";
	}
	
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
		
		List<Observation> observations = new ArrayList<>();
		int total_headlines = 0;
		int total_text_elems = 0;
		double headline_score = 0;
		double text_score = 0;
		
		List<ElementState> mid_header_contrast = new ArrayList<>();
		List<ElementState> low_header_contrast = new ArrayList<>();

		List<ElementState> mid_text_contrast = new ArrayList<>();
		List<ElementState> low_text_contrast = new ArrayList<>();

		List<ElementState> element_list = new ArrayList<>();
		log.warn("Elements available for TEXT COLOR CONTRAST evaluation ...  "+page_state.getElements().size());
		//filter elements that aren't text elements
		for(ElementState element : page_state.getElements()) {
			if(element.getText()==null || element.getText().trim().isEmpty()) {
				continue;
			}
			element_list.add(element);
		}
		
		
		//identify all colors used on page. Images are not considered
		for(ElementState element : element_list) {
			//check element for color css property
			String color = element.getPreRenderCssValues().get("color");
			
			//check element for background-color css property
			String background_color = element.getPreRenderCssValues().get("background-color");
			
			
			if(color == null) {
				continue;
			}
			
			color = color.replace("transparent", "");
			color = color.replace("!important", "");
			color = color.trim();
			
			ColorData color_data = new ColorData(color.trim());
			ColorData background_color_data = null;
			ElementState parent = element.clone();
			background_color = background_color.replace("transparent", "");
			background_color = background_color.replace("!important", "");
			background_color = background_color.trim();

			if(background_color == null || background_color.isEmpty()) {
				do {
					parent = element_state_service.getParentElement(page_state.getKey(), parent.getKey());
					
					if(parent == null) {
						continue;
					}
					background_color = parent.getPreRenderCssValues().get("background-color");
					if(background_color != null) {
						//extract r,g,b,a from color css		
						background_color = background_color.replace("transparent", "");
						background_color = background_color.replace("!important", "");
						background_color = background_color.trim();
					}
				}while((background_color == null || background_color.isEmpty()) && parent != null);
			}
			
			if((background_color == null  || background_color.isEmpty())) {
				background_color = "#ffffff";
			}
			
			
			background_color_data = new ColorData(background_color.trim());
			
			double max_luminosity = 0.0;
			double min_luminosity = 0.0;
			
			if(color_data.getLuminosity() > background_color_data.getLuminosity()) {
				min_luminosity = background_color_data.getLuminosity();
				max_luminosity = color_data.getLuminosity();
			}
			else {
				min_luminosity = color_data.getLuminosity();
				max_luminosity = background_color_data.getLuminosity();
			}
			double contrast = 0.0;
			if(ElementStateUtils.isHeader(element)) {
				//score header element
				//calculate contrast between text color and background-color
				contrast = (max_luminosity + 0.05) / (min_luminosity + 0.05);
				total_headlines++;
				/*
				headlines < 3; value = 1
				headlines > 3 and headlines < 4.5; value = 2
				headlines >= 4.5; value = 3
				 */
				if(contrast < 3) {
					headline_score += 1;
					low_header_contrast.add(element);
				}
				else if(contrast >= 3 && contrast < 4.5) {
					headline_score += 2;
					mid_header_contrast.add(element);
				}
				else if(contrast >= 4.5) {
					headline_score += 3;
				}
			}
			else if(ElementStateUtils.isTextContainer(element)) {
				contrast = (max_luminosity + 0.05) / (min_luminosity + 0.05);
				total_text_elems++;
				/*
				text < 4.5; value = 1
				text >= 4.5 and text < 7; value = 2
				text >=7; value = 3
				 */
				if(contrast < 4.5) {
					text_score += 1;
					low_text_contrast.add(element);
				}
				else if(contrast >= 4.5 && contrast < 7) {
					text_score += 2;
					mid_text_contrast.add(element);
				}
				else if(contrast >= 7) {
					text_score += 3;
				}
			}
		}
		
		ElementObservation mid_header_contrast_observation = new ElementObservation(mid_header_contrast, "Headers with contrast between 3 and 4.5");
		ElementObservation low_header_contrast_observation = new ElementObservation(low_header_contrast, "Headers with contrast below 3");
		ElementObservation mid_header_text_observation = new ElementObservation(mid_text_contrast, "Headers with contrast between 4.5 and 7");
		ElementObservation low_header_text_observation = new ElementObservation(low_text_contrast, "Headers with contrast below 4.5");
		
		observations.add(mid_header_text_observation);
		observations.add(mid_header_contrast_observation);
		observations.add(low_header_text_observation);
		observations.add(low_header_contrast_observation);
		
		double score = (headline_score+text_score)/((total_headlines*3) + (total_text_elems*3));		
		log.warn("TEXT COLOR CONTRAST AUDIT SCORE   ::   "+score);
		return new Audit(AuditCategory.COLOR_MANAGEMENT, buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.TEXT_BACKGROUND_CONTRAST, score, observations, AuditLevel.PAGE);
	}
}
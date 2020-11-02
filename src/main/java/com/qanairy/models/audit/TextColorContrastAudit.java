package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.services.ElementStateService;
import com.qanairy.services.ObservationService;
import com.qanairy.services.PageStateService;
import com.qanairy.utils.BrowserUtils;
import com.qanairy.utils.ElementStateUtils;
import com.qanairy.utils.ImageUtils;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class TextColorContrastAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TextColorContrastAudit.class);
	
	@Autowired
	private ObservationService observation_service;
	
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
		
		List<ElementState> high_header_contrast = new ArrayList<>();
		List<ElementState> mid_header_contrast = new ArrayList<>();
		List<ElementState> low_header_contrast = new ArrayList<>();

		List<ElementState> high_text_contrast = new ArrayList<>();
		List<ElementState> mid_text_contrast = new ArrayList<>();
		List<ElementState> low_text_contrast = new ArrayList<>();

		//List<ElementState> element_list = new ArrayList<>();
		List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
		//filter elements that aren't text elements
		List<ElementState> element_list = BrowserUtils.getTextElements(elements);
		
		//analyze screenshots of all text images for contrast
		for(ElementState element : element_list) {			
			try {
			
				String color = element.getRenderedCssValues().get("color");
				ColorData text_color = new ColorData(color);
				
				//Identify background color by getting largest color used in picture
				
				ColorData background_color_data = ImageUtils.extractBackgroundColor(element);
				
				log.warn("Background color :: "+background_color_data);
				double contrast = ColorData.computeContrast(background_color_data, text_color);
				if(ElementStateUtils.isHeader(element.getName())) {
					//score header element
					//calculate contrast between text color and background-color
					log.warn("Element is a header with contrast :: "+contrast);
					total_headlines++;
					/*
					headlines < 3; value = 1
					headlines > 3 and headlines < 4.5; value = 2
					headlines >= 4.5; value = 3
					 */
					if(contrast < 3) {
						//No points are rewarded for low contrast headers
						low_header_contrast.add(element);
					}
					else if(contrast >= 3 && contrast < 4.5) {
						headline_score += 1;
						mid_header_contrast.add(element);
					}
					else if(contrast >= 4.5) {
						headline_score += 2;
						high_header_contrast.add(element);
					}
				}
				else {
					total_text_elems++;
					/*
						text < 4.5; value = 1
						text >= 4.5 and text < 7; value = 2
						text >=7; value = 3
					 */
					log.warn("Text element has contrast of "+contrast);
					if(contrast < 4.5) {
						//No points are rewarded for low contrast text
						log.warn("contrast less than 4.5");
						low_text_contrast.add(element);
					}
					else if(contrast >= 4.5 && contrast < 7) {
						log.warn("contrast less than 7");
						text_score += 1;
						mid_text_contrast.add(element);
					}
					else if(contrast >= 7) {
						log.warn("contrast greater than 7");
						text_score += 2;
						high_text_contrast.add(element);
					}
				}
			} catch (Exception e) {
				log.warn("element screenshot url  :: "+element.getScreenshotUrl());
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		List<Observation> observations = new ArrayList<>();
		if(!high_header_contrast.isEmpty()) {
			ElementStateObservation high_header_contrast_observation = new ElementStateObservation(high_header_contrast, "Headers with contrast above 4.5");
			observations.add(observation_service.save(high_header_contrast_observation));
		}
		if(!mid_header_contrast.isEmpty()) {
			ElementStateObservation mid_header_contrast_observation = new ElementStateObservation(mid_header_contrast, "Headers with contrast between 3 and 4.5");
			observations.add(observation_service.save(mid_header_contrast_observation));
		}
		if(!low_header_contrast.isEmpty()) {
			ElementStateObservation low_header_contrast_observation = new ElementStateObservation(low_header_contrast, "Headers with contrast below 3");
			observations.add(observation_service.save(low_header_contrast_observation));
		}
		
		if(!high_text_contrast.isEmpty()) {
			ElementStateObservation high_text_observation = new ElementStateObservation(high_text_contrast, "Text with contrast above 7");
			observations.add(observation_service.save(high_text_observation));
		}
		if(!mid_text_contrast.isEmpty()) {
			ElementStateObservation mid_text_observation = new ElementStateObservation(mid_text_contrast, "Text with contrast between 4.5 and 7");
			observations.add(observation_service.save(mid_text_observation));
		}
		if(!low_text_contrast.isEmpty()) {
			ElementStateObservation low_text_observation = new ElementStateObservation(low_text_contrast, "Text with contrast below 4.5");
			observations.add(observation_service.save(low_text_observation));
		}
		
		int total_possible_points = ((total_headlines*2) + (total_text_elems*2));
		log.warn("TEXT COLOR CONTRAST AUDIT SCORE   ::   " + (headline_score+text_score) + " : " + total_possible_points);
		return new Audit(AuditCategory.COLOR_MANAGEMENT, AuditSubcategory.TEXT_BACKGROUND_CONTRAST, (headline_score+text_score), observations, AuditLevel.PAGE, total_possible_points, page_state.getUrl());
	}

	private String getParentXpath(String xpath) {
		int idx = xpath.lastIndexOf("/");
		return xpath.substring(0, idx);
	}

	
}
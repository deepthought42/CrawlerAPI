package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class NonTextColorContrastAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(NonTextColorContrastAudit.class);

	List<ElementState> flagged_elements = new ArrayList<>();
	
	@Autowired
	private ElementStateService element_state_service;
	
	private static String getAuditDescription() {
		return "Color contrast between background and non text elements such as buttons, inputs, images, etc.";
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
		
		int score = 0;
		//get all button elements
		List<ElementState> non_text_elements = getAllButtons(page_state);
		non_text_elements.addAll(getAllInputs(page_state));
		//non_text_elements.addAll(getAllIcons(page_state));
		
		List<ElementState> low_contrast_elements = new ArrayList<>();
		List<ElementState> mid_contrast_elements = new ArrayList<>();
		
		for(ElementState button : non_text_elements) {
			//get parent element of button
			
			String button_background_color = button.getRenderedCssValues().get("background-color");
			
			if("button".contentEquals(button.getName()) && button_background_color == null) {
				log.warn("BUTTON BACKGROUND IS NULL!!!!!!");
				//TODO add observation of button without background
			}
			if("input".contentEquals(button.getName()) && button_background_color == null){
				log.warn("element css :: "+button.getRenderedCssValues());
				button_background_color = "#ffffff";
			}
			
			ElementState parent_element = element_state_service.findByPageStateAndChild(page_state.getKey(), button.getKey());
			if(parent_element == null) {
				continue;
			}

			String parent_background_color = null;
			if(parent_background_color == null || parent_background_color.isEmpty()) {
				do {
					parent_element = element_state_service.getParentElement(page_state.getKey(), parent_element.getKey());
					
					if(parent_element == null) {
						continue;
					}
					parent_background_color = parent_element.getRenderedCssValues().get("background-color");
					if(parent_background_color != null) {
						//extract r,g,b,a from color css		
						parent_background_color = parent_background_color.replace("transparent", "");
						parent_background_color = parent_background_color.replace("!important", "");
						parent_background_color = parent_background_color.trim();
					}
				}while((parent_background_color == null || parent_background_color.isEmpty()) && parent_element != null);
			}
			
			if((parent_background_color == null  || parent_background_color.isEmpty())) {
				parent_background_color = "#ffffff";
			}
			log.warn("button background color :: "+button_background_color);
			log.warn("parent background color :: "+parent_background_color);

			double contrast = ColorData.computeContrast(new ColorData(parent_background_color.trim()), new ColorData(button_background_color.trim()));
			
			//calculate contrast of button background with background of parent element
			if(contrast < 3.0){
				score += 1;
				low_contrast_elements.add(button);
			}else if(contrast >= 3.0 && contrast < 4.5) {
				score += 2;
				mid_contrast_elements.add(button);
			}
			else {
				score += 3;
			}
		} 
		
		List<Observation> observations = new ArrayList<>();
		ElementObservation low_contrast_observation = new ElementObservation(low_contrast_elements, "Elements with a contrast below 3.0");
		ElementObservation mid_contrast_observation = new ElementObservation(mid_contrast_elements, "Elements with a contrast between 3.0 and 4.5");
		observations.add(low_contrast_observation);
		observations.add(mid_contrast_observation);
		
		//setObservations(observations);
		
		log.warn("NON TEXT ELEMENT CONTRAST SCORE  ::   "+score);
		return new Audit(AuditCategory.COLOR_MANAGEMENT, AuditSubcategory.NON_TEXT_BACKGROUND_CONTRAST, score, new ArrayList<>(), AuditLevel.PAGE, non_text_elements.size() *3);
	}

	private List<ElementState> getAllIcons(PageState page_state) {
		return null;
	}

	private List<ElementState> getAllInputs(PageState page_state) {
		return page_state.getElements().parallelStream().filter(p ->p.getName().equalsIgnoreCase("input")).distinct().collect(Collectors.toList());  // iterating price 

	}

	private List<ElementState> getAllButtons(PageState page_state) {
		return page_state.getElements().parallelStream().filter(p ->p.getName().equalsIgnoreCase("button")).distinct().collect(Collectors.toList());  // iterating price 
	}
}
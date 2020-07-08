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
		
		double score = 0.0;
		//get all button elements
		List<ElementState> non_text_elements = getAllButtons(page_state);
		non_text_elements.addAll(getAllInputs(page_state));
		//non_text_elements.addAll(getAllIcons(page_state));
			
		for(ElementState button : non_text_elements) {
			//get parent element of button
			log.warn("element state service :: "+element_state_service);
			ElementState parent_element = element_state_service.findByPageStateAndChild(page_state.getKey(), button.getKey());
			if(parent_element == null) {
				continue;
			}
			String parent_background_color = parent_element.getCssValues().get("background-color");
			String button_background_color = button.getCssValues().get("background-color");
			
			double contrast = ColorData.computeContrast(new ColorData(parent_background_color), new ColorData(button_background_color));
			
			//calculate contrast of button background with background of parent element
			if(contrast < 3.0){
				score += 1;
				//flagged_element.add(button);
			}else if(contrast >= 3.0 && contrast < 4.5) {
				score += 2;
			}
			else {
				score += 3;
			}
		} 
		
		score = score/(non_text_elements.size() *3);
		//setObservations(observations);
		//setScore(score);
		return new Audit(AuditCategory.COLOR_MANAGEMENT, buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.NON_TEXT_BACKGROUND_CONTRAST, score, new ArrayList<>(), AuditLevel.PAGE);
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
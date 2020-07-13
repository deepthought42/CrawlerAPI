package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.utils.ElementStateUtils;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class TextColorContrastAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TextColorContrastAudit.class);
	
	@Relationship(type="FLAGGED")
	List<ElementState> flagged_elements = new ArrayList<>();
	
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
		
		List<String> observations = new ArrayList<>();
		int total_headlines = 0;
		int total_text_elems = 0;
		double headline_score = 0;
		double text_score = 0;
		
		log.warn("Elements available for color evaluation ...  "+page_state.getElements().size());
		//identify all colors used on page. Images are not considered
		for(ElementState element : page_state.getElements()) {
			//check element for color css property
			String color = element.getCssValues().get("color");
			
			//check element for background-color css property
			String background_color = element.getCssValues().get("background-color");
			if(color == null || background_color == null) {
				continue;
			}
			ColorData color_data = new ColorData(color);
			
			ColorData background_color_data = null;
			//extract r,g,b,a from color css		
			background_color_data = new ColorData(background_color);
			
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
					flagged_elements.add(element);
				}
				else if(contrast >= 3 && contrast < 4.5) {
					headline_score += 2;
					flagged_elements.add(element);
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
					flagged_elements.add(element);
				}
				else if(contrast >= 4.5 && contrast < 7) {
					text_score += 2;
					flagged_elements.add(element);
				}
				else if(contrast >= 7) {
					text_score += 3;
				}
			}
		}
		
		double score = (headline_score+text_score)/((total_headlines*3) + (total_text_elems*3));		
		return new Audit(AuditCategory.COLOR_MANAGEMENT, buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.TEXT_BACKGROUND_CONTRAST, score, observations, AuditLevel.PAGE);
	}
}
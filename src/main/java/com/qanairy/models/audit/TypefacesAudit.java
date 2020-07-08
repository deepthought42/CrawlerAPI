package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class TypefacesAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TypefacesAudit.class);

	
	@Relationship(type="FLAGGED")
	List<ElementState> flagged_elements = new ArrayList<>();
	
	public TypefacesAudit() {
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
		
		Map<String, Integer> family_scores = new HashMap<>();
		List<String> font_families = new ArrayList<>();
		
		for(ElementState element : page_state.getElements()) {
			String font_family = element.getCssValues().get("font-family");
			font_families.add(font_family);
			
			if(family_scores.containsKey(font_family)) {
				family_scores.put(font_family, family_scores.get(font_family)+1);
			}
			else {
				family_scores.put(font_family, 1);				
			}
		}
		font_families.remove(null);
		
		
		log.warn("Font families found  :::   "+font_families);
	
		
		//
		double score = 3.0;
		return new Audit(AuditCategory.TYPOGRAPHY, buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.TYPEFACES, score, new ArrayList<>(), AuditLevel.PAGE);
	}
}
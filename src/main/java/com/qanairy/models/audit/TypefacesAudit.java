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

import com.qanairy.models.Element;
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
	List<Element> flagged_elements = new ArrayList<>();
	
	public TypefacesAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.TEXT_BACKGROUND_CONTRAST);
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
			String font_family = element.getRenderedCssValues().get("font-family");
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
		int score = 3;
		int total_possible_points = 3;
		return new Audit(AuditCategory.TYPOGRAPHY, AuditSubcategory.TYPEFACES, score, new ArrayList<>(), AuditLevel.PAGE, total_possible_points);
	}
}
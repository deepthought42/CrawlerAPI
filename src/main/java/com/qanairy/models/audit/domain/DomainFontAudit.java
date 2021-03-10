package com.qanairy.models.audit.domain;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.Domain;
import com.qanairy.models.Element;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageVersion;
import com.qanairy.models.PageState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditName;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.services.DomainService;
import com.qanairy.services.PageVersionService;
import com.qanairy.services.PageStateService;
import com.qanairy.utils.ElementStateUtils;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class DomainFontAudit implements IExecutableDomainAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DomainFontAudit.class);

	@Autowired
	private PageVersionService page_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private DomainService domain_service;
	
	@Relationship(type="FLAGGED")
	private List<Element> flagged_elements = new ArrayList<>();
	
	public DomainFontAudit() {	}

	/**
	 * {@inheritDoc} 

	 *  
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(Domain domain) {
		assert domain != null;
		
		//get all pages
		List<PageVersion> pages = domain_service.getPages(domain.getHost());
		Map<String, List<ElementState>> header_element_map = new HashMap<>();
		
		log.warn("Domain pages :: "+pages.size());
		//get most recent page state for each page
		for(PageVersion page : pages) {
			
			//for each page state get elements
			PageState page_state = page_service.getMostRecentPageState(page.getKey());
			log.warn("Domain Font Page State :: "+page_state);
			log.warn("Domain Font Page key :: "+page.getKey());
			
			List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
			log.warn("page state elements for domain audit :: "+elements.size());
			for(ElementState element : elements) {
				if(ElementStateUtils.isHeader(element.getName())) {
					if(header_element_map.containsKey(element.getName())) {
						header_element_map.get(element.getName()).add(element);
					}
					else {
						List<ElementState> element_states = new ArrayList<>();
						element_states.add(element);
						header_element_map.put(element.getName(), element_states);
					}
				}
			}
		}
		List<Observation> observations = new ArrayList<>();
		
		int score = 0;
		int max_score = 0;
		//check header buckets for consistency within bucket
		for(String header_tag : header_element_map.keySet()) {
			List<String> font_sizes = new ArrayList<>();
			List<String> line_heights = new ArrayList<>();
			List<String> font_weights = new ArrayList<>();
			List<String> font_variants = new ArrayList<>();
			
			log.warn("Header tag :: "+header_tag);
			for(ElementState element : header_element_map.get(header_tag)) {
				String font_size = element.getRenderedCssValues().get("font-size");
				String line_height = element.getRenderedCssValues().get("line-height");
				String font_weight = element.getRenderedCssValues().get("font-weight");
				String font_variant = element.getRenderedCssValues().get("font-variant");
				
				font_sizes.add(font_size);
				line_heights.add(line_height);
				font_weights.add(font_weight);
				font_variants.add(font_variant);
				
				//log.warn("font size :: "+element.getRenderedCssValues().get("font-size"));
				//log.warn("line height :: "+element.getRenderedCssValues().get("line-height"));
				//log.warn("font weight :: "+element.getRenderedCssValues().get("font-weight"));
				//log.warn("font variant :: "+element.getRenderedCssValues().get("font-variant"));
				//log.warn("font family :: "+element.getRenderedCssValues().get("font-family"));
			}
			
			font_sizes = makeDistinct(font_sizes);
			line_heights = makeDistinct(line_heights);
			font_weights = makeDistinct(font_weights);
			font_variants = makeDistinct(font_variants);
			
			if(font_sizes.size() > 1) {
				log.warn("font sizes :: "+font_sizes);
				score += 1;
			}
			else {
				score += 3;
			}
			max_score +=3;
			
			if(line_heights.size() > 1) {
				log.warn("font sizes :: "+line_heights);
				score += 1;
			}
			else {
				score += 3;
			}
			max_score +=3;
			
			if(font_weights.size() > 1) {
				log.warn("font weights:: "+font_weights);
				score += 1;
			}
			else {
				score += 3;
			}
			max_score +=3;
			
			if(font_variants.size() > 1) {
				log.warn("font variants:: "+font_variants);
				score += 1;
			}
			else {
				score += 3;
			}
			max_score +=3;
			
			log.warn("#############################################################################");
		}
		
		
		log.warn("DOMAIN FONT AUDIT SCORE   ::   "+score +" / " +max_score);
		
		
		String why_it_matters = "Clean typography, with the use of only 1 to 2 typefaces, invites users to" + 
				"the text on your website. It plays an important role in how clear, distinct" + 
				"and legible the textual content is.";
		
		String ada_compliance = "Your typography meets ADA requirements." + 
				"Images of text are not used and text is resizable. San-Serif typeface has" + 
				"been used across the pages.";
		
		return new Audit(AuditCategory.AESTHETICS,
						 AuditSubcategory.TYPOGRAPHY,
						 AuditName.FONT, 
						 score, 
						 observations, 
						 AuditLevel.PAGE, 
						 max_score, 
						 domain.getHost());
	}
	

	public static List<String> makeDistinct(List<String> from){
		assert from != null;
	    from.removeAll(Collections.singleton(null));

		return from.stream().distinct().collect(Collectors.toList());
	}
	
}
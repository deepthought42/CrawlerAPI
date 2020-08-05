package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
public class FontAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(FontAudit.class);

	
	@Relationship(type="FLAGGED")
	List<ElementState> flagged_elements = new ArrayList<>();
	
	public FontAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.TEXT_BACKGROUND_CONTRAST);
	}
	
	private static String getAuditDescription() {
		return "";
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
		
		Map<String, List<ElementState>> header_element_map = new HashMap<>();
		for(ElementState element : page_state.getElements()) {
			if(ElementStateUtils.isHeader(element)) {
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
		
		
		log.warn("FONT AUDIT SCORE   ::   "+score +" / " +max_score);
		return new Audit(AuditCategory.TYPOGRAPHY, AuditSubcategory.FONT, score, observations, AuditLevel.PAGE, max_score);
	}
	

	public static List<String> makeDistinct(List<String> from){
		return from.stream().distinct().sorted().collect(Collectors.toList());
	}
	
}
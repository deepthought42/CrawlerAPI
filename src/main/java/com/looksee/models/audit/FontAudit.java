package com.looksee.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.looksee.models.Element;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.utils.ElementStateUtils;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class FontAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(FontAudit.class);
	
	@Relationship(type="FLAGGED")
	List<Element> flagged_elements = new ArrayList<>();
	
	public FontAudit() {
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
		
		Map<String, List<ElementState>> header_element_map = new HashMap<>();
		for(ElementState element : page_state.getElements()) {
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
		
		String why_it_matters = "Clean typography, with the use of only 1 to 2 typefaces, invites users to" + 
				" the text on your website. It plays an important role in how clear, distinct" + 
				" and legible the textual content is.";
		
		String ada_compliance = "Your typography meets ADA requirements." + 
				" Images of text are not used and text is resizable. San-Serif typeface has" + 
				" been used across the pages.";
		
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		
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
		String description = "";
		
		log.warn("FONT AUDIT SCORE   ::   "+score +" / " +max_score);
		return new Audit(AuditCategory.AESTHETICS,
						 AuditSubcategory.TYPOGRAPHY,
						 AuditName.FONT,
						 score,
						 issue_messages,
						 AuditLevel.PAGE,
						 max_score,
						 page_state.getUrl(), 
						 why_it_matters, 
						 description,
						 page_state);
	}
	

	public static List<String> makeDistinct(List<String> from){
		return from.stream().distinct().sorted().collect(Collectors.toList());
	}
	
}
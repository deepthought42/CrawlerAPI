package com.qanairy.models.audit;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.minion.browsing.Browser;
import com.qanairy.models.Element;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditName;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.services.ObservationService;
import com.qanairy.services.PageStateService;
import com.qanairy.utils.BrowserUtils;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class TypefacesAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TypefacesAudit.class);
	
	@Relationship(type="FLAGGED")
	private List<Element> flagged_elements = new ArrayList<>();
	
	@Autowired
	private ObservationService observation_service;
	
	@Autowired
	private PageStateService page_state_service;
	
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
		
		List<String> font_families = new ArrayList<>();
		List<Observation> observations = new ArrayList<>();

		List<String> raw_stylesheets = Browser.extractStylesheets(page_state.getSrc()); 
		
		//open stylesheet
		for(String stylesheet : raw_stylesheets) {
			font_families.addAll(BrowserUtils.extractFontFamiliesFromStylesheet(stylesheet));
		}
		
		log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		log.warn("font families   ::        "+font_families.size());
		log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		
		List<String> primary_typefaces = new ArrayList<>();
		Map<String, String[]> typeface_map = new HashMap<>();
		//identify 2 main typefaces. These are going to be the first fonts used in font-family sets
		for(String font_family_set : font_families) {
			String[] typeface_set = font_family_set.split(",");
			String primary_typeface = typeface_set[0];
			
			primary_typefaces.add(primary_typeface.trim().toLowerCase());
			typeface_map.put(primary_typeface, typeface_set);
		}
		
		primary_typefaces = primary_typefaces.stream().distinct().collect(Collectors.toList());
		log.warn("primary typefaces ::    " + primary_typefaces);
		log.warn("primary typefaces  size() ::    " + primary_typefaces.size());

		
		
		//SCORE PRIMARY TYPEFACES 
		int score = 0;
		int total_possible_points = 3;
		log.warn("Font families found  :::   "+primary_typefaces.size());
	
		//evaluate typefaces
		if(primary_typefaces.size() ==  2) {
			score += 2;
			TypefacesObservation observation = new  TypefacesObservation(primary_typefaces, "2 typefaces are used, which is the preferred amount of typefaces. Well done!");
			observations.add(observation_service.save(observation));
		}
		else if(primary_typefaces.size() < 2) {
			score += 1;
			TypefacesObservation observation = new  TypefacesObservation(primary_typefaces, "Only 1 typeface was found. You might want to consider using 2 typefaces for the best experience");
			observations.add(observation_service.save(observation));
		}
		else if(primary_typefaces.size() > 2) {
			score += 0;
			TypefacesObservation observation = new  TypefacesObservation(primary_typefaces, "Identified " +primary_typefaces.size()+" typefaces.  ( " + primary_typefaces+ "). With too many typefaces your user experience will seem incoherent and inconsistent. Simplicity is best and you should have no more than 2 typefaces");
			observations.add(observation_service.save(observation));
		}
		total_possible_points += 2;		
		
		
		//SCORE TYPEFACE SEQUENCE CONSISTENCY - TYPEFACES SHOULD ALWAYS APPEAR WITH THE SAME TYPEFACES.
		//validate that sets that start with the same typeface have the same number of typefaces for backup, and that they are in the same order
		
		
		//map font sequences
		Map<String, List<String>> forward_connection_graph = new HashMap<>();
		for(String primary_typeface : typeface_map.keySet()) {
			String[] typeface_arr = typeface_map.get(primary_typeface);
			
			for(int idx = 0; idx < typeface_arr.length; idx++) {
				List<String> linked_typefaces = new ArrayList<>();
				if(forward_connection_graph.containsKey(typeface_arr[idx])) {
					linked_typefaces = forward_connection_graph.get(typeface_arr[idx]);
				}
				if(idx < (typeface_arr.length - 1)){
					linked_typefaces.add(typeface_arr[idx+1]);
				}
				forward_connection_graph.put(typeface_arr[idx], linked_typefaces);
			}
		}
		
		//reduce graph instances to unique sets
		for(String font : forward_connection_graph.keySet()) {
			forward_connection_graph.put(font, forward_connection_graph.get(font).stream().distinct().collect(Collectors.toList()));
		}
		
		//check graph for loop - if any keys within graph have more than 1 font associated with it.
		boolean typeface_limit_msg_already_added = false;
		for(String font : forward_connection_graph.keySet() ) {
			//if connected set has more than 1 element then an inconsistency exists
			if(forward_connection_graph.get(font).size() > 1){
				score += 1;
				TypefacesObservation observation = new TypefacesObservation(forward_connection_graph.get(font), "Typefaces that are listed for font cascading should always appear in the same order. We found fonts that are competing for the same position in the cascading list. This can create an inconsistent experience and should be avoided.");
				observations.add(observation_service.save(observation));
			}
			else {
				score += 2;
			}
			
			total_possible_points += 2;
		}
		
		//END TYPEFACE SEQUENCE CONSISTENCY SCORING
		
		
		
		//GET TYPEFACES ACTUALLY RENDERED BY SYSTEM AND GENERATE SCORE BASED ON TYPEFACE CASCADE SETTINGS
		List<ElementState> element_list = BrowserUtils.getTextElements(page_state_service.getElementStates(page_state.getKey()));
		Set<String> observed_fonts = new HashSet<>();
		List<ElementState> no_fallback_font = new ArrayList<>();
		
		for(ElementState element : element_list) {
			
			String font_family = element.getRenderedCssValues().get("font-family");
			if(primary_typefaces.contains(font_family) ) {
				score +=2;
			}
			else {
				score +=1;
				no_fallback_font.add(element);
			}
			
			total_possible_points += 2;
			
			observed_fonts.add(font_family);
		}
		
		ElementStateObservation observation = new ElementStateObservation(no_fallback_font, "Text element rendered with a fallback typeface instead of the desired font.");
		observations.add(observation_service.save(observation));
		
		String why_it_matters = "Clean typography, with the use of only 1 to 2 typefaces, invites users to" + 
				" the text on your website. It plays an important role in how clear, distinct" + 
				" and legible the textual content is.";
		
		String ada_compliance = "Your typography meets ADA requirements." + 
				" Images of text are not used and text is resizable. San-Serif typeface has" + 
				" been used across the pages.";
				
		return new Audit(AuditCategory.AESTHETICS,
						 AuditSubcategory.TYPOGRAPHY,
						 AuditName.TYPEFACES,
						 score,
						 observations,
						 AuditLevel.PAGE,
						 total_possible_points,
						 page_state.getUrl(),
						 why_it_matters,
						 ada_compliance);
	}
}
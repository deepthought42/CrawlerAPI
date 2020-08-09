package com.qanairy.models.audit.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.Domain;
import com.qanairy.models.ElementState;
import com.qanairy.models.Page;
import com.qanairy.models.PageState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.ColorData;
import com.qanairy.models.audit.ColorPaletteObservation;
import com.qanairy.models.audit.ColorPaletteUtils;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.models.enums.ColorScheme;
import com.qanairy.services.DomainService;
import com.qanairy.services.PageService;
import com.qanairy.services.PageStateService;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class DomainColorPaletteAudit implements IExecutableDomainAudit{
	private static Logger log = LoggerFactory.getLogger(DomainColorPaletteAudit.class);

	private List<String> gray_colors = new ArrayList<>();
	private List<String> colors = new ArrayList<>();
	
	@Autowired
	private PageService page_service;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	public DomainColorPaletteAudit() {}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Identifies colors used on page, the color scheme type used, and the ultimately the score for how the colors used conform to scheme
	 */
	@Override
	public Audit execute(Domain domain) {
		assert domain != null;
		
		List<Observation> observations = new ArrayList<>();

		Map<String, Boolean> colors = new HashMap<String, Boolean>();

		//get all pages
		List<Page> pages = domain_service.getPages(domain.getHost());
		
		log.warn("Domain pages :: "+pages.size());
		//get most recent page state for each page
		for(Page page : pages) {
			
			//for each page state get elements
			PageState page_state = page_service.getMostRecentPageState(page.getKey());
			log.warn("Domain Font Page State :: "+page_state);
			log.warn("Domain Font Page key :: "+page.getKey());
			
			List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
			log.warn("COLOR PALETTE AUDIT :: Elements available for color evaluation ...  "+elements.size());

			for(ElementState element : elements) {
				//identify all colors used on page. Images are not considered
				
				//check element for color css property
				colors.put(element.getPreRenderCssValues().get("color"), Boolean.TRUE);
				//check element for text-decoration-color css property
				colors.put(element.getPreRenderCssValues().get("text-decoration-color"), Boolean.TRUE);
				//check element for text-emphasis-color
				colors.put(element.getPreRenderCssValues().get("text-emphasis-color"), Boolean.TRUE);
	
				//check element for background-color css property
				colors.put(element.getPreRenderCssValues().get("background-color"), Boolean.TRUE);
				//check element for caret-color
				colors.put(element.getPreRenderCssValues().get("caret-color"), Boolean.TRUE);
				//check element for outline-color css property NB: SPECIFICALLY FOR BOXES
				colors.put(element.getPreRenderCssValues().get("outline-color"), Boolean.TRUE);
				//check element for border-color, border-left-color, border-right-color, border-top-color, and border-bottom-color css properties NB: SPecifically for borders
				colors.put(element.getPreRenderCssValues().get("border-color"), Boolean.TRUE);
				colors.put(element.getPreRenderCssValues().get("border-left-color"), Boolean.TRUE);
				colors.put(element.getPreRenderCssValues().get("border-right-color"), Boolean.TRUE);
				colors.put(element.getPreRenderCssValues().get("border-top-color"), Boolean.TRUE);
				colors.put(element.getPreRenderCssValues().get("border-bottom-color"), Boolean.TRUE);
			}
			colors.remove("null");
			colors.remove(null);
		}
		
		Map<String, Boolean> gray_colors = new HashMap<String, Boolean>();
		Map<String, Boolean> filtered_colors = new HashMap<>();
		//discard any colors that are transparent
		for(String color_str : colors.keySet()) {
			color_str = color_str.trim();
			color_str = color_str.replace("transparent", "");
			color_str = color_str.replace("!important", "");
			if(color_str == null || color_str.isEmpty() || color_str.equalsIgnoreCase("transparent")) {
				continue;
			}

			//extract r,g,b,a from color_str
			ColorData color = new ColorData(color_str.trim());
			//if gray(all rgb values are equal) put in gray colors map otherwise filtered_colors
			String rgb_color_str = "rgb("+color.getRed()+","+color.getGreen()+","+color.getBlue()+")";
			//convert rgb to hsl, store all as Color object
			
			if( Math.abs(color.getRed() - color.getGreen()) < 4
					&& Math.abs(color.getRed() - color.getBlue()) < 4
					&& Math.abs(color.getBlue() - color.getGreen()) < 4) {
				gray_colors.put(rgb_color_str, Boolean.TRUE);
			}
			else {
				filtered_colors.put(rgb_color_str, Boolean.TRUE);
			}
		}
		
		gray_colors.remove(null);
		filtered_colors.remove(null);
		log.warn("colors found :: "+filtered_colors);
		log.warn("gray colors :: "+gray_colors);
		
		//generate palette, identify color scheme and score how well palette conforms to color scheme
		Map<ColorData, Set<ColorData>> palette = ColorPaletteUtils.extractPalette(filtered_colors.keySet());
		for(ColorData primary_color : palette.keySet()) {
			log.warn("Primary color :: "+primary_color.rgb() + "   ;   " + primary_color.getLuminosity());
		}
		ColorScheme color_scheme = ColorPaletteUtils.getColorScheme(palette);
		//score colors found against scheme
		Map<String, Set<String>> palette_stringified = convertPaletteToStringRepresentation(palette);
		
		ColorPaletteObservation observation = new ColorPaletteObservation(palette_stringified, new ArrayList<>(filtered_colors.keySet()), new ArrayList<>(gray_colors.keySet()), color_scheme, "This is a color scheme description");
		observations.add(observation);
			
		for(ColorData primary_color : palette.keySet()) {
			log.warn("Primary color :: "+primary_color.rgb() + "   ;   " + primary_color.getLuminosity());
		}
		ColorScheme scheme = ColorPaletteUtils.getColorScheme(palette);
		int score = ColorPaletteUtils.getPaletteScore(palette, scheme);
		
		//score colors found against scheme
		setGrayColors(new ArrayList<>(gray_colors.keySet()));
		setColors(new ArrayList<>(colors.keySet()));
		 
		
		return new Audit(AuditCategory.COLOR_MANAGEMENT, AuditSubcategory.COLOR_PALETTE, score, observations, AuditLevel.DOMAIN, 3);
	}

	

	public List<String> getGrayColors() {
		return gray_colors;
	}

	public void setGrayColors(List<String> gray_colors) {
		this.gray_colors = gray_colors;
	}

	public List<String> getColors() {
		return colors;
	}

	public void setColors(List<String> colors) {
		this.colors = colors;
	}
	
	private Map<String, Set<String>> convertPaletteToStringRepresentation(Map<ColorData, Set<ColorData>> palette) {
		Map<String, Set<String>> stringified_map = new HashMap<>();
		for(ColorData primary : palette.keySet()) {
			Set<String> secondary_colors = new HashSet<>();
			for(ColorData secondary : palette.get(primary)) {
				if(secondary == null) {
					continue;
				}
				secondary_colors.add(secondary.rgb());
			}
			stringified_map.put(primary.rgb(), secondary_colors);
		}
		return null;
	}
}



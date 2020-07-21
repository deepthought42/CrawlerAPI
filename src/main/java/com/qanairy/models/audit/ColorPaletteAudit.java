package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.models.enums.ColorScheme;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class ColorPaletteAudit implements IExecutablePageStateAudit {
	private static Logger log = LoggerFactory.getLogger(ColorPaletteAudit.class);

	private List<String> gray_colors = new ArrayList<>();
	private List<String> colors = new ArrayList<>();
	
	public ColorPaletteAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.COLOR_PALETTE);
	}
	
	private static String getAuditDescription() {
		return "The colors that make up the style guide of your website.";
	}

	private static List<String> buildBestPractices() {
		List<String> best_practices = new ArrayList<>();
		best_practices.add("A brand guide typically should have 3 colours. You may use different shades of your brand colours to diversify the visual aesthetics of the website but be sure to remain consistent.");
		best_practices.add("This can be further divided into primary, secondary, tertiary colors.");
		
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
		
		List<Observation> observations = new ArrayList<>();

		Map<String, Boolean> colors = new HashMap<String, Boolean>();

		log.warn("COLOR PALETTE AUDIT :: Elements available for color evaluation ...  "+page_state.getElements().size());
		
		for(ElementState element : page_state.getElements()) {
			//identify all colors used on page. Images are not considered
			
			//check element for color css property
			colors.put(element.getPreRenderCssValues().get("color"), Boolean.TRUE);
			//check element for text-decoration-color css property
			colors.put(element.getPreRenderCssValues().get("text-decoration-color"), Boolean.TRUE);
			//check element for text-emphasis-color
			colors.put(element.getPreRenderCssValues().get("text-emphasis-color"), Boolean.TRUE);

			//check element for background-color css property
			colors.put(element.getPreRenderCssValues().get("background-color"), Boolean.TRUE);
			log.warn("element xpath :: "+element.getXpath());
			log.warn("background color : "+element.getPreRenderCssValues().get("background-color"));
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
		double score = ColorPaletteUtils.getPaletteScore(palette, color_scheme)/3.0;
		Map<String, Set<String>> palette_stringified = convertPaletteToStringRepresentation(palette);
		
		ColorPaletteObservation observation = new ColorPaletteObservation(palette_stringified, new ArrayList<>(filtered_colors.keySet()), new ArrayList<>(gray_colors.keySet()), color_scheme, "This is a color scheme description");
		observations.add(observation);
		
		return new Audit(AuditCategory.COLOR_MANAGEMENT, buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.COLOR_PALETTE, score, observations, AuditLevel.PAGE); 
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
}
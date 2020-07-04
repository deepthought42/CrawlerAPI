package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditSubcategory;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
public class ColorPaletteAudit extends ColorManagementAudit {
	private static Logger log = LoggerFactory.getLogger(ColorPaletteAudit.class);

	private List<String> gray_colors = new ArrayList<>();
	private List<String> colors = new ArrayList<>();
	
	public ColorPaletteAudit() {
		super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.COLOR_PALETTE);
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
	public double execute(PageState page_state, String user_id) throws MalformedURLException, URISyntaxException {
		assert page_state != null;
		assert user_id != null;
		
		List<String> observations = new ArrayList<>();

		Map<String, Boolean> colors = new HashMap<String, Boolean>();

		log.warn("COLOR PALETTE AUDIT :: Elements available for color evaluation ...  "+page_state.getElements().size());
		
		for(ElementState element : page_state.getElements()) {
			//identify all colors used on page. Images are not considered
			
			//check element for color css property
			colors.put(element.getCssValues().get("color"), Boolean.TRUE);
			//check element for text-decoration-color css property
			colors.put(element.getCssValues().get("text-decoration-color"), Boolean.TRUE);
			//check element for text-emphasis-color
			colors.put(element.getCssValues().get("text-emphasis-color"), Boolean.TRUE);

			//check element for background-color css property
			colors.put(element.getCssValues().get("background-color"), Boolean.TRUE);
			
			//check element for caret-color
			colors.put(element.getCssValues().get("caret-color"), Boolean.TRUE);
			//check element for outline-color css property NB: SPECIFICALLY FOR BOXES
			colors.put(element.getCssValues().get("outline-color"), Boolean.TRUE);
			//check element for border-color, border-left-color, border-right-color, border-top-color, and border-bottom-color css properties NB: SPecifically for borders
			colors.put(element.getCssValues().get("border-color"), Boolean.TRUE);
			colors.put(element.getCssValues().get("border-left-color"), Boolean.TRUE);
			colors.put(element.getCssValues().get("border-right-color"), Boolean.TRUE);
			colors.put(element.getCssValues().get("border-top-color"), Boolean.TRUE);
			colors.put(element.getCssValues().get("border-bottom-color"), Boolean.TRUE);
		}
		colors.remove("null");
		colors.remove(null);
		
		Map<String, Boolean> gray_colors = new HashMap<String, Boolean>();
		Map<String, Boolean> filtered_colors = new HashMap<>();
		//discard any colors that are transparent
		for(String color_str : colors.keySet()) {
			if(color_str == null || color_str.isEmpty()) {
				continue;
			}

			//extract r,g,b,a from color_str
			ColorData color = new ColorData(color_str);
			//if gray(all rgb values are equal) put in gray colors map otherwise filtered_colors
			String rgb_color_str = "rgb("+color.red+","+color.green+","+color.blue+")";
			//convert rgb to hsl, store all as Color object
			
			if( Math.abs(color.red - color.green) < 4
					&& Math.abs(color.red - color.blue) < 4
					&& Math.abs(color.blue - color.green) < 4) {
				gray_colors.put(rgb_color_str, Boolean.TRUE);
			}
			else {
				filtered_colors.put(rgb_color_str, Boolean.TRUE);
			}
		}
		log.warn("colors found :: "+colors);
		log.warn("filtered colors :: "+filtered_colors);
		log.warn("gray colors :: "+gray_colors);
		
		//TEMP SOLUTION score by how many primary colors are used. TODO replace with scoring based on color scheme
		
		if(filtered_colors.size() < 3) {
			setScore(1.0);
		}
		else if(filtered_colors.size() > 3) {
			setScore(2.0);
		}
		else if(filtered_colors.size() == 3) {
			setScore(3.0);
		}
		
		
		
		
		//score colors found against scheme
		setGrayColors(new ArrayList<>(gray_colors.keySet()));
		setColors(new ArrayList<>(filtered_colors.keySet()));
		setObservations(observations);
		setKey(generateKey());
		
		return getScore();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ColorPaletteAudit clone() {
		ColorPaletteAudit audit = new ColorPaletteAudit();
		audit.setScore(getScore());
		audit.setKey(getKey());
		return audit;
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
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


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
public class ColorPaletteAudit extends ColorManagementAudit {
	private static Logger log = LoggerFactory.getLogger(ColorPaletteAudit.class);

	private List<String> gray_colors = new ArrayList<>();
	private List<String> colors = new ArrayList<>();
	
	public ColorPaletteAudit() {
		super(buildBestPractices(), getAdaDescription(), getAuditDescription(), "color_palette");
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
		double overall_score = 0.0;

		Map<String, Boolean> colors = new HashMap<String, Boolean>();
		System.out.println("Elements available for color evaluation ...  "+page_state.getElements().size());
		//identify all colors used on page. Images are not considered
		for(ElementState element : page_state.getElements()) {
			//check element for color css property
			colors.put(element.getCssValues().get("color"), Boolean.TRUE);
			
			//check element for background-color css property
			colors.put(element.getCssValues().get("background-color"), Boolean.TRUE);
			//check element for text-decoration-color css property
			colors.put(element.getCssValues().get("text-decoration-color"), Boolean.TRUE);
			//check element for text-emphasis-color
			colors.put(element.getCssValues().get("text-emphasis-color"), Boolean.TRUE);
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
		
		Map<String, Boolean> gray_colors = new HashMap<String, Boolean>();
		Map<String, Boolean> filtered_colors = new HashMap<>();
		//discard any colors that are transparent
		for(String color_str : colors.keySet()) {
			if(color_str == null) {
				continue;
			}

			//extract r,g,b,a from color_str		
			Color color = new Color(color_str);
			//if gray(all rgb values are equal) put in gray colors map otherwise filtered_colors
			String rgb_color_str = "rgb("+color.red+","+color.green+","+color.blue+")";
			if(color.red == color.green && color.green == color.blue) {
				gray_colors.put(rgb_color_str, Boolean.TRUE);
			}
			else {
				filtered_colors.put(rgb_color_str, Boolean.TRUE);
			}
		}
		colors.remove("null");
		System.out.println("colors found :: "+colors);
		log.warn("Total filtered colors found (Includes shades, excludes transparency, and gray) ... "+filtered_colors.size());
		log.warn("filtered colors :: "+filtered_colors);
		log.warn("Total grayscale colors ... "+gray_colors.size());
		log.warn("filtered colors :: "+gray_colors);
		
		//group colors based on which colors are closes to each other in hue? or maybe saturation? to identify primary colors
		
		//Identify color scheme type by how many primary colors are used
		
		
		//score colors found against scheme
		setGrayColors(new ArrayList<>(gray_colors.keySet()));
		setColors(new ArrayList<>(filtered_colors.keySet()));
		setObservations(observations);
		setScore( overall_score/colors.size() );
		setKey(generateKey());
		
		return getScore();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Audit clone() {
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

/**
 * Represents an both rgb and hsl setting simulatenously
 *
 */
class Color{
	int red;
	int green;
	int blue;
	
	double transparency;
	double value;
	double hue;
	double saturation;
	double luminosity;
	
	public Color(String rgba_string) {
		//extract r,g,b,a from color_str
		String tmp_color_str = rgba_string.replace(")", "");
		tmp_color_str = tmp_color_str.replace("rgba(", "");
		tmp_color_str = tmp_color_str.replace("rgb(", "");
		tmp_color_str = tmp_color_str.replaceAll(" ", "");
		String[] rgba = tmp_color_str.split(",");
		
		this.red = Integer.parseInt(rgba[0]);
		this.green = Integer.parseInt(rgba[1]);
		this.blue = Integer.parseInt(rgba[2]);
		
		if(rgba.length == 4) {
			transparency = Double.parseDouble(rgba[3]);
		}
		this.hue = 0.0;
		this.saturation = 0.0;
		this.value = 0.0;
		this.luminosity = 0.0;
	}
}

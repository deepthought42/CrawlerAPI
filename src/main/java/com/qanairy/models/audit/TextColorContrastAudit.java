package com.qanairy.models.audit;

import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.utils.ElementStateUtils;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
public class TextColorContrastAudit extends ColorManagementAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TextColorContrastAudit.class);

	private double headline_score;
	private double text_score;
	private double header_contrast;
	private double text_contrast;
	
	public TextColorContrastAudit() {
		super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.TEXT_BACKGROUND_CONTRAST);
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
	public double execute(PageState page_state, String user_id) throws MalformedURLException, URISyntaxException {
		assert page_state != null;
		assert user_id != null;
		
		List<String> observations = new ArrayList<>();
		int total_headlines = 0;
		int total_text_elems = 0;
		
		System.out.println("Elements available for color evaluation ...  "+page_state.getElements().size());
		//identify all colors used on page. Images are not considered
		for(ElementState element : page_state.getElements()) {
			//check element for color css property
			String color = element.getCssValues().get("color");
			
			//check element for background-color css property
			String background_color = element.getCssValues().get("background-color");
			if(color == null || background_color == null) {
				continue;
			}
			ColorData color_data = new ColorData(color);
			//convert rgb to hsl, store all as Color object
			float[] hsb = Color.RGBtoHSB(color_data.red,color_data.green,color_data.blue, null);
			color_data.hue = hsb[0];
			color_data.saturation = hsb[1];
			color_data.brightness = hsb[2];
			
			ColorData background_color_data = null;
			//extract r,g,b,a from color css		
			background_color_data = new ColorData(background_color);
			//convert rgb to hsl, store all as Color object
			hsb = Color.RGBtoHSB(background_color_data.red,background_color_data.green,background_color_data.blue, null);
			background_color_data.hue = hsb[0];
			background_color_data.saturation = hsb[1];
			background_color_data.brightness = hsb[2];
			
			
			double max_brightness = 0.0;
			double min_brightness = 0.0;
			
			if(color_data.brightness > background_color_data.brightness) {
				min_brightness = background_color_data.brightness;
				max_brightness = color_data.brightness;
			}
			else {
				min_brightness = color_data.brightness;
				max_brightness = background_color_data.brightness;
			}
			
			if(ElementStateUtils.isHeader(element)) {
				//score header element
				//calculate contrast between text color and background-color
				header_contrast = (min_brightness + 0.05) / (max_brightness + 0.05);
				total_headlines++;
			}
			else if(ElementStateUtils.isTextContainer(element)) {
				text_contrast = (min_brightness + 0.05) / (max_brightness + 0.05);
				total_text_elems++;
			}
			
			/*
				headlines < 3; value = 1
				headlines > 3 and headlines < 4.5; value = 2
				headlines >= 4.5; value = 3
			 */
			if(header_contrast < 3) {
				headline_score += 1;
			}
			else if(header_contrast >= 3 && header_contrast < 4.5) {
				headline_score += 2;
			}
			else if(header_contrast >= 4.5) {
				headline_score += 3;
			}
			
			/*
				text < 4.5; value = 1
				text >= 4.5 and text < 7; value = 2
				text >=7; value = 3
			*/
			if(text_contrast < 4.5) {
				text_score += 1;
			}
			else if(text_contrast >= 4.5 && text_contrast < 7) {
				text_score += 2;
			}
			else if(text_contrast >= 7) {
				text_score += 3;
			}

		}
		
		setScore((headline_score+text_score)/(total_headlines + total_text_elems));
		//score colors found against scheme
		setObservations(observations);
		setKey(generateKey());
		
		return getScore();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TextColorContrastAudit clone() {
		TextColorContrastAudit audit = new TextColorContrastAudit();
		audit.setScore(getScore());
		audit.setKey(getKey());
		return audit;
	}
}
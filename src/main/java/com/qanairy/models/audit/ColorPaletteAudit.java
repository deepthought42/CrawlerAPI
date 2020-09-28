package com.qanairy.models.audit;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.gcp.CloudVisionUtils;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.models.enums.ColorScheme;
import com.qanairy.services.ObservationService;
import com.qanairy.services.PageStateService;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class ColorPaletteAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ColorPaletteAudit.class);
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ObservationService observation_service;
	
	private List<String> gray_colors = new ArrayList<>();
	private List<String> colors = new ArrayList<>();
	
	public ColorPaletteAudit() {}

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
		
		List<ColorUsageStat> color_usage_list = new ArrayList<>();

		log.warn("color management page state :: "+page_state.getKey());
		List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
		
		try {
			color_usage_list.addAll(extractColorsFromPageState(new URL(page_state.getFullPageScreenshotUrl()), elements));
			log.warn("color_map ::   "+color_usage_list);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Map<String, Boolean> gray_colors = new HashMap<String, Boolean>();
		Map<String, Boolean> filtered_colors = new HashMap<>();
		//discard any colors that are transparent
		for(ColorUsageStat color: color_usage_list) {
			//color_str = color_str.trim();
			//color_str = color_str.replace("transparent", "");
			//color_str = color_str.replace("!important", "");
			//if(color_str == null || color_str.isEmpty() || color_str.equalsIgnoreCase("transparent")) {
			//	continue;
			//}

			//extract r,g,b,a from color_str
			//ColorData color = new ColorData(color_str.trim());
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
			log.warn("secondary colors .... "+palette.get(primary_color));
		}
		ColorScheme color_scheme = ColorPaletteUtils.getColorScheme(palette);
		//score colors found against scheme
		Map<String, Set<String>> palette_stringified = ColorPaletteUtils.convertPaletteToStringRepresentation(palette);
		
		List<Observation> observations = new ArrayList<>();

		ColorPaletteObservation observation = new ColorPaletteObservation(palette_stringified, new ArrayList<>(filtered_colors.keySet()), new ArrayList<>(gray_colors.keySet()), color_scheme, "This is a color scheme description");
		observations.add(observation_service.save(observation));
			
		ColorScheme scheme = ColorPaletteUtils.getColorScheme(palette);
		Score score = ColorPaletteUtils.getPaletteScore(palette, scheme);
		
		//score colors found against scheme
		setGrayColors(new ArrayList<>(gray_colors.keySet()));
		setColors(colors);
		 
		return new Audit(AuditCategory.COLOR_MANAGEMENT, AuditSubcategory.COLOR_PALETTE, score.getPointsAchieved(), observations, AuditLevel.PAGE, score.getMaxPossiblePoints());
	}
	
	/**
	 * 
	 * @param screenshot_url
	 * @param elements
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private List<ColorUsageStat> extractColorsFromPageState(URL screenshot_url,
			List<ElementState> elements) throws MalformedURLException, IOException {		
		log.warn("Loading image from url ::  "+screenshot_url);
		//copy page state full page screenshot
		BufferedImage screenshot = ImageIO.read(screenshot_url);
		
		for(ElementState element : elements) {
			if(!element.getName().contentEquals("img")) {
				continue;
			}
			
			for(int x_pixel = element.getXLocation(); x_pixel < (element.getXLocation()+element.getWidth()); x_pixel++) {
				if(x_pixel > screenshot.getWidth()) {
					break;
				}
				
				if(x_pixel < 0) {
					continue;
				}
				for(int y_pixel = element.getYLocation(); y_pixel < (element.getYLocation()+element.getHeight()); y_pixel++) {
					if(y_pixel > screenshot.getHeight()) {
						break;
					}
					
					if(y_pixel < 0) {
						continue;
					}
					screenshot.setRGB(x_pixel, y_pixel, new Color(0,0,0).getRGB());
				}	
			}
		}
		
		return CloudVisionUtils.extractImageProperties(screenshot);
		
		/*
		//resize image
		BufferedImage thumbnail = Scalr.resize(screenshot, Scalr.Method.QUALITY, screenshot.getWidth()/8, screenshot.getHeight()/8);
		
		//analyze image for color use percentages
		for(int x_pixel = 0; x_pixel < thumbnail.getWidth(); x_pixel++) {
			for(int y_pixel = 0; y_pixel < thumbnail.getHeight(); y_pixel++) {
				Color color = new Color(thumbnail.getRGB(x_pixel, y_pixel));
				String color_str = color.getRed()+","+color.getGreen()+","+color.getBlue();

				if(!color_map.containsKey(color_str)) {
					color_map.put(color_str, 1);
					log.warn("thumbnail rgb value  as rgb string ::  "+color_str+"");
				}
				else {
					color_map.put(color_str, color_map.get(color_str)+1 );
				}
			}	
		}
		
		int total_pixels = thumbnail.getWidth() * thumbnail.getHeight();
		
		for(String color_key : color_map.keySet()) {
			Double percentage = color_map.get(color_key)/(double)total_pixels;
			color_percentages.put(color_key, percentage);
		}
		*/
		//Map<String, Double> color_percentages = new HashMap<String, Double>();
		//return color_percentages;
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
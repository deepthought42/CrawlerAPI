package com.qanairy.models.audit.domain;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.gcp.CloudVisionUtils;
import com.qanairy.models.Domain;
import com.qanairy.models.ElementState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.ColorData;
import com.qanairy.models.audit.ColorPaletteObservation;
import com.qanairy.models.audit.ColorPaletteUtils;
import com.qanairy.models.audit.ColorUsageStat;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.audit.Score;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.models.enums.ColorScheme;
import com.qanairy.services.AuditService;
import com.qanairy.services.DomainService;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class DomainColorPaletteAudit implements IExecutableDomainAudit{
	private static Logger log = LoggerFactory.getLogger(DomainColorPaletteAudit.class);

	private List<String> gray_colors = new ArrayList<>();
	private List<String> colors = new ArrayList<>();
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private AuditService audit_service;
	
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
		
		//get all color palette audits associated with most recent audit record for domain host
		Set<Audit> color_palette_audits = domain_service.getMostRecentAuditRecordColorPaletteAudits(domain.getHost());
		int points = 0;
		int max_points = 0;
		Map<String, Set<String>> palette_colors = new HashMap<>();
		Map<String, Boolean> schemes_recognized = new HashMap<>();
		List<String> color_strings = new ArrayList<>();
		List<String> gray_color_strings = new ArrayList<>();

		//iterate over color palette audits
		for(Audit audit : color_palette_audits) {
			List<Observation> page_audit_observations = audit_service.getObservations(audit.getKey());
			//extract colors into global color map
			//calculate global color usage percentages using page state audits
			for(Observation observation : page_audit_observations) {
				if(observation instanceof ColorPaletteObservation) {
					ColorPaletteObservation palette_observation = (ColorPaletteObservation)observation;
					schemes_recognized.put(palette_observation.getColorScheme().getShortName(), Boolean.TRUE);
					
					gray_color_strings.addAll(palette_observation.getGrayColors());
					color_strings.addAll(palette_observation.getColors());
					palette_colors.putAll(palette_observation.getPalette());
				}
			}
			
			points += audit.getPoints();
			max_points += audit.getTotalPossiblePoints();
		}

		//unpack color palette into ColorData object map
		Map<ColorData, Set<ColorData>> color_data_palette_map = new HashMap<>();
		for(String primary_color : palette_colors.keySet()) {
			ColorData primary =  new ColorData(primary_color);
			Set<ColorData> secondary_colors = new HashSet<>();
			for(String secondary_color : palette_colors.get(primary_color)) {
				secondary_colors.add(new ColorData(secondary_color));
			}
			
			color_data_palette_map.put(primary, secondary_colors);
		}
		
		if(schemes_recognized.size() == 1) {
			points += 3;
		}
		max_points += 3;
		
		
		DomainColorPaletteObservation observation = new DomainColorPaletteObservation(
															palette_colors, 
															color_strings, 
															gray_color_strings, 
															schemes_recognized.keySet(), 
															"This is a color scheme description");
		observations.add(observation);
			
		ColorScheme scheme = ColorPaletteUtils.getColorScheme(color_data_palette_map);
		
		Score score = ColorPaletteUtils.getPaletteScore(color_data_palette_map, scheme);
		points += score.getPointsAchieved();
		max_points += score.getMaxPossiblePoints();
		
		//score colors found against scheme
		setGrayColors(new ArrayList<>(gray_color_strings));
		setColors(new ArrayList<>(color_strings));
		 
		
		return new Audit(AuditCategory.COLOR_MANAGEMENT, AuditSubcategory.COLOR_PALETTE, points, observations, AuditLevel.DOMAIN, max_points);
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		//REMOVE THE FOLLOWING BECUASE IT IS OLD AND NO LONGER NEEDED
		/*
		
		//get all pages
		List<PageVersion> pages = domain_service.getPages(domain.getHost());
		
		//get most recent page state for each page
		for(PageVersion page : pages) {
			log.warn("color management page version key :: "+page.getKey());
			//for each page state get elements
			PageState page_state = page_service.getMostRecentPageState(page.getKey());
			log.warn("color management page state :: "+page_state.getKey());
			List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
			
			//get image attributes from google cloud vision
			//cloudVisionTemplate.analyzeImage(imageResource, featureTypes)
			//retrieve image colors based on screenshots minus the contents of image elements
			try {
				color_usage_list.addAll(extractColorsFromPageState(new URL(page_state.getFullPageScreenshotUrl()), elements));
				log.warn("color_map ::   "+color_usage_list);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		log.warn("colors :: "+colors.size());
		
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
		
		ColorPaletteObservation observation = new ColorPaletteObservation(palette_stringified, new ArrayList<>(filtered_colors.keySet()), new ArrayList<>(gray_colors.keySet()), color_scheme, "This is a color scheme description");
		observations.add(observation);
			
		ColorScheme scheme = ColorPaletteUtils.getColorScheme(palette);
		int score = ColorPaletteUtils.getPaletteScore(palette, scheme);
		
		//score colors found against scheme
		setGrayColors(new ArrayList<>(gray_colors.keySet()));
		setColors(new ArrayList<>(colors.keySet()));
		 
		
		return new Audit();
		 */
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



package com.looksee.models.audit.aesthetics;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
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

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.ColorData;
import com.looksee.models.audit.ColorPaletteIssueMessage;
import com.looksee.models.audit.ColorPaletteUtils;
import com.looksee.models.audit.ColorUsageStat;
import com.looksee.models.audit.IExecutablePageStateAudit;
import com.looksee.models.audit.PaletteColor;
import com.looksee.models.audit.Score;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.ColorScheme;
import com.looksee.models.enums.Priority;
import com.looksee.services.PageStateService;
import com.looksee.services.PaletteColorService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.ImageUtils;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class ColorPaletteAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ColorPaletteAudit.class);
	
	@Autowired
	private PaletteColorService palette_color_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private UXIssueMessageService ux_issue_service;
		
	/**
	 * {@inheritDoc}
	 * 
	 * Identifies colors used on page, the color scheme type used, and the ultimately the score for how the colors used conform to scheme
	 *  
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system) {
		assert page_state != null;
		
		String why_it_matters = "Studies have found that it takes 90 seconds for a customer to form an" + 
				" opinion on a product. 62–90% of that interaction is determined by the" + 
				" color of the product alone." + 
				" Color impacts how a user feels when they interact with your website; it is" + 
				" key to their experience. The right usage of colors can brighten a website" + 
				" and communicates the tone of your brand. Furthermore, using your brand" + 
				" colors consistently makes the website appear cohesive and collected," + 
				" while creating a sense of familiarity for the user.";
		
		String ada_compliance = "There are no ADA compliance guidelines regarding the website color" + 
				" palette. However, keeping a cohesive color palette allows you to create" + 
				" a webpage easy for everyone to read. ";
		
		
		List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
		List<ColorUsageStat> color_usage_list = new ArrayList<>();
		
		try {
			color_usage_list.addAll(extractColorsFromScreenshot(new URL(page_state.getFullPageScreenshotUrlOnload()), elements));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//extract declared css color properties
		/*
		List<ColorData> colors_declared = new ArrayList<>();
		List<String> raw_stylesheets = Browser.extractStylesheets(page_state.getSrc()); 
		
		//open stylesheet
		for(String stylesheet : raw_stylesheets) {
			colors_declared.addAll(BrowserUtils.extractColorsFromStylesheet(stylesheet));
		}
		*/
		Map<String, ColorData> color_map = new HashMap<>();
		for(ColorUsageStat stat : color_usage_list) {
			ColorData color = new ColorData(stat);
			if(color.getUsagePercent() < 0.0005) {
				continue;
			}
			color.setUsagePercent(stat.getPixelPercent());
			log.warn("Color :: " + color.rgb() + "  :  " + color.getUsagePercent());
			
			color_map.put(color.rgb().trim(), color);
		}

		log.warn("###########################################################################");
		log.warn("###########################################################################");
		log.warn("###########################################################################");
		
		/*
		Map<ColorUsageStat, Boolean> gray_colors = new HashMap<ColorUsageStat, Boolean>();
		Map<ColorUsageStat, Boolean> filtered_colors = new HashMap<>();
		//discard any colors that are transparent
		for(ColorUsageStat color: color_usage_list) {
			String rgb_color_str = "rgb("+color.getRed()+","+color.getGreen()+","+color.getBlue()+")";
			//convert rgb to hsl, store all as Color object
			color_map.put(rgb_color_str, new ColorData(rgb_color_str));
			if( Math.abs(color.getRed() - color.getGreen()) < 4
					&& Math.abs(color.getRed() - color.getBlue()) < 4
					&& Math.abs(color.getBlue() - color.getGreen()) < 4) {
				gray_colors.put(color, Boolean.TRUE);
			}
			else {
				filtered_colors.put(color, Boolean.TRUE);
			}
		}
		gray_colors.remove(null);
		filtered_colors.remove(null);
		 */
		List<ColorData> colors = new ArrayList<ColorData>(color_map.values());
		/*
		for(ColorUsageStat color : color_usage_list) {
			if(color.getPixelPercent() >= 0.025) {
				colors.add(new ColorData(color));
			}
		}
		*/
		//colors.addAll(colors);
		//generate palette, identify color scheme and score how well palette conforms to color scheme
		List<PaletteColor> palette_colors = new ArrayList<>();
		for(PaletteColor palette : ColorPaletteUtils.extractPalette(colors)) {
			palette_colors.add( palette_color_service.save(palette) );
		}
		ColorScheme color_scheme = ColorPaletteUtils.getColorScheme(palette_colors);

		Set<String> labels = new HashSet<>();
		labels.add("accessibility");
		labels.add("color");
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.AESTHETICS.toString());
		String title = "Sub-optimal color palette";
		String description = "Your color palette has too much color and can be overwhelming for users to process";
		String recommendation = "Color palettes with 2 main colors are perceived as most pleasant to the human eye";
				
		
		UXIssueMessage palette_issue_message = new ColorPaletteIssueMessage(
																Priority.HIGH,
																description,
																recommendation,
																new ArrayList<>(),
																palette_colors,
																color_scheme,
																AuditCategory.AESTHETICS,
																labels, 
																ada_compliance,
																title, 0, 0);
		
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		issue_messages.add(ux_issue_service.save(palette_issue_message));
		
		//score colors found against scheme
		Score score = ColorPaletteUtils.getPaletteScore(palette_colors, color_scheme);
		//observations.add(observation_service.save(observation));
		//score colors found against scheme
		//setGrayColors(new ArrayList<>(gray_colors));
		
		return new Audit(AuditCategory.AESTHETICS,
						 AuditSubcategory.COLOR_MANAGEMENT,
						 AuditName.COLOR_PALETTE,
						 score.getPointsAchieved(),
						 issue_messages,
						 AuditLevel.PAGE,
						 score.getMaxPossiblePoints(),
						 page_state.getUrl(),
						 why_it_matters, 
						 description,
						 true);
	}
	
	/**
	 * 
	 * @param screenshot_url
	 * @param elements
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private List<ColorUsageStat> extractColorsFromScreenshot(URL screenshot_url,
															 List<ElementState> elements
	) throws MalformedURLException, IOException {		
		//copy page state full page screenshot
		BufferedImage screenshot = ImageIO.read(screenshot_url);
		
		for(ElementState element : elements) {
			if(!element.getName().contentEquals("img")) {
				continue;
			}
			
			for(int x_pixel = element.getXLocation(); x_pixel < (element.getXLocation()+element.getWidth()); x_pixel++) {
				if(x_pixel >= screenshot.getWidth()) {
					break;
				}
				
				if(x_pixel < 0) {
					continue;
				}
				for(int y_pixel = element.getYLocation(); y_pixel < (element.getYLocation()+element.getHeight()); y_pixel++) {
					if(y_pixel >= screenshot.getHeight()) {
						break;
					}
					
					if(y_pixel < 0) {
						continue;
					}
					screenshot.setRGB(x_pixel, y_pixel, new Color(0,0,0).getRGB());
				}	
			}
		}
		
		//return CloudVisionUtils.extractImageProperties(screenshot);
		return ImageUtils.extractImageProperties(screenshot);
	}
}
package com.qanairy.models.audit;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.gcp.CloudVisionUtils;
import com.qanairy.models.Element;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.services.ObservationService;
import com.qanairy.services.PageStateService;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class NonTextColorContrastAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(NonTextColorContrastAudit.class);

	List<Element> flagged_elements = new ArrayList<>();

	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ObservationService observation_service;
	
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
		
		//get all button elements
		List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
		List<ElementState> non_text_elements = getAllButtons(elements);
		non_text_elements.addAll(getAllInputs(elements));

		log.warn("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
		log.warn("non text elements identified :: "+non_text_elements.size());
		
		return evaluateNonTextContrast(page_state, non_text_elements);
	}


	private List<ElementState> getAllIcons(List<ElementState> elements) {
		//identify font awesome icons
		
		return null;
	}

	private List<ElementState> getAllInputs(List<ElementState> elements) {
		return elements.parallelStream().filter(p ->p.getName().equalsIgnoreCase("input")).distinct().collect(Collectors.toList());  // iterating price 

	}

	private List<ElementState> getAllButtons(List<ElementState> elements) {
		return elements.parallelStream().filter(p ->p.getName().equalsIgnoreCase("button")).distinct().collect(Collectors.toList());  // iterating price 
	}
	
	public Color getPixelColor(URL image_url, int x, int y) throws IOException {
		BufferedImage image = ImageIO.read(image_url);
		return new Color(image.getRGB(x, y));
	}

	/**
	 * Evaluates non text elements for contrast with parent element
	 * 
	 * @param page_state
	 * @param non_text_elements
	 * @return
	 */
	private Audit evaluateNonTextContrast(PageState page_state, List<ElementState> non_text_elements) {
		assert page_state != null;
		assert non_text_elements != null;
		
		int score = 0;
		List<ElementState> low_contrast_elements = new ArrayList<>();
		List<ElementState> mid_contrast_elements = new ArrayList<>();
		List<ElementState> high_contrast_elements = new ArrayList<>();

		for(ElementState element : non_text_elements) {
			//get parent element of button
			try {
				ColorUsageStat most_used_color = extractMostUsedColor(element);
				
				//randomly sample colors just outside the perimeter of the element within page state screenshot
				int x_position = element.getXLocation();
				int y_position = element.getYLocation();

				//choose elemtn just to the right of the elemnt in the page screenshot
				Color parent_background_color = getPixelColor(new URL(page_state.getFullPageScreenshotUrl()), x_position-5, y_position-5);				
				String parent_rgb = "rgb(" + parent_background_color.getRed()+ "," + parent_background_color.getGreen() + "," + parent_background_color.getBlue() + ")";
				double contrast = ColorData.computeContrast(new ColorData(parent_rgb), new ColorData(most_used_color.getRGB()));
				//calculate contrast of button background with background of parent element
				if(contrast < 3.0){
					score += 1;
					low_contrast_elements.add(element);
				}else if(contrast >= 3.0 && contrast < 4.5) {
					score += 2;
					mid_contrast_elements.add(element);
				}
				else {
					score += 3;
					high_contrast_elements.add(element);
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		} 

		List<Observation> observations = new ArrayList<>();
		if(!low_contrast_elements.isEmpty()) {
			ElementStateObservation low_contrast_observation = new ElementStateObservation(low_contrast_elements, "Elements with a contrast below 3.0");
			observations.add(observation_service.save(low_contrast_observation));
		}
		if(!mid_contrast_elements.isEmpty()) {
			ElementStateObservation mid_contrast_observation = new ElementStateObservation(mid_contrast_elements, "Elements with a contrast between 3.0 and 4.5");
			observations.add(observation_service.save(mid_contrast_observation));
		}
		if(!high_contrast_elements.isEmpty()) {
			ElementStateObservation high_contrast_observation = new ElementStateObservation(high_contrast_elements, "Elements with a contrast greater than 4.5");
			observations.add(observation_service.save(high_contrast_observation));
		}
		if(observations.isEmpty()) {
			ElementStateObservation success_observation = new ElementStateObservation(non_text_elements, "All text and header elements are exceeding WCAG standards");
			observations.add(observation_service.save(success_observation));
		}
		
		return new Audit(AuditCategory.COLOR_MANAGEMENT, AuditSubcategory.NON_TEXT_BACKGROUND_CONTRAST, score, observations, AuditLevel.PAGE, non_text_elements.size() *3);
	}


	private ColorUsageStat extractMostUsedColor(ElementState button) throws IOException {
		assert button != null;
		
		ColorUsageStat most_used_color = null;

		float most_common_color = 0.0f;
		URL url = new URL(button.getScreenshotUrl());
		BufferedImage screenshot_reader = ImageIO.read(url);
		List<ColorUsageStat> image_props = CloudVisionUtils.extractImageProperties(screenshot_reader);
		//get most used color as background color
		for(ColorUsageStat stat : image_props) {
			if(most_common_color < stat.getPixelPercent()){
				most_common_color = stat.getPixelPercent();
				most_used_color = stat;
			}
		}
		
		return most_used_color;
	}
}
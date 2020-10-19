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
import com.looksee.gcp.GoogleCloudStorage;
import com.qanairy.models.Element;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.services.ElementStateService;
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
	private ElementStateService element_state_service;
	
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
	
	public Color getPixelColor(String image_url, int x, int y) throws IOException {
		BufferedImage image = GoogleCloudStorage.getImage(image_url, BrowserType.CHROME);
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
		int max_points = 0;
		List<ElementState> low_contrast_elements = new ArrayList<>();
		List<ElementState> mid_contrast_elements = new ArrayList<>();
		List<ElementState> high_contrast_elements = new ArrayList<>();

		for(ElementState element : non_text_elements) {
			//get parent element of button
			try {
				//ColorUsageStat most_used_color = extractMostUsedColor(element);
				
				//randomly sample colors just outside the perimeter of the element within page state screenshot
				//int x_position = element.getXLocation();
				//int y_position = element.getYLocation();
				//ElementState parent = element_state_service.getParentElement(page_state.getKey(), element.getKey());
				
				//retrieve all elements for page state
				//evaluate each element to see if xpath is a subset of element xpath, keeping the elements with shortest difference
				int diff_length = Integer.MAX_VALUE;
				ElementState parent = null;
				List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
				for(ElementState element_state : elements) {
					if(element_state.getKey().contentEquals(element.getKey())) {
						continue;
					}
					
					if(element.getXpath().contains(element_state.getXpath())) {
						/*if(parent.getRenderedCssValues().get("background-color").contentEquals(element.getRenderedCssValues().get("background-color"))) {
							continue;
						}
						*/
						int temp_diff = element.getXpath().length() - element_state.getXpath().length();
						if(temp_diff < diff_length) {
							diff_length = temp_diff;
							parent = element_state;
						}
					}
				}
				//choose elemtn just to the right of the elemnt in the page screenshot
				//Color parent_background_color = getPixelColor(page_state.getFullPageScreenshotUrl(), x_position-10, y_position-10);				
				//String parent_rgb = "rgb(" + parent_background_color.getRed()+ "," + parent_background_color.getGreen() + "," + parent_background_color.getBlue() + ")";
				log.warn("page state url ::   "+page_state.getUrl());
				log.warn("element key :: "+element.getKey());
				log.warn("parent element :: "+parent.getXpath());
				log.warn("element element :: "+element.getXpath());
				log.warn("parent background color  ::  "+parent.getRenderedCssValues().get("background-color"));
				log.warn("element background color :: "+element.getRenderedCssValues().get("background-color"));
				double contrast = ColorData.computeContrast(new ColorData(parent.getRenderedCssValues().get("background-color")), new ColorData(element.getRenderedCssValues().get("background-color")));
				//calculate contrast of button background with background of parent element
				if(contrast < 3.0){
					//no points are rewarded for low contrast
					low_contrast_elements.add(element);
				}else if(contrast >= 3.0 && contrast < 4.5) {
					score += 1;
					mid_contrast_elements.add(element);
				}
				else {
					score += 2;
					high_contrast_elements.add(element);
				}
				max_points+=2;
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
		
		return new Audit(AuditCategory.COLOR_MANAGEMENT, AuditSubcategory.NON_TEXT_BACKGROUND_CONTRAST, score, observations, AuditLevel.PAGE, max_points, page_state.getUrl());
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
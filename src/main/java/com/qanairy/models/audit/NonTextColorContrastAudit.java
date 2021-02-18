package com.qanairy.models.audit;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.IIOException;
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
import com.qanairy.models.enums.AuditName;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.services.ElementStateService;
import com.qanairy.services.ObservationService;
import com.qanairy.services.PageStateService;
import com.qanairy.utils.ImageUtils;


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
		BufferedImage image = GoogleCloudStorage.getImage(image_url);
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
				//retrieve all elements for page state
				//evaluate each element to see if xpath is a subset of element xpath, keeping the elements with shortest difference
				ColorData parent_bkg = null;
				List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
				for(ElementState element_state : elements) {
					if(element_state.getKey().contentEquals(element.getKey())) {
						continue;
					}
					
					//log.warn("checking if element is parent ::  "+element_state.getXpath());
					if(element.getXpath().contains(element_state.getXpath())) {
						int element_area = element.getWidth() * element.getHeight();
						int parent_area = element_state.getWidth() * element_state.getHeight();
						
						
						log.warn("element area  :   "+element_area);
						log.warn("parent area  :   "+parent_area);
						log.warn("is parent twice the size of element ??    "+(parent_area > (element_area * 2)));
						
						if(parent_area > (element_area * 3)) {
							//parent = element_state;
							//parent_bkg = ImageUtils.extractBackgroundColor(element_state);
							
							parent_bkg = new ColorData(element_state.getBackgroundColor());
						}
					}
				}
				//choose elemtn just to the right of the elemnt in the page screenshot
				//Color parent_background_color = getPixelColor(page_state.getFullPageScreenshotUrl(), x_position-10, y_position-10);				
				//String parent_rgb = "rgb(" + parent_background_color.getRed()+ "," + parent_background_color.getGreen() + "," + parent_background_color.getBlue() + ")";

				log.warn("page state url ::   "+page_state.getUrl());
				log.warn("parent element :: "+parent_bkg);
				log.warn("element key :: "+element.getKey());
				//log.warn("parent element :: "+parent.getXpath());
				log.warn("element xpath :: "+element.getXpath());
				//ColorData parent_bkg = new ColorData(parent.getRenderedCssValues().get("background-color"));
				//ColorData element_bkg = ImageUtils.extractBackgroundColor(element);
				ColorData element_bkg = new ColorData(element.getBackgroundColor());

				//if element has border color different than element then set element_bkg to border color
				if(!element.getName().contentEquals("input")
						&& hasContinuousBorder(element) 
						&& !borderColorMatchesBackground(element))
				{
					element_bkg = getBorderColor(element);
				}
				
				double contrast = ColorData.computeContrast(parent_bkg, element_bkg);
				element.setNonTextContrast(contrast);
				element_state_service.save(element);
				
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
			catch(NullPointerException e) {
				log.warn("null pointer..." + e.getMessage());
				e.printStackTrace();
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
		/*
		if(!high_contrast_elements.isEmpty()) {
			ElementStateObservation high_contrast_observation = new ElementStateObservation(high_contrast_elements, "Elements with a contrast greater than 4.5");
			observations.add(observation_service.save(high_contrast_observation));
		}
		*/
		
		
		String why_it_matters = "<p>Icons are an easily recognizable, fun element, and a great way to\n" + 
				"communicate with your user beyond just using text. Icons should be\n" + 
				"familiar and captivating.</p>" + 
				"<p>Bright colors have higher conversion rates, so it is important for your\n" + 
				"button to have a high contrast score to create an eye-catching effect\n" + 
				"and be evidently clickable.</p>";
		String ada_compliance = "Non-text items meet the minimum required ratio level of 3:1.";
		
		return new Audit(AuditCategory.AESTHETICS,
						 AuditSubcategory.COLOR_MANAGEMENT,
						 AuditName.NON_TEXT_BACKGROUND_CONTRAST,
						 score,
						 observations,
						 AuditLevel.PAGE,
						 max_points,
						 page_state.getUrl(),
						 why_it_matters,
						 ada_compliance,
						 new HashSet<>());
	}


	private ColorData getBorderColor(ElementState element) {
		return new ColorData(element.getRenderedCssValues().get("border-bottom-color"));
	}


	private boolean borderColorMatchesBackground(ElementState element) {
		String border = element.getRenderedCssValues().get("border-bottom-color");
		String background = element.getRenderedCssValues().get("background-color");
		return border.contentEquals(background);
	}


	private boolean hasContinuousBorder(ElementState element) {
		String bottom = element.getRenderedCssValues().get("border-bottom-color");
		String top = element.getRenderedCssValues().get("border-top-color");
		String left = element.getRenderedCssValues().get("border-left-color");
		String right = element.getRenderedCssValues().get("border-right-color");
		return bottom.contentEquals(top)
				&& top.contentEquals(left)
				&& left.contentEquals(right);
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
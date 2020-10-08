package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
import com.qanairy.services.ObservationService;
import com.qanairy.services.PageStateService;
import com.qanairy.utils.BrowserUtils;
import com.qanairy.utils.ElementStateUtils;
import com.qanairy.utils.ImageUtils;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class TextColorContrastAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TextColorContrastAudit.class);
	
	@Autowired
	private ObservationService observation_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	public TextColorContrastAudit() {}

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
		
		int total_headlines = 0;
		int total_text_elems = 0;
		int headline_score = 0;
		int text_score = 0;
		
		List<ElementState> high_header_contrast = new ArrayList<>();
		List<ElementState> mid_header_contrast = new ArrayList<>();
		List<ElementState> low_header_contrast = new ArrayList<>();

		List<ElementState> high_text_contrast = new ArrayList<>();
		List<ElementState> mid_text_contrast = new ArrayList<>();
		List<ElementState> low_text_contrast = new ArrayList<>();

		//List<ElementState> element_list = new ArrayList<>();
		log.warn("page state key :: "+ page_state.getKey());
		List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
		log.warn("Elements available for TEXT COLOR CONTRAST evaluation ...  "+elements.size());
		//filter elements that aren't text elements
		List<ElementState> element_list = BrowserUtils.getTextElements(elements);
		
		log.warn("getting contrast for elements :: "+ element_list.size());
		log.warn("evaluating elements for page ....  "+page_state.getUrl());
		//analyze screenshots of all text images for contrast
		for(ElementState element : element_list) {			
			List<ColorUsageStat> color_data_list = new ArrayList<>();
			try {
				log.warn("extracting image properties for element ::   "+element.getName());
				color_data_list.addAll( ImageUtils.extractImageProperties(ImageIO.read(new URL(element.getScreenshotUrl()))) );
				log.warn("successfully extracted image properties for element ::   "+element.getName());

				color_data_list.sort((ColorUsageStat h1, ColorUsageStat h2) -> Float.compare(h1.getPixelPercent(), h2.getPixelPercent()));
	
				ColorUsageStat background_usage = color_data_list.get(color_data_list.size()-1);
				ColorUsageStat foreground_usage = color_data_list.get(color_data_list.size()-2);
	
				ColorData background_color_data = new ColorData("rgb("+ background_usage.getRed()+","+background_usage.getGreen()+","+background_usage.getBlue()+")");
				ColorData text_color = new ColorData("rgb("+ foreground_usage.getRed()+","+foreground_usage.getGreen()+","+foreground_usage.getBlue()+")");
				float largest_pixel_percent = 0;
			    
				//extract background colors
				for(ColorUsageStat color_stat : color_data_list) {
					//get color most used for background color
					if(color_stat.getPixelPercent() > largest_pixel_percent) {
						largest_pixel_percent = color_stat.getPixelPercent();
					}
				}
				
				double max_luminosity = 0.0;
				double min_luminosity = 0.0;
				
				if(text_color.getLuminosity() > background_color_data.getLuminosity()) {
					min_luminosity = background_color_data.getLuminosity();
					max_luminosity = text_color.getLuminosity();
				}
				else {
					min_luminosity = text_color.getLuminosity();
					max_luminosity = background_color_data.getLuminosity();
				}
				
				double contrast = 0.0;
				if(ElementStateUtils.isHeader(element.getName())) {
					//score header element
					//calculate contrast between text color and background-color
					contrast = (max_luminosity + 0.001) / (min_luminosity + 0.001);
					total_headlines++;
					/*
					headlines < 3; value = 1
					headlines > 3 and headlines < 4.5; value = 2
					headlines >= 4.5; value = 3
					 */
					if(contrast < 3) {
						headline_score += 1;
						low_header_contrast.add(element);
					}
					else if(contrast >= 3 && contrast < 4.5) {
						headline_score += 2;
						mid_header_contrast.add(element);
					}
					else if(contrast >= 4.5) {
						headline_score += 3;
						high_header_contrast.add(element);
					}
				}
				else {
					contrast = (max_luminosity + 0.001) / (min_luminosity + 0.001);
					total_text_elems++;
					/*
					text < 4.5; value = 1
					text >= 4.5 and text < 7; value = 2
					text >=7; value = 3
					 */
					if(contrast < 4.5) {
						text_score += 1;
						low_text_contrast.add(element);
					}
					else if(contrast >= 4.5 && contrast < 7) {
						text_score += 2;
						mid_text_contrast.add(element);
					}
					else if(contrast >= 7) {
						text_score += 3;
						high_text_contrast.add(element);
					}
				}
			} catch (Exception e) {
				log.warn("element screenshot url  :: "+element.getScreenshotUrl());
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		List<Observation> observations = new ArrayList<>();
		if(!high_header_contrast.isEmpty()) {
			ElementStateObservation high_header_contrast_observation = new ElementStateObservation(high_header_contrast, "Headers with contrast above 4.5");
			observations.add(observation_service.save(high_header_contrast_observation));
		}
		if(!mid_header_contrast.isEmpty()) {
			ElementStateObservation mid_header_contrast_observation = new ElementStateObservation(mid_header_contrast, "Headers with contrast between 3 and 4.5");
			observations.add(observation_service.save(mid_header_contrast_observation));
		}
		if(!low_header_contrast.isEmpty()) {
			ElementStateObservation low_header_contrast_observation = new ElementStateObservation(low_header_contrast, "Headers with contrast below 3");
			observations.add(observation_service.save(low_header_contrast_observation));
		}
		
		if(!high_text_contrast.isEmpty()) {
			ElementStateObservation high_text_observation = new ElementStateObservation(high_text_contrast, "Text with contrast above 7");
			observations.add(observation_service.save(high_text_observation));
		}
		if(!mid_text_contrast.isEmpty()) {
			ElementStateObservation mid_text_observation = new ElementStateObservation(mid_text_contrast, "Text with contrast between 4.5 and 7");
			observations.add(observation_service.save(mid_text_observation));
		}
		if(!low_text_contrast.isEmpty()) {
			ElementStateObservation low_text_observation = new ElementStateObservation(low_text_contrast, "Text with contrast below 4.5");
			observations.add(observation_service.save(low_text_observation));
		}
		
		int total_possible_points = ((total_headlines*3) + (total_text_elems*3));
		log.warn("TEXT COLOR CONTRAST AUDIT SCORE   ::   " + (headline_score+text_score) + " : " + total_possible_points);
		return new Audit(AuditCategory.COLOR_MANAGEMENT, AuditSubcategory.TEXT_BACKGROUND_CONTRAST, (headline_score+text_score), observations, AuditLevel.PAGE, total_possible_points);
	}

	
}
package com.looksee.models.audit.aesthetics;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.api.MessageBroadcaster;
import com.looksee.gcp.GoogleCloudStorage;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.ColorContrastIssueMessage;
import com.looksee.models.audit.ColorData;
import com.looksee.models.audit.ElementStateIssueMessage;
import com.looksee.models.audit.IExecutablePageStateAudit;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.audit.recommend.ColorContrastRecommendation;
import com.looksee.models.audit.recommend.Recommendation;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.Priority;
import com.looksee.services.AuditService;
import com.looksee.services.ElementStateService;
import com.looksee.services.PageStateService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.ColorUtils;
import com.looksee.utils.ImageUtils;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class NonTextColorContrastAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(NonTextColorContrastAudit.class);

	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ElementStateService element_state_service;
	
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
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
		
		//get all button elements
		List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
		if(page_state.getUrl().contains("apple.com/shop/buy-watch/apple-watch")) {
			log.warn("-------------------------------------------------------------");
			log.warn(elements.size()+" page elements found");
		}
		List<ElementState> non_text_elements = getAllButtons(elements);
		
		if(page_state.getUrl().contains("apple.com/shop/buy-watch/apple-watch")) {
			log.warn(non_text_elements.size()+" page elements found");
			log.warn("-------------------------------------------------------------");
		}
		non_text_elements.addAll(getAllInputs(elements));
			
		return evaluateNonTextContrast(page_state, non_text_elements, design_system);
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
	 * @param design_system TODO
	 * @return
	 */
	private Audit evaluateNonTextContrast(PageState page_state, List<ElementState> non_text_elements, DesignSystem design_system) {
		assert page_state != null;
		assert non_text_elements != null;
		
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		Set<String> labels = new HashSet<>();
		labels.add(AuditCategory.AESTHETICS.toString().toLowerCase());
		labels.add("color contrast");
		labels.add("accessibility");
		
		if(page_state.getUrl().contains("apple.com/shop/buy-watch/apple-watch")) {
			log.warn("-------------------------------------------------------------");
			log.warn(non_text_elements.size()+" non-text elements being evaluated");
			log.warn("-------------------------------------------------------------");
		}
		for(ElementState element : non_text_elements) {
			ColorData font_color = new ColorData(element.getRenderedCssValues().get("color"));
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

					if(element.getXpath().contains(element_state.getXpath())) {
						int element_area = element.getWidth() * element.getHeight();
						int parent_area = element_state.getWidth() * element_state.getHeight();
							
						if(parent_area > (element_area * 3)) {
							//parent = element_state;
							//parent_bkg = ImageUtils.extractBackgroundColor(element_state);
							String bg_color_css = element_state.getRenderedCssValues().get("background-color");
							String bg_image = element_state.getRenderedCssValues().get("background-image");
							String bg_color = "255,255,255";
							
							if(!bg_color_css.contains("inherit") 
								&& !bg_color_css.contains("rgba") 
								&& (bg_image == null || bg_image.isEmpty() ) 
							) {
								bg_color = bg_color_css;
							}
							else if(bg_color_css.contains("rgba") 
									&& !element_state.getScreenshotUrl().isEmpty()
							) {
								//extract opacity color
								ColorData bkg_color = ImageUtils.extractBackgroundColor( 
																			new URL(element_state.getScreenshotUrl()), 
																			font_color);
								bg_color = bkg_color.rgb();	
							}
							else if(!element_state.getScreenshotUrl().isEmpty()) {
								ColorData bkg_color = ImageUtils.extractBackgroundColor( 
																			new URL(element_state.getScreenshotUrl()), 
																			font_color);
								bg_color = bkg_color.rgb();
							}
							
							element_state.setBackgroundColor(bg_color);
							element_state = element_state_service.save(element_state);
							
							parent_bkg = new ColorData(bg_color);
						}
					}
				}

				//choose elemtn just to the right of the elemnt in the page screenshot
				//Color parent_background_color = getPixelColor(page_state.getFullPageScreenshotUrl(), x_position-10, y_position-10);				
				//String parent_rgb = "rgb(" + parent_background_color.getRed()+ "," + parent_background_color.getGreen() + "," + parent_background_color.getBlue() + ")";

				//ColorData parent_bkg = new ColorData(parent.getRenderedCssValues().get("background-color"));
				//ColorData element_bkg = ImageUtils.extractBackgroundColor(element);
				String bg_color_css = element.getRenderedCssValues().get("background-color");
				String bg_image = element.getRenderedCssValues().get("background-image");
				String bg_color = "255,255,255";

				if(!bg_color_css.contains("inherit") && !bg_color_css.contains("rgba") && (bg_image == null || bg_image.isEmpty() ) ) {
					bg_color = bg_color_css;
				}
				else if(bg_color_css.contains("rgba") && !element.getScreenshotUrl().isEmpty()) {
					//extract opacity color
					ColorData bkg_color = ImageUtils.extractBackgroundColor( 
																new URL(element.getScreenshotUrl()),
																font_color);
					bg_color = bkg_color.rgb();	
				}
				else if(!element.getScreenshotUrl().isEmpty()) {
					ColorData bkg_color = ImageUtils.extractBackgroundColor( 
																new URL(element.getScreenshotUrl()),
																font_color);
					bg_color = bkg_color.rgb();
				}
				
				element.setBackgroundColor(bg_color);
				element = element_state_service.save(element);
				
				//getting border color
				ColorData element_bkg = new ColorData(element.getBackgroundColor());
				String border_color_rgb = element_bkg.rgb();
				if(element.getRenderedCssValues().get("border-inline-start-width") != "0px") {
					border_color_rgb = element.getRenderedCssValues().get("border-inline-start-color");
				}
				else if(element.getRenderedCssValues().get("border-inline-end-width") != "0px") {
					border_color_rgb = element.getRenderedCssValues().get("border-inline-end-color");
				}
				else if(element.getRenderedCssValues().get("border-block-start-width") != "0px") {
					border_color_rgb = element.getRenderedCssValues().get("border-block-start-color");
				}
				else if(element.getRenderedCssValues().get("border-block-end-width") != "0px") {
					border_color_rgb = element.getRenderedCssValues().get("border-block-end-color");
				}

				ColorData border_color = new ColorData(border_color_rgb);
				
				//if element has border color different than element then set element_bkg to border color
				if(!element.getName().contentEquals("input")
						&& hasContinuousBorder(element) 
						&& !borderColorMatchesBackground(element))
				{
					element_bkg = getBorderColor(element);
				}
				
				if(parent_bkg == null) {
					parent_bkg = new ColorData("rgb(255,255,255)");
				}
				
				double contrast = ColorData.computeContrast(parent_bkg, element_bkg);
				double border_contrast = ColorData.computeContrast(parent_bkg, border_color);
				double highest_contrast = 0.0;
				if(contrast > border_contrast) {
					highest_contrast = contrast;
				}
				else {
					highest_contrast = border_contrast;
				}
				element.setNonTextContrast(highest_contrast);
				element = element_state_service.save(element);
				
				//calculate contrast of button background with background of parent element
				if(highest_contrast < 3.0){
					String title = "Element has low contrast";
					String description = "Element background has low contrast against the surrounding background";
					//no points are rewarded for low contrast
					
					String ada_compliance = "Non-text items should have a minimum contrast ratio of 3:1.";
					
					String recommendation = "use a darker/lighter shade of "+ element.getBackgroundColor() +" to achieve a contrast of 3:1";
					Set<Recommendation> recommendations = generateNonTextContrastRecommendations(element, 
																								 parent_bkg);
					
					ColorContrastIssueMessage low_contrast_issue = new ColorContrastIssueMessage(
																				Priority.HIGH,
																				description,
																				highest_contrast,
																				element_bkg.rgb(),
																				parent_bkg.rgb(),
																				element,
																				AuditCategory.AESTHETICS,
																				labels,
																				ada_compliance,
																				title,
																				null, 
																				0, 
																				1, 
																				recommendations,
																				recommendation);
					
					issue_messages.add(low_contrast_issue);
					MessageBroadcaster.sendIssueMessage(page_state.getId(), low_contrast_issue);
				}
				else {
					String title = "Element contrast is accessisible";
					String description = "Element background has appropriate contrast for accessibility";
					//no points are rewarded for low contrast
					
					String ada_compliance = "Element is compliant with WCAG 2.1 " + design_system.getWcagComplianceLevel() + " standards.";
					
					String recommendation = "";
					Set<Recommendation> recommendations = generateNonTextContrastRecommendations(element, 
																								 parent_bkg);
					
					ColorContrastIssueMessage accessible_contrast = new ColorContrastIssueMessage(
																				Priority.HIGH,
																				description,
																				highest_contrast,
																				element_bkg.rgb(),
																				parent_bkg.rgb(),
																				element,
																				AuditCategory.AESTHETICS,
																				labels,
																				ada_compliance,
																				title,
																				null, 
																				1, 
																				1, 
																				recommendations,
																				recommendation);
					
					issue_messages.add(accessible_contrast);
				}
			}
			catch(NullPointerException e) {
				log.warn("null pointer..." + e.getMessage());
				e.printStackTrace();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		} 

		String why_it_matters = "<p>Icons are an easily recognizable, fun element, and a great way to\n" + 
				"communicate with your user beyond just using text. Icons should be\n" + 
				"familiar and captivating.</p>" + 
				"<p>Bright colors have higher conversion rates, so it is important for your\n" + 
				"button to have a high contrast score to create an eye-catching effect\n" + 
				"and be obviously clickable.</p>";

		int points_earned = 0;
		int max_points = 0;
		for(UXIssueMessage issue_msg : issue_messages) {
			points_earned += issue_msg.getPoints();
			max_points += issue_msg.getMaxPoints();
			
			if(issue_msg.getScore() < 90 && issue_msg instanceof ElementStateIssueMessage) {
				log.warn("ux issue score :: "+issue_msg.getScore());
				ElementStateIssueMessage element_issue_msg = (ElementStateIssueMessage)issue_msg;
				log.warn("Retrieving example for LINKS");
				List<ElementState> good_examples = audit_service.findGoodExample(AuditName.NON_TEXT_BACKGROUND_CONTRAST, 100);
				if(good_examples.isEmpty()) {
					log.warn("Could not find element for good example...");
					continue;
				}
				Random random = new Random();
				ElementState good_example = good_examples.get(random.nextInt(good_examples.size()-1));
				log.warn("example that was retrieved :: "+good_example);
				log.warn("Setting good example on issue message :: "+good_example.getId());
				element_issue_msg.setGoodExample(good_example);
				log.warn("saving element state to issue message");
				issue_message_service.save(element_issue_msg);
			}
		}
		
		String description = "Color contrast of text";		
		return new Audit(AuditCategory.AESTHETICS,
						 AuditSubcategory.COLOR_MANAGEMENT,
						 AuditName.NON_TEXT_BACKGROUND_CONTRAST,
						 points_earned,
						 issue_messages,
						 AuditLevel.PAGE,
						 max_points,
						 page_state.getUrl(),
						 why_it_matters,
						 description,
						 true);
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
	
	/**
	 * Generates {@link Set} of {@link ColorContrastRecommendation recommendations} based on the text color, background color and font_size
	 * 	NOTE : assumes a light color scheme only. Doesn't currently account for dark color scheme
	 * 
	 * @param font_color
	 * @param background_color
	 * @param font_size
	 * @param is_bold TODO
	 * 
	 * @pre font_color != null
	 * @pre background_color != null
	 * 
	 * @return
	 */
	private Set<Recommendation> generateNonTextContrastRecommendations(ElementState element,
																	 ColorData background_color) {
		assert element != null;
		assert background_color != null;
		
		Set<Recommendation> recommendations = new HashSet<>();
		
		//generate color suggestions with different background color shades (text doesn't change)
		
		boolean is_dark_theme = false;
		//if text is lighter than background then it's dark theme
		//otherwise light theme
		ColorContrastRecommendation recommended_bg_color = ColorUtils.findCompliantNonTextBackgroundColor(new ColorData(element.getBackgroundColor()), 
																											background_color, 
																											is_dark_theme);
		recommendations.add( recommended_bg_color);
		
		
		//generate color suggestions with different text color shades (background doesn't change)
		Set<ColorContrastRecommendation> recommended_font_color = ColorUtils.findCompliantElementColors(element, 
																										background_color, 
																										is_dark_theme);
		recommendations.addAll( recommended_font_color);
		
		
		//generate color suggestions with varying text and background colors that are within a bounded range of the original color
		// NOTE: This involves pushing these values in opposing directions until we find a pair that meets WCAG 2.1 AAA standards. 
		//       Then, the pair of colors are shifted together to find new color pairs
		
		
		return recommendations;
	}
}
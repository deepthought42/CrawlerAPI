package com.qanairy.models.audit.domain;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.vision.CloudVisionTemplate;
import org.springframework.stereotype.Component;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.ColorInfo;
import com.google.cloud.vision.v1.DominantColorsAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;
import com.qanairy.models.Domain;
import com.qanairy.models.Element;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageVersion;
import com.qanairy.models.PageState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.ColorData;
import com.qanairy.models.audit.ColorPaletteObservation;
import com.qanairy.models.audit.ColorPaletteUtils;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.models.enums.ColorScheme;
import com.qanairy.services.DomainService;
import com.qanairy.services.PageVersionService;
import com.qanairy.services.PageStateService;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class DomainColorPaletteAudit implements IExecutableDomainAudit{
	private static Logger log = LoggerFactory.getLogger(DomainColorPaletteAudit.class);

	private List<String> gray_colors = new ArrayList<>();
	private List<String> colors = new ArrayList<>();
	
	@Autowired
	private PageVersionService page_service;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired 
	private CloudVisionTemplate cloudVisionTemplate;

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

		Map<String, Boolean> colors = new HashMap<String, Boolean>();

		//get all pages
		List<PageVersion> pages = domain_service.getPages(domain.getHost());
		Map<String, Double> color_map = new HashMap<>();
		
		//get most recent page state for each page
		for(PageVersion page : pages) {
			log.warn("color management page version ::  "+page.getKey());
			for(Element element : page_service.getElements(page.getKey())) {
					log.warn("element css :: "+element.getPreRenderCssValues());
			}
			//for each page state get elements
			PageState page_state = page_service.getMostRecentPageState(page.getKey());
			log.warn("color management page state :: "+page_state.getKey());
			List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
			
			//get image attributes from google cloud vision
			cloudVisionTemplate.analyzeImage(imageResource, featureTypes)
			
			//retrieve image colors based on screenshots minus the contents of image elements
			try {
				color_map.putAll(extractColorsFromPageState(new URL(page_state.getFullPageScreenshotUrl()), elements));
				log.warn("color_map ::   "+color_map);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			/*
			log.warn("elements identified ::: " + elements.size());
			for(ElementState element : elements) {
				//identify all colors used on page. Images are not considered
				//check element for color css property
				colors.put(element.getRenderedCssValues().get("color"), Boolean.TRUE);
				//check element for text-decoration-color css property
				colors.put(element.getRenderedCssValues().get("text-decoration-color"), Boolean.TRUE);
				//check element for text-emphasis-color
				colors.put(element.getRenderedCssValues().get("text-emphasis-color"), Boolean.TRUE);
	
				//check element for background-color css property
				colors.put(element.getRenderedCssValues().get("background-color"), Boolean.TRUE);
				//check element for caret-color
				colors.put(element.getRenderedCssValues().get("caret-color"), Boolean.TRUE);
				//check element for outline-color css property NB: SPECIFICALLY FOR BOXES
				colors.put(element.getRenderedCssValues().get("outline-color"), Boolean.TRUE);
				//check element for border-color, border-left-color, border-right-color, border-top-color, and border-bottom-color css properties NB: SPecifically for borders
				colors.put(element.getRenderedCssValues().get("border-color"), Boolean.TRUE);
				colors.put(element.getRenderedCssValues().get("border-left-color"), Boolean.TRUE);
				colors.put(element.getRenderedCssValues().get("border-right-color"), Boolean.TRUE);
				colors.put(element.getRenderedCssValues().get("border-top-color"), Boolean.TRUE);
				colors.put(element.getRenderedCssValues().get("border-bottom-color"), Boolean.TRUE);
			}
			colors.remove("null");
			colors.remove(null);
			*/
		}
		
		log.warn("colors :: "+colors.size());
		Map<String, Double> filtered_color_map = new HashMap<>();
		for(String color_key : color_map.keySet()) {
			if(color_map.get(color_key) > 0.01 ) {
				filtered_color_map.put(color_key, color_map.get(color_key));
				log.warn("color    :: "+color_key + "   :    "+color_map.get(color_key));
			}
		}
		
		Map<String, Boolean> gray_colors = new HashMap<String, Boolean>();
		Map<String, Boolean> filtered_colors = new HashMap<>();
		//discard any colors that are transparent
		for(String color_str : filtered_color_map.keySet()) {
			color_str = color_str.trim();
			//color_str = color_str.replace("transparent", "");
			//color_str = color_str.replace("!important", "");
			//if(color_str == null || color_str.isEmpty() || color_str.equalsIgnoreCase("transparent")) {
			//	continue;
			//}

			//extract r,g,b,a from color_str
			ColorData color = new ColorData(color_str.trim());
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
		/*
		log.warn("colors :: "+colors.size());
		for(String color_key : colors.keySet()) {
			log.warn("color    :: "+color_key);
		}
		
		
		Map<String, Boolean> gray_colors = new HashMap<String, Boolean>();
		Map<String, Boolean> filtered_colors = new HashMap<>();
		//discard any colors that are transparent
		for(String color_str : colors.keySet()) {
			color_str = color_str.trim();
			color_str = color_str.replace("transparent", "");
			color_str = color_str.replace("!important", "");
			if(color_str == null || color_str.isEmpty() || color_str.equalsIgnoreCase("transparent")) {
				continue;
			}

			//extract r,g,b,a from color_str
			ColorData color = new ColorData(color_str.trim());
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
		*/
		
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
		Map<String, Set<String>> palette_stringified = convertPaletteToStringRepresentation(palette);
		
		ColorPaletteObservation observation = new ColorPaletteObservation(palette_stringified, new ArrayList<>(filtered_colors.keySet()), new ArrayList<>(gray_colors.keySet()), color_scheme, "This is a color scheme description");
		observations.add(observation);
			
		ColorScheme scheme = ColorPaletteUtils.getColorScheme(palette);
		int score = ColorPaletteUtils.getPaletteScore(palette, scheme);
		
		//score colors found against scheme
		setGrayColors(new ArrayList<>(gray_colors.keySet()));
		setColors(new ArrayList<>(colors.keySet()));
		 
		
		return new Audit(AuditCategory.COLOR_MANAGEMENT, AuditSubcategory.COLOR_PALETTE, score, observations, AuditLevel.DOMAIN, 3);
	}

	
	/**
	 * 
	 * @param screenshot_url
	 * @param elements
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private Map<String, Double> extractColorsFromPageState(URL screenshot_url,
			List<ElementState> elements) throws MalformedURLException, IOException {
		Map<String, Integer> color_map = new HashMap<>();
		
		log.warn("Loading image from url ::  "+screenshot_url);
		//copy page state full page screenshot
		BufferedImage screenshot = ImageIO.read(screenshot_url);
		
		for(ElementState element : elements) {
			if(!element.getName().contentEquals("img")) {
				continue;
			}
			
			for(int x_pixel = element.getXLocation(); x_pixel < (element.getXLocation()+element.getWidth()); x_pixel++) {
				for(int y_pixel = element.getYLocation(); y_pixel < (element.getYLocation()+element.getHeight()); y_pixel++) {
					screenshot.setRGB(x_pixel, y_pixel, new Color(0,0,0).getRGB());
				}	
			}
		}
		
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
		
		Map<String, Double> color_percentages = new HashMap<String, Double>();
		for(String color_key : color_map.keySet()) {
			Double percentage = color_map.get(color_key)/(double)total_pixels;
			color_percentages.put(color_key, percentage);
		}
		
		return color_percentages;
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
	
	private Map<String, Set<String>> convertPaletteToStringRepresentation(Map<ColorData, Set<ColorData>> palette) {
		Map<String, Set<String>> stringified_map = new HashMap<>();
		for(ColorData primary : palette.keySet()) {
			Set<String> secondary_colors = new HashSet<>();
			for(ColorData secondary : palette.get(primary)) {
				if(secondary == null) {
					continue;
				}
				secondary_colors.add(secondary.rgb());
			}
			stringified_map.put(primary.rgb(), secondary_colors);
		}
		return null;
	}
	
	/**
	 * Detects image properties such as color frequency from the specified local image.
	 * 
	 * @param image_url
	 * @throws IOException
	 */
	public static void detectProperties(String image_url) throws IOException {
	    List<AnnotateImageRequest> requests = new ArrayList<>();
	    InputStream url_input_stream = new URL(image_url).openStream();
	    
	    ByteString imgBytes = ByteString.readFrom(url_input_stream);
	
	    Image img = Image.newBuilder().setContent(imgBytes).build();
	    Feature feat = Feature.newBuilder().setType(Feature.Type.IMAGE_PROPERTIES).build();
	    AnnotateImageRequest request =
	        AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
	    requests.add(request);
	
	    // Initialize client that will be used to send requests. This client only needs to be created
	    // once, and can be reused for multiple requests. After completing all of your requests, call
	    // the "close" method on the client to safely clean up any remaining background resources.
	    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
	    	BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
	    	List<AnnotateImageResponse> responses = response.getResponsesList();
	
	      	for (AnnotateImageResponse res : responses) {
		        if (res.hasError()) {
		          System.out.format("Error: %s%n", res.getError().getMessage());
		          return;
		        }
		
		        // For full list of available annotations, see http://g.co/cloud/vision/docs
		        DominantColorsAnnotation colors = res.getImagePropertiesAnnotation().getDominantColors();
		        for (ColorInfo color : colors.getColorsList()) {
		          System.out.format(
		              "fraction: %f%nr: %f, g: %f, b: %f%n",
		              color.getPixelFraction(),
		              color.getColor().getRed(),
		              color.getColor().getGreen(),
		              color.getColor().getBlue());
		        }
	      	}
	    }
	}
}



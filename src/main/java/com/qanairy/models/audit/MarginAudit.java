package com.qanairy.models.audit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.Element;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.audit.Score;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditName;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.models.enums.Priority;
import com.qanairy.services.PageStateService;

/**
 * Responsible for executing an audit on the margins used within a page state as part of  the 
 * 	information architecture audit category
 */
@Component
public class MarginAudit implements IExecutablePageStateAudit {
	private static Logger log = LoggerFactory.getLogger(MarginAudit.class);

	private String[] size_units = {"px", "pt", "%", "em", "rem", "ex", "vh", "vw", "vmax", "vmin", "mm", "cm", "in", "pc"};
	
	@Autowired
	private PageStateService page_state_service;

	
	public MarginAudit() {	}
	
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

		List<Observation> observations = new ArrayList<>();
		Map<ElementState, List<String>> elements_margin_map = new HashMap<>(); 

		//get all pages
		//get most recent page state for each page
		
		List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
		log.warn("page state elements for domain audit :: "+elements.size());
		for(ElementState element : elements) {
			String margin_value = "";
			List<String> margins = new ArrayList<>();

			if( element.getRenderedCssValues().containsKey("margin-top")) {
				margin_value = element.getRenderedCssValues().get("margin-top").trim();
				if(!margin_value.contentEquals("0px")) {
					margins.add(margin_value);
				}
			}

			if( element.getRenderedCssValues().containsKey("margin-bottom")) {
				margin_value = element.getRenderedCssValues().get("margin-bottom").trim();
				if(!margin_value.contentEquals("0px")) {
					margins.add(margin_value);
				}			}
			
			if( element.getRenderedCssValues().containsKey("margin-right")) {
				margin_value = element.getRenderedCssValues().get("margin-right").trim();
				if(!margin_value.contentEquals("0px")) {
					margins.add(margin_value);
				}			}
			
			if( element.getRenderedCssValues().containsKey("margin-left")) {
				margin_value = element.getRenderedCssValues().get("margin-left").trim();
				if(!margin_value.contentEquals("0px")) {
					margins.add(margin_value);
				}			}
			
			elements_margin_map.put(element, margins);
		}

		

			
		log.warn("Element margin map size :: "+elements_margin_map.size());
		// Score spacing_score = evaluateSpacingConsistency(elements_margin_map);     //commented out because this is old greatest common divisor methodology
		Score spacing_score = evaluateSpacingMultipleOf8(elements_margin_map);
		//Score unit_score = evaluateUnits(elements_margin_map);

		Score margin_as_padding_score = scoreMarginAsPadding(elements_margin_map.keySet());
		
		observations.addAll(spacing_score.getObservations());
		//observations.addAll(unit_score.getObservations());
		observations.addAll(margin_as_padding_score.getObservations());
		
		log.warn("spacing score : "+spacing_score.getPointsAchieved() + " / " +spacing_score.getMaxPossiblePoints());
		//log.warn("unit score : "+spacing_score.getPointsAchieved() + " / " +spacing_score.getMaxPossiblePoints());
		log.warn("margin as padding score : "+margin_as_padding_score.getPointsAchieved() + " / " +margin_as_padding_score.getMaxPossiblePoints());

		int points = spacing_score.getPointsAchieved() + margin_as_padding_score.getPointsAchieved();
		int max_points = spacing_score.getMaxPossiblePoints() + margin_as_padding_score.getMaxPossiblePoints();
		//calculate score for question "Is margin used as margin?" NOTE: The expected calculation expects that margins are not used as margin
		//log.warn("MARGIN SCORE  :::   " + points + " / 100" );	

		return new Audit(AuditCategory.AESTHETICS,
						 AuditSubcategory.WHITESPACE,
						 AuditName.MARGIN,
						 points,
						 observations,
						 AuditLevel.PAGE,
						 max_points,
						 page_state.getUrl());
	}

	/**
	 * Generates {@link Score score} for spacing consistency across elements
	 * 
	 * @param elements_margin_map
	 * 
	 * @return {@link Score score}
	 * 
	 * @pre elements_margin_map != null
	 */
	@Deprecated
	private Score evaluateSpacingConsistency(Map<ElementState, List<String>> elements_margin_map) {
		assert elements_margin_map != null;
		
		int points_earned = 0;
		int max_points = 0;
		Set<Observation> observations = new HashSet<>();
		
		Map<String, List<Double>> gcd_map = new HashMap<>();
		Map<String, List<Double>> units = new HashMap<>();
		for(ElementState element : elements_margin_map.keySet()) {
			//START UNIT SCORE HERE
			units.putAll(sortSizeUnits(elements_margin_map.get(element)));
		}
		
		//extract multiples for margins
		//most common multiples, the highest multiples that can be found to satisfy the list of unique margin values
		for(String unit : units.keySet()) {
			//scale units values by 100 and make unique
			List<Double> distinct_list =  sortAndMakeDistinct(units.get(unit));
			
			if(distinct_list.size() == 1) {
				gcd_map.put(unit, distinct_list);
				continue;
			}
			
			List<Double> gcd_list = new ArrayList<>();
			for(int idx = 0; idx < distinct_list.size()-1; idx++) {
				for(int idx2 = idx+1; idx2 < distinct_list.size(); idx2++) {
					gcd_list.add(findGCD(distinct_list.get(idx), distinct_list.get(idx2)));
				}
			}
			
			gcd_list.remove(new Double(1));
			//reduce gcd again.
			gcd_map.put(unit, gcd_list);
		}			
		
		//reduce gcd_list until no value is divisible by any other
		//rank gcd list based on frequency values that are multiples of gcd
		//generate score for each element margin based on gcd divisibility
		
		//COMPUTE SCORE FOR MARGIN BASED ON GCD VALUES
		Map<String, List<Double>> unit_gcd_lists = new HashMap<>();
		for(String unit : gcd_map.keySet()) {
			List<Double> most_common_gcd_values = new ArrayList<>();
			if(gcd_map.get(unit).size() == 1) {
				log.warn("unit : "+unit+"  has only 1 gcd!!");
				points_earned += 3;
				most_common_gcd_values.addAll(gcd_map.get(unit));
			}
			else {
				List<Double> margin_list = units.get(unit);
				List<Double> gcd_values = gcd_map.get(unit);
				do {
					Map<Double, List<Double>> gcd_match_lists = new HashMap<>();
					
					//find highest gcd values that define the set
					for(double gcd : gcd_values) {
						gcd_match_lists.put(gcd, new ArrayList<Double>());
						for(double value : margin_list) {
							if(value % gcd == 0 && gcd != 1){
								gcd_match_lists.get(gcd).add(value);
							}
						}
					}
					
					//identify gcd with most matches
					int largest_gcd_count = 0;
					double largest_gcd = 0;
					for(Double gcd : gcd_match_lists.keySet()) {
						if(gcd_match_lists.get(gcd).size() >= largest_gcd_count ) {
							largest_gcd_count = gcd_match_lists.get(gcd).size();
							
							if(gcd > largest_gcd) {
								largest_gcd = gcd;
							}
						}
					}
					
					//remove gcd value from input gcd list
					gcd_values.remove(largest_gcd);
					
					if(largest_gcd_count > 0) {						
						//add the largest gcd to the list of most applicable gcd values
						most_common_gcd_values.add(largest_gcd);
					}
					
					//remove gcd matches from vertical margin list
					List<Double> largest_gcd_matches = gcd_match_lists.get(largest_gcd);
					if(largest_gcd_matches != null) {
						margin_list.removeAll(largest_gcd_matches);
					}
					
				}while(!margin_list.isEmpty() && !gcd_values.isEmpty());
				
				
				if(most_common_gcd_values.size() == 2) {
					points_earned += 2;
				}
				else {
					points_earned += 1;
				}
			}
			unit_gcd_lists.put(unit, most_common_gcd_values);
			max_points += 3;
		}
		
		return new Score(points_earned, max_points, observations);
	}

	/**
	 * Generates {@link Score score} for spacing consistency across elements
	 * 
	 * @param elements_margin_map
	 * 
	 * @return {@link Score score}
	 * 
	 * @pre elements_margin_map != null
	 */
	private Score evaluateSpacingMultipleOf8(Map<ElementState, List<String>> elements_margins) {
		assert elements_margins != null;
		
		int points_earned = 0;
		int max_points = 0;
		Set<Observation> observations = new HashSet<>();
		List<ElementState> elements = new ArrayList<ElementState>();
		
		for(ElementState element : elements_margins.keySet()) {
			for(String size_str : elements_margins.get(element)) {
				if(isMultipleOf8(size_str)) {
					points_earned += 1;
					//elements.add(element);
				}
				//else create observation that element is unlikely to scale gracefully
				else {
					elements.add(element);
				}
				max_points++;
			}
		}
		

		String why_it_matters = "Keeping your use of margins to a miminum, and when you use them making sure"
				+ " the margin values are a multiple of 8 dpi ensures your site is more responsive. Not all users"
				+ " have screens that are the same size as those used by the design team, but all monitor sizes"
				+ " are multiple of 8.";
		
		String ada_compliance = "There are no ADA requirements for use of margins";
		Set<String> recommendations = new HashSet<>();
		recommendations.add("For a responsive design we recommend using margin values that are a multiple of 8.");
		
		Set<String> labels = new HashSet<>();
		labels.add("whitespace");
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.AESTHETICS.name());
		
		observations.add(new ElementStateObservation(
								elements, 
								"Has at least one margin value that isn't a multiple of 8.", 
								why_it_matters, 
								ada_compliance, 
								Priority.LOW, 
								recommendations,
								labels,
								categories));
		//observations.add(new ElementStateObservation(elements, "Margin values are multiple of 8"));
		
		return new Score(points_earned, max_points, observations);
	}
	
	/**
	 * Generates {@link Score score} for spacing consistency across elements
	 * 
	 * @param elements_margin_map
	 * 
	 * @return {@link Score score}
	 * 
	 * @pre elements_margin_map != null
	 */
	private Score evaluateSpacingAppliedEvenly(Map<ElementState, List<String>> elements_margins) {
		assert elements_margins != null;
		
		int points_earned = 0;
		int max_points = 0;
		Set<Observation> observations = new HashSet<>();
		List<ElementState> elements = new ArrayList<ElementState>();
		
		for(ElementState element : elements_margins.keySet()) {
			for(String size_str : elements_margins.get(element)) {
				if(isMultipleOf8(size_str)) {
					points_earned += 1;
					//elements.add(element);
				}
				//else create observation that element is unlikely to scale gracefully
				else {
					elements.add(element);
				}
				max_points++;
			}
		}
		

		String why_it_matters = "Keeping your use of margins to a miminum, and when you use them making sure"
				+ " the margin values are a multiple of 8 dpi ensures your site is more responsive. Not all users"
				+ " have screens that are the same size as those used by the design team, but all monitor sizes"
				+ " are multiple of 8.";
		
		String ada_compliance = "There are no ADA requirements for use of margins";
		Set<String> recommendations = new HashSet<>();
		recommendations.add("For a responsive design we recommend using margin values that are a multiple of 8.");
		
		Set<String> labels = new HashSet<>();
		labels.add("whitespace");
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.AESTHETICS.name());
		
		observations.add(new ElementStateObservation(
								elements, 
								"Has at least one margin value that isn't a multiple of 8.", 
								why_it_matters, 
								ada_compliance, 
								Priority.LOW, 
								recommendations,
								labels,
								categories));
		//observations.add(new ElementStateObservation(elements, "Margin values are multiple of 8"));
		
		return new Score(points_earned, max_points, observations);
	}
	
	public static boolean isMultipleOf8(String size_str) {
		double size = Double.parseDouble(cleanSizeUnits(size_str));
		if(size == 0.0) {
			return true;
		}
		//check if size is a multiple of 8
		int remainder = 0;
		if(size > 8) {
			remainder = (int)size % 8;
		}
		else {
			remainder = 8 % (int)size;
		}
		//if multiple of 8 then note as well done
		if(remainder <=1 ) {
			return true;
		}
		
		return false;
	}

	/**
	 * Generates {@link Score score} based on which units (ie, %, em, rem, px, pt, etc.) are used for vertical(top,bottom) padding
	 * 
	 * @param vertical_margin_values
	 * 
	 * @return
	 */
	private Score evaluateUnits(Map<ElementState, List<String>> element_margin_map) {
		assert element_margin_map != null;
		
		int vertical_score = 0;
		int max_vertical_score = 0;
		Set<Observation> observations = new HashSet<>();
		List<ElementState> unscalable_margin_elements = new ArrayList<>();

		for(ElementState element : element_margin_map.keySet()) {
			for(String margin_value : element_margin_map.get(element)) {
				//determine unit measure
				String unit = extractMeasureUnit(margin_value);
				
				vertical_score += scoreMeasureUnit(unit);
				max_vertical_score += 3;
				
				if(vertical_score == 1) {
					unscalable_margin_elements.add(element);
				}
			}
		}
		
		if(!unscalable_margin_elements.isEmpty()) {
			Set<String> labels = new HashSet<>();
			labels.add("responsiveness");
			labels.add("whitespace");
			
			Set<String> categories = new HashSet<>();
			categories.add(AuditCategory.AESTHETICS.name());
			
			observations.add(new ElementStateObservation(
					unscalable_margin_elements, 
					"Elements with unscalable margin units", 
					"", 
					"", 
					Priority.LOW, 
					new HashSet<>(),
					labels,
					categories));
		}
		return new Score(vertical_score, max_vertical_score, observations);
	}


	private String extractMeasureUnit(String padding_value) {
		if(padding_value.contains("rem")) {
			return "rem";
		}
		else if( padding_value.contains("em")) {
			return "em";
		}
		else if( padding_value.contains("%") ){
			return "%";
		}
		else if(padding_value.contains("vh")) {
			return "vh";
		}
		else if(padding_value.contains("vw") ) {
			return "vw";
		}
		else if(padding_value.contains("vmin")) {
			return "vmin";
		}
		else if(padding_value.contains("vmax")) {
			return "vmax";
		}
		else if(padding_value.contains("px")) {
			return "px";
		}
		else if(padding_value.contains("ex") ) {
			return "ex";
		}
		else if(padding_value.contains("pt")) {
			return "pt";
		}
		else if(padding_value.contains("cm")) {
			return "cm";
		}
		else if(padding_value.contains("mm")) {
			return "mm";
		}
		else if(padding_value.contains("in")) {
			return "in";
		}
		else if(padding_value.contains("pc")) {
			return "pc";
		}
		
		return "";
	}
	
	
	/**
	 * Identifies elements that are using margin when they should be using margin
	 * 
	 * @param elements
	 * 
	 * @return 
	 * 
	 * @pre elements != null
	 */
	private Score scoreMarginAsPadding(Set<ElementState> elements) {
		assert elements != null;
		
		int score = 0;
		int max_score = 0;
		Set<Observation> observations = new HashSet<>();
		List<ElementState> flagged_elements = new ArrayList<>();
		for(ElementState element : elements) {
			if(element == null) {
				log.warn("margin padding audit Element :: "+element);
				continue;
			}

			//identify elements that own text and have margin but not padding set
			if(element.getOwnedText() != null && !element.getOwnedText().trim().isEmpty()) {
				//check if element has margin but not padding set for any direction(top, bottom, left, right)
				boolean margin_used_as_padding = false;
				String margin_top = element.getRenderedCssValues().get("margin-top");
				if(!isSpacingValueZero(margin_top) && isSpacingValueZero(element.getRenderedCssValues().get("padding-top"))) {
					log.warn("margin top : "+margin_top+";      padding-top  :  "+element.getRenderedCssValues().get("padding-top"));
					margin_used_as_padding = true;
				}
				else if(!isSpacingValueZero(element.getRenderedCssValues().get("margin-right")) && isSpacingValueZero(element.getRenderedCssValues().get("padding-right"))) {
					log.warn("margin right : "+element.getRenderedCssValues().get("margin-right")+";      padding-right  :  "+element.getRenderedCssValues().get("padding-right"));

					margin_used_as_padding = true;
				}
				else if(!isSpacingValueZero(element.getRenderedCssValues().get("margin-bottom")) && isSpacingValueZero(element.getRenderedCssValues().get("padding-bottom"))) {
					log.warn("margin bottom : "+element.getRenderedCssValues().get("margin-bottom")+";      padding-bottom  :  "+element.getRenderedCssValues().get("padding-bottom"));

					margin_used_as_padding = true;
				}
				else if(!isSpacingValueZero(element.getRenderedCssValues().get("margin-left")) && isSpacingValueZero(element.getRenderedCssValues().get("padding-left"))) {
					log.warn("margin left : "+element.getRenderedCssValues().get("margin-left")+";      padding-left  :  "+element.getRenderedCssValues().get("padding-left"));

					margin_used_as_padding = true;
				}
				else {
					score += 3;
				}
				
				if(margin_used_as_padding) {
					score += 1;
					flagged_elements.add(element);
				}
				max_score += 3;
			}
		}
		if(!flagged_elements.isEmpty()) {

			String why_it_matters = "Keeping your use of margins to a miminum, and when you use them making sure"
					+ " the margin values are a multiple of 8 dpi ensures your site is more responsive. Not all users"
					+ " have screens that are the same size as those used by the design team, but all monitor sizes"
					+ " are multiple of 8.";
			
			String ada_compliance = "There are no ADA requirements for use of margins";
			
			Set<String> labels = new HashSet<>();
			labels.add("whitespace");
			
			Set<String> categories = new HashSet<>();
			categories.add(AuditCategory.AESTHETICS.getShortName());
			
			observations.add(new ElementStateObservation(
									flagged_elements, 
									"Elements that appear to use margin as padding", 
									why_it_matters, 
									ada_compliance,
									Priority.LOW, 
									new HashSet<>(),
									labels,
									categories));
		}
		return new Score(score, max_score, observations);
	}
	
	private boolean isSpacingValueZero(String spacing) {
		spacing = cleanSizeUnits(spacing);
		return spacing == null || ( !spacing.isEmpty() || !spacing.equals("0") || !spacing.equals("auto"));
	}

	/**
	 * TODO
	 * 
	 * @param elements
	 * @return
	 */
	private int scoreNonCollapsingMargins(List<Element> elements) {
		for(Element element : elements) {
			//identify situations of margin collapse by finding elements that are 
				//positioned vertically where 1 is above the other and both have margins
				//element is empty and has margins set
				//first and last child margin collapse when their parent has margin set
			
			
		}
		
		return 0;
		
	}

	public static String URLReader(URL url) throws IOException {
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        
        log.warn("Content encoding for URL connection ::  " + con.getContentEncoding());
        if(con.getContentEncoding() != null && con.getContentEncoding().equalsIgnoreCase("gzip")) {
        	return readGzipStream(con.getInputStream());
        }
        else {
        	return readStream(con.getInputStream());
        }
	}
	
	private static String readGzipStream(InputStream inputStream) {
		 StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream( inputStream )));) {
            String nextLine = "";
            while ((nextLine = reader.readLine()) != null) {
                sb.append(nextLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
	}

	private static String readStream(InputStream in) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in));) {
            String nextLine = "";
            while ((nextLine = reader.readLine()) != null) {
                sb.append(nextLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


	/**
	 * Sort units into buckets by mapping unit type to margin sizes
	 * 
	 * @param margin_set
	 * @return
	 */
	private Map<String, List<Double>> sortSizeUnits(List<String> margin_set) {
		Map<String, List<Double>> sorted_margins = new HashMap<>();
		//replace all px values with em for values that contain decimals
		
		for(String margin_value : margin_set) {
			if(margin_value == null 
					|| "0".equals(margin_value.trim()) 
					|| margin_value.contains("auto")) {
				continue;
			}
			
			for(String unit : size_units) {
				if(margin_value != null && margin_value.contains(unit)) {
					List<Double> values = new ArrayList<Double>();

					if(sorted_margins.containsKey(unit)) {
						values = sorted_margins.get(unit);
					}
					
					String value = cleanSizeUnits(margin_value);
					
					//values = cleanSizeUnits(values);
					//List<Double> converted_values = convertList(values, s -> Double.parseDouble(s));
					values.add(Double.parseDouble(value));
					sorted_margins.put(unit, values);
				}
			}
		}
		return sorted_margins;
	}
	
	public static List<Double> sortAndMakeDistinct(List<Double> from){
		return from.stream().filter(n -> n != 0.0).distinct().sorted().collect(Collectors.toList());
	}
	
	public static List<String> cleanSizeUnits(List<String> from){
		return from.stream()
				.map(line -> line.replaceAll("px", ""))
				.map(line -> line.replaceAll("%", ""))
				.map(line -> line.replaceAll("em", ""))
				.map(line -> line.replaceAll("rem", ""))
				.map(line -> line.replaceAll("pt", ""))
				.map(line -> line.replaceAll("ex", ""))
				.map(line -> line.replaceAll("vm", ""))
				.map(line -> line.replaceAll("vh", ""))
				.map(line -> line.replaceAll("cm", ""))
				.map(line -> line.replaceAll("mm", ""))
				.map(line -> line.replaceAll("in", ""))
				.map(line -> line.replaceAll("pc", ""))
				.map(line -> line.indexOf(".") > -1 ? line.substring(0, line.indexOf(".")) : line)
				.collect(Collectors.toList());
	}
	
	public static String cleanSizeUnits(String value){
		return value.replaceAll("px", "")
					.replaceAll("%", "")
					.replaceAll("em", "")
					.replaceAll("rem", "")
					.replaceAll("pt", "")
					.replaceAll("ex", "")
					.replaceAll("vm", "")
					.replaceAll("vh", "")
					.replaceAll("cm", "")
					.replaceAll("mm", "")
					.replaceAll("in", "")
					.replaceAll("pc", "")
					.replaceAll("auto", "")
					.replaceAll("!important", "")
					.trim();
	}
	
	public static List<Integer> removeZeroValues(List<Integer> from){
		return from.stream().filter(n -> n != 0).collect(Collectors.toList());
	}
	
	//for lists
	public static <T, U> List<U> convertList(List<T> from, Function<T, U> func) {
	    return from.stream().map(func).collect(Collectors.toList());
	}
	
	/* * Java method to find GCD of two number using Euclid's method * @return GDC of two numbers in Java */ 
	private static double findGCD(double number1, double number2) { 
		//base case 
		if(number2 == 0){ 
			return number1; 
		} 
		return findGCD(number2, number1%number2);
	}
	
	private int scoreMeasureUnit(String unit) {
		if(unit.contains("rem") || unit.contains("em") || unit.contains("%") ){
			return 2;
		}
		else if(unit.contains("vh") || unit.contains("vw") || unit.contains("vmin") || unit.contains("vmax")) {
			return 1;
		}
		else if(unit.contains("px") || unit.contains("ex") || unit.contains("pt") || unit.contains("cm") || unit.contains("mm") || unit.contains("in") || unit.contains("pc")) {
			return 0;
		}
		
		return 2;
	}
	
	public static List<Double> deflateGCD(List<Double> gcd_list){
		return gcd_list.stream().map(s -> s/100.0).distinct().sorted().collect(Collectors.toList());
	}
}
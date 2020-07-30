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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;

/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class MarginAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(MarginAudit.class);

	private String[] size_units = {"px", "pt", "%", "em", "rem", "ex", "vh", "vw", "vmax", "vmin", "mm", "cm", "in", "pc"};
	
	
	public MarginAudit() {	}
	
	private static String getAuditDescription() {
		return "";
	}

	private static List<String> buildBestPractices() {
		List<String> best_practices = new ArrayList<>();
		best_practices.add("");
		
		return best_practices;
	}
	
	private static String getAdaDescription() {
		return "None";
	}
	
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
		
		List<String> vertical_margin_values = new ArrayList<>();
		List<String> horizontal_margin_values = new ArrayList<>();

		List<Observation> observations = new ArrayList<>();


		//extract vertical and horizontal margin values
		for(ElementState element : page_state.getElements()) {
			String margin_value = "";
			if(element.getPreRenderCssValues().containsKey("margin")) {
				margin_value = element.getPreRenderCssValues().get("margin");
				String[] separated_values = margin_value.split(" ");
				
				if(separated_values.length == 1) {
					vertical_margin_values.add(separated_values[0]);
					horizontal_margin_values.add(separated_values[0]);
				}
				else {
					for(int idx = 0; idx < separated_values.length; idx++) {
						if(idx % 2 == 0) {
							vertical_margin_values.add(separated_values[idx]);
						}
						else {
							horizontal_margin_values.add(separated_values[idx]);
						}
					}
				}
			}
			else if( element.getPreRenderCssValues().containsKey("margin-top")) {
				margin_value = element.getPreRenderCssValues().get("margin-top");
				String[] separated_values = margin_value.split(" ");
				//extract vertical and horizontal
				for(String value : separated_values) {											
					vertical_margin_values.add( value);
				}
			}
			else if( element.getPreRenderCssValues().containsKey("margin-bottom")) {
				margin_value = element.getPreRenderCssValues().get("margin-bottom");
				String[] separated_values = margin_value.split(" ");
				//extract vertical and horizontal
				for(String value : separated_values) {											
					vertical_margin_values.add( value);
				}
			}
			else if( element.getPreRenderCssValues().containsKey("margin-right")) {
				margin_value = element.getPreRenderCssValues().get("margin-right");
				String[] separated_values = margin_value.split(" ");
				//extract vertical and horizontal
				for(String value : separated_values) {											
					horizontal_margin_values.add( value);
				}
			}
			else if( element.getPreRenderCssValues().containsKey("margin-left")) {
				margin_value = element.getPreRenderCssValues().get("margin-left");
				String[] separated_values = margin_value.split(" ");
				//extract vertical and horizontal
				for(String value : separated_values) {											
					horizontal_margin_values.add( value);
				}
			}	
		}
		
		
		
		Map<String, List<Double>> vertical_units = sortSizeUnits(vertical_margin_values);
		Map<String, List<Double>> vertical_gcd_list = new HashMap<>();
		//extract multiples for vertical margins
		//most common multiples, the highest multiples that can be found to satisfy the list of unique margin values
		for(String unit : vertical_units.keySet()) {
			//scale units values by 100 and make unique
			List<Double> distinct_list =  sortAndMakeDistinct(vertical_units.get(unit));

			if(distinct_list.size() == 1) {
				vertical_gcd_list.put(unit, deflateGCD(distinct_list));
				continue;
			}
			
			List<Double> gcd_list = new ArrayList<>();
			for(int idx = 0; idx < distinct_list.size()-1; idx++) {
				for(int idx2 = idx+1; idx2 < distinct_list.size(); idx2++) {
					gcd_list.add(findGCD(distinct_list.get(idx), distinct_list.get(idx2)));
				}
			}
			
			gcd_list = deflateGCD(gcd_list);
			gcd_list.remove(new Double(1));
			vertical_gcd_list.put(unit, gcd_list);
		}
		
		
		//COMPUTE SCORE FOR VERTICAL PADDING BASED ON GCD VALUES
		int total_vertical_score = 0;
		int vertical_score = 0;
		
		Map<String, List<Double>> unit_gcd_lists = new HashMap<>();
		for(String unit : vertical_gcd_list.keySet()) {
			List<Double> most_common_gcd_values = new ArrayList<>();
			if(vertical_gcd_list.get(unit).size() == 1) {
				log.warn("unit : "+unit+"  has only 1 gcd!!");
				vertical_score += 3;
				most_common_gcd_values.addAll(vertical_gcd_list.get(unit));
			}
			else {
				List<Double> vertical_margin_list = vertical_units.get(unit);
				List<Double> vertical_gcd_values = vertical_gcd_list.get(unit);
				do {
					Map<Double, List<Double>> gcd_match_lists = new HashMap<>();

					//find highest gcd values that define the set
					for(double gcd : vertical_gcd_values) {
						gcd_match_lists.put(gcd, new ArrayList<Double>());
						for(double value : vertical_margin_list) {
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
					vertical_gcd_values.remove(largest_gcd);
					
					if(largest_gcd_count > 0) {						
						//add the largest gcd to the list of most applicable gcd values
						most_common_gcd_values.add(largest_gcd);
					}
					
					//remove gcd matches from vertical margin list
					vertical_margin_list.removeAll(gcd_match_lists.get(largest_gcd));
					
				}while(!vertical_margin_list.isEmpty() && !vertical_gcd_values.isEmpty());

				
				if(most_common_gcd_values.size() == 2) {
					vertical_score += 2;
				}
				else {
					vertical_score += 1;
				}
				
			}
			unit_gcd_lists.put(unit, most_common_gcd_values);
			total_vertical_score += 3;
		}
		
		
			
		
		
		
		Map<String, List<Double>> horizontal_unit_gcd_lists = new HashMap<>();
		Map<String, List<Double>> horizontal_units = sortSizeUnits(horizontal_margin_values);
		Map<String, List<Double>> horizontal_gcd_list = new HashMap<>();
		//extract multiples for horizontal margins
		//most common multiples, the highest multiples that can be found to satisfy the list of unique margin values
		for(String unit : horizontal_units.keySet()) {
			//scale units values by 100 and make unique
			List<Double> distinct_list =  sortAndMakeDistinct(horizontal_units.get(unit));

			if(distinct_list.size() == 1) {
				horizontal_gcd_list.put(unit, deflateGCD(distinct_list));
				continue;
			}
			
			List<Double> gcd_list = new ArrayList<>();
			for(int idx = 0; idx < distinct_list.size()-1; idx++) {
				for(int idx2 = idx+1; idx2 < distinct_list.size(); idx2++) {
					gcd_list.add(findGCD(distinct_list.get(idx), distinct_list.get(idx2)));
				}
			}
			
			gcd_list = deflateGCD(gcd_list);
			gcd_list.remove(new Double(1));
			horizontal_gcd_list.put(unit, gcd_list);
		}
		
		
		//COMPUTE SCORE FOR VERTICAL PADDING BASED ON GCD VALUES
		int total_horizontal_score = 0;
		int horizontal_score = 0;

		for(String unit : horizontal_gcd_list.keySet()) {
			List<Double> most_common_gcd_values = new ArrayList<>();
			if(horizontal_gcd_list.get(unit).size() == 1) {
				log.warn("unit : "+unit+"  has only 1 gcd!!");
				horizontal_score += 3;
				most_common_gcd_values.addAll(horizontal_gcd_list.get(unit));
			}
			else {
				List<Double> horizontal_margin_list = horizontal_units.get(unit);
				List<Double> horizontal_gcd_values = horizontal_gcd_list.get(unit);
				do {
					Map<Double, List<Double>> gcd_match_lists = new HashMap<>();
					//find highest gcd values that define the set
					for(double gcd : horizontal_gcd_values) {
						gcd_match_lists.put(gcd, new ArrayList<Double>());
						for(double value : horizontal_margin_list) {
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
					horizontal_gcd_values.remove(largest_gcd);
					
					if(largest_gcd_count > 0) {						
						//add the largest gcd to the list of most applicable gcd values
						most_common_gcd_values.add(largest_gcd);
					}
					
					//remove gcd matches from horizontal margin list
					List<Double> gcd_match_list = gcd_match_lists.get(largest_gcd);
					if(gcd_match_list != null) {
						log.warn("gcd match list :: "+gcd_match_list);
						log.warn("horizontal margin list :: "+horizontal_margin_list);
						horizontal_margin_list.removeAll(gcd_match_list);
					}
					
				}while(!horizontal_margin_list.isEmpty() && !horizontal_gcd_values.isEmpty());

				
				if(most_common_gcd_values.size() == 2) {
					horizontal_score += 2;
				}
				else {
					horizontal_score += 1;
				}
				
			}
			horizontal_unit_gcd_lists.put(unit, most_common_gcd_values);
			total_horizontal_score += 3;
		}
		
		
		
		
		
		//extract differences
		//check if a unique list of differences has only 1 element
		//GENERATE SCORE BASED ON UNITS WITH PADDING EVENLY DIVISIBLE BY UNIT GCD VALUES IDENTIFIED IN PREVIOUS STEPS
		vertical_margin_values = new ArrayList<>();
		horizontal_margin_values = new ArrayList<>();
		
		List<ElementState> elements_unmatched_gcd = new ArrayList<>();
		List<ElementState> unscalable_margin_elements = new ArrayList<>();
		
		for(ElementState element : page_state.getElements()) {
			if(element.getPreRenderCssValues().containsKey("margin")) {
				String margin_value = element.getPreRenderCssValues().get("margin");
				String[] separated_values = margin_value.split(" ");
				
				if(separated_values.length == 1) {
					vertical_margin_values.add(separated_values[0]);
					horizontal_margin_values.add(separated_values[0]);
				}
				else {
					for(int idx = 0; idx < separated_values.length; idx++) {
						if(idx % 2 == 0) {
							vertical_margin_values.add(separated_values[idx]);
						}
						else {
							horizontal_margin_values.add(separated_values[idx]);
						}
						
						String unit = extractMeasureUnit(separated_values[idx]);
						int unit_score = scoreMeasureUnit(unit);
						if(unit_score == 1) {
							unscalable_margin_elements.add(element);
						}
					}
				}
			}
			else if( element.getPreRenderCssValues().containsKey("margin-top")) {
				String margin_value = element.getPreRenderCssValues().get("margin-top");
				String[] separated_values = margin_value.split(" ");
				//extract vertical and horizontal
				for(String value : separated_values) {											
					if(!value.startsWith("0")) {
						vertical_margin_values.add( value);
						//calculate score unit. If unit is less than 3, record unit
						//determine unit measure
						String unit = extractMeasureUnit(margin_value);
						int unit_score = scoreMeasureUnit(unit);
						if(unit_score == 1) {
							unscalable_margin_elements.add(element);
						}
						vertical_score += unit_score;
						total_vertical_score += 3;
						
						//strip unit measure values 
						margin_value = cleanSizeUnits(margin_value).trim();
					}					
				}
			}
			else if( element.getPreRenderCssValues().containsKey("margin-bottom")) {
				String margin_value = element.getPreRenderCssValues().get("margin-bottom");
				String[] separated_values = margin_value.split(" ");
				//extract vertical and horizontal
				for(String value : separated_values) {											
					if(!value.startsWith("0")) {
						vertical_margin_values.add( value);
						//calculate score unit. If unit is less than 3, record unit
						//determine unit measure
						String unit = extractMeasureUnit(margin_value);
						int unit_score = scoreMeasureUnit(unit);
						if(unit_score == 1) {
							unscalable_margin_elements.add(element);
						}
						vertical_score += unit_score;
						total_vertical_score += 3;
						
						//strip unit measure values 
						margin_value = cleanSizeUnits(margin_value).trim();
					}					
				}
			}
			else if( element.getPreRenderCssValues().containsKey("margin-left")) {
				String margin_value = element.getPreRenderCssValues().get("margin-left");
				String[] separated_values = margin_value.split(" ");
				//extract vertical and horizontal
				for(String value : separated_values) {		
					if(!value.startsWith("0")) {
						horizontal_margin_values.add( value);
						//calculate score unit. If unit is less than 3, record unit
						//determine unit measure
						String unit = extractMeasureUnit(margin_value);
						int unit_score = scoreMeasureUnit(unit);
						if(unit_score == 1) {
							unscalable_margin_elements.add(element);
						}
						
						horizontal_score += unit_score;
						total_horizontal_score += 3;
						
						//if(
						//strip unit measure values 
						margin_value = cleanSizeUnits(margin_value).trim();
					}
				}
			}
			else if( element.getPreRenderCssValues().containsKey("margin-right")) {
				String margin_value = element.getPreRenderCssValues().get("margin-right");
				String[] separated_values = margin_value.split(" ");
				//extract vertical and horizontal
				for(String value : separated_values) {		
					if(!value.startsWith("0")) {
						horizontal_margin_values.add( value);
						//calculate score unit. If unit is less than 3, record unit
						//determine unit measure
						String unit = extractMeasureUnit(margin_value);
						int unit_score = scoreMeasureUnit(unit);
						if(unit_score == 1) {
							unscalable_margin_elements.add(element);
						}
						
						horizontal_score += unit_score;
						total_horizontal_score += 3;
						
						//if(
						//strip unit measure values 
						margin_value = cleanSizeUnits(margin_value).trim();
					}
				}
			}
			
			for(String margin_value : vertical_margin_values) {
				//determine unit measure
				String unit = extractMeasureUnit(margin_value);
				/*
				vertical_score += scoreMeasureUnit(unit);
				total_vertical_score += 3;
				*/
				//strip unit measure values 
				margin_value = cleanSizeUnits(margin_value).trim();
				
				//convert measure to Double
				if(margin_value == null || margin_value.isEmpty()) {
					continue;
				}
				double margin_number = Double.parseDouble(margin_value);
				
				//compute converted measure modulo gcd value
				boolean gcd_found = false;
				//log.warn("evaluating unit :: "+unit+"  unit gcd list value ::    "+unit_gcd_lists);
				if(unit_gcd_lists.containsKey(unit)) {
					for(double gcd : unit_gcd_lists.get(unit)) {
						if(margin_number % gcd == 0) {
							gcd_found = true;
							break;
						}
					}
				}
				
				if(gcd_found) {
					vertical_score += 3;
				}
				else {
					vertical_score += 1;
				}
				
				total_vertical_score += 3;
				//log.warn("matching gcd found :: " + gcd_found);

			}
			
			for(String margin_value : horizontal_margin_values) {
				//determine unit measure
				String unit = extractMeasureUnit(margin_value);
				
				//horizontal_score += scoreMeasureUnit(unit);
				//total_horizontal_score += 3;
				//strip unit measure values 
				margin_value = cleanSizeUnits(margin_value);
				
				//convert measure to Double
				if(margin_value == null || margin_value.isEmpty()) {
					continue;
				}
				double margin_number = Double.parseDouble(margin_value);
				
				//compute converted measure modulo gcd value
				boolean gcd_found = false;
				//log.warn("evaluating unit :: "+unit+"  unit gcd list value ::    "+horizontal_unit_gcd_lists);
				if(horizontal_unit_gcd_lists.containsKey(unit)) {
					for(double gcd : horizontal_unit_gcd_lists.get(unit)) {
						if(margin_number % gcd == 0) {
							gcd_found = true;
							break;
						}
					}
				}
				
				if(gcd_found) {
					horizontal_score += 3;
				}
				else {
					elements_unmatched_gcd.add(element);
					horizontal_score += 1;
				}
				
				total_horizontal_score += 3;
				//log.warn("matching gcd found :: " + gcd_found);

			}
		}		
		
		ElementObservation element_observation = new ElementObservation(elements_unmatched_gcd, "Elements that are not consistently sized relative to other margins across the site can create an uneven experience");
		ElementObservation unscalable_margin_observations = new ElementObservation(unscalable_margin_elements, "Using unscalable units (ie px, in, cm, mm, pt)");

		//PropertyMapObservation gcd_observation = new PropertyMapObservation(vertical_gcd_list, "gcd description goes here");
		
		observations.add(element_observation);
		observations.add(unscalable_margin_observations);
		
		//calculate score for question "Is margin used as margin?" NOTE: The expected calculation expects that margins are not used as margin
		double margin_score = (vertical_score + horizontal_score) / (double)(total_vertical_score + total_horizontal_score);

		
		int margin_as_margin_score = scoreMarginAsPadding(page_state.getElements());
		int score = ((int)margin_score + margin_as_margin_score);
		//int score = (margin_size_score + margin_as_margin_score);
		
		//log.warn("MARGIN SIZE SCORE  :::   "+margin_size_score);	
		log.warn("MARGIN AS PADDING SCORE  :::   "+margin_as_margin_score);	
		log.warn("MARGIN SCORE  :::   "+margin_score);	

		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE, AuditSubcategory.MARGIN, (vertical_score + horizontal_score), observations, AuditLevel.PAGE, (total_vertical_score + total_horizontal_score));
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
	 * @param elements
	 * @return
	 */
	private int scoreMarginAsPadding(List<ElementState> elements) {
		for(ElementState element : elements) {
			//identify elements that own text and have margin but not padding set
			
			
		}
		
		return 0;
		
	}
	
	private int scoreNonCollapsingMargins(List<ElementState> elements) {
		for(ElementState element : elements) {
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
	 * Generate score for margin usage
	 * 
	 * @param margin_set
	 * @return
	 */
	private int scoreMarginSizes(List<String> margin_set) {
		int score = 0;
		int total_possible_score = 0;
		//sort margin values into em, percent and px measure types
		Map<String, List<Double>> converted_unit_buckets = sortSizeUnits(margin_set);
		//reduce lists in map to unique values;
		log.warn("converted bucket list  "+converted_unit_buckets);
		
		//SCORING 1 - Check if all values have a similar multiple
		for(String unit : converted_unit_buckets.keySet()) {
			List<Double> margin_values = converted_unit_buckets.get(unit);
		
			//sort margin values and make them unique
			margin_values = sortAndMakeDistinct(margin_values);
			log.warn("Margin values identified :: " + margin_values);
			Double smallest_value = margin_values.get(0);
			
			for(int idx = 1; idx < margin_values.size(); idx++) {
				if(margin_values.get(idx) % smallest_value == 0) {
					score += 3;
				}
				else {
					score += 1;
				}
				total_possible_score+= 3;
			}
			
		}
		//SCORING 1a - Check if all values have the same difference
		
		if(total_possible_score == 0){
			total_possible_score = 3;
		}
		
		//CALCULATE OVERALL SCORE :: 
		log.warn("Margin score :: "+(score/total_possible_score));
		if(score == 0.0) {
			return score;
		}
		return score / total_possible_score;
	}

	private Map<String, List<String>> categorizeSizeUnits(Map<String, List<String>> converted_unit_buckets) {
		Map<String, List<String>> unit_categories = new HashMap<>();
		
		for(String unit : converted_unit_buckets.keySet()) {
			List<String> units = new ArrayList<String>();
			if("rem".equals(unit) || "em".equals(unit) || "%".equals(unit)) {
				if(unit_categories.containsKey("scalable-high")) {
					units = unit_categories.get("scalable-high");
				}
				units.add(unit);
				unit_categories.put("scalable-high", units);
			}
			else if("vh".equals(unit) || "vw".equals(unit) || "vmin".equals(unit) || "vmax".equals(unit)) {
				if(unit_categories.containsKey("scalable-low")) {
					units = unit_categories.get("scalable-low");
				}
				units.add(unit);
				unit_categories.put("scalable-low", units);
			}
			else if("px".equals(unit) || "ex".equals(unit) || "pt".equals(unit) || "cm".equals(unit) || "mm".equals(unit) || "in".equals(unit) || "pc".equals(unit)) {
				if(unit_categories.containsKey("constant")) {
					units = unit_categories.get("constant");
				}
				units.add(unit);
				unit_categories.put("constant", units);
			}
		}
		
		return unit_categories;
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
			return 3;
		}
		else if(unit.contains("vh") || unit.contains("vw") || unit.contains("vmin") || unit.contains("vmax")) {
			return 2;
		}
		else if(unit.contains("px") || unit.contains("ex") || unit.contains("pt") || unit.contains("cm") || unit.contains("mm") || unit.contains("in") || unit.contains("pc")) {
			return 1;
		}
		
		return 3;
	}
	
	public static List<Double> deflateGCD(List<Double> gcd_list){
		return gcd_list.stream().map(s -> s/100.0).distinct().sorted().collect(Collectors.toList());
	}
}
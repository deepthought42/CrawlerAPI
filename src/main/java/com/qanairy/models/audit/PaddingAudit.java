package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.services.ObservationService;



/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class PaddingAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PaddingAudit.class);
	
	private String[] size_units = {"px", "pt", "%", "em", "rem", "ex", "vh", "vw", "vmax", "vmin", "mm", "cm", "in", "pc"};
	
	@Autowired
	private ObservationService observation_service;
	
	public PaddingAudit() {	}
	
	private static String getAuditDescription() {
		return "The space between contents of an element";
	}

	private static List<String> buildBestPractices() {
		List<String> best_practices = new ArrayList<>();
		best_practices.add("When applying margins and padding, you should avoid using absolute units . This is because these units won’t adapt to the changes in font size or screen width.");
		
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
		
		int vertical_gcd_score = 0;
		int horizontal_gcd_score = 0;
		int total_possible_gcd_score = 0;
		
		int vertical_unit_score = 0;
		int horizontal_unit_score = 0;
		int total_possible_unit_score = 0;
		
		
		
		
		List<String> vertical_padding_values = new ArrayList<>();
		List<String> horizontal_padding_values = new ArrayList<>();

		List<Observation> observations = new ArrayList<>();


		//extract vertical and horizontal padding values
		for(ElementState element : page_state.getElements()) {
			String padding_value = "";
			if(element.getPreRenderCssValues().containsKey("padding")) {
				padding_value = element.getPreRenderCssValues().get("padding");
				String[] separated_values = padding_value.split(" ");
				
				if(separated_values.length == 1) {
					vertical_padding_values.add(separated_values[0]);
					horizontal_padding_values.add(separated_values[0]);
				}
				else {
					for(int idx = 0; idx < separated_values.length; idx++) {
						if(idx % 2 == 0) {
							vertical_padding_values.add(separated_values[idx]);
						}
						else {
							horizontal_padding_values.add(separated_values[idx]);
						}
					}
				}
			}
			else if( element.getPreRenderCssValues().containsKey("padding-top")) {
				padding_value = element.getPreRenderCssValues().get("padding-top");
				String[] separated_values = padding_value.split(" ");
				//extract vertical and horizontal
				for(String value : separated_values) {											
					vertical_padding_values.add( value);
				}
			}
			else if( element.getPreRenderCssValues().containsKey("padding-bottom")) {
				padding_value = element.getPreRenderCssValues().get("padding-bottom");
				String[] separated_values = padding_value.split(" ");
				//extract vertical and horizontal
				for(String value : separated_values) {											
					vertical_padding_values.add( value);
				}
			}
			else if( element.getPreRenderCssValues().containsKey("padding-right")) {
				padding_value = element.getPreRenderCssValues().get("padding-right");
				String[] separated_values = padding_value.split(" ");
				//extract vertical and horizontal
				for(String value : separated_values) {											
					horizontal_padding_values.add( value);
				}
			}
			else if( element.getPreRenderCssValues().containsKey("padding-left")) {
				padding_value = element.getPreRenderCssValues().get("padding-left");
				String[] separated_values = padding_value.split(" ");
				//extract vertical and horizontal
				for(String value : separated_values) {											
					horizontal_padding_values.add( value);
				}
			}	
		}
		
		
		
		Map<String, List<Double>> vertical_units = sortSizeUnits(vertical_padding_values);
		Map<String, List<Double>> vertical_gcd_list = new HashMap<>();
		//extract multiples for vertical paddings
		//most common multiples, the highest multiples that can be found to satisfy the list of unique padding values
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
		log.warn("----------------------------------------------------------");
		log.warn("----------------------------------------------------------");
		log.warn("vertical gcd list ::   "+vertical_gcd_list);
		
		
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
				List<Double> vertical_padding_list = vertical_units.get(unit);
				List<Double> vertical_gcd_values = vertical_gcd_list.get(unit);
				do {
					Map<Double, List<Double>> gcd_match_lists = new HashMap<>();

					//find highest gcd values that define the set
					for(double gcd : vertical_gcd_values) {
						gcd_match_lists.put(gcd, new ArrayList<Double>());
						for(double value : vertical_padding_list) {
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
					
					//remove gcd matches from vertical padding list
					vertical_padding_list.removeAll(gcd_match_lists.get(largest_gcd));
					
				}while(!vertical_padding_list.isEmpty() && !vertical_gcd_values.isEmpty());

				
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
		Map<String, List<Double>> horizontal_units = sortSizeUnits(horizontal_padding_values);
		Map<String, List<Double>> horizontal_gcd_list = new HashMap<>();
		//extract multiples for horizontal paddings
		//most common multiples, the highest multiples that can be found to satisfy the list of unique padding values
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
		log.warn("----------------------------------------------------------");
		log.warn("----------------------------------------------------------");
		log.warn("horizontal gcd list ::   "+horizontal_gcd_list);
		
		
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
				List<Double> horizontal_padding_list = horizontal_units.get(unit);
				List<Double> horizontal_gcd_values = horizontal_gcd_list.get(unit);
				do {
					Map<Double, List<Double>> gcd_match_lists = new HashMap<>();
					//find highest gcd values that define the set
					for(double gcd : horizontal_gcd_values) {
						gcd_match_lists.put(gcd, new ArrayList<Double>());
						for(double value : horizontal_padding_list) {
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
					
					//remove gcd matches from horizontal padding list
					horizontal_padding_list.removeAll(gcd_match_lists.get(largest_gcd));
					
				}while(!horizontal_padding_list.isEmpty() && !horizontal_gcd_values.isEmpty());

				
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
		
		log.warn("horizontal score value  1   ::   "+total_horizontal_score);
		
		
		
		
		
		
		
		//extract differences
		//check if a unique list of differences has only 1 element
		//GENERATE SCORE BASED ON UNITS WITH PADDING EVENLY DIVISIBLE BY UNIT GCD VALUES IDENTIFIED IN PREVIOUS STEPS
		vertical_padding_values = new ArrayList<>();
		horizontal_padding_values = new ArrayList<>();
		
		List<ElementState> elements_unmatched_gcd = new ArrayList<>();
		List<ElementState> unscalable_padding_elements = new ArrayList<>();
		
		for(ElementState element : page_state.getElements()) {
			if(element.getPreRenderCssValues().containsKey("padding")) {
				String padding_value = element.getPreRenderCssValues().get("padding");
				String[] separated_values = padding_value.split(" ");
				
				if(separated_values.length == 1) {
					vertical_padding_values.add(separated_values[0]);
					horizontal_padding_values.add(separated_values[0]);
				}
				else {
					for(int idx = 0; idx < separated_values.length; idx++) {
						if(idx % 2 == 0) {
							vertical_padding_values.add(separated_values[idx]);
						}
						else {
							horizontal_padding_values.add(separated_values[idx]);
						}
						
						String unit = extractMeasureUnit(separated_values[idx]);
						int unit_score = scoreMeasureUnit(unit);
						if(unit_score == 1) {
							unscalable_padding_elements.add(element);
						}
					}
				}
			}
			else if( element.getPreRenderCssValues().containsKey("padding-top")) {
				String padding_value = element.getPreRenderCssValues().get("padding-top");
				String[] separated_values = padding_value.split(" ");
				//extract vertical and horizontal
				for(String value : separated_values) {											
					if(!value.startsWith("0")) {
						vertical_padding_values.add( value);
						//calculate score unit. If unit is less than 3, record unit
						//determine unit measure
						String unit = extractMeasureUnit(padding_value);
						int unit_score = scoreMeasureUnit(unit);
						if(unit_score == 1) {
							unscalable_padding_elements.add(element);
						}
						vertical_score += unit_score;
						total_vertical_score += 3;
						
						//strip unit measure values 
						padding_value = cleanSizeUnits(padding_value).trim();
					}					
				}
			}
			else if( element.getPreRenderCssValues().containsKey("padding-bottom")) {
				String padding_value = element.getPreRenderCssValues().get("padding-bottom");
				String[] separated_values = padding_value.split(" ");
				//extract vertical and horizontal
				for(String value : separated_values) {											
					if(!value.startsWith("0")) {
						vertical_padding_values.add( value);
						//calculate score unit. If unit is less than 3, record unit
						//determine unit measure
						String unit = extractMeasureUnit(padding_value);
						int unit_score = scoreMeasureUnit(unit);
						if(unit_score == 1) {
							unscalable_padding_elements.add(element);
						}
						vertical_score += unit_score;
						total_vertical_score += 3;
						
						//strip unit measure values 
						padding_value = cleanSizeUnits(padding_value).trim();
					}					
				}
			}
			else if( element.getPreRenderCssValues().containsKey("padding-left")) {
				String padding_value = element.getPreRenderCssValues().get("padding-left");
				String[] separated_values = padding_value.split(" ");
				//extract vertical and horizontal
				for(String value : separated_values) {		
					if(!value.startsWith("0")) {
						horizontal_padding_values.add( value);
						//calculate score unit. If unit is less than 3, record unit
						//determine unit measure
						String unit = extractMeasureUnit(padding_value);
						int unit_score = scoreMeasureUnit(unit);
						if(unit_score == 1) {
							unscalable_padding_elements.add(element);
						}
						
						horizontal_score += unit_score;
						total_horizontal_score += 3;
						
						//if(
						//strip unit measure values 
						padding_value = cleanSizeUnits(padding_value).trim();
					}
				}
			}
			else if( element.getPreRenderCssValues().containsKey("padding-right")) {
				String padding_value = element.getPreRenderCssValues().get("padding-right");
				String[] separated_values = padding_value.split(" ");
				//extract vertical and horizontal
				for(String value : separated_values) {		
					if(!value.startsWith("0")) {
						horizontal_padding_values.add( value);
						//calculate score unit. If unit is less than 3, record unit
						//determine unit measure
						String unit = extractMeasureUnit(padding_value);
						int unit_score = scoreMeasureUnit(unit);
						if(unit_score == 1) {
							unscalable_padding_elements.add(element);
						}
						
						horizontal_score += unit_score;
						total_horizontal_score += 3;
						
						//if(
						//strip unit measure values 
						padding_value = cleanSizeUnits(padding_value).trim();
					}
				}
			}
			
			for(String padding_value : vertical_padding_values) {
				//determine unit measure
				String unit = extractMeasureUnit(padding_value);
				/*
				vertical_score += scoreMeasureUnit(unit);
				total_vertical_score += 3;
				*/
				//strip unit measure values 
				padding_value = cleanSizeUnits(padding_value).trim();
				
				//convert measure to Double
				if(padding_value == null || padding_value.isEmpty()) {
					continue;
				}
				double padding_number = Double.parseDouble(padding_value);
				
				//compute converted measure modulo gcd value
				boolean gcd_found = false;
				//log.warn("evaluating unit :: "+unit+"  unit gcd list value ::    "+unit_gcd_lists);
				if(unit_gcd_lists.containsKey(unit)) {
					for(double gcd : unit_gcd_lists.get(unit)) {
						if(padding_number % gcd == 0) {
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
			
			for(String padding_value : horizontal_padding_values) {
				//determine unit measure
				String unit = extractMeasureUnit(padding_value);
				
				//horizontal_score += scoreMeasureUnit(unit);
				//total_horizontal_score += 3;
				//strip unit measure values 
				padding_value = cleanSizeUnits(padding_value);
				
				//convert measure to Double
				if(padding_value == null || padding_value.isEmpty()) {
					continue;
				}
				double padding_number = Double.parseDouble(padding_value);
				
				//compute converted measure modulo gcd value
				boolean gcd_found = false;
				//log.warn("evaluating unit :: "+unit+"  unit gcd list value ::    "+horizontal_unit_gcd_lists);
				if(horizontal_unit_gcd_lists.containsKey(unit)) {
					for(double gcd : horizontal_unit_gcd_lists.get(unit)) {
						if(padding_number % gcd == 0) {
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
		
		ElementObservation element_observation = new ElementObservation(elements_unmatched_gcd, "Elements that are not consistently sized relative to other paddings across the site can create an uneven experience");
		ElementObservation unscalable_padding_observations = new ElementObservation(unscalable_padding_elements, "Using unscalable units (ie px, in, cm, mm, pt)");

		//PropertyMapObservation gcd_observation = new PropertyMapObservation(vertical_gcd_list, "gcd description goes here");
		
		observations.add(element_observation);
		observations.add(unscalable_padding_observations);
		
		//observations.add(gcd_observation);
		log.warn("vertical score :: "+vertical_score + "/"+total_vertical_score + "      :    "+(vertical_score/(double)total_vertical_score));

		log.warn("horizontal score :: "+horizontal_score + "/"+total_horizontal_score + "      :    "+(horizontal_score/(double)total_horizontal_score));
		
		//calculate score

		//double score = scorePaddingUsage(padding_values);
		double score = (vertical_score + horizontal_score) / (double)(total_vertical_score + total_horizontal_score);
		//double score = scorePaddingUsage(padding_values);
		log.warn("PADDING SCORE  :::   "+score);	

		
		//		What sort of observations can exist for padding?		
		
		
		
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE, AuditSubcategory.PADDING, (vertical_score + horizontal_score), observations, AuditLevel.PAGE, (total_vertical_score + total_horizontal_score));
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

	private List<Integer> amplifyAndConvertToInt(List<Double> values) {
		return values.stream().map(s -> s*100).map(s -> s.intValue()).distinct().collect(Collectors.toList());
	}


	private Map<String, List<String>> categorizeSizeUnits(Set<String> set) {
		Map<String, List<String>> unit_categories = new HashMap<>();
		
		for(String unit : set) {
			List<String> units = new ArrayList<>();
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
	 * Sort units into buckets by mapping unit type to padding sizes
	 * 
	 * @param padding_set
	 * @return
	 */
	private Map<String, List<Double>> sortSizeUnits(List<String> padding_set) {
		Map<String, List<Double>> sorted_paddings = new HashMap<>();
		
		for(String padding_value : padding_set) {
			if(padding_value == null 
					|| "0".equals(padding_value.trim()) 
					|| padding_value.contains("auto")) {
				continue;
			}
			
			for(String unit : size_units) {
				if(padding_value != null && padding_value.contains(unit)) {
					List<Double> values = new ArrayList<>();

					if(sorted_paddings.containsKey(unit)) {
						values = sorted_paddings.get(unit);
					}
					
					String value = cleanSizeUnits(padding_value);
					//amplify padding values by multipying by 100. Doing this moves the decimal place and makes truncation without data loss possible. 
					//We can perform the gcd functions then reduce all values by a factor of 100
					//int amplified_value = amplifyDecimal(value);
					//values = cleanSizeUnits(values);
					//List<Double> converted_values = convertList(values, s -> Double.parseDouble(s));
					values.add(Double.parseDouble(value));
					sorted_paddings.put(unit, values);
				}
			}
		}
		return sorted_paddings;
	}
	
	public static List<Double> sortAndMakeDistinct(List<Double> from){
		return from.stream().filter(n -> n != 0).map(s -> s*100).distinct().sorted().collect(Collectors.toList());
	}
	
	public static List<Double> makeDistinct(List<Double> from){
		return from.stream().distinct().sorted().collect(Collectors.toList());
	}
	
	public static List<Double> deflateGCD(List<Double> gcd_list){
		return gcd_list.stream().map(s -> s/100.0).distinct().sorted().collect(Collectors.toList());
	}
	
	public static List<String> cleanSizeUnits(List<String> from){
		return from.stream()
				.map(line -> line.replaceAll("px", ""))
				.map(line -> line.replaceAll("%", ""))
				.map(line -> line.replaceAll("em", ""))
				.map(line -> line.replaceAll("rem", ""))
				.map(line -> line.replaceAll("pt", ""))
				.map(line -> line.replaceAll("ex", ""))
				.map(line -> line.replaceAll("vw", ""))
				.map(line -> line.replaceAll("vh", ""))
				.map(line -> line.replaceAll("cm", ""))
				.map(line -> line.replaceAll("mm", ""))
				.map(line -> line.replaceAll("in", ""))
				.map(line -> line.replaceAll("pc", ""))
				.map(line -> line.replaceAll("!important", ""))
				.map(line -> line.indexOf(".") > -1 ? line.substring(0, line.indexOf(".")) : line)
				.collect(Collectors.toList());
	}
	
	public static String cleanSizeUnits(String value){
		return value.replace("!important", "")
					.replaceAll("px", "")
					.replaceAll("%", "")
					.replaceAll("em", "")
					.replaceAll("rem", "")
					.replaceAll("pt", "")
					.replaceAll("ex", "")
					.replaceAll("vw", "")
					.replaceAll("vh", "")
					.replaceAll("cm", "")
					.replaceAll("mm", "")
					.replaceAll("in", "")
					.replaceAll("pc", "");
					
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
}
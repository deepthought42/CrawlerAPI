package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
public class PaddingAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PaddingAudit.class);
	
	private String[] size_units = {"px", "pt", "%", "em", "rem", "ex", "vh", "vw", "vmax", "vmin", "mm", "cm", "in", "pc"};
	
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
		
		List<String> padding_tops = new ArrayList<>();
		List<String> padding_bottoms = new ArrayList<>();
		List<String> padding_rights = new ArrayList<>();
		List<String> padding_lefts = new ArrayList<>();
		
		for(ElementState element : page_state.getElements()) {
			String padding_top = element.getCssValues().get("padding-top");
			String padding_left = element.getCssValues().get("padding-left");
			String padding_right = element.getCssValues().get("padding-right");
			String padding_bottom = element.getCssValues().get("padding-bottom");

			padding_tops.add(padding_top);
			padding_lefts.add(padding_left);
			padding_rights.add(padding_right);
			padding_bottoms.add(padding_bottom);			
		}
		padding_tops.remove(null);
		padding_lefts.remove(null);
		padding_rights.remove(null);
		padding_bottoms.remove(null);
		
	
		double score_top = scorePaddingUsage(padding_tops);
		double score_bottom = scorePaddingUsage(padding_bottoms);
		double score_left = scorePaddingUsage(padding_lefts);
		double score_right = scorePaddingUsage(padding_rights);

		double score = (score_top + score_left + score_bottom + score_right)/4;
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE, buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.PADDING, score, new ArrayList<>(), AuditLevel.PAGE);
	}

	/**
	 * Generate score for padding usage
	 * 
	 * @param padding_set
	 * @return
	 */
	private double scorePaddingUsage(List<String> padding_set) {
		double score = 0.0;
		double total_possible_score = 0.0;
		//sort padding values into em, percent and px measure types
		Map<String, List<String>> bucketed_units = sortSizeUnits(padding_set);
		
		Map<String, List<Double>> converted_unit_buckets = new HashMap<>();

		//reduce padding_sets to unique values and remove 0 values
		for(String unit : bucketed_units.keySet()) {
			List<Double> measures = convertList(cleanSizeUnits(bucketed_units.get(unit)), s -> Double.parseDouble(s));
			measures = removeZeroValues(measures);
			measures.parallelStream().distinct().sorted().collect(Collectors.toList());
			log.warn("Total measures found for unit "+unit+"   :    "+measures);
			converted_unit_buckets.put(unit, measures);
		}
		
		//SCORING 1 - Check if which padding measures are used and assign a score based on type used
		// (scalable-high) rem, em, percent = 3  ---  (scalable-low)vh=2, vw=2, vmin=2, vmax=2 --  (constant/print)px = 1, ex=1, pt=1, cm=1, mm=1, in=1, pc=1, 
		// The primary set is defined as the set with the highest precendence(scalable-high, scalable-low, constant/print)
		for(String unit : converted_unit_buckets.keySet()) {
			if("rem".equals(unit) || "em".equals(unit) || "%".equals(unit)) {
				score += 3.0;
			}
			else if("vh".equals(unit) || "vw".equals(unit) || "vmin".equals(unit) || "vmax".equals(unit)) {
				score += 2.0;
			}
			else if("px".equals(unit) || "ex".equals(unit) || "pt".equals(unit) || "cm".equals(unit) || "mm".equals(unit) || "in".equals(unit) || "pc".equals(unit)) {
				score += 1.0;
			}
		}
		total_possible_score += (converted_unit_buckets.keySet().size() * 3);
		
		
		// SCORING 2 - score each set present for consistency 
		// Sets should comply with a predefined pattern. 1 of the following should be true, 
		//    1.  every number being a multiple of the lowest padding value used
		//    2.  All padding values are separated by the same value (for example: paddings = (10, 15, 20, 25) -> differences(5, 5, 5). What would not qualify: paddings = (2, 5, 8, 15) -> differences (3, 3, 7)
		//get first number. it should be the lowest value in the list
		for(String unit : converted_unit_buckets.keySet()) {
			List<Double> padding_sizes = converted_unit_buckets.get(unit);
			log.warn("paddings identified for unit "+unit+"  :  "+padding_sizes);
			
			double smallest_padding = padding_sizes.get(0);
			List<Double> padding_gcd = new ArrayList<>();
	
			//Modulo all other numbers in the list of paddings by the smallest padding. 
			for(int idx = 1; idx < padding_sizes.size()-1; idx++) {
				double gcd = findGCD(smallest_padding, padding_sizes.get(idx));
				padding_gcd.add(gcd);
			}
	
			padding_gcd = padding_gcd.parallelStream().distinct().sorted().collect(Collectors.toList());
			
			//check if gcd is not empty and only has 1 element, then score = 3
			if(padding_gcd.size() == 1) {
				score += 3.0;
			}
			else {
				score += 1.0;
			}
			
			log.warn("padding gcd values :: "+padding_gcd);
			//check if the difference between each consecutive padding size is the same for the entire set 
			List<Double> padding_diffs = new ArrayList<>();
			for(int idx = 1; idx < padding_sizes.size()-1; idx++) {
				double diff = Math.abs(padding_sizes.get(idx) - padding_sizes.get(idx+1));
				padding_diffs.add(diff);
			}
			
			log.warn("padding diffs :: "+padding_diffs);

		}
		total_possible_score += (converted_unit_buckets.size() * 3);
		
		
		// SCORING 4 - overall consistency / regret value for  mixing measure types
		//  if multiple measure type classes(scalable-high, scalable-low, contant/print) were used across site then add a 20% penalty for inconsistency
		
		//   group units by general bucket instead of unit
		Map<String, List<String>> bucketed_sizes = categorizeSizeUnits(converted_unit_buckets);
		if(bucketed_sizes.size() == 1) {
			score += 3;
		}
		else {
			score += 1;
		}
		total_possible_score += 3;
		
		//CALCULATE OVERALL SCORE :: 
		log.warn("Padding score :: "+(score/total_possible_score));
		return score / total_possible_score;
	}
	
	private Map<String, List<String>> categorizeSizeUnits(Map<String, List<Double>> converted_unit_buckets) {
		Map<String, List<String>> unit_categories = new HashMap<>();
		
		for(String unit : converted_unit_buckets.keySet()) {
			if("rem".equals(unit) || "em".equals(unit) || "%".equals(unit)) {
				List<String> units = new ArrayList<String>();
				if(unit_categories.containsKey("scalable-high")) {
					units = unit_categories.get(unit);
				}
				units.add(unit);
				unit_categories.put("scalable-high", units);
			}
			else if("vh".equals(unit) || "vw".equals(unit) || "vmin".equals(unit) || "vmax".equals(unit)) {
				List<String> units = new ArrayList<String>();
				if(unit_categories.containsKey("scalable-low")) {
					units = unit_categories.get(unit);
				}
				units.add(unit);
				unit_categories.put("scalable-low", units);
			}
			else if("px".equals(unit) || "ex".equals(unit) || "pt".equals(unit) || "cm".equals(unit) || "mm".equals(unit) || "in".equals(unit) || "pc".equals(unit)) {
				List<String> units = new ArrayList<String>();
				if(unit_categories.containsKey("constant")) {
					units = unit_categories.get(unit);
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
	private Map<String, List<String>> sortSizeUnits(List<String> padding_set) {
		Map<String, List<String>> sorted_paddings = new HashMap<String, List<String>>();
		for(String padding_value : padding_set) {
			for(String unit : size_units) {
				if(padding_value.contains(unit)) {
					List<String> values = new ArrayList<String>();

					if(sorted_paddings.containsKey(unit)) {
						values = sorted_paddings.get(unit);
					}
					values.add(padding_value);
					sorted_paddings.put(unit, values);
				}
			}
		}
		return sorted_paddings;
	}

	private List<String> roundDecimals(List<String> size_list) {
		return size_list.stream().map(line -> Double.parseDouble(line)).map(size -> Math.round(size)+"").collect(Collectors.toList());
	}

	public static List<String> cleanSizeUnits(List<String> from){
		return from.stream()
				.map(line -> line.replaceAll("px", ""))
				.map(line -> line.replaceAll("%", ""))
				.map(line -> line.replaceAll("em", ""))
				.map(line -> line.replaceAll("rem", ""))
				.map(line -> line.replaceAll("pt", ""))
				.map(line -> line.replaceAll("ex", ""))
				.map(line -> line.replaceAll("cm", ""))
				.map(line -> line.replaceAll("mm", ""))
				.map(line -> line.replaceAll("in", ""))
				.map(line -> line.replaceAll("pc", ""))
				.map(line -> line.replaceAll("%", ""))
				.map(line -> line.indexOf(".") > -1 ? line.substring(0, line.indexOf(".")) : line).collect(Collectors.toList());
	}
	
	public static List<Double> removeZeroValues(List<Double> from){
		return from.stream().filter(n -> n != 0.0).collect(Collectors.toList());
	}
	
	//for lists
	public static <T, U> List<U> convertList(List<T> from, Function<T, U> func) {
	    return from.stream().map(func).collect(Collectors.toList());
	}
	
	/* * Java method to find GCD of two number using Euclid's method * @return GDC of two numbers in Java */ 
	private static int findGCD(int number1, int number2) { 
		//base case 
		if(number2 == 0){ 
			return number1; 
		} 
		return findGCD(number2, number1%number2);
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
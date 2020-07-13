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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;

import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.Declaration;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.StyleSheet;
import cz.vutbr.web.csskit.RuleFontFaceImpl;
import cz.vutbr.web.csskit.RuleKeyframesImpl;
import cz.vutbr.web.csskit.RuleMediaImpl;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class MeasureUnitAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(MeasureUnitAudit.class);
	
	private String[] measure_units = {"px", "pt", "%", "em", "rem", "ex", "vh", "vw", "vmax", "vmin", "mm", "cm", "in", "pc"};
	
	public MeasureUnitAudit() {	}
	
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
		
		Document doc = Jsoup.parse(page_state.getSrc());	
		List<String> raw_stylesheets = new ArrayList<>();
		
		Elements stylesheets = doc.select("link");
		for(Element stylesheet : stylesheets) {
			if("text/css".equalsIgnoreCase(stylesheet.attr("type"))) {
				String stylesheet_url = stylesheet.absUrl("href");
				//parse the style sheet
				try {
					raw_stylesheets.add(URLReader(new URL(stylesheet_url)));
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		
		List<String> css_prop_values = new ArrayList<>();
		
		for(String raw_stylesheet : raw_stylesheets) {
			//parse the style sheet
			try {
				StyleSheet sheet = CSSFactory.parseString(raw_stylesheet, new URL(page_state.getUrl()));
				for(int idx = 0; idx < sheet.size(); idx++) {
					if(sheet.get(idx) instanceof RuleFontFaceImpl 
							|| sheet.get(idx) instanceof RuleMediaImpl
							|| sheet.get(idx) instanceof RuleKeyframesImpl) {
						continue;
					}
					
					//access the rules and declarations
					RuleSet rule = (RuleSet) sheet.get(idx);       //get the first rule
					for(CombinedSelector selector : rule.getSelectors()) {
	
						//if selector name is a class or id then clean selector name 
						//if selector name is used in the page source then
						//extract any padding property values
						
						//if selector name is a tagname then check if tag name is used in document
						//if selector is used in document then 
						//extract any padding property values

						String selector_str = selector.toString();
						if(selector_str.startsWith(".")
							|| selector_str.startsWith("#")) 
						{
							selector_str = selector_str.substring(1);
						}

						if(page_state.getSrc().contains(selector_str)) {
							//TODO look for padding and add it to the document
							for(Declaration declaration : rule) {
								String property_string = declaration.getProperty();
								if(containsMeasureUnit(property_string)) {
									//TODO parse string to remove al but measure value and unit	
									property_string = property_string.substring(property_string.indexOf(":"));
									property_string = property_string.replace(";", "").trim();
									
									String[] property_values = property_string.split(" ");
									for(String value : property_values) {
										css_prop_values.add(value);
									}
								}
							}
						}
					}
				}
				//or even print the entire style sheet (formatted)
			} catch (MalformedURLException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (CSSException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		}
		
		
		//calculate score
		double score = scoreMeasureUnitUsage(css_prop_values);
		log.warn("Measure Unit Audit score :: "+ score );
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE, buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.MEASURE_UNITS, score, new ArrayList<>(), AuditLevel.PAGE);
	}

	private boolean containsMeasureUnit(String property) {
		for(String unit : measure_units) {
			if(property.contains(unit)) {
				return true;
			}
		}
		return false;
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
	 * Generate score for padding usage
	 * 
	 * @param padding_set
	 * @return
	 */
	private double scoreMeasureUnitUsage(List<String> padding_set) {
		double score = 0.0;
		double total_possible_score = 0.0;
		//sort padding values into em, percent and px measure types
		Map<String, List<String>> converted_unit_buckets = sortSizeUnits(padding_set);
		
		//SCORING 1 - Check if which measures are used and assign a score based on type used
		// (scalable-high) rem, em, percent = 3  ---  (scalable-low)vh=2, vw=2, vmin=2, vmax=2 --  (constant/print)px = 1, ex=1, pt=1, cm=1, mm=1, in=1, pc=1, 
		// The primary set is defined as the set with the highest precendence(scalable-high, scalable-low, constant/print)
		for(String unit : converted_unit_buckets.keySet()) {
			if("rem".equals(unit) || "em".equals(unit) || "%".equals(unit)) {
				score += (converted_unit_buckets.get(unit).size() * 3);
			}
			else if("vh".equals(unit) || "vw".equals(unit) || "vmin".equals(unit) || "vmax".equals(unit)) {
				score += (converted_unit_buckets.get(unit).size() * 2);
			}
			else if("px".equals(unit) || "ex".equals(unit) || "pt".equals(unit) || "cm".equals(unit) || "mm".equals(unit) || "in".equals(unit) || "pc".equals(unit)) {
				score += converted_unit_buckets.get(unit).size();
			}
			total_possible_score += (converted_unit_buckets.get(unit).size() * 3);
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
	 * Sort units into buckets by mapping unit type to padding sizes
	 * 
	 * @param padding_set
	 * @return
	 */
	private Map<String, List<String>> sortSizeUnits(List<String> padding_set) {
		Map<String, List<String>> sorted_paddings = new HashMap<String, List<String>>();
		//replace all px values with em for values that contain decimals
		
		for(String padding_value : padding_set) {
			if(padding_value == null 
					|| "0".equals(padding_value.trim()) 
					|| padding_value.contains("auto")) {
				continue;
			}
			
			for(String unit : measure_units) {
				if(padding_value != null && padding_value.contains(unit)) {

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
	
	public static List<Integer> removeZeroValues(List<Integer> from){
		return from.stream().filter(n -> n != 0).collect(Collectors.toList());
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
}
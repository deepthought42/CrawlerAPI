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
		
		List<String> padding_values = new ArrayList<>();
		
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
								if(declaration.getProperty().contains("padding")) {
									String raw_property_value = declaration.toString();
									raw_property_value = raw_property_value.replace("padding:", "");
									raw_property_value = raw_property_value.replace("padding-top:", "");
									raw_property_value = raw_property_value.replace("padding-bottom:", "");
									raw_property_value = raw_property_value.replace("padding-right:", "");
									raw_property_value = raw_property_value.replace("padding-left:", "");
									raw_property_value = raw_property_value.replace(";", "");
									
									String[] separated_values = raw_property_value.split(" ");
									for(String value : separated_values) {											
										padding_values.add(value);
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
		double score = scorePaddingUsage(padding_values);
		log.warn("PADDING SCORE  :::   "+score);	
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE, buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.PADDING, score, new ArrayList<>(), AuditLevel.PAGE);
 
		
		
		
		/*
		
		//THE FOLLOWING WORKS TO GET RENDERED CSS VALUES FOR EACH ELEMENT THAT ACTUALLY HAS CSS
		Tidy tidy = new Tidy(); // obtain a new Tidy instance
		tidy.setXHTML(true); // set desired config options using tidy setters 
		                          // (equivalent to command line options)

		org.w3c.dom.Document w3c_document = tidy.parseDOM(new ByteArrayInputStream(doc.outerHtml().getBytes()), null);
		
		List<String> padding = new ArrayList<>();

		//count all elements with non 0 px values that aren't decimals
		MediaSpec media = new MediaSpecAll(); //use styles for all media
		StyleMap map = null;
		try {
			map = CSSFactory.assignDOM(w3c_document, "UTF-8", new URL(page_state.getUrl()), media, true);
			
			log.warn("css dom map ::   "+map.size());
			for(ElementState element : page_state.getElements()) {
	
				//create the style map
	
				XPath xPath = XPathFactory.newInstance().newXPath();
				try {
					Node node = (Node)xPath.compile(element.getXpath()).evaluate(w3c_document, XPathConstants.NODE);
					NodeData style = map.get((org.w3c.dom.Element)node); //get the style map for the element
					log.warn("element node ::   "+node);
					log.warn("Element styling  ::  "+style);
					if(style != null) {
						log.warn("Element styling  ::  "+style.getProperty("padding-top"));
					}
					
					//StyleSheet sheet = CSSFactory.parseString(raw_stylesheet, new URL(page_state.getUrl()));
				} catch (XPathExpressionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				String padding_top = element.getCssValues().get("padding-top");
				String padding_left = element.getCssValues().get("padding-left");
				String padding_right = element.getCssValues().get("padding-right");
				String padding_bottom = element.getCssValues().get("padding-bottom");
	
				padding.add(padding_top);
				padding.add(padding_bottom);			
				padding.add(padding_left);
				padding.add(padding_right);
			}
			padding.remove(null);
			double score = scorePaddingUsage(padding);
			return new Audit(AuditCategory.INFORMATION_ARCHITECTURE, buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.PADDING, score, new ArrayList<>(), AuditLevel.PAGE);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
		*/
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
	private double scorePaddingUsage(List<String> padding_set) {
		double score = 0.0;
		double total_possible_score = 0.0;
		//sort padding values into em, percent and px measure types
		Map<String, List<Double>> converted_unit_buckets = sortSizeUnits(padding_set);
		//reduce lists in map to unique values;
		
		//SCORING 1 - Check if all values have a similar multiple
		for(String unit : converted_unit_buckets.keySet()) {
			List<Double> padding_values = converted_unit_buckets.get(unit);
		
			//sort padding values and make them unique
			padding_values = sortAndMakeDistinct(padding_values);
			
			Double smallest_value = padding_values.get(0);
			
			for(int idx = 1; idx < padding_values.size(); idx++) {
				if(padding_values.get(idx) % smallest_value == 0) {
					score += 3;
				}
				else {
					score += 1;
				}
				total_possible_score+= 3;
			}
			
			int score2 = 0;
			
			for(int idx = 1; idx < padding_values.size(); idx++) {
				if(padding_values.get(idx) % smallest_value == 0) {
					score2 += 3;
				}
				else {
					score2 += 1;
				}
				//total_possible_score+= 3;
			}
			
			if(score2 > score) {
				score = score2;
			}
			
		}
		//SCORING 1a - Check if all values have the same difference
		

		//CALCULATE OVERALL SCORE :: 
		log.warn("Padding score :: "+(score/total_possible_score));
		
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
	 * Sort units into buckets by mapping unit type to padding sizes
	 * 
	 * @param padding_set
	 * @return
	 */
	private Map<String, List<Double>> sortSizeUnits(List<String> padding_set) {
		Map<String, List<Double>> sorted_paddings = new HashMap<>();
		//replace all px values with em for values that contain decimals
		
		for(String padding_value : padding_set) {
			if(padding_value == null 
					|| "0".equals(padding_value.trim()) 
					|| padding_value.contains("auto")) {
				continue;
			}
			
			for(String unit : size_units) {
				if(padding_value != null && padding_value.contains(unit)) {
					List<Double> values = new ArrayList<Double>();

					if(sorted_paddings.containsKey(unit)) {
						values = sorted_paddings.get(unit);
					}
					
					String value = cleanSizeUnits(padding_value);
					
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
	private static int findGCD(int number1, int number2) { 
		//base case 
		if(number2 == 0){ 
			return number1; 
		} 
		return findGCD(number2, number1%number2);
	}
}